package com.example.naveend.stepcounterassignment1;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
   //initialize
    SensorManager sensorManager=null;
    TextView stepstextview;
    TextView stepcountertextview;
    ProgressBar circleprogressbar;
    int totalsteps=0;
    int totalstepcountersteps=0;
    boolean running=false;
    Sensor accelerometer;
    Sensor stepCounter;
    // line for raw magnitude Blue
    private LineGraphSeries<DataPoint> rawData;
    // line for avg magnitude(NO G) Red
    private LineGraphSeries<DataPoint> avgrawData;
    // line for smoothed magnitude(NO G) Green
    private LineGraphSeries<DataPoint> smoothrawData;
    private GraphView graphView;
    Double[] gravity={0.0,0.0,0.0};
    Double sum=0.0;
    Double sumNoG=0.0;
    Double meanNoG=0.0;
    int samplesize=15;
    int point=0;
    Double total=0.0;
    double[] readings;
    int _curReadIndex=0;
    double totalmean=0.0;
    double movingaverage=0.0;
    double minstandarddeviation=0.0;
    int stdcount=0;
    double avgpeak=0.0;
    Bouncer1.MyView bouncer;
    int target=2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        stepstextview = (TextView) findViewById(R.id.tv_steps);
        stepcountertextview= (TextView) findViewById(R.id.tv_stepcountersteps);
        bouncer = (Bouncer1.MyView) findViewById(R.id.myview);
        circleprogressbar=(ProgressBar) findViewById(R.id.progressbar);
        graphView = (GraphView) findViewById(R.id.rawGraph);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(-40);
        graphView.getViewport().setMaxY(30);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(4);
        graphView.getViewport().setMaxX(80);
        rawData = new LineGraphSeries<>();
        rawData.setTitle("Raw Data");
        rawData.setColor(Color.BLUE);
        graphView.addSeries(rawData);
        avgrawData = new LineGraphSeries<>();
        avgrawData.setTitle("avg Raw Data");
        avgrawData.setColor(Color.RED);
        graphView.addSeries(avgrawData);
        smoothrawData = new LineGraphSeries<>();
        smoothrawData.setTitle("smooth Raw Data");
        smoothrawData.setColor(Color.GREEN);
        graphView.addSeries(smoothrawData);
        stepstextview.setText("debug info:");
        readings=  new double[samplesize];

    }


    @Override
    protected void onResume() {
        super.onResume();
        //initialize sensors
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepCounter=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (accelerometer!=null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (stepCounter!=null) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
        {
            double[] reading = new double[3];
            reading[0] = event.values[0];
            reading[1] = event.values[1];
            reading[2] = event.values[2];

            double magnitude = getmagnitude(reading);
            totalmean = totalmean + magnitude;
            double adjmagnitude = magnitude - movingaverage;
            rawData.appendData(new DataPoint(point++, magnitude), true, 1000);
            total = total - readings[_curReadIndex];
            readings[_curReadIndex] = adjmagnitude;
            total = total + readings[_curReadIndex];
            _curReadIndex++;
            if (_curReadIndex >= samplesize) {
                // ...wrap around to the beginning:
                _curReadIndex = 0;
                movingaverage = totalmean / samplesize;
                totalmean = 0;
                totalsteps = totalsteps + findpeaks(readings, total / samplesize);
            }

            stepstextview.setText(Integer.toString(totalsteps));
            avgrawData.appendData(new DataPoint(point, adjmagnitude), true, 1000);
            smoothrawData.appendData(new DataPoint(point, total / samplesize), true, 1000);
        }
        if (event.sensor.getType()==Sensor.TYPE_STEP_COUNTER)
        {
            stepcountertextview.setText(String.valueOf(event.values[0]));
        }

    }
    public double getmagnitude(double[] reading)
    {
        double magnitude= Math.sqrt(reading[0]*reading[0]+reading[1]*reading[1]+reading[2]*reading[2]);
        return magnitude;
    }
    public int findpeaks(double[] data, double average) {
        int steps=0;
        double minpeak=getstddevNoG(data,average);
        for(double val:data) {
            if (val >= 1.8*minpeak) {
                steps++;
                stdcount++;
                minstandarddeviation=minstandarddeviation+minpeak;
                bouncer.startAnimation();
                circleprogressbar.setProgress(50);
            }
        }
        if (stdcount==samplesize){
            avgpeak=minstandarddeviation/stdcount;
            stdcount=0;
            minstandarddeviation=0;
        }
        return steps;
    }
    public double getstddevNoG(double[] data, double average){
        double stdsum=0;
        int count=0;
        for (double val : data) {
            if (val > 0) {
                stdsum += (val - meanNoG) * (val - meanNoG);
                count++;
            }
        }
        double std = (Math.sqrt(stdsum / count - 1));
        return std;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
