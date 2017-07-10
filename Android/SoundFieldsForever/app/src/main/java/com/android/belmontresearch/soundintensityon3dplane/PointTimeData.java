package com.android.belmontresearch.soundintensityon3dplane;

/**
 * Created by aetstudent on 6/6/17.
 */

public class PointTimeData {
    float x;
    float y;
    float z;
    double db;
    double[] dbf;
    PointTimeData nextNode;

    public PointTimeData(float[] xyz, double soundLevel, double[] soundLevelFiltered) {
        if(xyz != null) {
            x = xyz[0];
            y = xyz[1];
            z = xyz[2];
        }

//        db = 20 * Math.log10(soundLevel / 32767);
        db = soundLevel;
        dbf = soundLevelFiltered;
    }

    public PointTimeData(String load) {
        String[] s = load.split(",");
        x = Float.parseFloat(s[0].split(":")[1]);
        y = Float.parseFloat(s[1].split(":")[1]);
        z = Float.parseFloat(s[2].split(":")[1]);
        db = Float.parseFloat(s[3].split(":")[1]);
    }

    public void setNextNode(PointTimeData node) {
        nextNode = node;
    }

    public PointTimeData getNextNode() {
        return nextNode;
    }

    public double getDb() {
        return db;
    }

    public String toString() {
        return x + "," + y + "," + z + "," + Math.round(db) + "," + Math.round(dbf[0]) + "," + Math.round(dbf[1]) + "," + Math.round(dbf[2]) + "," + Math.round(dbf[3]) + "," + Math.round(dbf[4]) + "," + Math.round(dbf[5]) + "," + Math.round(dbf[6]);
    }

}
