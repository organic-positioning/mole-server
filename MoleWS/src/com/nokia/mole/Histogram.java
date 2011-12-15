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

import org.apache.log4j.Logger;

public class Histogram implements Serializable {

	private static final long serialVersionUID = -2480404326744496313L;
	static Logger log = Logger.getLogger(Histogram.class);
		
	private static final int MIN_RSSI = 0;
	private static final int MAX_RSSI = 100;
	
	int readingCount;
	double weight;
	int min = MAX_RSSI;
	int max = MIN_RSSI;
	double histogram [] = new double [MAX_RSSI-MIN_RSSI];
	
	static double overlap (Histogram histA, Histogram histB) {
		int min = histA.min > histB.min ? histA.min : histB.min;
		int max = histB.max < histB.max ? histA.max : histB.max;
		double sum = 0.;
		for (int i = min; i < max; ++i) {
			double area = histA.histogram[i];
			if (histA.histogram[i] > histB.histogram[i]) {
				area = histB.histogram[i];
			}
			sum += area;
		}
		return sum;
	}
			
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = min; i < max; ++i) {
			if (histogram[i] > 0) {
				sb.append(i+"="+histogram[i]+" ");
			}
		}
		return "Histogram [histogram=[" + sb + "], max="
				+ max + ", min=" + min + ", readingCount=" + readingCount
				+ ", weight=" + weight + "]";
	}

	public Histogram () { }

	public void addValue(int v) {
		if (v > MIN_RSSI + 5 && v < MAX_RSSI - 5) {
			// min is inclusive, max is exclusive
			if (v-4 < min) {
				min = v-4;
			}
			if (v+5 > max) {
				max = v+5;
			}
			// add kernel centered around index v
			histogram[v] += 0.2042;
			histogram[v-1] += 0.1802;
			histogram[v+1] += 0.1802;
			histogram[v-2] += 0.1238;
			histogram[v+2] += 0.1238;
			histogram[v-3] += 0.0663;
			histogram[v+3] += 0.0663;
			histogram[v-4] += 0.0276;
			histogram[v+4] += 0.0276;

			readingCount++;
		} else {
			log.warn("addValue out of bounds v="+v);
		}
	}

	public void normalizeAndWeight(int totalReadingCount) {
		// normalize by number of readings
		for (int i = min; i < max; i++) {
			histogram[i] /= (double)readingCount;
		}
		weight = (double)readingCount / (double)totalReadingCount;
	}
}
