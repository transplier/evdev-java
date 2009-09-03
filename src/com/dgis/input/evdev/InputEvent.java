package com.dgis.input.evdev;

import java.io.IOException;
import java.nio.ShortBuffer;

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
 * Analog to the input_event struct in linux/input.h.  
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

public class InputEvent {
	public static final int STRUCT_SIZE_BYTES = 24;
	public long time_sec;
	public long time_msec;
	public /*__u16*/ short type;
	public /*__u16*/ short code;
	public /*__s32*/ int value;
	
	public static InputEvent parse(ShortBuffer shortBuffer) throws IOException {
		InputEvent e = new InputEvent();
		short a,b,c,d;
		a=shortBuffer.get();
		b=shortBuffer.get();
		c=shortBuffer.get();
		d=shortBuffer.get();
		e.time_sec = (d<<48) | (c<<32) | (b<<16) | a;
		a=shortBuffer.get();
		b=shortBuffer.get();
		c=shortBuffer.get();
		d=shortBuffer.get();
		e.time_msec = (d<<48) | (c<<32) | (b<<16) | a;
		e.type = shortBuffer.get();
		e.code = shortBuffer.get();
		c=shortBuffer.get();
		d=shortBuffer.get();
		e.value = (d<<16) | c;
		return e;
	}
	
	@Override
	public String toString() {
		//TODO Java sucks at printing unsigned longs. Dur...
		return String.format("Event: time %d.%06d, type %d, code %d, value %02x",
				time_sec, time_msec, type, code, value);
	}
	
}
