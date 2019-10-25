package com.xiazeyu.flyingpigeon.util;

public class SystemUtil {

    public static final String CHARSET_WIN = "GBK";

    public static final String CHARSET_LINUX = "UTF-8";

    public static final String NET_INFO_WIN = "ipconfig";

    public static final String NET_INFO_LINUX = "ifconfig";

    private static volatile Boolean isWindowsFlag;

    public static boolean isWindows() {
        if (isWindowsFlag == null) {
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                isWindowsFlag = true;
            } else {
                isWindowsFlag = false;
            }
        }
        return isWindowsFlag;
    }

}
