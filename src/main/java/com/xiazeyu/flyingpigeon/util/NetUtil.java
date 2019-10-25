package com.xiazeyu.flyingpigeon.util;

import com.xiazeyu.flyingpigeon.bean.NetworkAdapter;
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

@Slf4j
public class NetUtil {

    // 命令行下原始数据
    private List<String> originalContext;

    // 所有的网络适配器
    private List<NetworkAdapter> networkAdapters;

    // 有效的网络适配器
    private List<NetworkAdapter> usableNetworkAdapters;

    // 网段下所有存在的ip地址
    private List<String> sameNetIps;

    // 可以ping通的ip地址
    private Map<String, String> ipMap;

    public List<NetworkAdapter> searchNetworkAdapters() {
        originalContext = new ArrayList<>();
        networkAdapters = new ArrayList<>();
        String command;
        String charsetName;
        if (SystemUtil.isWindows()) {
            command = SystemUtil.NET_INFO_WIN;
            charsetName = SystemUtil.CHARSET_WIN;
        } else {
            command = SystemUtil.NET_INFO_LINUX;
            charsetName = SystemUtil.CHARSET_LINUX;
        }
        Runtime runtime = Runtime.getRuntime();
        try {
            Process exec = runtime.exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(exec.getInputStream(), charsetName));
            String line;
            while ((line = br.readLine()) != null) {
                originalContext.add(line);
                if (line.contains("默认网关")) {
                    int lastIndex = originalContext.size() - 1;
                    NetworkAdapter networkAdapter = new NetworkAdapter();
                    String gateway = line.substring(line.indexOf(":") + 1).trim();
                    networkAdapter.setGateway(gateway);
                    line = originalContext.get(lastIndex - 1);
                    String mask = line.substring(line.indexOf(":") + 1).trim();
                    networkAdapter.setMask(mask);
                    line = originalContext.get(lastIndex - 2);
                    String ipv4 = line.substring(line.indexOf(":") + 1).trim();
                    networkAdapter.setIpv4(ipv4);
                    line = originalContext.get(lastIndex - 3);
                    String ipv6 = line.substring(line.indexOf(":") + 1).trim();
                    networkAdapter.setIpv6(ipv6);
                    line = originalContext.get(lastIndex - 4);
                    String dnsSuffix = line.substring(line.indexOf(":") + 1).trim();
                    networkAdapter.setDnsSuffix(dnsSuffix);
                    line = originalContext.get(lastIndex - 6);
                    String name = line.substring(0, line.indexOf(":")).trim();
                    networkAdapter.setName(name);
                    networkAdapters.add(networkAdapter);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (log.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder();
            for (String context : originalContext) {
                builder.append(context).append("\n");
            }
            log.debug(builder.toString());
        }
        return networkAdapters;
    }

    public List<NetworkAdapter> searchUsableNetworkAdapters() {
        if (networkAdapters == null) {
            searchNetworkAdapters();
        }
        usableNetworkAdapters = new ArrayList<>();
        for (NetworkAdapter networkAdapter : networkAdapters) {
            if (networkAdapter.isUsable()) {
                usableNetworkAdapters.add(networkAdapter);
            }
        }
        return usableNetworkAdapters;
    }

    public List<String> searchSameNetIps() {
        if (usableNetworkAdapters == null) {
            searchUsableNetworkAdapters();
        }
        sameNetIps = new ArrayList<>();
        for (NetworkAdapter usableNetworkAdapter : usableNetworkAdapters) {
            sameNetIps.addAll(usableNetworkAdapter.calcSameNetworkSegmentIPs());
        }
        return sameNetIps;
    }

    public Map<String, String> searchUsableIps() {
        if (sameNetIps == null) {
            searchSameNetIps();
        }
        ipMap = new ConcurrentHashMap<>();
        final CountDownLatch latch = new CountDownLatch(sameNetIps.size());
        for (String ip : sameNetIps) {
            ThreadPoolUtil.executor.execute(new NetUtil.PingTask(ip, latch));
        }
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.debug(e.getMessage());
        }
        return ipMap;
    }

    private class PingTask implements Runnable {

        private String targetIp;

        private CountDownLatch latch;

        public PingTask(String targetIp, CountDownLatch latch) {
            this.targetIp = targetIp;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                InetAddress addr = InetAddress.getByName(targetIp);
                if (addr.isReachable(5000)) {
                    ipMap.put(targetIp, "");
                }
            } catch (IOException e) {
                log.debug("permission denied <{}>", targetIp);
            } finally {
                latch.countDown();
            }
        }
    }

}
