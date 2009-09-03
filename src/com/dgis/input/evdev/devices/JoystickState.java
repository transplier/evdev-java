package com.dgis.input.evdev.devices;

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
 * Represents the state of a joystick (buttons and axes)
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

public class JoystickState {
	private int numButtons;
	private int numAxes;
	private boolean buttonStates[];
	private int axisStates[];
	private int axisMinValue[];
	private int axisMaxValue[];
	
	public JoystickState(int numButtons, int numAxes) {
		buttonStates = new boolean[numButtons];
		axisStates = new int[numAxes];
		axisMinValue = new int[numAxes];
		axisMaxValue = new int[numAxes];
	}

	public int getNumButtons() {
		return numButtons;
	}

	public int getNumAxes() {
		return numAxes;
	}

	public boolean getButtonState(int button) {
		return buttonStates[button];
	}

	public int getAxisState(int axis) {
		return axisStates[axis];
	}

	public int getAxisMinValue(int axis) {
		return axisMinValue[axis];
	}

	public int getAxisMaxValue(int axis) {
		return axisMaxValue[axis];
	}

	void setButtonState(int button, boolean state) {
		buttonStates[button] = state;;
	}

	void setAxisState(int axis, int state) {
		axisStates[axis] = state;
	}

	void setAxisMinValue(int axis, int min) {
		axisMinValue[axis] = min;
	}

	void setAxisMaxValue(int axis, int max) {
		axisMaxValue[axis] = max;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer("Buttons: [ ");
		for(boolean b : buttonStates) buf.append((b?1:0) + " ");
		buf.append("] Axes: [ ");
		for(int a : axisStates) buf.append(a + " ");
		buf.append("]");
		return buf.toString();
	}
}
