package com.xiazeyu.flyingpigeon.controller;

import com.xiazeyu.flyingpigeon.bean.InternalNode;
import com.xiazeyu.flyingpigeon.bean.SocketParam;
import com.xiazeyu.flyingpigeon.socket.SocketClient;
import com.xiazeyu.flyingpigeon.socket.SocketServer;
import com.xiazeyu.flyingpigeon.util.NetUtil;
import com.xiazeyu.flyingpigeon.util.ThreadPoolUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class SocketController {

    @Value("${flyingpigeon.port}")
    private String port;

    @RequestMapping("/searchUsableNode")
    public List<InternalNode> searchUsableNode() {
        if (ThreadPoolUtil.search_running_flag) {
            throw new RuntimeException("正在运行中，请稍后");
        }
        ThreadPoolUtil.search_running_flag = true;
        List<InternalNode> result = new ArrayList<>();
        NetUtil netUtil = new NetUtil();
        List<String> gateWays = netUtil.getGateWays();
        for (String gateWay : gateWays) {
            Map<String, String> ips = netUtil.getIps(gateWay);
            for (String ip : ips.keySet()) {
                System.out.println(ip);
                SocketClient socketClient = new SocketClient(ip, Integer.valueOf(port), null, 1);
                if (socketClient.deal(socketClient.new SocketClientHandler())) {
                    result.add(new InternalNode(ip));
                }
            }
        }
        ThreadPoolUtil.search_running_flag = false;
        return result;
    }

    @RequestMapping("/open")
    public String openSelfNode(SocketParam param) {
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
        SocketClient socketClient = new SocketClient(param.getIp(), Integer.valueOf(port), param.getInPath(), 10);
        socketClient.deal(socketClient.new FileSocketClientHandler());
        return "over";
    }

}
