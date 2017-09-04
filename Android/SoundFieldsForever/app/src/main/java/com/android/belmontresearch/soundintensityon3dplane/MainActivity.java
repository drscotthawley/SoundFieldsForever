package com.android.belmontresearch.soundintensityon3dplane;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.tangosupport.TangoSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Tango mTango;
    private TangoConfig mConfig;

    private AtomicBoolean mIsTangoPoseReady = new AtomicBoolean(false);

    private double[] dummArray = {0, 0, 0, 0, 0, 0};
    private PointTimeData nodeListStart = new PointTimeData(null, 0, dummArray);
    private PointTimeData currentNode = nodeListStart;

    private boolean isCapturingData = false;

    // Visual elements
    private TextView xValue;
    private TextView yValue;
    private EditText setFreq;
    private Button vButton;
    private EditText setSocketName;
    private ConstraintLayout layout;

    // Preferences File
    public static final String PREFS_NAME = "Preferences";
    private String socketName = "";

    // RecordingThread is the object used to manage audio
    private RecordingThread mRecordingThread;

    // Socket
    private WebView webview;

    private String content;
    private int nodeBuffer = 0;
    private int dataPoints = 0;
    private double rmsDbXAverage = 0;
    private double[] rmsdBFilteredAverage = {0, 0, 0, 0, 0, 0};
    private float x;
    private float y;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resetContentString();

        mRecordingThread = new RecordingThread(new AudioDataReceivedListener() {
            @Override
            public void onAudioDataReceived(short[] data) {
                Log.i(TAG, Arrays.toString(data));
            }
        });

        // Restore Preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        socketName = settings.getString("socketName", "hedges.belmont.edu:3000/");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout = (ConstraintLayout) findViewById(R.id.constraintLayout);
                xValue = (TextView) findViewById(R.id.textView_xValue);
                yValue = (TextView) findViewById(R.id.textView_yValue);
                vButton = (Button) findViewById(R.id.button_collectionState);
                setFreq = (EditText) findViewById(R.id.editTextFrequency);
                setSocketName = (EditText) findViewById(R.id.editTextSocketName);
                webview = (WebView) findViewById(R.id.webView);
                setFreq.setText(mRecordingThread.centerFrequency + "");
                setSocketName.setText(socketName);
            }
        });

        // WebView

//        webview.getSettings().setJavaScriptEnabled(true);
//        webview.getSettings().setLoadWithOverviewMode(true);
//        webview.getSettings().setUseWideViewPort(true);
//
//        webview.setWebViewClient(new WebViewClient() {
//            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//            }
//        });
//
//        webview.loadUrl("http://" + socketName);

//        Attempt to fit WebView better by removing certain elements from page
//        webview.loadUrl("javascript:(function(){ " +
//                "document.getElementsByClassName('dropdown')[0].style.display='none'; " +
//                "})()");



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.
        mTango = new Tango(MainActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there is no UI
            // thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (MainActivity.this) {
                    try {
                        TangoSupport.initialize();
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                        showsToastAndFinishOnUiThread(R.string.exception_out_of_date);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_error);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_invalid);
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("CrashFix", "Tango down");

        mIsTangoPoseReady.compareAndSet(true, false);
        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread. Tango.disconnect
        // will block here until all Tango callback calls are finished. If you lock against this
        // object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            try {
                mTango.disconnect();
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecordingThread.stopRecording();
    }

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Create a new Tango Configuration and enable the MotionTrackingActivity API.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        // Tango Service should automatically attempt to recover when it enters an invalid state.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);

        // Drift correction allows motion tracking to recover after it loses tracking.
        // The drift corrected pose is available through the frame pair with
        // base frame AREA_DESCRIPTION and target frame DEVICE.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);

        try {
            TangoConfig mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
            mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        } catch (TangoErrorException e) {
            // handle exception
        }

        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to pose data.
     */
    private void startupTango() {
        // Select coordinate frame pair.
        Log.i("CrashFix", "Tango Started");
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
//        framePairs.add(new TangoCoordinateFramePair(
//                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
//                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
//        framePairs.add(new TangoCoordinateFramePair(
//                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
//                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));

        // Listen for new Tango data.
        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                synchronized (MainActivity.this) {
                    // When we receive the first onPoseAvailable callback, we know the device has
                    // located itself.
                    TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
                    framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION;
                    framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;

                    TangoPoseData timePose = mTango.getPoseAtTime(pose.timestamp, framePair);

                    mRecordingThread.startRecording();

                    // Gets rms value for microphone input
                    final double rmsDb = mRecordingThread.getRmsdB();
                    final double rmsDbX = mRecordingThread.getRmsdBX();

                    mIsTangoPoseReady.compareAndSet(false, true);
                    final float[] xyz = timePose.getTranslationAsFloats();

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            //stuff that updates ui
                            xValue.setText(String.valueOf(xyz[0]));
                            yValue.setText(String.valueOf(xyz[1]));

                            vButton.setText(String.valueOf(round(rmsDbX,1)));
                            setBackgroundColor((int)round(rmsDbX,1));
                        }
                    });

