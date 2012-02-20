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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class Fingerprint implements Serializable {

	private static final long serialVersionUID = -7456500317937021899L;
	static Logger log = Logger.getLogger(Fingerprint.class);
	final static double penalty = 4.;
	final static double normalizer = (1./penalty)*2.;
	
	Map<Mac,Histogram> mac2histogram = new HashMap<Mac,Histogram>();
		
	protected Fingerprint() {
	}

	public Fingerprint(List<Scan> scans) {
		int readingCount = 0;
		for (Scan scan : scans) {
			for (Reading reading : scan.readings) {
				Mac mac = reading.bssid;
				int level = reading.level;
				Histogram histogram = mac2histogram.get(mac);
				if (histogram == null) {
					histogram = new Histogram();
					mac2histogram.put(mac, histogram);
				}
				histogram.addValue(level);
				readingCount++;
			}
		}
		if (readingCount > 0) {
			for (Histogram histogram : mac2histogram.values()) {
				histogram.normalizeAndWeight(readingCount);
			}
		} else {
			log.warn("histogram has no readings");
		}
	}

	public Set<Mac> getMacs() {
		return mac2histogram.keySet();
	}

	public int size() {
		return mac2histogram.size();
	}
	
	static double similarity (Fingerprint fpA, Fingerprint fpB) {
		double score = 0.;
		int hitCount = 0;
		log.debug ("start similarity fpA size="+fpA.size()+ " fpB size="+fpB.size());
		for (Map.Entry<Mac,Histogram> macFpA : fpA.mac2histogram.entrySet()) {
			Mac mac = macFpA.getKey();
			Histogram histA = macFpA.getValue();
			if (fpB.mac2histogram.containsKey(mac)) {
				Histogram histB = fpB.mac2histogram.get(mac);
				double overlap = Histogram.overlap (histA, histB);
				// increment by the area under the curve (=overlap) normalized by the mean weight
				score += overlap * ((histA.weight + histB.weight) / 2.);
				log.debug ("mac="+mac+" overlap="+overlap+ " score="+score + " histA="+histA+" histB="+histB);
				hitCount++;
			} else {
				score -= histA.weight / penalty;
				log.debug ("missing A mac="+mac+" score="+score+ " weight="+histA.weight);
			}
		}
		
		for (Map.Entry<Mac,Histogram> macFpB : fpB.mac2histogram.entrySet()) {
			Mac mac = macFpB.getKey();
			Histogram histB = macFpB.getValue();
			if (!fpA.mac2histogram.containsKey(mac)) {
				score -= histB.weight / penalty;
				log.debug ("missing B mac="+mac+" score="+score+ " weight="+histB.weight);
			}
		}

		if (hitCount > 0) {
			// normalize from [1...0...2xMaxPenalty] -> [1...0]
			double normalized = (score + normalizer) / (1. + normalizer);
			log.debug("score="+score+" normalized="+normalized);
			return normalized;
		} else {
			return 0;
		}
	}
	
}
