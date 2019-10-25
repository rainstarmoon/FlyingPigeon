package com.xiazeyu.flyingpigeon.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolUtil {

    public static volatile boolean search_running_flag = false;

    public static volatile boolean server_running_flag = false;

    public static final int THREAD_NUM = 256;

    public static ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUM);

}