//                   reduces the amount of UI manipulation
//
                    if(isCapturingData) {

                        if(nodeBuffer == 0) {
                            rmsDbXAverage = rmsDbX;
                            rmsdBFilteredAverage = mRecordingThread.getRmsdBFiltered();
                            x = xyz[0];
                            y = xyz[1];

                            nodeBuffer++;
                        } else if (nodeBuffer < 5) {
                            rmsDbXAverage += rmsDbX;
                            for(int i=0; i<rmsdBFilteredAverage.length; i++) {
                                rmsdBFilteredAverage[i] += mRecordingThread.getRmsdBFiltered()[i];
                            }
                            x += xyz[0];
                            y += xyz[1];
                            nodeBuffer++;
                        } else {
                            rmsDbXAverage = rmsDbXAverage/5;
                            for(int j=0; j<rmsdBFilteredAverage.length; j++) {
                                rmsdBFilteredAverage[j] = rmsdBFilteredAverage[j]/5;
                            }
                            x = x/5;
                            y = y/5;
                            float[] xyzMod = new float[]{x, y, xyz[2]};
                            PointTimeData averageNode = new PointTimeData(xyzMod, rmsDbXAverage, rmsdBFilteredAverage);
                            content += System.lineSeparator() + averageNode.toString();
                            nodeBuffer = 0;
                            dataPoints++;
                            Log.i("Data", averageNode.toString());
                            Log.i("Data", dataPoints + "");
                        }
//                            Code enables gridded mapping
//                            if(Math.round(xyz[0] * 100) % 2 == 0 && Math.round(xyz[1] * 100) % 2 == 0) {
//                                xyz[0] = Math.round(xyz[0] * 100);
//                                xyz[1] = Math.round(xyz[1] * 100);
//                            }
                        if (dataPoints == 40) {
                            DiskWrite.writeToDisk(content, socketName);
                            Log.i("Write", content);
                            resetContentString();
                            dataPoints = 0;
                        }

                    }
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
                Log.i(TAG, xyzIj.xyz.toString());
            }

            @Override
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData) {
                // We are not using onPointCloudAvailable for this app.
            }

            @Override
            public void onTangoEvent(final TangoEvent event) {
                // Ignoring TangoEvents.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We are not using onFrameAvailable for this application.
            }
        });
    }

    private void setBackgroundColor(int dbInInt) {
        if (dbInInt > 99) {
            vButton.setBackgroundColor(Color.parseColor("#f44242"));
            vButton.setText("MAX");
        } else if (dbInInt > 95) {
            vButton.setBackgroundColor(Color.parseColor("#f4417a"));
        } else if (dbInInt > 90) {
            vButton.setBackgroundColor(Color.parseColor("#f4419a"));
        } else if (dbInInt > 85) {
            vButton.setBackgroundColor(Color.parseColor("#f441df"));
        } else if (dbInInt > 80) {
            vButton.setBackgroundColor(Color.parseColor("#d041f4"));
        } else if (dbInInt > 75) {
            vButton.setBackgroundColor(Color.parseColor("#b541f4"));
        } else if (dbInInt > 70) {
            vButton.setBackgroundColor(Color.parseColor("#8841f4"));
        } else if (dbInInt > 65) {
            vButton.setBackgroundColor(Color.parseColor("#6d41f4"));
        } else if (dbInInt > 60) {
            vButton.setBackgroundColor(Color.parseColor("#4143f4"));
        } else if (dbInInt > 55) {
            vButton.setBackgroundColor(Color.parseColor("#283396"));
        } else {
            vButton.setBackgroundColor(Color.parseColor("#1b236d"));
        }
    }

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    // Method that handles the onClick event of the record-state button
    public void stateChange(View view) {
        Button v = (Button) view;
        if (isCapturingData) {

            v.setTextSize(16);
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Saving Data...",
                            Toast.LENGTH_LONG).show();
                }
            });
            isCapturingData = false;
//            DiskWrite.writeToDisk(content, socketName);
            v.setText("Start Collection!");
        } else {
            isCapturingData = true;
            v.setTextSize(40);
        }
    }

    // Method called upon clicking the set frequency button
    public void setFrequency(View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordingThread.centerFrequency = Integer.parseInt(setFreq.getText().toString());
                layout.requestFocus();
            }
        });

    }

    private static double round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    private void resetContentString() {
        content = "x,y,z,dB,dB0,dB1,dB2,dB3,dB4,dB5,dB6";
    }

    public void setSocketName(View view) {
        socketName = setSocketName.getText().toString();

        // Adds socketName to preferences 
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("socketName", socketName);

    }

}
