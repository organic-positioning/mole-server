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
import java.util.List;
import java.util.Properties;

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
 
public class MoleWS extends AbstractHandler
{
    static String version = "v0.1.0";
	
    static Properties properties;
    static Logger log;

    private DB db;
    private Gson gson;
   
    static {

    	String log4j_conf = System.getProperty ("moleWS.log4cfg", "config/log.cfg");
    	String main_conf = System.getProperty ("moleWS.config", "config/moleWS.cfg");
    	
    	System.out.println ("main_conf="+main_conf + " logj_conf="+log4j_conf);

    	PropertyConfigurator.configure(log4j_conf);
    	log = Logger.getLogger(MoleWS.class);

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

    public static int SERVER_PORT = Integer.parseInt (MoleWS.getProperty("moleWS.server_port", "8090"));

    boolean useDynamo = false;
    public MoleWS () throws IOException {
    	useDynamo = true;
    	if (useDynamo) {
    	db = new DynamoDB();
    	} else {    	
    		db = MemoryDB.loadDB();
    	}
    	GsonBuilder gBuilder = new GsonBuilder();
    	gBuilder.registerTypeAdapter(Mac.class, new Mac().new MacDeserializer()).create();
    	gson = gBuilder.create();
    	log.info ("Started MoleWS "+ "version="+version);
    }
  
    void accept (HttpServletResponse response, String text) throws IOException {

    }

    void handleBind(HttpServletRequest request, MoleWSResponse response) throws Exception {
    	BufferedReader reader = request.getReader();
    	Bind bind = gson.fromJson (reader, Bind.class);
    	bind.setSource (request.getLocalAddr());
    	boolean ok = db.bind(bind);
    	if (!ok) {
    		response.setStatus("Error: bind failed");
    	} else {
    		response.setStatus("OK");
    	}
    	log.info ("bind ok="+ok+ " bind="+bind);
    }

    void handleQuery(HttpServletRequest request, MoleWSResponse response) throws Exception {
    	BufferedReader reader = request.getReader();
    	Query query = gson.fromJson (reader, Query.class);
    	query.setSource (request.getLocalAddr());
    	List<LocationProbability> result = db.query(query);
    	response.setStatus("OK");
    	response.setQueryResult(result);
    	log.info ("query result="+result);
    }

    void handleRemove(HttpServletRequest request, MoleWSResponse response) throws Exception {
    	BufferedReader reader = request.getReader();
    	Remove remove = gson.fromJson (reader, Remove.class);
    	remove.setSource (request.getLocalAddr());
    	boolean ok = db.remove(remove);
    	if (!ok) {
    		response.setStatus("Error: Location not found");
    	} else {
    		response.setStatus("OK");
    	}
    	log.info ("query ok="+ok+ " remove="+remove);
    }

    public void handle(String target, Request baseRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

    	log.debug ("new request method="+httpRequest.getMethod()+" URI="+httpRequest.getRequestURI()); 
    	boolean dirty = false;
    	
    	try {
        	MoleWSResponse response = new MoleWSResponse("Error");
        	httpResponse.setStatus(HttpServletResponse.SC_OK);
        	
    		if (httpRequest.getRequestURI().equals("/bind") && httpRequest.getMethod().equals("POST")) {
    			handleBind(httpRequest, response);
    			dirty = true;
    		} else if (httpRequest.getRequestURI().equals("/query") && httpRequest.getMethod().equals("POST")) {
    			handleQuery(httpRequest, response);
    		} else if (httpRequest.getRequestURI().equals("/remove") && httpRequest.getMethod().equals("POST")) {
    			handleRemove(httpRequest, response);
    			dirty = true;
    		} else {
    			log.info ("URI not found method="+httpRequest.getMethod()+" URI="+httpRequest.getRequestURI());
    			httpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
    		}

        	httpResponse.setContentType("application/json;charset=utf-8");
    		String json = gson.toJson(response);
        	httpResponse.getWriter().println(json);
    		
    	} catch (Exception ex) {
    		log.debug ("ex="+ex);
    		ex.printStackTrace();
    	}	

    	baseRequest.setHandled(true);

    	// simple save of the db each time
    	if (dirty) {
    		if (useDynamo) {
    			MemoryDB.saveDB(db);
    		}	
    	}
    }
 
    public static void main(String[] args) throws Exception {
    	System.out.println ("Starting MoleWS version "+version);
    	//BasicConfigurator.configure();
    	Server server = new Server(SERVER_PORT);
    	//ThreadPool threadPool = new QueuedThreadPool (1);
    	//server.setThreadPool(threadPool);
    	server.setHandler(new MoleWS());
    	server.start();
    	System.out.println ("Started MoleWS");
    	server.join();
    }

    public static String getProperty (String key) {
    	return getProperty(key, null);
    }
    
    public static String getProperty (String key, String default_value) {

    	if (properties.containsKey(key)) {
    		return properties.getProperty (key);
    	} else {
    		return default_value;
    	}
    }
}
