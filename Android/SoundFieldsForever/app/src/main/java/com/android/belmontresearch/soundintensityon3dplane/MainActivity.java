package com.android.belmontresearch.soundintensityon3dplane;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
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

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
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
    private TextView zValue;
    private TextView bandPassF;
    private Button vButton;
    private View root;
    private View someView;

    private RecordingThread mRecordingThread;

    private static WebSocketClient mWebSocketClient;

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

        xValue = (TextView) findViewById(R.id.textView_xValue);
        yValue = (TextView) findViewById(R.id.textView_yValue);
        zValue = (TextView) findViewById(R.id.textView_zValue);
        vButton = (Button) findViewById(R.id.button_collectionState);
        bandPassF = (TextView) findViewById(R.id.textView_bandpassF);
        SeekBar frequencySeek = (SeekBar) findViewById(R.id.seekBar_frequency);
        frequencySeek.setOnSeekBarChangeListener(onProgressChanged);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

    }

    private SeekBar.OnSeekBarChangeListener onProgressChanged =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    bandPassF.setText(progress + "");
                    mRecordingThread.centerFrequency = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };

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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Setting Frames",
                        Toast.LENGTH_LONG).show();
            }
        });

        someView = findViewById(R.id.constraintLayout);


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
                    root = someView.getRootView();

                    // Gets rms value for microphone input
                    double rms = mRecordingThread.getRmsdB();

                    if(isCapturingData) {
                        if (rms > -9000) {
                            mIsTangoPoseReady.compareAndSet(false, true);
                            final float[] xyz = timePose.getTranslationAsFloats();

                            final PointTimeData newNode = new PointTimeData(xyz, rms);
                            currentNode.setNextNode(newNode);
                            currentNode = currentNode.getNextNode();

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);

                                    //stuff that updates ui
                                    xValue.setText(String.valueOf(xyz[0]));
                                    yValue.setText(String.valueOf(xyz[1]));
                                    zValue.setText(String.valueOf(xyz[2]));

                                    int dbInInt = (int) Math.round(newNode.getDb());
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
            root.setBackgroundColor(Color.parseColor("#f44242"));
        } else if (dbInInt > -10) {
            root.setBackgroundColor(Color.parseColor("#f4417a"));
        } else if (dbInInt > -25) {
            root.setBackgroundColor(Color.parseColor("#f4419a"));
        } else if (dbInInt > -30) {
            root.setBackgroundColor(Color.parseColor("#f441df"));
        } else if (dbInInt > -45) {
            root.setBackgroundColor(Color.parseColor("#d041f4"));
        } else if (dbInInt > -60) {
            root.setBackgroundColor(Color.parseColor("#b541f4"));
        } else if (dbInInt > -85) {
            root.setBackgroundColor(Color.parseColor("#8841f4"));
        } else if (dbInInt > -100) {
            root.setBackgroundColor(Color.parseColor("#6d41f4"));
        } else if (dbInInt > -115) {
            root.setBackgroundColor(Color.parseColor("#4143f4"));
        } else if (dbInInt > -130) {
            root.setBackgroundColor(Color.parseColor("#283396"));
        } else {
            root.setBackgroundColor(Color.parseColor("#1b236d"));
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
            root.setBackgroundColor(Color.parseColor("#ffffff"));

            // Connects the WebSocket
//            connectWebSocket();

            boolean written = DiskWrite.writeToDisk(MainActivity.this, nodeListStart, mWebSocketClient);
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

    public void connectWebSocket() {
        URI uri;
        try {
            uri = new URI("wss://sandbox.kaazing.net/echo");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.e(TAG, e + "");
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }

}
