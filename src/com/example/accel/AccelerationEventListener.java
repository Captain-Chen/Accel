package com.example.accel;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

public class AccelerationEventListener {
	// Stuff for recordings
		private PrintWriter printWriter;
		private static final String CSV_HEADER =
		"X Axis,Y Axis,Z Axis,Acceleration,Time";
		private static final char CSV_DELIM = ',';
		private static final String TAG = MainActivity.class.getName();
	    private static final String FILENAME = "myFile.txt";
		
		private long mSensorTimeStamp, mCpuTimeStamp;
		private int time_counter = 0;
		
public AccelerationEventListener(File dataFile){
	}
}

