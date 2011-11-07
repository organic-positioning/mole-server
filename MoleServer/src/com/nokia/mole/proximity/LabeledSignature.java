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

package com.nokia.mole.proximity;

import java.io.Serializable;
import java.util.Date;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LabeledSignature implements Serializable{
	static Logger log = Logger.getLogger(LabeledSignature.class);
	static final long serialVersionUID = 0L;

	// 10 minutes
	private static final long EXPIRATION_MS = 1000*5;
	//private static final long EXPIRATION_MS = 1000*60*10;
	
	private String name;
	private Signature sig;
	private Date expirationStamp;
	private String id;
	
	
	public LabeledSignature(String name, Signature sig, Date expirationStamp) {
		this.name = name;
		this.sig = sig;
		this.expirationStamp = expirationStamp;
	}

	public LabeledSignature() {
		this.name = null;
		this.sig = null;
		
	}

	public String getName() {
		return name;
	}

	public Signature getSig() {
		return sig;
	}

	public Date getExpirationStamp() {
		return expirationStamp;
	}

	public void setId(String _id) {
		id = _id;
	}

	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return "LabeledSignature [expirationStamp=" + expirationStamp
				+ ", name=" + name + ", sig=" + sig + "]";
	}
	
	public static void main(String[] args) throws Exception
	{
		System.out.println ("Testing Signatures");
		GsonBuilder gBuilder = new GsonBuilder();
		Gson gson = gBuilder.create();

		Signature sig = new Signature ();
		sig.setTestSignature();
		LabeledSignature lSig = new LabeledSignature ("foo", sig, new Date());
		String json = gson.toJson(lSig);
		System.out.println (json);

	}

	public static void initAndValidate(LabeledSignature labeledSig) {
		Date date = new Date ();
		date.setTime(date.getTime() + EXPIRATION_MS);
		labeledSig.expirationStamp = date;
		Signature.initAndValidate (labeledSig.sig);
		if (labeledSig.sig == null) {
			log.debug("rejected labeledSig="+labeledSig.getName());
			labeledSig = null;
		} else {
			log.debug("validated labeledSig="+labeledSig.getName());
		}
		
	}
}



	
	