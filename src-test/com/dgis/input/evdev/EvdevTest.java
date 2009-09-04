package com.dgis.input.evdev;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map.Entry;

/*
 * Copyright (C) 2009 Giacomo Ferrari
 * This file is part of evdev-java.
 *  evdev-java is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  evdev-java is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with evdev-java.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Simple program that prints info and all events from a device to STDOUT. 
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

public class EvdevTest {
	
	public static void main(String[] args) throws IOException {
		
		if(args.length == 0) {
			System.out.println("Usage: EvdevTest /dev/input/event*");
			System.exit(1);
		}
		
		String fn = args[0];
		EventDevice dev = new EventDevice(fn);
		int version = dev.getEvdevVersion();
		System.out.printf("Input driver version is %d.%d.%d\n",
				version >>> 16, (version >>> 8) & 0xff, version & 0xff);
		
		System.out.printf("Input device ID: bus 0x%x vendor 0x%x product 0x%x version 0x%x\n",
				dev.getBusID(), dev.getVendorID(), dev.getProductID(), dev.getVersionID());
		System.out.println("Input device name: "+dev.getDeviceName());
		System.out.println("Supported events:");
		
		for(Entry<Integer, List<Integer>> e : dev.getSupportedEvents().entrySet()) {
			System.out.println("Event type "+e.getKey()+" :");
			for(int ev : e.getValue()) {
				System.out.println("\tEvent code "+ev);
				if(e.getKey() == InputEvent.EV_ABS) {
					System.out.println("\t\t"+dev.getAxisParameters(ev));
				}
			}
		}
		
		System.out.println("Hit enter to quit.");
		
		dev.addListener(new InputListener() {
			@Override
			public void event(InputEvent e) {
				System.out.println(e);
			}
		});
		
		//Wait for newline.
		new BufferedReader(new InputStreamReader(System.in)).readLine();
		
		dev.close();
	}
}
