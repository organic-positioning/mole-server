/*
 * 
 * Mole - Mobile Organic Localisation Engine
 * Copyright (C) 2010-2011 Nokia Corporation.  All rights reserved.
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

package com.nokia.mole;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nokia.mole.common.Bind;
import com.nokia.mole.common.Posti;
import com.nokia.mole.db.DB;
import com.nokia.mole.db.WhereAmI;
import com.nokia.mole.proximity.LabeledSignature;
import com.nokia.mole.proximity.ProximityResolver;
import com.nokia.mole.util.PathUtil;
 
public class MoleServer extends AbstractHandler
{
    static String version = "v0.6.0";

	
    static Properties properties;
    static Logger log;

    private DB db;
    private ProximityResolver proximityResolver;
    private WhereAmI whereAmI;
    private Gson gson;

    private boolean started = false;
	
    static {

	String log4j_conf = System.getProperty ("moleserver.log4cfg");
	String main_conf = System.getProperty ("moleserver.config");
	System.out.println ("main_conf="+main_conf + " logj_conf="+log4j_conf);

	PropertyConfigurator.configure(log4j_conf);
	log = Logger.getLogger(MoleServer.class);
		
	properties = new Properties ();
	InputStream is = null;
	try {
	    is = new FileInputStream (main_conf);
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	    log.fatal(e);
	    System.exit(-1);
	}
	try {
	    properties.load(is);
	} catch (IOException e) {
	    e.printStackTrace();
	    log.fatal(e);
	    System.exit(-1);
	}
		
    }

    public static int SERVER_PORT = Integer.parseInt
	(MoleServer.getProperty("mole.server_port", "8080"));

    public MoleServer () throws IOException {
	db = new DB();
	proximityResolver = new ProximityResolver();
	whereAmI = new WhereAmI ();
	GsonBuilder gBuilder = new GsonBuilder();
	gBuilder.registerTypeAdapter(Date.class, new DateDeserializer()).create();
	gBuilder.registerTypeAdapter(Date.class, new DateSerializer()).create();
	gson = gBuilder.create();
	log.info ("Started MoleServer "+ "version="+version);

    }

    void accept (HttpServletResponse response, String text) throws IOException {
	response.setStatus(HttpServletResponse.SC_OK);
	response.setContentType("text/text;charset=utf-8");
	response.getWriter().println(text);
    }
	
    public void handle(String target,
		       Request baseRequest,
		       HttpServletRequest request,
		       HttpServletResponse response) 
	throws IOException, ServletException
    {

	// only request currently that is accessed via a browser
	if (request.getRequestURI().equals("/whereami")) {
		
	    log.debug ("whereami start");

	    String user_est_list = whereAmI.listHtml ();
	    log.debug ("user_est_list " + user_est_list);
	    response.setStatus(HttpServletResponse.SC_OK);
	    response.setContentType("text/html;charset=utf-8");

	    response.getWriter().println
		("<html><head><title>Mol&eacute; Where am I?</title></head>\n"+
		 "<body><table border=1>");
	    response.getWriter().println(user_est_list);
	    response.getWriter().println
		("</table></html>");
	    log.debug ("whereami end");
	    baseRequest.setHandled(true);
	    return;

	}


	RequestState state = RequestState.newRequestState (request);

	//log.debug ("uri "+state.URI);

	// sorry.  total hack.
	if (state == null) {
	    if (request == null) {
		log.info ("state and request are null");
	    } else {
		log.info ("state is null request="+ request + " uri="+request.getRequestURI()+
				" src="+request.getRemoteAddr() + ":" +request.getRemotePort());
	    }
	    response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
	    return;
	}

	log.info ("state="+state);
	//log.debug ("state"); 
	//log.debug ("state "+state.URI); 
		
	if (state.URI.equals("/bind") && state.method.equals("POST")) {

	    /*
	      List<AP_Scan> ap_scans = new ArrayList<AP_Scan> ();
	      for (int i = 0; i < 3; i++) {
	      ap_scans.add(AP_Scan.newRandomInstance(new Date()));
	      }
			
	      //BasicBind bb = new BasicBind ("0.1", new Cookie("foo"), new Location("bar"),
	      //		new Location("baz"), new Date(), new Date(), new Date(), "unknown", "unknown", ap_scans);
	      BasicBind bb = new BasicBind ("0.1", ap_scans);
	      String bbJson = gson.toJson(bb);
	      log.debug(bbJson);
	    */


	    boolean valid_bind = false;

            BufferedReader reader = request.getReader();
            
            try {
            	//BasicBind bb2 = gson.fromJson (bbJson, BasicBind.class);
            	//log.debug (bb2);
            	
            	//Location location = gson.fromJson (reader, Location.class);
            	//Cookie bind = gson.fromJson (reader, Cookie.class);
            	//String bind = gson.fromJson(reader, String.class);
            	
            	Bind bind = gson.fromJson (reader, Bind.class);

            	bind.setState (state);
            	log.info ("remote bind "+bind);
            	//log.debug ("location "+location);
            	valid_bind = db.recordBind(bind);

            	
            	valid_bind = true;
            	
            } catch (Exception ex) {
            	log.debug ("ex="+ex);
            	ex.printStackTrace();
            }

            accept (response, "OK");

	    log.info ("bind valid="+valid_bind+ " state="+state);
			
	} else if (state.URI.equals("/getAreas") && state.method.equals("GET")) {


	    String mac = request.getParameter("mac");
	    String mac2 = request.getParameter("mac2");
	    int areas_found = 0;

	    if (mac == null || mac.isEmpty() || mac.length() > 20 ||
		(mac2 != null && !mac2.isEmpty() && mac2.length() > 20)) {
		response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		log.warn("getAreas invalid mac="+mac);

	    } else {

		//List<String> areas = new ArrayList<String> ();
		Set<String> areas = new HashSet<String> ();
		try {
		    db.findArea(mac, areas);
		    if (mac2 != null && !mac2.isEmpty()) {
			db.findArea(mac2, areas);
		    }
					 
		} catch (Exception ex) {
		    log.warn ("findArea exception "+ex);
		    ex.printStackTrace();
		}

		log.debug("getAreas after findArea");
				
		areas_found = areas.size();
		StringBuffer area_buffer = new StringBuffer ();
		for (String area : areas) {
		    area_buffer.append (area+"\n");
		}

		log.debug("sending mac_to_areas response");

		accept (response, new String (area_buffer));
				
		// set cacheable
		response.setHeader("Cache-Control","max-age=60");
				
		//HttpHeaders.CACHE_CONTROL_PUBLIC + ", "
		// HttpHeaders.CACHE_CONTROL_MAXAGE + "60");
		//response.setHeader("Cache-Control", "60");
		//response.setDateHeader(HttpHeaders.EXPIRES, now + 6000);
					
	    }
	    log.info("getAreas mac="+mac+" areas="+areas_found+" state="+state);

	} else if (state.URI.equals("/proximity") && state.method.equals("POST")) {

		
		BufferedReader reader = request.getReader();
        
		String responseStr = "{\"error\":\"invalid input\"}";
        try {
        	LabeledSignature labeledSig = gson.fromJson (reader, LabeledSignature.class);
        	LabeledSignature.initAndValidate (labeledSig);
        	if (labeledSig == null) {
        		log.warn ("null labeledSig");
        		responseStr = "{\"error\":\"invalid input\"}";
        	} else {
        		labeledSig.setId (state.cookie);
        		log.debug ("received labeledSig "+labeledSig);
        		Map<String,Double> labeledSimilarities = proximityResolver.findNearby(labeledSig);
        		responseStr = gson.toJson(labeledSimilarities);
      	      	log.debug("prox resp="+responseStr);
        	}
        	
        } catch (Exception ex) {
        	log.debug ("ex="+ex);
        	ex.printStackTrace();
        	responseStr = "{\"error\":\"server error\"}";
        }

        accept (response, responseStr);
        log.info ("OK proximity request state="+state);
		
		
		
	} else if (state.URI.equals("/posti/pub") && state.method.equals("POST")) {
	    boolean valid_pub = false;

	    /*
	      List<String> sig_list = new ArrayList<String> ();
	      sig_list.add("sig_a");
	      sig_list.add("sig_b");
	      Posti p = new Posti ("here is the text", "the_username", sig_list);
	      String pJson = gson.toJson(p);
	      log.debug(pJson);
	    */
			
	    BufferedReader reader = request.getReader();
        
        try {
        	//BasicBind bb2 = gson.fromJson (bbJson, BasicBind.class);
        	//log.debug (bb2);
        	
        	//Location location = gson.fromJson (reader, Location.class);
        	//Cookie bind = gson.fromJson (reader, Cookie.class);
        	//String bind = gson.fromJson(reader, String.class);
        	
        	Posti posti = gson.fromJson (reader, Posti.class);
	if (posti == null) {
	    log.warn ("null posti");
	} else {
	    posti.setClient (request.getRemoteAddr(),request.getRemotePort());
	    log.debug ("remote posti "+posti);
	    //log.debug ("location "+location);
	    valid_pub = db.recordPost(posti);
	}
        	
        } catch (Exception ex) {
        	log.debug ("ex="+ex);
        	ex.printStackTrace();
        }

        accept (response, "OK");
        log.info ("post valid="+valid_pub+ " state="+state);
		
			
	} else if (state.URI.equals("/posti/sub") && state.method.equals("GET")) {

	    List<Posti> postis = db.getPostis();
	    StringBuffer sb = new StringBuffer ();
	    for (Posti posti : postis) {
		sb.append(gson.toJson(posti, Posti.class)+"\n");
	    }
			
	    accept (response, new String(sb));


	} else if (state.URI.equals("/feedback") && state.method.equals("POST")) {
		    
		   
	    // TODO also record their cookies
	    PathUtil.recordFeedback (request, state);

	    accept (response, "OK");

	} else if (state.URI.equals("/iamhere")) {
		    
	    //log.debug ("iamhere start");
	    String username = request.getParameter("username");
	    log.debug ("iamhere username = "+ username);


	    String estimate = request.getParameter("est");
	    //log.debug ("iamhere est = "+ estimate);

	    whereAmI.add (username, estimate);

	    //log.debug ("iamhere sending response");
			
	    accept (response, "OK");

	    //log.debug ("iamhere end");
			
	} else {
	    log.info ("URI not found state="+state);
	    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	baseRequest.setHandled(true);

	if (!started) {
	    db.start();
	    started = true;
	}

    }
 
    public static void main(String[] args) throws Exception
    {
	System.out.println ("Starting Mole Server version "+version);
		
	//BasicConfigurator.configure();
	Server server = new Server(SERVER_PORT);
		
	//ThreadPool threadPool = new QueuedThreadPool (1);
	//server.setThreadPool(threadPool);
		
	server.setHandler(new MoleServer());
 
	server.start();
		
	System.out.println ("Started MoleServer");
		
	server.join();
    }
    
    public static String getProperty (String key, String default_value) {
	//System.out.println ("get prop");	
		
	if (properties.containsKey(key)) {
	    return properties.getProperty (key);
	} else {
	    return default_value;
	}
    }

    /*
      class BasicBindInstanceCreator implements InstanceCreator<BasicBind> {
      @Override
      public BasicBind createInstance(Type type) {
      log.debug("creator");
      return new BasicBind(new Cookie(), new Location());
      }
      }
    */
	
    /*
      class BasicBindDeserializer implements JsonDeserializer<BasicBind> {
      public BasicBind deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
      log.debug("a");
      //String a = new String(json.getAsJsonPrimitive().getAsString());
      String a = new String(json.getAsString());
      log.debug("b");
      Cookie c = new Cookie(a);
      String b = new String(json.getAsJsonPrimitive().getAsString());
      Location l = new Location(b);
      log.debug("d");
      return new BasicBind (c, l);
      }
      }
    */
	
    /*
      class BasicBindDeserializer implements JsonDeserializer<BasicBind> {
      public BasicBind deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
			
			
			
      log.debug("a");
      //String a = new String(json.getAsJsonPrimitive().getAsString());
      String a = new String(json.getAsString());
      log.debug("b");
      Cookie c = new Cookie(a);
      String b = new String(json.getAsJsonPrimitive().getAsString());
      Location l = new Location(b);
      log.debug("d");
      return new BasicBind (c, l);
      }
      }
    */
	
    class DateDeserializer implements JsonDeserializer<Date> {
    	public Date deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context)
    	    throws JsonParseException {
    	    long sec = json.getAsLong();
    	    //log.debug ("parse got sec="+sec);
    	    return new Date (sec*1000);
    	}
        }
	
    class DateSerializer implements JsonSerializer<Date> {
	@Override
	    public JsonElement serialize(Date date, Type typeOfSrc,
					 JsonSerializationContext context) throws JsonParseException {
	    return new JsonPrimitive(date.getTime()/1000);
	}
    }
	
	/*
    class LabeledSignatureDeserializer implements JsonDeserializer<LabeledSignature> {
    	public LabeledSignature deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context)
    	    throws JsonParseException {
    	    String label = json.getAsString();
    	    json.getAsJsonArray();
    	    return new LabeledSignature (json.getAsJsonPrimitive().getAsString());
    	}
        }
    */
}
