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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Signature {

	static Logger log = Logger.getLogger(Signature.class);
	public Map<String,MacSig> macsigs = new HashMap<String,MacSig> ();
	final static double penalty = 4.;
    public static final Pattern MacPattern = Pattern.compile("^[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]$");
	
	static double similarity (Signature sigA, Signature sigB) {
		double score = 0.;
		int hitCount = 0;
		log.debug ("start similarity sigA size="+sigA.macsigs.size()+ " sigB size="+sigB.macsigs.size());
		for (Map.Entry<String,MacSig> subsigA : sigA.macsigs.entrySet()) {
			String mac = subsigA.getKey();
			MacSig macSigA = subsigA.getValue();
			if (sigB.macsigs.containsKey(mac)) {
				MacSig macSigB = sigB.macsigs.get(mac);
				double overlap = MacSig.overlap (macSigA, macSigB);
				// increment by the area under the curve (=overlap) normalized by the mean weight
				score += overlap * ((macSigA.weight + macSigB.weight) / 2.);
				log.debug ("overlap "+overlap+ " score"+score);
				hitCount++;
			} else {
				score -= macSigA.weight / penalty;
				log.debug ("missing A score="+score+ " weight="+macSigA.weight);
			}
		}
		
		for (Map.Entry<String,MacSig> subsigB : sigB.macsigs.entrySet()) {
			String mac = subsigB.getKey();
			MacSig macSigB = subsigB.getValue();
			if (!sigA.macsigs.containsKey(mac)) {
				score -= macSigB.weight / penalty;
				log.debug ("missing B score="+score+ " weight="+macSigB.weight);
			}
		}
		
		return score;
	}
	
	public Signature () {
	}
	
	public void setTestSignature () {
		Map<Integer,Double> histA = new HashMap<Integer,Double> ();
		histA.put(30, 0.73);
		histA.put(31, 0.24);
		histA.put(32, 0.05);
		MacSig wSigA = new MacSig (0.6, histA);
		macsigs.put("macA", wSigA);
		//sigs.put("macB", wSigB);
	}

	public static void initAndValidate(Signature sig) {
		int count = 0;
		Map<String,MacSig> macsigsNorm = new HashMap<String,MacSig> ();
		log.debug("initAndValidate size="+sig.macsigs.size());
		
		for (Map.Entry<String, MacSig> mac2macsig : sig.macsigs.entrySet()) {
		
			String mac = mac2macsig.getKey();
			MacSig macsig = mac2macsig.getValue();
			
			// validate mac
			String macNorm = mac.toLowerCase();
			if (macNorm.length() > 20) {
			    macNorm = macNorm.substring(0, 20);
			}
			
			Matcher matcher = MacPattern.matcher (macNorm);
			if (!matcher.find()) {
			    log.warn ("invalid mac "+macNorm);
			    sig = null;
			    return;
			}
			
			// validate macsig
			MacSig.initAndValidate(macsig);
			if (macsig == null) {
				sig = null;
				return;
			}

			macsigsNorm.put(macNorm,macsig);
			
			count++;
			//log.debug("mac="+mac);
		}
		if (count == 0) {
			sig = null;
			return;
		}
		log.debug("initAndValidate macCount="+count);
		sig.macsigs = macsigsNorm;
	}
	
}
