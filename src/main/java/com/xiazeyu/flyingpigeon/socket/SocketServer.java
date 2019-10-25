package com.xiazeyu.flyingpigeon.socket;

import com.xiazeyu.flyingpigeon.socket.handler.BaseSocketHandler;
import com.xiazeyu.flyingpigeon.util.ThreadPoolUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SocketServer extends Thread {

    private Map<Long, String> threadIdPool = new ConcurrentHashMap<>();

    private static int defaultTimeout = 2 * 60 * 1000;

    private static String defaultRootPath = "./";

    private String rootPath;

    private int port;

    private int timeout;

    private byte[] bytes;

    private ServerSocket serverSocket;

    public SocketServer(String rootPath, int port) {
        this(rootPath, port, defaultTimeout);
    }

    public SocketServer(String rootPath, int port, int timeout) {
        if (rootPath == null) {
            rootPath = defaultRootPath;
        }
        rootPath = rootPath.replace("\\", "/");
        if (!rootPath.endsWith("/")) {
            rootPath = rootPath + "/";
        }
        this.rootPath = rootPath;
        this.port = port;
        this.timeout = timeout;
        this.bytes = new byte[4096];
        init();
    }

    public void init() {
        try {
            serverSocket = new ServerSocket(port);
            if (timeout > 0) {
                serverSocket.setSoTimeout(timeout);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (true) {
            Socket socket = null;
            try {
                log.info("服务器启动完成");
                // 一旦有堵塞, 则表示服务器与客户端获得了连接
                socket = serverSocket.accept();
                // 处理这次连接
                SocketServerHandler socketServerHandler = new SocketServerHandler(socket);
                Thread thread = new Thread(socketServerHandler);
                thread.start();
                long id = thread.getId();
                socketServerHandler.setThreadId(id);
                threadIdPool.put(id, "running");
            } catch (Exception e) {
                log.debug(e.getMessage(), e);
                if (threadIdPool.isEmpty()) {
                    break;
                }
            }
        }
        ThreadPoolUtil.server_running_flag = false;
    }

    @Setter
    @Getter
    public class SocketServerHandler extends BaseSocketHandler implements Runnable {

        private long threadId;

        public SocketServerHandler(Socket socket) {
            super(socket);
        }

        @Override
        public void run() {
            execute();
            threadIdPool.remove(threadId);
        }

        @Override
        public boolean handler(DataInputStream input, DataOutputStream output) throws IOException {
            boolean resultFlag = false;
            FileOutputStream fos = null;
            try {
                String firstSignal = input.readUTF();
                log.info("第一个信号，内容[{}]", firstSignal);
                if ("areyouliving".equals(firstSignal)) {
                    output.writeUTF("iamliving");
                    resultFlag = true;
                } else if ("canreceivefile".equals(firstSignal)) {
                    output.writeUTF("ican");
                    String fileName = input.readUTF();
                    log.info("文件名称，内容[{}]", fileName);
                    output.writeUTF("iamready");
                    fos = new FileOutputStream(rootPath + fileName);
                    int len;
                    do {
                        len = input.read(bytes);
                        fos.write(bytes, 0, len);
                        fos.flush();
                    } while (len == bytes.length);
                    resultFlag = true;
                } else {
                    log.info("无法处理的信号，内容[{}]", firstSignal);
                }
            } catch (IOException e) {
                throw e;
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        log.debug(e.getMessage(), e);
                    }
                }
            }
            return resultFlag;
        }
    }

}