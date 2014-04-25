package com.example.accel;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.util.Log;
import android.view.View;
import android.view.Menu;

import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity{

	private Button start_exercise;

	// Reading
	private BufferedReader bufferedReader;
	private static final String TAG = MainActivity.class.getName();
	
	// Called when the activity is first created
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		start_exercise = (Button)findViewById(R.id.button1);
	}
	
	// Not exactly sure what this is at the moment
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void startExercise(View view){
		Intent workout_screen = new Intent(getApplicationContext(), Workout_Selection.class );
		workout_screen.putExtra("hasStarted", true);
		startActivity(workout_screen);
	}
	
	// Reads from the CSV file to be read into an array separated by a comma
	private void readFromFile(){
		// Prepare file to be read
		String line = "";
		File logFile = new File(Environment.getExternalStorageDirectory().toString(), "logfile.csv");
		try{
			bufferedReader = new BufferedReader(new FileReader(logFile));
			// while there's still stuff to read
			while((line = bufferedReader.readLine()) != null){
				String[] record = line.split(",");
				Log.w("debug_message", record[0]);
				}
			}catch (IOException e){
				Log.e(TAG, "Error reading CSV file");
			}
		}
}
