package com.xiazeyu.flyingpigeon.bean;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class NetworkAdapter {

    private String name;

    private String dnsSuffix;

    private String ipv6;

    private String ipv4;

    private String mask;

    private String gateway;

    public boolean isUsable() {
        return gateway != null && gateway.length() != 0;
    }

    public List<String> calcSameNetworkSegmentIPs() {
        LinkedList<String> ips = new LinkedList<>();
        long binaryGateway = calcBinaryGateway();
        long binaryMask = calcBinaryMask();
        long netAddress = binaryGateway & binaryMask;
        long curAddress = netAddress;
        while (true) {
            curAddress++;
            if ((curAddress & binaryMask) == netAddress) {
                ips.add(translateToIp(curAddress));
            } else {
                break;
            }
        }
        ips.removeLast();
        return ips;
    }

    private long calcBinaryGateway() {
        return stringToBinary(gateway);
    }

    private long calcBinaryMask() {
        return stringToBinary(mask);
    }

    private long stringToBinary(String value) {
        String[] values = value.split("[.]");
        long[] temps = new long[4];
        temps[0] = Long.valueOf(values[0]) * 256 * 256 * 256;
        temps[1] = Long.valueOf(values[1]) * 256 * 256;
        temps[2] = Long.valueOf(values[2]) * 256;
        temps[3] = Long.valueOf(values[3]);
        return temps[0] + temps[1] + temps[2] + temps[3];
    }

    private String translateToIp(long address) {
        String[] values = new String[4];
        values[3] = String.valueOf(address % 256);
        values[2] = String.valueOf(address / 256 % 256);
        values[1] = String.valueOf(address / 256 / 256 % 256);
        values[0] = String.valueOf(address / 256 / 256 / 256);
        return values[0] + "." + values[1] + "." + values[2] + "." + values[3];
    }

}
