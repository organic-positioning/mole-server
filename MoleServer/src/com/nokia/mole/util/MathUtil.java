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

package com.nokia.mole.util;

import java.util.Random;

public class MathUtil {

	public static Random random = new Random ();
	
	public static double poisson (double mean) {
		double u = 0.;
		while (u == 0.) {
			u = randPct(); 
		}
		return (-mean) * Math.log(u);
	}
	
	public static int poisson (int mean) {
		return (int) (poisson((double)mean));
	}
	public static long poisson (long mean) {
		return (long) (poisson((double)mean));
	}
	
	public static double randPct () {
		return random.nextDouble();
	}
	
	public static final int byteArrayToInt(byte [] b) {
		return (b[0] << 24)
		+ ((b[1] & 0xFF) << 16)
		+ ((b[2] & 0xFF) << 8)
		+ (b[3] & 0xFF);
	}
	public static final byte[] intToByteArray(int value) {
		return new byte[] {
				(byte)(value >>> 24),
				(byte)(value >>> 16),
				(byte)(value >>> 8),
				(byte)value};
	}

	
}
