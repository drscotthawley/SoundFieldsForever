package com.android.belmontresearch.soundintensityon3dplane;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

/**
 * Created by sebastianalegre on 6/13/17.
 * Object that handles audio
 */

public class RecordingThread {
    private static final String LOG_TAG = RecordingThread.class.getSimpleName();
    private static final int SAMPLE_RATE = 22050;
    public int centerFrequency = 1000;
    private short[] audioBuffer;
    private float[] audioBufferX;
    private float[] audioBuffer0;
    private float[] audioBuffer1;
    private float[] audioBuffer2;
    private float[] audioBuffer3;
    private float[] audioBuffer4;
    private float[] audioBuffer5;
    private float[] audioBuffer6;
    private double rms;
    private double rmsX;
    private double rms0;
    private double rms1;
    private double rms2;
    private double rms3;
    private double rms4;
    private double rms5;
    private double rms6;
    private double mGain;
    private double rmsdB;
    private double rmsdBX;
    private double rmsdB0;
    private double rmsdB1;
    private double rmsdB2;
    private double rmsdB3;
    private double rmsdB4;
    private double rmsdB5;
    private double rmsdB6;


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
        rms = 0;
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        audioBuffer = new short[bufferSize / 2];
        audioBufferX = new float[bufferSize / 2];
        audioBuffer0 = new float[bufferSize / 2];
        audioBuffer1 = new float[bufferSize / 2];
        audioBuffer2 = new float[bufferSize / 2];
        audioBuffer3 = new float[bufferSize / 2];
        audioBuffer4 = new float[bufferSize / 2];
        audioBuffer5 = new float[bufferSize / 2];
        audioBuffer6 = new float[bufferSize / 2];

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

            /*
             * Noise level meter begins here
             */
            // Compute the RMS value. (Note that this does not remove DC).
            for (int i = 2; i < audioBuffer.length; i++) {

                audioBufferX[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBufferX[i], audioBufferX[i - 1], audioBufferX[i - 2], SAMPLE_RATE, centerFrequency, 30);
                audioBuffer0[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBuffer0[i], audioBuffer0[i - 1], audioBuffer0[i - 2], SAMPLE_RATE, 63, 30);
                audioBuffer1[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBuffer1[i], audioBuffer1[i - 1], audioBuffer1[i - 2], SAMPLE_RATE, 125, 30);
                audioBuffer2[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBuffer2[i], audioBuffer2[i - 1], audioBuffer2[i - 2], SAMPLE_RATE, 250, 30);
                audioBuffer3[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBuffer3[i], audioBuffer3[i - 1], audioBuffer3[i - 2], SAMPLE_RATE, 500, 30);
                audioBuffer4[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBuffer4[i], audioBuffer4[i - 1], audioBuffer4[i - 2], SAMPLE_RATE, 1000, 30);
                audioBuffer5[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBuffer5[i], audioBuffer5[i - 1], audioBuffer5[i - 2], SAMPLE_RATE, 2000, 30);
                audioBuffer6[i] = BiQuad.bqfilter(audioBuffer[i], audioBuffer[i - 1], audioBuffer[i - 2], audioBuffer6[i], audioBuffer6[i - 1], audioBuffer6[i - 2], SAMPLE_RATE, 4000, 30);

                rms += audioBuffer[i] * audioBuffer[i];
                rmsX += audioBufferX[i] * audioBufferX[i];
                rms0 += audioBuffer0[i] * audioBuffer0[i];
                rms1 += audioBuffer1[i] * audioBuffer1[i];
                rms2 += audioBuffer2[i] * audioBuffer2[i];
                rms3 += audioBuffer3[i] * audioBuffer3[i];
                rms4 += audioBuffer4[i] * audioBuffer4[i];
                rms5 += audioBuffer5[i] * audioBuffer5[i];
                rms6 += audioBuffer6[i] * audioBuffer6[i];
            }
            rms = Math.sqrt(rms / (audioBuffer.length-2));
            rmsX = Math.sqrt(rmsX / (audioBuffer.length-2));
            rms0 = Math.sqrt(rms0 / (audioBuffer.length-2));
            rms1 = Math.sqrt(rms1 / (audioBuffer.length-2));
            rms2 = Math.sqrt(rms2 / (audioBuffer.length-2));
            rms3 = Math.sqrt(rms3 / (audioBuffer.length-2));
            rms4 = Math.sqrt(rms4 / (audioBuffer.length-2));
            rms5 = Math.sqrt(rms5 / (audioBuffer.length-2));
            rms6 = Math.sqrt(rms6 / (audioBuffer.length-2));

            mGain = 1.0/32767;
            float cal0 = 114.71f;
            float calSlope = 0.9818f;
            float cal1 = -20.2f;
            float calSlope1 = 0.9356f;

            rmsdB = calSlope * 20.0 * Math.log10(mGain * rms) + cal0;
            rmsdBX = (calSlope * 20.0 * Math.log10(mGain * rmsX) + cal0);
            rmsdB0 = (calSlope * 20.0 * Math.log10(mGain * rms0) + cal0);
            rmsdB1 = (calSlope * 20.0 * Math.log10(mGain * rms1) + cal0);
            rmsdB2 = (calSlope * 20.0 * Math.log10(mGain * rms2) + cal0);
            rmsdB3 = (calSlope * 20.0 * Math.log10(mGain * rms3) + cal0);
            rmsdB4 = (calSlope * 20.0 * Math.log10(mGain * rms4) + cal0);
            rmsdB5 = (calSlope * 20.0 * Math.log10(mGain * rms5) + cal0);
            rmsdB6 = (calSlope * 20.0 * Math.log10(mGain * rms6) + cal0);
        }

        record.stop();
        record.release();

        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }

    public double getRmsdB() {
        return rmsdB;
    }

    public double getRmsdBX() {
        return rmsdBX;
    }

    public double[] getRmsdBFiltered() {
        double[] rmsdBF = {rmsdB0, rmsdB1, rmsdB2, rmsdB3, rmsdB4, rmsdB5, rmsdB6};
        return rmsdBF;
    }
}
