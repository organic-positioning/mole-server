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
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.eclipse.jetty.util.log.Log;

public class ProximityResolver {

	public static final double MIN_SIMILARITY = -0.25;
	static Logger log = Logger.getLogger(ProximityResolver.class);
	
	Map<String,LabeledSignature> labeledSignatures = new HashMap<String,LabeledSignature> ();
	
	public Map<String, Double> findNearby(LabeledSignature labeledSig) {
		Map<String, Double> result = new HashMap <String, Double>();

		synchronized (labeledSignatures) {
			if (labeledSignatures.containsKey(labeledSig.getId())) {
				labeledSignatures.remove(labeledSig.getId());
			}

			for (LabeledSignature lS : labeledSignatures.values()) {
				double similarity = Signature.similarity(lS.getSig(), labeledSig.getSig());
				log.debug("name="+lS.getName()+" sim="+similarity);
				if (similarity > MIN_SIMILARITY) {
					result.put(lS.getName(), similarity);
				}
			}

			labeledSignatures.put(labeledSig.getId(), labeledSig);
		}
		log.debug("match count="+result.size());
		return result;
	}

	class CleanExpiredTask extends TimerTask {

		public void run() {
			log.debug("CleanExpiredTask starting");
			
			synchronized (labeledSignatures) {
				Date now = new Date ();
				Iterator<String> it = labeledSignatures.keySet().iterator();
				int count = 0;
				while (it.hasNext()) {
					String name = it.next();
					Date expiration = labeledSignatures.get(name).getExpirationStamp();
					if (expiration.before(now)) {
						it.remove();
						count++;
					}
				}
				if (count > 0) {
					log.debug("CleanExpiredTask removed="+count);
				}
			}
		}
		
	}
	
	public ProximityResolver () {
		CleanExpiredTask cleanExpiredTask = new CleanExpiredTask (); 
		Timer timer = new Timer();
		//timer.scheduleAtFixedRate(cleanExpiredTask, 1000, 1000);
		timer.scheduleAtFixedRate(cleanExpiredTask, 60*1000, 60*1000);
	}
	
}
