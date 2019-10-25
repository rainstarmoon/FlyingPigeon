package com.xiazeyu.flyingpigeon.socket.handler;

import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@Slf4j
public abstract class BaseSocketHandler implements SocketHandler {

    private Socket socket;

    public BaseSocketHandler() {
    }

    public BaseSocketHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public boolean execute() {
        boolean resultFlag = false;
        DataInputStream input = null;
        DataOutputStream output = null;
        try {
            // 读取客户端数据
            input = new DataInputStream(socket.getInputStream());
            // 向客户端回复信息
            output = new DataOutputStream(socket.getOutputStream());
            resultFlag = handler(input, output);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
        return resultFlag;
    }

}
