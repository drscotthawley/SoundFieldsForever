package com.android.belmontresearch.soundintensityon3dplane;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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

    private PointTimeData nodeListStart = new PointTimeData(null, 0);
    private PointTimeData currentNode = nodeListStart;

    private boolean isCapturingData = false;

    private TextView xValue;
    private TextView yValue;
    private EditText setFreq;
    private Button vButton;
    private ConstraintLayout layout;

    private RecordingThread mRecordingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecordingThread = new RecordingThread(new AudioDataReceivedListener() {
            @Override
            public void onAudioDataReceived(short[] data) {
                Log.i(TAG, Arrays.toString(data));
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout = (ConstraintLayout) findViewById(R.id.constraintLayout);
                xValue = (TextView) findViewById(R.id.textView_xValue);
                yValue = (TextView) findViewById(R.id.textView_yValue);
                vButton = (Button) findViewById(R.id.button_collectionState);
                setFreq = (EditText) findViewById(R.id.editTextFrequency);
            }
        });

        setFreq.setText(mRecordingThread.centerFrequency + "");

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
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Listen for new Tango data.
        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                synchronized (MainActivity.this) {
                    // When we receive the first onPoseAvailable callback, we know the device has
                    // located itself.
                    TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
                    framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE;
                    framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;

                    TangoPoseData timePose = mTango.getPoseAtTime(pose.timestamp, framePair);

                    // Gets rms value for microphone input
                    double rmsDb = mRecordingThread.getRmsdB();

//                  Math.round(pose.timestamp * 10)%2==0 reduces the amount of UI manipulation
//
                    if(isCapturingData && Math.round(pose.timestamp * 10)%2==0) {
                        if (rmsDb > -9000 && rmsDb != 0) {
                            mIsTangoPoseReady.compareAndSet(false, true);
                            final float[] xyz = timePose.getTranslationAsFloats();

                            final PointTimeData newNode = new PointTimeData(xyz, rmsDb);
                            currentNode.setNextNode(newNode);
                            currentNode = currentNode.getNextNode();

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    //stuff that updates ui
                                    xValue.setText(String.valueOf(xyz[0]));
                                    yValue.setText(String.valueOf(xyz[1]));

                                    int dbInInt = (int) newNode.getDb();
                                    vButton.setText(String.valueOf(dbInInt));
                                    setBackgroundColor(dbInInt);
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    vButton.setText("no data");
                                }
                            });
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
        if (dbInInt > 0) {
            vButton.setBackgroundColor(Color.parseColor("#f44242"));
        } else if (dbInInt > -10) {
            vButton.setBackgroundColor(Color.parseColor("#f4417a"));
        } else if (dbInInt > -25) {
            vButton.setBackgroundColor(Color.parseColor("#f4419a"));
        } else if (dbInInt > -30) {
            vButton.setBackgroundColor(Color.parseColor("#f441df"));
        } else if (dbInInt > -45) {
            vButton.setBackgroundColor(Color.parseColor("#d041f4"));
        } else if (dbInInt > -60) {
            vButton.setBackgroundColor(Color.parseColor("#b541f4"));
        } else if (dbInInt > -85) {
            vButton.setBackgroundColor(Color.parseColor("#8841f4"));
        } else if (dbInInt > -100) {
            vButton.setBackgroundColor(Color.parseColor("#6d41f4"));
        } else if (dbInInt > -115) {
            vButton.setBackgroundColor(Color.parseColor("#4143f4"));
        } else if (dbInInt > -130) {
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
            mRecordingThread.stopRecording();
            v.setTextSize(16);
            v.setText("Saving Data");
            isCapturingData = false;

            boolean written = DiskWrite.writeToDisk(MainActivity.this, nodeListStart);
            while (written != true) {
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Data Saved!",
                            Toast.LENGTH_LONG).show();
                }
            });
            v.setText("Start Collection!");
        } else {
            mRecordingThread.startRecording();
            isCapturingData = true;
            v.setTextSize(40);
        }
    }
    
    public void setFrequency(View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordingThread.centerFrequency = Integer.parseInt(setFreq.getText().toString());
                layout.requestFocus();
            }
        });

    }

}
