package com.android.belmontresearch.soundintensityon3dplane;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by sebastianalegre on 6/15/17.
 */

public class DiskWrite {

    static File file = new File(getAlbumStorageDir(), "raw_data.txt");
    static Socket socket;

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

        FileOutputStream fop;

        currentNode = currentNode.nextNode;

        try {
            fop = new FileOutputStream(file);

            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String content = "x,y,z,dB" + System.lineSeparator();

            while (currentNode.getNextNode() != null) {
                content += currentNode.toString() + System.lineSeparator();
                currentNode = currentNode.getNextNode();
            }
            content += currentNode.toString();
            byte[] contentInBytes = content.getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

            try {
                connectWebSocket(content);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            return true;

        } catch (final IOException e) {
            return false;
        }
    }

    private static void connectWebSocket(final String content) throws URISyntaxException {
        socket = IO.socket("http://hedges.belmont.edu:3000/");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                socket.emit("chat message", content);
                socket.disconnect();
            }

        }).on("event", new Emitter.Listener() {

            @Override
            public void call(Object... args) {}

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {}

        });
        socket.connect();
    }


}
