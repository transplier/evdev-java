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
	private String device;
	
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
		if(ioctlGetDeviceName(device, devName, devName.length)) {
			deviceNameResponse = new String(devName);
		} else {
			System.err.println("WARN: couldn't get device name: "+device);
			deviceNameResponse = "Unknown Device";
		}
		
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
			return InputEvent.parse(inputBuffer.asShortBuffer());
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
		// TODO Auto-generated method stub
		return new HashMap<Integer, List<Integer>>();
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
	
	
	////BEGIN JNI METHODS////
	private native boolean ioctlGetID(String device, short[] resp);
	private native int ioctlGetEvdevVersion(String device);
	private native boolean ioctlGetDeviceName(String device, byte[] resp, int len);
}