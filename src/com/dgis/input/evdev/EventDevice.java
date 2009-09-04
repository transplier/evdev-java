package com.dgis.input.evdev;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
 * Represents a connection to a Linux Evdev device.
 * For additional info, see input/input.txt and input/input-programming.txt in the Linux kernel Documentation.
 * IMPORTANT: If you want higher-level access for your joystick/pad/whatever, check com.dgis.input.evdev.devices
 * for useful drivers to make your life easier!
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

interface IEventDevice {
	/**
	 * @return The version of Evdev reported by the kernel.
	 */
	public int getEvdevVersion();
	
	/**
	 * @return the bus ID of the attached device.
	 */
	public short getBusID();
	/**
	 * @return the vendor ID of the attached device.
	 */
	public short getVendorID();
	/**
	 * @return the product ID of the attached device.
	 */
	public short getProductID();
	/**
	 * @return the version ID of the attached device.
	 */
	public short getVersionID();
	/**
	 * @return the name of the attached device.
	 */
	public String getDeviceName();
	/**
	 * @return A mapping from device supported event types to list of supported event codes.
	 */
	public Map<Integer, List<Integer>> getSupportedEvents();
	
	/**
	 * Obtains the configurable parameters of an absolute axis (value, min, max, fuzz, flatspot) from the device. 
	 * @param axis The axis number (an event code under event type 3 (abs)).
	 * @return The parameters, or null if there was an error. Modifications to this object will be reflected in the device.
	 */
	public InputAxisParameters getAxisParameters(int axis);
	
	/**
	 * Adds an event listener to this device.
	 * When an event is received from Evdev, all InputListeners registered
	 * will be notified by a call to event().
	 * If the listener is already on the listener list,
	 * this method has no effect.
	 * @param list The listener to add. Must not be null.
	 */
	public void addListener(InputListener list);
	/**
	 * Removes an event listener to this device.
	 * If the listener is not on the listener list,
	 * this method has no effect.
	 * @param list The listener to remove. Must not be null.
	 */
	public void removeListener(InputListener list);
	
	/**
	 * Releases all resources held by this EventDevice. No more events will be generated.
	 * It is impossible to restart an EventDevice once this method is called. 
	 */
	public void close();
}

