package com.example.dbmeter;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity implements MqttCallback {
    double x=-1,y;
    GraphView graph;
    TextView Live_value;
    Button start;
    MediaRecorder my_recorder;
    Thread running_thread;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;
    LineGraphSeries<DataPoint> series;

    final  Runnable updater = new Runnable() {
        @Override
        public void run() {
            updateTv();
            updateGraph();
        }
    };

    final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        graph = findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();

        FirebaseApp.initializeApp(this);
        Live_value = findViewById(R.id.readings);
        start = findViewById(R.id.start);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecorder();
            }
        });

        if(running_thread == null){
            running_thread = new Thread(){
                public void run(){
                    while (running_thread != null){
                        try{
                            Thread.sleep(1000);
                            Log.i("Noise", "Tock");
                        }
                        catch (InterruptedException e){

                        }
                        mHandler.post(updater);
                    }
                }
            };
            running_thread.start();
            Log.d("Noise", "start running_thread()");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRecorder();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecorder();
    }

    public void startRecorder(){
        if(my_recorder == null){
            my_recorder = new MediaRecorder();
            my_recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            my_recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            my_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            my_recorder.setOutputFile("/dev/null");
            try{
                my_recorder.prepare();

            }catch (java.io.IOException ioe){
                android.util.Log.e("[CGB]", "IOException: " + android.util.Log.getStackTraceString(ioe));
            }catch (java.lang.SecurityException e){
                android.util.Log.e("[CGB]", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }

            try{
                my_recorder.start();
            }catch (java.lang.SecurityException e){
                android.util.Log.e("[CGB]", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }

        }
    }


    public void stopRecorder(){
        if(my_recorder != null){
            my_recorder.stop();
            my_recorder.release();
            my_recorder = null;
        }
    }

    public void updateTv(){
        Live_value.setText(Double.toString((soundDb(10))) + "dB");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");
        myRef.push().setValue(Double.toString((soundDb(10)))+"dB");


        String filename = "myfile";
        String fileContents = (String) Live_value.getText();
        FileOutputStream outputStream;


        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateGraph(){

        x=x+1;
        y=soundDb(10);
        series.appendData(new DataPoint(x,y), true, 50000);
        graph.addSeries(series);
    }

    public double soundDb(double ampl){
        return 20*Math.log10(getAmplitudeEMA()/ampl);
    }

    public double getAmplitude(){
        if(my_recorder != null){
            return (my_recorder.getMaxAmplitude());
        }else {
            return 0;
        }
    }

    public double getAmplitudeEMA(){
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        Toast.makeText(MainActivity.this, "Topic: "+topic+"\nMessage: "+message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
