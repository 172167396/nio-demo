package com.dailu.nioserver.controller;

import com.dailu.nioserver.server.NioServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class NioServerController {

    @Resource
    NioServer nioServer;

    @GetMapping("/stop")
    public String stop(){
        nioServer.stop();
        return "服务已关闭";
    }

    @GetMapping("/forcePush")
    public String message(String address){
        nioServer.push(address,"服务端主动推送");
        return "消息发送成功";
    }

}
