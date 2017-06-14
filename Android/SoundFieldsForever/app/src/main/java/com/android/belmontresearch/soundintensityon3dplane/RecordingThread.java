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
    private short[] audioBuffer;
    private short[] audioBuffer2;
    private double rms = 0;
    private double mAlpha;
    private double mGain;
    private double mRmsSmoothed;
    private double rmsdB;
    public BiQuad biquad;
    private short[] y;

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
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        biquad = new BiQuad();

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
        Log.i(LOG_TAG, audioBuffer.length + "");

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            shortsRead += numberOfShort;

            audioBuffer2 = biquad.bqfilter(audioBuffer, audioBuffer2, SAMPLE_RATE, 1000, 1.414f);
            audioBuffer2 = biquad.bqfilter(audioBuffer2, audioBuffer2, SAMPLE_RATE, 1000, 5);

            /*
             * Noise level meter begins here
             */
            // Compute the RMS value. (Note that this does not remove DC).
            for (int i = 0; i < audioBuffer2.length; i++) {
                rms += audioBuffer2[i] * audioBuffer2[i];
            }
            rms = Math.sqrt(rms / audioBuffer2.length);
            mAlpha = 1;   mGain = 1.0/32767; //0.0044;
            /*Compute a smoothed version for less flickering of the
            // display.*/
//            mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * rms;
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
