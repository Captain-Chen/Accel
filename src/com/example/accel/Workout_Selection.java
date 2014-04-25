package com.example.accel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class Workout_Selection extends Activity implements SensorEventListener{
	// Sensor declarations	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	
	// For calculations
	private final float NOISE_THRESHOLD = (float)2.0;
	private float[] gravity = new float[3];
	private static final float ALPHA = 0.8f;
		
	private float[] values;
	private float last_pos;
	private ArrayList<Float> xValues = new ArrayList<Float>();
	
	// HashMap of phone masses based on model
	private HashMap<String, Double> phone_mass = new HashMap<String, Double>();
	
	// Writing to file
	private PrintWriter printWriter;
	private static final String TAG = MainActivity.class.getName();
	
	// Reading from file
	private BufferedReader bufferedReader;
	
	// Has device already been initialized?
	private boolean mInitialized;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.workout_selection);
		
		TextView prev_force = (TextView)findViewById(R.id.prev_force);
		prev_force.setText("Level");
		
		// Initialize the sensors
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
		// Populate the HashMap of phone weights based on model (how to scale for many phones?)
		phone_mass.put("LG-P690b", 0.12919);
		phone_mass.put("GT-S5830D", 0.113);
		
		// Read previous file for recording
		readFromFile();
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// not sure what it does at the moment, not used
	}

	// Method that will be called when the accelerometer detects any change (main loop)
		@Override
		public void onSensorChanged(SensorEvent event) {
			
			// Set a small delay to save CPU time or something
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			TextView tvX = (TextView)findViewById(R.id.x_axis);
			//TextView tvY = (TextView)findViewById(R.id.y_axis);
			//TextView tvZ = (TextView)findViewById(R.id.z_axis);
					
			// Copies the x,y,z values into a new array 'values'
			values = event.values.clone();
			
			/* Pass the values of the array into the highPass method in order to filter out the noise
			 Note: Direction of the x-axis has been reversed (Should be) in order to make calculations
			 easier
			Returns filtered values
			*/
			values = highPass(values[0],values[1],values[2]);
			
			// Some math stuff
			double sumOfSquares = (values[0] * values[0])
	                + (values[1] * values[1])
	                + (values[2] * values[2]);
	        double acceleration = Math.sqrt(sumOfSquares);
	        
	        if(!mInitialized){
	        	tvX.setText("0.0");
	        	//tvY.setText("0.0");
	        	//tvZ.setText("0.0");
	        	//time1.setText("Nothing");
	        	mInitialized = true;
	        }
	        Intent started = getIntent();
	        Boolean hasStarted = started.getExtras().getBoolean("hasStarted");
	        // Checks for "movement" of the device by comparing its total acceleration to threshold
	        if((acceleration > NOISE_THRESHOLD) && (hasStarted == true)){
				
	        	// Get the current time
	        	/*
	        	mSensorTimeStamp = event.timestamp;
				mCpuTimeStamp = System.nanoTime();
	        	*/
	        	
				// Delays the execution of the method call for 2s
				/*Handler handler = new Handler();
				handler.postDelayed(new Runnable(){
					public void run(){
					//Set the current values of acceleration (x,y,z)
					 */
						setAcceleration(values);
					/*}
				}, 2000);
				 */
	        }		
		}

	/*
     * This method derived from the Android documentation and is available under
     * the Apache 2.0 license.
     * 
     * @see http://developer.android.com/reference/android/hardware/SensorEvent.html
     */
    private float[] highPass(float x, float y, float z)
    {
        float[] filteredValues = new float[3];
        
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x;
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y;
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z;

        filteredValues[0] = x - gravity[0];
        filteredValues[1] = y - gravity[1];
        filteredValues[2] = z - gravity[2];
        
        return filteredValues;
    }
	// Methods calculates the average acceleration
	private static double calcAvg(ArrayList<Float> list){
		// 'average' is undefined if there are no elements in the list.
	    if (list == null || list.isEmpty())
	        return (double) 0.0;
	    // Calculate the summation of the elements in the list
	    long sum = 0;
	    int n = list.size(); // should be 5 (from reps)
	    // Iterating manually is faster than using an enhanced for loop.
	    for (int i = 0; i < n; i++)
	        sum += list.get(i);
	    // We don't want to perform an integer division, so the cast is mandatory.
	    return (double) sum / n;
	}
	
	// Takes in an array of values of type float
			private void setAcceleration(float[] values){
				TextView tvX = (TextView)findViewById(R.id.x_axis);
				//TextView tvY = (TextView)findViewById(R.id.y_axis);
				//TextView tvZ = (TextView)findViewById(R.id.z_axis);
				TextView avg_accel = (TextView)findViewById(R.id.avg_accel);
				TextView measured_force = (TextView)findViewById(R.id.force);
				
				tvX.setText("X-Acceleration: " + Float.toString(values[0]));
				//tvY.setText(Float.toString(values[1]));
				//tvZ.setText(Float.toString(values[2]));
				
				// Stuff to check last position
				float xChange = last_pos - values[0];
				last_pos = values[0];
				
				// Check whether the device is moving away or toward the user
				if (xChange > 2){
					//time1.setText("Device is moving left");
					// Add to the ArrayList, x-values are multiplied by -1 to reverse directions
					// in order to measure "positive" acceleration
					xValues.add(Math.abs(-1*values[0]));
					Log.w("debug_message", "Device is moving left");
					//Log.w("debug_message", Float.toString(xValues.get(i)));
					//i++;
					// Once 5 measurements are taken, pass to the method
					if(xValues.size() == 5){
						double avgAccel = calcAvg(xValues);
						double mass = getWeight();
						double force = 0.0;
						
						// Check if mass is known (i.e. not zero)
						if(mass != 0.0){
							// Calculate the force in Newtons
							force = mass * avgAccel;
							measured_force.setText("Force: " + Double.toString(force) + "N");
							// Log message to display mass in kg
							Log.w("String", "Mass: " + Double.toString(mass));
							// Log message to display the exerted force
							Log.w("Average", "Force: " + Double.toString(force));
							// Prepare to write to the CSV file
							StringBuffer data = new StringBuffer().append(force);
							writeToFile(data);
						}else{
							// Otherwise, cannot perform calculation (No mass value)
							measured_force.setText("Cannot calculate force");
						}
						// Show average acceleration
						avg_accel.setText("Average Acceleration: " + Double.toString(avgAccel) + "m/s^2");
					}
					// Prepare data to write
					//StringBuffer data = new StringBuffer().append(b);
					/*StringBuffer data = new StringBuffer().append(values[0])
							.append(CSV_DELIM).append(values[1]).append(CSV_DELIM).append(values[2]).append(CSV_DELIM).append(time);
					writeToFile(data);
					*/
				}else if(xChange < -2){
					// Don't really care if the device is moving away
					Log.w("debug_message", "Device is right");
				}
			}	
	
	private void writeToFile(StringBuffer data) {
		
		// Preparing the file to write to
		File logFile = new File(Environment.getExternalStorageDirectory().toString(), "logfile.csv");
		
	try{			
		// Check if the file already exists, if it does not exist create the file
		if(!logFile.exists()){logFile.createNewFile();}
					
		printWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
		printWriter.println(data);
								
		// Close when done writing to file
		printWriter.close();	
		}catch(IOException e){
		Log.e(TAG, "Could not open CSV file", e);
		}
    }
	
	// Reads from the CSV file to be read into an array separated by a comma
		private void readFromFile(){
			TextView prev_force = (TextView)findViewById(R.id.prev_force);
			// Prepare file to be read
			String line = "";
			File logFile = new File(Environment.getExternalStorageDirectory().toString(), "logfile.csv");
			try{
				bufferedReader = new BufferedReader(new FileReader(logFile));
				// while there's still stuff to read
				while((line = bufferedReader.readLine()) != null){
					String[] record = line.split(",");
					prev_force.setText("L" + record[0]);
					}
				}catch (IOException e){
					Log.e(TAG, "Error reading CSV file");
		}
	}
		
	// Method that will return the phone's mass in kg if model is known
	// Will return 0.0 otherwise
	private double getWeight(){
		String model = Build.MODEL;
		Log.w("String", model);
		// Check if phone model is known (exists in the hashmap)
		if(phone_mass.get(model) == null){
			// If phone model is not known, return "unknown" mass
			return 0.0;
		}else{
			// If phone model is known (i.e. not null), return its mass
			return phone_mass.get(model);
		}
	}
    
    // Method that will run when device is turned back on, i.e. turn back on the sensors
 	@Override
 	protected void onResume(){
 		super.onResume();
 		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);	
 	}
 	
 	// Method that will run when the device is turned off, i.e. turn off the sensors to conserve batteries
 	@Override
 	protected void onPause(){
 		super.onPause();
 		mSensorManager.unregisterListener(this);
 	}
}
