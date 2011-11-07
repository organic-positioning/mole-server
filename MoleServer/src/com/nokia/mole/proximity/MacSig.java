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

import java.util.Map;

import org.eclipse.jetty.util.log.Log;

public class MacSig {
	
	private static final int MIN_RSSI = 0;
	private static final int MAX_RSSI = 100;
	
	final public double weight;
	final public Map<Integer,Double> histogram;
	
	transient int min = MAX_RSSI;
	transient int max = MIN_RSSI;
	transient double hist [] = new double [MAX_RSSI-MIN_RSSI];
	
	static double overlap (MacSig histA, MacSig histB) {
		int min = histA.min > histB.min ? histA.min : histB.min;
		int max = histB.max < histB.max ? histA.max : histB.max;
		double sum = 0.;
		for (int i = min; i < max; ++i) {
			double area = histA.hist[i];
			if (histA.hist[i] > histB.hist[i]) {
				area = histB.hist[i];
			}
			sum += area;
		}
		return sum;
	}
	
	private MacSig () {
		weight = 0;
		histogram = null;
	}
			
	public MacSig (double _weight, Map<Integer,Double> _histogramMap) {
		weight = _weight;
		histogram = _histogramMap;
	}

	public static void initAndValidate(MacSig macsig) {
		double sum = 0.;
		int count = 0;
		for (Map.Entry<Integer, Double> key2value : macsig.histogram.entrySet()) {
			int key = key2value.getKey();
			double value = key2value.getValue();
			if (value > 0.) {
				if (key < MAX_RSSI && key > MIN_RSSI) {
					macsig.hist[key] = value;
					sum += value;
					count++;
					if (key < macsig.min) {
						macsig.min = key;
					}
					if (key > macsig.max){
						macsig.max = key;
					}
				}
			}
		}

		if (sum > 1.1 || count == 0) {
			Log.info("invalid macsig sum="+sum+" count="+count);
			macsig = null;
		}

	}
	
}
