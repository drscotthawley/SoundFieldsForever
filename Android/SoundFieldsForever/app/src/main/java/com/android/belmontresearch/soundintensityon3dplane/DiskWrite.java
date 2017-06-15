package com.android.belmontresearch.soundintensityon3dplane;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by sebastianalegre on 6/15/17.
 */

public class DiskWrite {

    static File file = new File(getAlbumStorageDir(), "raw_data.txt");

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "raw_data");
        if (!directory.mkdirs()) {
        }

        return directory;
    }

    public static boolean writeToDisk(final Context context, PointTimeData currentNode) {

        FileOutputStream fop = null;

        try {
            fop = new FileOutputStream(file);

            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String content = "";




            while (currentNode.getNextNode() != null) {
                content += currentNode.toString() + System.lineSeparator();
                currentNode = currentNode.getNextNode();
            }
            content += currentNode.toString();

            byte[] contentInBytes = content.getBytes();

//            PrintWriter pOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
//                    .getOutputStream())), true);
//            try {
//                if(socket != null) {
//                    pOut.print(contentInBytes);
//                    pOut.flush();
//                    DOS.write(contentInBytes);
//                    DOS.flush();
//                    Log.i(TAG, "Wrote to server");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

            return true;

        } catch (final IOException e) {
            return false;
        }
    }
}
