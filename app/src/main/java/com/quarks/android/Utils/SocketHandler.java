package com.quarks.android.Utils;

import io.socket.client.Socket;

public class SocketHandler {
    private static Socket socket;

    public static Socket getSocket(){
        return socket;
    }

    public static void setSocket(Socket socket){
        SocketHandler.socket = socket;
    }

    public static void cleanSocket(){
        SocketHandler.socket = null;
    }
}
