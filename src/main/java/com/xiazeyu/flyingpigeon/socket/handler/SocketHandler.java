package com.xiazeyu.flyingpigeon.socket.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public interface SocketHandler {

    void setSocket(Socket socket);

    boolean execute();

    boolean handler(DataInputStream input, DataOutputStream output) throws IOException;

}
