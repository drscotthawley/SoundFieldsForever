package com.android.belmontresearch.soundintensityon3dplane;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by sebastianalegre on 6/15/17.
 */

public class DiskWrite {

    static Socket socket;

    public static boolean writeToDisk(String content, String socketName) {

        try {
            connectWebSocket(content, socketName);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return true;
    }

    private static void connectWebSocket(final String content, String socketName) throws URISyntaxException {
        socket = IO.socket("http://" + socketName);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                socket.emit("tango data", content);
                socket.disconnect();
            }

        }).on("event", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
            }

        });
        socket.connect();
    }


}
