package com.top1.client.music;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public final class AudioLevel {
	private interface Ole32 extends Library {
		Ole32 INSTANCE = Native.load("ole32", Ole32.class);

		int CoInitializeEx(Pointer reserved, int flags);

		int CoCreateInstance(Pointer clsid, Pointer outer, int context, Pointer iid, PointerByReference result);
	}

	private static final String CLSID_ENUMERATOR = "BCDE0395-E52F-467C-8E3D-C4579291692E";
	private static final String IID_ENUMERATOR = "A95664D2-9614-4F35-A746-DE8DB63617E6";
	private static final String IID_METER = "C02216F6-8C67-4B5B-9D00-D008E73E0064";
	private static final int CLSCTX_ALL = 23;

	private static Pointer meter;
	private static Memory peakOut;
	private static boolean initialized;
	private static boolean broken;

	public static float peak() {
		if(broken) return -1.0F;
		try{
			if(!initialized) init();
			if(meter == null) return -1.0F;
			int hr = call(meter, 3, peakOut);
			if(hr != 0){
				release();
				return -1.0F;
			}
			return peakOut.getFloat(0);
		}catch(Throwable e){
			broken = true;
			return -1.0F;
		}
	}

	private static void init() {
		initialized = true;
		if(!System.getProperty("os.name").toLowerCase().startsWith("windows")){
			broken = true;
			return;
		}
		Ole32.INSTANCE.CoInitializeEx(null, 0);
		PointerByReference enumeratorRef = new PointerByReference();
		int hr = Ole32.INSTANCE.CoCreateInstance(guid(CLSID_ENUMERATOR), null, CLSCTX_ALL,
			guid(IID_ENUMERATOR), enumeratorRef);
		if(hr != 0) return;
		Pointer enumerator = enumeratorRef.getValue();
		try{
			PointerByReference deviceRef = new PointerByReference();
			if(call(enumerator, 4, 0, 0, deviceRef) != 0) return;
			Pointer device = deviceRef.getValue();
			try{
				PointerByReference meterRef = new PointerByReference();
				if(call(device, 3, guid(IID_METER), CLSCTX_ALL, null, meterRef) != 0) return;
				meter = meterRef.getValue();
				peakOut = new Memory(4);
			} finally {
				call(device, 2);
			}
		} finally {
			call(enumerator, 2);
		}
	}

	private static void release() {
		if(meter != null){
			try{
				call(meter, 2);
			}catch(Throwable ignored){
			}
			meter = null;
		}
		initialized = false;
	}

	private static int call(Pointer object, int index, Object... args) {
		Pointer vtable = object.getPointer(0);
		Pointer method = vtable.getPointer((long) index * Native.POINTER_SIZE);
		Object[] full = new Object[args.length + 1];
		full[0] = object;
		System.arraycopy(args, 0, full, 1, args.length);
		return Function.getFunction(method).invokeInt(full);
	}

	private static Memory guid(String text) {
		String clean = text.replace("-", "");
		byte[] bytes = new byte[16];
		long part1 = Long.parseLong(clean.substring(0, 8), 16);
		int part2 = Integer.parseInt(clean.substring(8, 12), 16);
		int part3 = Integer.parseInt(clean.substring(12, 16), 16);
		bytes[0] = (byte) part1;
		bytes[1] = (byte) (part1 >> 8);
		bytes[2] = (byte) (part1 >> 16);
		bytes[3] = (byte) (part1 >> 24);
		bytes[4] = (byte) part2;
		bytes[5] = (byte) (part2 >> 8);
		bytes[6] = (byte) part3;
		bytes[7] = (byte) (part3 >> 8);
		for(int i = 0; i < 8; i++){
			bytes[8 + i] = (byte) Integer.parseInt(clean.substring(16 + i * 2, 18 + i * 2), 16);
		}
		Memory memory = new Memory(16);
		memory.write(0, bytes, 0, 16);
		return memory;
	}

	private AudioLevel() {
	}
}
