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

package com.nokia.mole;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class RequestState {

	static Logger log = Logger.getLogger(RequestState.class);

	public final String URI;
	public final String method;
	public final String cookie;
	public final String session;
	public final String agent;
	public final String client_address;
	public final int client_port;
	public final int version;

	public static RequestState newRequestState(HttpServletRequest request) {

		String URI = request.getRequestURI();
		String method = request.getMethod();

		String client_address = request.getRemoteAddr();
		int client_port = request.getRemotePort();

		String agent = request.getHeader("user-agent");

		String cookie = null;
		String session = null;
		int version = -1;

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			log.debug("cookie count=" + cookies.length);
			for (int c = 0; c < cookies.length; c++) {
				log.debug("cookie name=" + cookies[c].getName() + " value="
						+ cookies[c].getValue());
				if (cookies[c].getName().equals("cookie")) {
					cookie = cookies[c].getValue();
				} else if (cookies[c].getName().equals("mole_version")) {
					try {
						version = Integer.parseInt(cookies[c].getValue());
					} catch (NumberFormatException ex) {
						log.warn("failed to parse version cookie "
								+ cookies[c].getValue());
						return null;
					}

				} else if (cookies[c].getName().equals("session")) {
					session = cookies[c].getValue();
				}
			}
		} else {
			log.debug("cookies is null");
			return null;
		}

		if (cookie == null || session == null || version < 0) {
			log.info("required cookies not found"+
				 " cookie="+cookie+
				 " session="+session+
				 " version="+version);
			return null;
		}

		return new RequestState(URI, method, cookie, session, agent,
				client_address, client_port, version);

	}

	private RequestState(String uRI, String method, String cookie,
			String session, String agent, String clientAddress, int clientPort,
			int version) {
		URI = uRI;
		this.method = method;
		this.cookie = cookie;
		this.session = session;
		this.agent = agent;
		client_address = clientAddress;
		client_port = clientPort;
		this.version = version;
	}

	public String toString() {
		return client_address + ":" + client_port + " [" + agent +"]"
				+ " " + URI + " " + method + " c="+cookie+
		    " s="+session+" v="+version;

	}

}
