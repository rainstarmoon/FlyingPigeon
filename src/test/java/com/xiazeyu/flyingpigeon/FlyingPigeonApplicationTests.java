package com.xiazeyu.flyingpigeon;

import com.xiazeyu.flyingpigeon.bean.InternalNode;
import com.xiazeyu.flyingpigeon.bean.SocketParam;
import com.xiazeyu.flyingpigeon.controller.SocketController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class FlyingPigeonApplicationTests {

    @Autowired
    SocketController controller;

    @Test
    public void openSelfNode() {
        SocketParam param = new SocketParam();
        //param.setInPath("");
        param.setOutPath("E:\\");
        //param.setIp();



        controller.openSelfNode(param);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long beg = System.currentTimeMillis();
        //List<InternalNode> internalNodes = controller.searchUsableNode();
        //System.out.println(internalNodes);
        long end = System.currentTimeMillis();
        System.out.println("花费时间：" + (end - beg) / 1000 + "秒");
    }


}
