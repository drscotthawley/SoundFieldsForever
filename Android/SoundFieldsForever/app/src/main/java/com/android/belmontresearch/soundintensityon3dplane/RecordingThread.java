package com.android.belmontresearch.soundintensityon3dplane;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by sebastianalegre on 6/13/17.
 */

public class RecordingThread {
    private static final String LOG_TAG = RecordingThread.class.getSimpleName();
    private static final int SAMPLE_RATE = 44100;
    public float centerFrequency = 5000;
    private short[] audioBuffer;
    private short[] audioBuffer2;
    private double mGain;
    private double rmsdB;

    public RecordingThread(AudioDataReceivedListener listener) {
        mListener = listener;
    }

    private boolean mShouldContinue;
    private AudioDataReceivedListener mListener;
    private Thread mThread;

    public boolean recording() {
        return mThread != null;
    }

    public void startRecording() {
        if (mThread != null)
            return;

        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        mThread.start();
    }

    public void stopRecording() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    private void record() {
        Log.v(LOG_TAG, "Start");
        double rms = 0;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        audioBuffer = new short[bufferSize / 2];
        audioBuffer2 = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            shortsRead += numberOfShort;

//            audioBuffer2 = BiQuad.bqfilter(audioBuffer, audioBuffer2, SAMPLE_RATE, centerFrequency, 5);
//            audioBuffer2 = BiQuad.bqfilter(audioBuffer2, audioBuffer2, SAMPLE_RATE, centerFrequency, 5);

            /*
             * Noise level meter begins here
             */
            // Compute the RMS value. (Note that this does not remove DC).
            for (int i = 2; i < audioBuffer2.length; i++) {
                if(i % 3 == 0) {
                    audioBuffer2[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBuffer2[i], audioBuffer2[i - 1], audioBuffer2[i - 2], SAMPLE_RATE, centerFrequency, 40);
                    audioBuffer2[i] = BiQuad.bqfilter(audioBuffer2[i], audioBuffer2[i - 1], audioBuffer2[i - 2], audioBuffer2[i], audioBuffer2[i - 1], audioBuffer2[i - 2], SAMPLE_RATE, centerFrequency, 40);
                    rms += audioBuffer2[i] * audioBuffer2[i];
                }
            }
            rms = Math.sqrt(rms / audioBuffer2.length);
            mGain = 1.0/32767; //0.0044;
            rmsdB = 20.0 * Math.log10(mGain * rms);
        }

        record.stop();
        record.release();

        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }

    public short[] getAudioBuffer() {
        return audioBuffer;
    }

    public double getRmsdB() {
        return rmsdB;
    }
}
