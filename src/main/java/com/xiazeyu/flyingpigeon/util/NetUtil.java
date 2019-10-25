package com.xiazeyu.flyingpigeon.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.xiazeyu.flyingpigeon.util.ThreadPoolUtil.THREAD_NUM;

@Slf4j
public class NetUtil {

    private static final String CHARSET_NAME = "GBK";

    private static final String IP_CONFIG = "ipconfig";

    private List<String> originalContext = new ArrayList<>();

    private List<String> gateways = new ArrayList<>();

    private Map<String, String> ips = new ConcurrentHashMap<>();

    public List<String> getGateWays() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process exec = runtime.exec(IP_CONFIG);
            BufferedReader br = new BufferedReader(new InputStreamReader(exec.getInputStream(), CHARSET_NAME));
            String line;
            while ((line = br.readLine()) != null) {
                originalContext.add(line);
                if (line.contains("默认网关")) {
                    line = line.substring(line.lastIndexOf(":") + 1).trim();
                    if (!"".equals(line)) {
                        gateways.add(line);
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return gateways;
    }

    public Map<String, String> getIps(String gatewayAddress) {
        startPing(gatewayAddress);
        return ips;
    }


    private void startPing(String gatewayAddress) {
        if (gatewayAddress == null) {
            return;
        }
        final CountDownLatch latch = new CountDownLatch(THREAD_NUM);
        for (int i = 0; i < THREAD_NUM; i++) {
            ThreadPoolUtil.executor.execute(new NetUtil.PingTask(gatewayAddress, i, latch));
        }
        try {
            latch.await(6, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private class PingTask implements Runnable {

        private String targetAddress;

        private CountDownLatch latch;

        public PingTask(String gatewayAddress, int lastAddress, CountDownLatch latch) {
            this.targetAddress = gatewayAddress.substring(0, gatewayAddress.lastIndexOf(".") + 1) + lastAddress;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                InetAddress addr = InetAddress.getByName(targetAddress);
                if (addr.isReachable(5000)) {
                    ips.put(targetAddress, "");
                }
            } catch (IOException e) {
                //log.error("permission denied <{}>", targetAddress);
            } finally {
                latch.countDown();
            }
        }
    }

}
