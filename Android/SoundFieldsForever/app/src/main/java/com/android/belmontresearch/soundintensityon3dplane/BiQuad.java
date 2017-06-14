package com.android.belmontresearch.soundintensityon3dplane;

/**
 * Created by sebastianalegre on 6/12/17.
 */

public class BiQuad {

    double a0, a1, a2, b0, b1, b2;

    public BiQuad() {
    }

    public double[] filtercoeffs(float f0, float Fs, float Q) {
//      f0 = center freq of filter
//      Fs = sample rate
//      Q = Q of filter

//      Using DSP EQ Cookbook for:
        double pi = 3.1415926525897;
        double w0 = 2 * pi * f0 / Fs;
        double alpha = Math.sin(w0) / (2 * Q);

        int filter_choice = 0;
//      BPF:H(s) = (s / Q) / (s ^ 2 + s / Q + 1) (constant 0dB peakgain)
        
        b0 = alpha;
        b1 = 0;
        b2 = -alpha;
        a0 = 1 + alpha;
        a1 = -2 * Math.cos(w0);
        a2 = 1 - alpha;

        if (filter_choice == 1) {
//            BPF:H(s) = s / (s ^ 2 + s / Q + 1) (constant skirt gain, peak gain = Q)
            b0 = Q * alpha;
            b2 = -Q * alpha;
        }

        double coeffs[] = {a0, a1, a2, b0, b1, b2};
        return coeffs;
    }

    public short[] bqfilter(short[] x,float Fs, float f0, float Q) {
//            #Biquad IIRfilter
//    #x =input(buffer of audio signal)
//    #y =output(including previous values of output)
//    #Fs =sample rate
//    n = currenttime step

        double coeffs[] = filtercoeffs(f0, Fs, Q);
        a0 = coeffs[0];
        a1 = coeffs[1];
        a2 = coeffs[2];
        b0 = coeffs[3];
        b1 = coeffs[4];
        b2 = coeffs[5];

        short y[] = new short[x.length];
//        # initialize y with x
        y[0] = 0;
        y[1] = 0;
        for(int i=2; i<x.length; i++) {
            y[i] = (short)((b0 / a0) * x[i] + (b1 / a0) * x[i - 1] + (b2 / a0) * x[i - 2] - (a1 / a0) * y[i - 1] - (a2 / a0) * y[i - 2]);
        }
        return y;

    }


}

