package com.dgis.input.evdev;

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
 * Represents configurable parameters of an input axis. set*() should affect the value in the device.
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

interface InputAxisParameters {

	public int getValue();

	public void setValue(int value);

	public int getMin();

	public void setMin(int min);

	public int getMax();

	public void setMax(int max);

	public int getFuzz();

	public void setFuzz(int fuzz);

	public int getFlat();

	public void setFlat(int flat);

}