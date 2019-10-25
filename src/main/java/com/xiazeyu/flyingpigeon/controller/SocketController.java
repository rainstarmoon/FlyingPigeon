package com.xiazeyu.flyingpigeon.controller;

import com.xiazeyu.flyingpigeon.bean.InternalNode;
import com.xiazeyu.flyingpigeon.bean.SocketParam;
import com.xiazeyu.flyingpigeon.socket.SocketClient;
import com.xiazeyu.flyingpigeon.socket.SocketServer;
import com.xiazeyu.flyingpigeon.util.NetUtil;
import com.xiazeyu.flyingpigeon.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class SocketController {

    @Value("${flyingpigeon.port}")
    private String port;

    @RequestMapping("/searchUsableNode")
    public Set<InternalNode> searchUsableNode() {
        log.info("收到请求 /searchUsableNode ");
        long beg = System.currentTimeMillis();
        if (ThreadPoolUtil.search_running_flag) {
            throw new RuntimeException("正在运行中，请稍后");
        }
        ThreadPoolUtil.search_running_flag = true;
        Map<InternalNode, String> result = new ConcurrentHashMap<>();
        NetUtil netUtil = new NetUtil();
        Map<String, String> usableIps = netUtil.searchUsableIps();
        final CountDownLatch latch = new CountDownLatch(usableIps.size());
        for (String ip : usableIps.keySet()) {
            ThreadPoolUtil.executor.execute(() -> {
                SocketClient socketClient = new SocketClient(ip, Integer.valueOf(port), null, 1);
                if (socketClient.deal(socketClient.new SocketClientHandler())) {
                    result.put(new InternalNode(ip), "");
                }
                latch.countDown();
            });
        }
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.debug(e.getMessage());
        }
        ThreadPoolUtil.search_running_flag = false;
        long end = System.currentTimeMillis();
        log.info("searchUsableNode一次执行时间；{}秒", (end - beg) / 1000);
        return result.keySet();
    }

    @RequestMapping("/open")
    public String openSelfNode(SocketParam param) {
        log.info("收到请求 /open 参数为<{}>", param);
        if (!ThreadPoolUtil.server_running_flag) {
            ThreadPoolUtil.server_running_flag = true;
            new SocketServer(param.getOutPath(), Integer.valueOf(port)).start();
            return "success";
        } else {
            return "fail";
        }
    }

    @RequestMapping("/send")
    public String sendFileToNode(SocketParam param) {
        log.info("收到请求 /send 参数为<{}>", param);
        SocketClient socketClient = new SocketClient(param.getIp(), Integer.valueOf(port), param.getInPath(), 10);
        socketClient.deal(socketClient.new FileSocketClientHandler());
        return "over";
    }


}
