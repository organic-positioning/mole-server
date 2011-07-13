/*
 * 
 * Mole - Mobile Organic Localisation Engine
 * Copyright (C) 2010 Nokia Corporation.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.nokia.mole.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.nokia.mole.MoleServer;
import com.nokia.mole.common.AP_Reading;
import com.nokia.mole.common.AP_Scan;
import com.nokia.mole.common.Bind;
import com.nokia.mole.common.Location;
import com.nokia.mole.common.Posti;


public class DB {

	public static double PCT_MEMORY_FLUSH = Double.parseDouble
	(MoleServer.getProperty("mole.pct_memory_flush", "0.025"));

	public static long MAX_ELAPSED_FLUSH = Long.parseLong
	(MoleServer.getProperty("mole.max_elapsed_flush", "10000"));

	//public static long CACHE_CLEAN_PERIOD = Long.parseLong
	//(OILServer.getProperty("oil.cache_clean_period", "30000"));

	public static long AREA_CACHE_EXPIRATION_MSEC = Long.parseLong
	(MoleServer.getProperty("mole.area_cache_exp_msec", "30000"));

	
	public static String DB_URL = MoleServer.getProperty("mole.dsn", "jdbc:mysql://localhost:3306/mole");
	public static String DB_USER = MoleServer.getProperty("mole.db_user","moleuser");
	public static String DB_PASS = MoleServer.getProperty("mole.db_pw","molepw");	
	
	static Logger log = Logger.getLogger(DB.class);

	Connection connection;
	List<Bind> binds = new ArrayList<Bind> ();
	List<Bind> binds_shadow = new ArrayList<Bind> ();
	Map<String,AreaDesc> bssid2area_list = new HashMap<String,AreaDesc> ();
	Map<String,Integer> location2id = new HashMap<String,Integer> ();
	
	long max_memory;
	private Date flush_time; 
	private Date flush_area_cache_time;
	
	PreparedStatement loc_query_stmt;
	PreparedStatement bssid_to_loc_query_stmt;
	PreparedStatement loc_insert_stmt;
	PreparedStatement bind_insert_stmt;
	PreparedStatement ap_reading_insert_stmt;

	private boolean dirty_binds = false;
	private long maintenanceCount = 0;
	private long flushCount = 0;

	private Date last_db_ping= new Date ();

	private Timer timer;

	private MaintenanceTask maintenance_task;
	private BindXmitTask bind_xmit_task;

	// DB Driver
	
	/*
	public static void main(String[] args) throws Exception {
		log.info("Starting DB Driver: findArea");
		DB db = new DB ();
		
		db.flushAreaCache();
		
		List<String> campus_areas = db.findArea ("FF");
		for (String campus_area : campus_areas) {
			log.info (campus_area);
		}
	
		Thread.sleep(3000);
		
		db.flushAreaCache();

		campus_areas = db.findArea ("FF");
		for (String campus_area : campus_areas) {
			log.info (campus_area);
		}

		
	}
	*/
	/*
	public static void main2(String[] args) throws Exception {
		log.info ("Starting DB Driver");
		DB db = new DB ();

		boolean res = db.connect();
		if (!res) {
			log.error("DB not connected");
		}
		
		long start_time = System.currentTimeMillis();


		List<Bind> binds = new ArrayList<Bind> ();

		for (int i = 0; i < 1000; i++) {
			Bind b = Bind.newRandomInstance();
			binds.add(b);
		}
		System.out.println("creating binds took "+(System.currentTimeMillis() - start_time));

		for (Bind b : binds) {
			db.recordBind(b);
			db.maintenance ();
		}


		BindwExtras b;
		for (int i = 0; i < 10; i++) {
			b = BindwExtras.newRandomInstance();
			//db.recordBind(b);
			db.maintenance ();
		}

		log.info("recording binds took "+ (System.currentTimeMillis() - start_time));

		//Location loc = new Location ("mit", "32", "268");

		try {
			//db.insertBind(b);
			//System.out.println ("inserted bind");
			//int id = db.getLocationID (loc);
			//System.out.println ("location id ="+id);

			db.flushBinds();
			log.info("flushing binds took "+(System.currentTimeMillis() - start_time));

		} catch (SQLException e) {
			// 
			e.printStackTrace();
		}

	}
	*/
	public DB () {

	    log.info ("Starting DB "+DB_URL+ " user="+DB_USER);
		
		connect ();
		
		flush_time = new Date ();
		flush_area_cache_time = new Date ();
		
		max_memory = Runtime.getRuntime().maxMemory();
		log.info("max_memory = "+max_memory);


		bind_xmit_task = new BindXmitTask();
		maintenance_task = new MaintenanceTask();
		timer = new Timer();

		
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	log.info("DB shutdown");
            	try {
            		// note that this does not actually flush what is in memory
					connection.close();
				} catch (SQLException e) {
					log.warn("Problem closing db connection: "+e);
					e.printStackTrace();
				}
            }
        });;
		
	}

	public void start () {
		timer.scheduleAtFixedRate(maintenance_task, 15*1000, 15*1000);
		timer.scheduleAtFixedRate(bind_xmit_task, 5*1000, 5*1000);
	}
	
	private boolean connect () {
		
		// TODO use jetty session pooling
		// http://docs.codehaus.org/display/JETTY/Session+Clustering+with+a+Database
		// http://dev.mysql.com/tech-resources/articles/connection_pooling_with_connectorj.html
	    
	    //String url = DB_URL + "?wait_timeout=2764800&interactive_timeout=2764800";
	    String url = DB_URL + "?autoReconnect=true";
	    

	    log.info("db url "+url);
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
			log.warn ("exception "+e1);
			return false;
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
			log.warn ("exception "+e1);
			return false;
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			log.warn ("exception "+e1);
			return false;
		}

		try {
			
		    connection = DriverManager.getConnection (url, DB_USER, DB_PASS);


			if (connection.isClosed()) {
				return false;
			}


		} catch (SQLException e) {
			e.printStackTrace();
			log.warn ("exception "+e);
			return false;
		}
		last_db_ping= new Date ();
		return true;
		

	}

	private boolean insertBind (Bind bind) throws SQLException {
		
		// note that the bind's validity has already been checked
		int location_id = getLocationID (bind.location);
		

		if (bind_insert_stmt == null) {
			bind_insert_stmt = connection.prepareStatement
			("insert into binds (start_stamp, bind_stamp, end_stamp, location_id, est_location_id,"+
			"cookie, device_model, wifi_model, client_ip, client_port, client_version)"+
			 " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
			 Statement.RETURN_GENERATED_KEYS);
		}

		/*
		log.debug("start "+bind.start_stamp+ " end "+bind.end_stamp +
			  " bind "+bind.bind_stamp+ " location "+bind.location_id+
			  " est_loc "+bind.est_location +
			  " device_model "+bind.device_model+ " wifi "+bind.wifi_model+
			  " client "+bind.client_ip+":"+bind.client_port+
			  " version "+bind.version);
		*/
		log.debug (bind.toString());

		bind_insert_stmt.setTimestamp(1, new Timestamp (bind.start_stamp.getTime()));
		bind_insert_stmt.setTimestamp(2, new Timestamp (bind.bind_stamp.getTime()));
		bind_insert_stmt.setTimestamp(3, new Timestamp (bind.end_stamp.getTime()));
		bind_insert_stmt.setInt(4, location_id);
		
		log.debug ("bind location_id="+location_id);
		if (bind.est_location.isValid()) {
			int est_location_id = getLocationID (bind.est_location);
			bind_insert_stmt.setInt(5, est_location_id);
			log.debug ("bind setInt est_location_id="+est_location_id);
		} else {
			bind_insert_stmt.setNull(5, java.sql.Types.INTEGER);
			log.debug ("bind set est_location_id=NULL");
		}
		bind_insert_stmt.setNString(6, bind.cookie.cookie);
		bind_insert_stmt.setNString(7, bind.device_model);
		bind_insert_stmt.setNString(8, bind.wifi_model);
		bind_insert_stmt.setNString (9, bind.client_ip);
		bind_insert_stmt.setInt (10, bind.client_port);
		bind_insert_stmt.setInt (11, bind.version);
		
		bind_insert_stmt.executeUpdate();
		
		ResultSet bindInsertRes = bind_insert_stmt.getGeneratedKeys();


		boolean first = bindInsertRes.first();
		if (!first) {
			log.warn("Error inserting bind");
			return false;
		}
		int bind_id = bindInsertRes.getInt(1);
		log.debug ("inserted bind_id= "+bind_id);
		bindInsertRes.close();

		if (ap_reading_insert_stmt == null) {

			ap_reading_insert_stmt = connection.prepareStatement
			("insert into ap_readings (bind_id, location_id, stamp, bssid, ssid, frequency, level)"+
			" values (?, ?, ?, ?, ?, ?, ?)");
		}

		log.debug ("inserting scans "+bind.ap_scans.size());		
		
		for (AP_Scan ap_scan : bind.ap_scans) {
			Timestamp stamp = new Timestamp (ap_scan.stamp.getTime());
						
			log.debug ("inserting readings "+ap_scan.readings.size());
			
			for (AP_Reading ap_reading : ap_scan.readings) {
				log.debug ("inserting reading "+ap_reading);
				ap_reading_insert_stmt.setInt (1, bind_id);
				ap_reading_insert_stmt.setInt(2, location_id);
				ap_reading_insert_stmt.setTimestamp(3, stamp); 
				ap_reading_insert_stmt.setNString (4, ap_reading.getBssid());
				ap_reading_insert_stmt.setNString (5, ap_reading.getSsid());
				ap_reading_insert_stmt.setInt (6, ap_reading.getFrequency());
				ap_reading_insert_stmt.setInt (7, ap_reading.getLevel());
				ap_reading_insert_stmt.executeUpdate();
			}

		}

		log.debug ("inserted readings for bind_id= "+bind_id+ "readings="+bind.ap_scans.size());
		
		synchronized (last_db_ping) {
			last_db_ping= new Date ();
		}
		
		return true;
	}



	private int getLocationID (Location location) throws SQLException {

		int location_id = 0;

		synchronized (location2id) {
			if (location2id.containsKey(location.full_space_name)) {
				return location2id.get(location.full_space_name);
			}
		}
		
		if (loc_query_stmt == null) {
			loc_query_stmt = connection.prepareStatement
			("select id from locations where country=? and region=? and city=? and area=? and name=?");
		}

		loc_query_stmt.setNString(1, location.getCountry());
		loc_query_stmt.setNString(2, location.getRegion());
		loc_query_stmt.setNString(3, location.getCity());
		loc_query_stmt.setNString(4, location.getArea());
		loc_query_stmt.setNString(5, location.getSpace());

		ResultSet loc_res = loc_query_stmt.executeQuery();
		boolean exists = loc_res.first ();

		if (exists) {
			location_id = loc_res.getInt(1);

		} else {

			if (loc_insert_stmt == null) {
				loc_insert_stmt = connection.prepareStatement
				    ("insert into locations (country, region, city, area, name) values (?, ?, ?, ?, ?)",
				     Statement.RETURN_GENERATED_KEYS);
			}

			loc_insert_stmt.setNString(1, location.getCountry());
			loc_insert_stmt.setNString(2, location.getRegion());
			loc_insert_stmt.setNString(3, location.getCity());
			loc_insert_stmt.setNString(4, location.getArea());
			loc_insert_stmt.setNString(5, location.getSpace());

			loc_insert_stmt.executeUpdate();

			ResultSet locInsertRes = loc_insert_stmt.getGeneratedKeys();
			locInsertRes.first();

			location_id = locInsertRes.getInt(1);
			locInsertRes.close();
		}

		loc_res.close();

		synchronized (location2id) {
			if (!location2id.containsKey(location.full_space_name)) {
				location2id.put(location.full_space_name, location_id);
			}
		}

		synchronized (last_db_ping) {
			last_db_ping= new Date ();
		}

		
		return location_id;

	}

	public boolean recordBind(Bind bind) throws Exception {
		// need to copy the bind out of the msg
		//log.debug ("bind = "+bind);
		//Bind bind_copy = Bind.copy(bind); 
		//log.debug ("bind_copy = "+bind_copy);
		//binds.add(bind_copy);

		if (bind.validate()) {

			synchronized (binds) {
				binds.add(bind);
				dirty_binds = true;
			}
			return true;
		} else {
			log.info("rejecting bind="+bind);
			return false;
		}
	}

	// periodically flush the binds to the actual DB
	// and do other cleanup

	class BindXmitTask extends TimerTask {

		public void run() {

			synchronized (binds) {
				if (!dirty_binds) {
					return;
				}
				dirty_binds = false;
			}
			
			try {
				flushBinds();
			} catch (SQLException e) {
				log.warn("Problem flushing binds: "+e);
				e.printStackTrace();
			}
		}
		
	}
	
	class MaintenanceTask extends TimerTask {

		public void run() {

			maintenanceCount++;
			long free_memory = Runtime.getRuntime().freeMemory();
			double pct_free_memory = free_memory / (double)max_memory;

			long current_time = new Date ().getTime(); 
			long time_diff = current_time - flush_time.getTime();

			double flushPct = flushCount / (double) maintenanceCount;

			log.debug ("pct_free "+pct_free_memory+ " free_memory="+free_memory+
					" elapsed="+time_diff + " flushPct="+flushPct);

			//if ((pct_free_memory < PCT_MEMORY_FLUSH) || (time_diff > MAX_ELAPSED_FLUSH)) {
			//if (maintenanceCount % 1000 == 0) {			

			/*
			try {

				flushBinds();
			} catch (SQLException e) {
				log.warn("Problem flushing binds: "+e);
				e.printStackTrace();
			}
			*/

			//long cache_time_diff = (new Date ()).getTime() - flush_area_cache_time.getTime();
			//if (cache_time_diff > CACHE_CLEAN_PERIOD) {

			flushAreaCache ();
			//}

			// if none of that caused a connection to the db 
			// and we haven't talked to the db in a long time,
			// just ping it to keep the connection up.
			// note that this only helps each thread's connection individually
			synchronized (last_db_ping) {
				if (current_time - last_db_ping.getTime() > (10*60*1000)) {
					try {
						fillAreaCache ("MaintenanceTask");
					} catch (Exception e) {
						log.warn("Ex pinging db in MaintenanceTask: "+e);
						e.printStackTrace();
					}				
				}
			}
			
		}

	}

	private void flushAreaCache () {

		log.debug ("START flushAreaCache entries="+bssid2area_list.size());

		synchronized (bssid2area_list) {

			Map<String,AreaDesc> _bssid2area_list = new HashMap<String,AreaDesc> ();
			Date current_time = new Date ();
			for (Map.Entry<String, AreaDesc> entry : bssid2area_list.entrySet()) {
				if (current_time.getTime() - entry.getValue().create_stamp.getTime() < AREA_CACHE_EXPIRATION_MSEC) {
					_bssid2area_list.put(entry.getKey(), entry.getValue());
				}
			}

			bssid2area_list = _bssid2area_list;
		}

		flush_area_cache_time = new Date ();


		log.debug ("END flushAreaCache entries="+bssid2area_list.size());

	}

	private void flushBinds() throws SQLException {


		log.debug ("start flush binds "+binds.size());		

		// copy (pointers to) binds to new structure
		// clear out the existing structure
		// release the lock
		// send the new structure to the db

		
		long pre_free_memory = Runtime.getRuntime().freeMemory();

		synchronized (binds) {
			synchronized (binds_shadow) {
				for (Bind bind : binds) {
					binds_shadow.add(bind);
				}
				binds.clear();
			}
 		}

		long post_free_memory = Runtime.getRuntime().freeMemory();

		synchronized (binds_shadow) {
			for (Bind bind : binds_shadow) {
				try {
					// Without this commit barrier, the builder can read the bind from the db
					// before it is all there -- i.e., some scans are missing
					// producing a broken signature.
					// This has been observed -- it's not theoretical.
					
					connection.setAutoCommit(false);
					insertBind (bind);
					connection.commit();
					connection.setAutoCommit(true);

				} catch (MySQLIntegrityConstraintViolationException ex) {
					// unfortunately the generic SQLIntegrityConstraintViolationException
					// does not catch this
					log.fatal("failed to insert bind="+bind+" mySQLerror="+ex);
					System.exit(-1);
				}

			}
			binds_shadow.clear();
		}

		log.debug ("memory used clearing binds, before="+pre_free_memory+" after="+post_free_memory);

		//long post_gc_memory = Runtime.getRuntime().freeMemory();
		//log.debug ("after gc="+post_gc_memory);

		flush_time = new Date ();
		flushCount++;
		log.debug("flushed binds "+flushCount);

	}

	class AreaDesc {
		public final Date create_stamp;
		public final List<String> areas;
		
		public AreaDesc (List<String> _areas) {
			create_stamp = new Date ();
			areas = _areas;
		}
		
	}

	void fillAreaCache (String bssid) throws Exception {

		if (bssid_to_loc_query_stmt == null) {

			log.debug("creating prepared statement");
			
			bssid_to_loc_query_stmt = connection.prepareStatement
				("select country, region, city, area from location_ap_stat, locations where bssid=? "+
				 "and location_ap_stat.location_id=locations.id and location_ap_stat.is_active=1");

		}

		bssid_to_loc_query_stmt.setNString(1, bssid);

		log.debug("executing prepared statement");

		ResultSet loc_id_res = bssid_to_loc_query_stmt.executeQuery();
		synchronized (last_db_ping) {
			last_db_ping= new Date ();
		}
		
		log.debug("executed prepared statement");

		List<String> fq_areas = new ArrayList<String> ();
		
		int campus_area_count = 0;
		while (loc_id_res.next()) {
			log.debug("loop through results "+campus_area_count);

			String country  = loc_id_res.getString (1);
			String region  = loc_id_res.getString (2);
			String city = loc_id_res.getString (3);
			String area = loc_id_res.getString (4);
										
			String fq_area = new String (country+"/"+region+"/"+city+"/"+area);
			if (country.length() > 0 && region.length() > 0 && city.length() > 0 && area.length() > 0) {
				fq_areas.add(fq_area);
				
				log.debug("added area "+fq_area);
				
			} else {
				log.warn("received invalid area from db="+fq_area);
			}

			campus_area_count++;
		}

		synchronized (bssid2area_list) {
		
			// insert into cache
			if (!bssid2area_list.containsKey(bssid)) {
				AreaDesc _desc = new AreaDesc (fq_areas); 
				bssid2area_list.put(bssid, _desc);
			}

		}
		
		log.info("bssid="+bssid+ " added campus_area="+campus_area_count);
				
		loc_id_res.close();

	}

	public void findArea(String bssid, List<String> areas) throws Exception {

		log.debug("bssid="+bssid);

		// concat bssid to 20 characters (=column width)
		if (bssid.length() > 20) {
			bssid = bssid.substring(0, 20);
			log.warn("concat bssid="+bssid);
		}

		boolean found = false;
		
		synchronized (bssid2area_list) {
			found = bssid2area_list.containsKey(bssid);
		}
		
		if (!found) {
			// load into cache if not found there
			fillAreaCache (bssid);
		}

		synchronized (bssid2area_list) {
 
			// happens if attempt to fill cache did not find any matching locations
			if (!bssid2area_list.containsKey(bssid)) {
				return;
			}

			// return from the cache
			for (String area : bssid2area_list.get (bssid).areas) { 
				areas.add(area);
			}
		}
	}

	List<Posti> postis = new ArrayList<Posti> ();
	public boolean recordPost(Posti posti) {
		postis.add(posti);
		return true;
	}
	
	public List<Posti> getPostis () {
		return postis;
	}

}