/**
 * Driver for a Linux Evdev character device.
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

public class EventDevice implements IEventDevice{

	/**
	 * Notify these guys about input events.
	 */
	private List<InputListener> listeners = new ArrayList<InputListener>();
	
	/**
	 * Device filename we're using.
	 */
	String device;
	
	/**
	 * Attached to device we're using.
	 */
	private FileChannel deviceInput;
	private ByteBuffer inputBuffer = ByteBuffer.allocate(InputEvent.STRUCT_SIZE_BYTES);

	
	/**
	 * When this is true, the reader thread should terminate ASAP.
	 */
	private volatile boolean terminate = false;

	/**
	 * This thread repeatedly calls readEvent().
	 */
	private Thread readerThread;
	
	private short[] idResponse = new short[4];

	private int evdevVersionResponse;

	private String deviceNameResponse;
	
	/**
	 * Maps supported event types (keys) to lists of supported event codes.
	 */
	private HashMap<Integer, List<Integer>> supportedEvents = new HashMap<Integer, List<Integer>>();
	
	
	/**
	 * Ensures only one instance of InputAxisParameters is created for each axis (more would be wasteful). 
	 */
	private HashMap<Integer, InputAxisParameters> axisParams = new HashMap<Integer, InputAxisParameters>();
	
	/**
	 * Create an EventDevice by connecting to the provided device filename.
	 * If the device file is accessible, open it and begin listening for events. 
	 * @param device The path to the device file. Usually one of /dev/input/event*
	 * @throws IOException If the device is not found, or is otherwise inaccessible.
	 */
	public EventDevice(String device) throws IOException {
		System.loadLibrary("evdev-java");
		this.device = device;
		inputBuffer.order(ByteOrder.LITTLE_ENDIAN);
		initDevice();
	}
	
	/**
	 * Get various ID info. Then, open the file, get the channel, and start the reader thread.
	 * @throws IOException
	 */
	private void initDevice() throws IOException {
		
		if(!ioctlGetID(device, idResponse)) {
			System.err.println("WARN: couldn't get device ID: "+device);
			Arrays.fill(idResponse, (short)0);
		}
		evdevVersionResponse = ioctlGetEvdevVersion(device);
		byte[] devName = new byte[255];
		if(ioctlGetDeviceName(device, devName)) {
			deviceNameResponse = new String(devName);
		} else {
			System.err.println("WARN: couldn't get device name: "+device);
			deviceNameResponse = "Unknown Device";
		}
		
		readSupportedEvents();
		
		FileInputStream fis = new FileInputStream(device);
		deviceInput = fis.getChannel();
		
		readerThread = new Thread() {
			@Override
			public void run() {
				while(!terminate) {
					InputEvent ev = readEvent();
					distributeEvent(ev);
				}
			}
		};
		readerThread.setDaemon(true); /* We don't want this thread to prevent the JVM from terminating */
		
		readerThread.start();
	}

	/**
	 * Get supported events from device, and place into supportedEvents.
	 * Adapted from evtest.c.
	 */
	private void readSupportedEvents() {
		//System.out.println("Detecting device capabilities...");
		long[][] bit = new long[InputEvent.EV_MAX][NBITS(InputEvent.KEY_MAX)];
		ioctlEVIOCGBIT(device, bit[0], 0, bit[0].length);
		/* Loop over event types */
		for (int i = 0; i < InputEvent.EV_MAX; i++) {
			if (testBit(bit[0], i)) { /* Is this event supported? */
				//System.out.printf("  Event type %d\n", i);
				if (i==0) continue;
				ArrayList<Integer> supportedTypes = new ArrayList<Integer>();
				ioctlEVIOCGBIT(device, bit[i], i, InputEvent.KEY_MAX);
				/* Loop over event codes for type */
				for (int j = 0; j < InputEvent.KEY_MAX; j++) 
					if (testBit(bit[i], j)) { /* Is this event code supported? */
						//System.out.printf("    Event code %d\n", j);
						supportedTypes.add(j);
					}
				supportedEvents.put(i, supportedTypes);
			}
		}
	}
	
	private boolean testBit(long[] array, int bit) {
		return ((array[LONG(bit)] >>> OFF(bit)) & 1)!=0;
	}
	private int LONG(int x) {
		return x/(64);
	}
	private int OFF(int x) {
		return x%(64);
	}
	private int NBITS(int x) {
		return ((((x)-1)/(8*8))+1);
	}

	/**
	 * Distribute an event to all registered listeners.
	 * @param ev The event to distribute.
	 */
	private void distributeEvent(InputEvent ev) {
		synchronized (listeners) {	
			for(InputListener il : listeners) {
				il.event(ev);
			}
		}
	}

	/**
	 * Obtain an InputEvent from the input channel. Delegate to InputEvent for parsing.
	 * @return
	 */
	private InputEvent readEvent() {
		try {
			/* Read exactly the amount of bytes specified by InputEvent.STRUCT_SIZE_BYTES (intrinsic size of inputBuffer)*/
			inputBuffer.clear();
			while(inputBuffer.hasRemaining()) deviceInput.read(inputBuffer);
			
			/* We want to read now */
			inputBuffer.flip();
			
			/* Delegate parsing to InputEvent.parse() */
			return InputEvent.parse(inputBuffer.asShortBuffer(), device);
		} catch (IOException e ) { 
			return null;
		}
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#close()
	 */
	@Override
	public void close() {
		terminate=true;
		try {
			readerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			deviceInput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#getBusID()
	 */
	@Override
	public short getBusID() {
		return idResponse[InputEvent.ID_BUS];
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#getDeviceName()
	 */
	@Override
	public String getDeviceName() {
		return deviceNameResponse;
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#getProductID()
	 */
	@Override
	public short getProductID() {
		// TODO Auto-generated method stub
		return idResponse[InputEvent.ID_PRODUCT];
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#getSupportedEvents()
	 */
	@Override
	public Map<Integer, List<Integer>> getSupportedEvents() {
		return supportedEvents;
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#getVendorID()
	 */
	@Override
	public short getVendorID() {
		return idResponse[InputEvent.ID_VENDOR];
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#getEvdevVersion()
	 */
	@Override
	public int getEvdevVersion() {
		return evdevVersionResponse;
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#getVersionID()
	 */
	@Override
	public short getVersionID() {
		return idResponse[InputEvent.ID_VERSION];
	}

	@Override
	public InputAxisParameters getAxisParameters(int axis) {
		InputAxisParameters params;
		if((params = axisParams.get(axis)) == null) {
			params = new InputAxisParametersImpl(this, axis); 
			axisParams.put(axis, params);
		}
		return params;
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#addListener(com.dgis.input.evdev.InputListener)
	 */
	@Override
	public void addListener(InputListener list) {
		synchronized (listeners) {
			listeners.add(list);
		}
	}

	/**
	 * @see com.dgis.input.evdev.IEventDevice#removeListener(com.dgis.input.evdev.InputListener)
	 */
	@Override
	public void removeListener(InputListener list) {
		synchronized (listeners) {
			listeners.remove(list);
		}
	}
	
	
	public String getDevicePath() {
		return device;
	}

	////BEGIN JNI METHODS////
	native boolean ioctlGetID(String device, short[] resp);
	native int ioctlGetEvdevVersion(String device);
	native boolean ioctlGetDeviceName(String device, byte[] resp);
	native boolean ioctlEVIOCGBIT(String device, long[] resp, int start, int stop);
	native boolean ioctlEVIOCGABS(String device, int[] resp, int axis);
}

class InputAxisParametersImpl implements InputAxisParameters {

	private EventDevice device;
	private int axis;

	private int value, min, max, fuzz, flat;

	public InputAxisParametersImpl(EventDevice device, int axis) {
		this.device = device;
		this.axis = axis;
		readStatus();
	}

	/**
	 * Repopulate values stored in this class with values read from the device.
	 */
	private void readStatus() {
		int[] resp = new int[5];
		device.ioctlEVIOCGABS(device.device, resp, axis);
		value = resp[0];
		min = resp[1];
		max = resp[2];
		fuzz = resp[3];
		flat = resp[4];
	}

	/**
	 * Repopulate values stored in the device with values read from this class.
	 */
	private void writeStatus() {
		throw new NotImplementedException();
	}

	public int getValue() {
		synchronized (this) {
			readStatus();
			return value;
		}
	}

	public void setValue(int value) {
		synchronized (this) {
			this.value = value;
			writeStatus();
		}
	}

	public int getMin() {
		synchronized (this) {
			readStatus();
			return min;
		}
	}

	public void setMin(int min) {
		synchronized (this) {
			this.min = min;
			writeStatus();
		}
	}

	public int getMax() {
		synchronized (this) {
			readStatus();
			return max;
		}
	}

	public void setMax(int max) {
		synchronized (this) {
			this.max = max;
			writeStatus();
		}
	}

	public int getFuzz() {
		synchronized (this) {
			readStatus();
			return fuzz;
		}
	}

	public void setFuzz(int fuzz) {
		synchronized (this) {
			this.fuzz = fuzz;
			writeStatus();
		}
	}

	public int getFlat() {
		synchronized (this) {
			readStatus();
			return flat;
		}
	}

	public void setFlat(int flat) {
		synchronized (this) {
			this.flat = flat;
			writeStatus();
		}
	}

	@Override
	public String toString() {
		return "Value: " + getValue() + " Min: " + getMin() + " Max: "
				+ getMax() + " Fuzz: " + getFuzz() + " Flat: " + getFlat();
	}
}