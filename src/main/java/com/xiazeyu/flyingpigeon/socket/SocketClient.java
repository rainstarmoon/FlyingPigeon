package com.xiazeyu.flyingpigeon.socket;

import com.xiazeyu.flyingpigeon.socket.handler.BaseSocketHandler;
import com.xiazeyu.flyingpigeon.socket.handler.SocketHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

@Slf4j
public class SocketClient {

    private static int defaultTimeout = 2000;

    private String ip;

    private int port;

    private String filePath;

    private String fileName;

    private int connectionNum;

    private byte[] bytes;

    public SocketClient(String ip, int port, String filePath, int connectionNum) {
        this.ip = ip;
        this.port = port;
        if (filePath == null) {
            filePath = "";
        }
        filePath = filePath.replace("\\", "/");
        this.filePath = filePath;
        this.fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        this.connectionNum = connectionNum;
        this.bytes = new byte[4096];
    }

    public boolean deal(SocketHandler socketHandler) {
        boolean isNormal = false;
        Socket socket = null;
        while (connectionNum-- > 0) {
            try {
                //创建一个流套接字并将其连接到指定主机上的指定端口号
                socket = new Socket();

                //不是连接超时的功能
                //socket = new Socket(ip, port);
                //socket.setSoTimeout(1000);

                SocketAddress endpoint = new InetSocketAddress(ip, port);
                socket.connect(endpoint, defaultTimeout);

                log.info("客户端连接完成");

                socketHandler.setSocket(socket);
                isNormal = socketHandler.execute();
                break;
            } catch (Exception e) {
                log.debug(e.getMessage() + "-----客户端剩余重试次数" + connectionNum);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                    }
                }
            }
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        return isNormal;
    }

    public class SocketClientHandler extends BaseSocketHandler {

        public SocketClientHandler() {

        }

        public SocketClientHandler(Socket socket) {
            super(socket);
        }

        @Override
        public boolean handler(DataInputStream input, DataOutputStream output) throws IOException {
            output.writeUTF("areyouliving");
            String secondSignal = input.readUTF();
            log.info("第二个信号，内容[{}]", secondSignal);
            if ("iamliving".equals(secondSignal)) {
                return true;
            }
            return false;
        }
    }

    public class FileSocketClientHandler extends BaseSocketHandler {

        public FileSocketClientHandler() {

        }

        public FileSocketClientHandler(Socket socket) {
            super(socket);
        }

        @Override
        public boolean handler(DataInputStream input, DataOutputStream output) throws IOException {
            output.writeUTF("canreceivefile");
            String secondSignal = input.readUTF();
            log.info("第二个信号，内容[{}]", secondSignal);
            if (!"ican".equals(secondSignal)) {
                return false;
            }
            output.writeUTF(fileName);
            String thirdSignal = input.readUTF();
            log.info("第三个信号，内容[{}]", thirdSignal);
            if (!"iamready".equals(thirdSignal)) {
                return false;
            }
            FileInputStream fis = new FileInputStream(filePath);
            int len;
            do {
                len = fis.read(bytes);
                output.write(bytes, 0, len);
                output.flush();
            } while (len == bytes.length);
            return true;
        }
    }

}
