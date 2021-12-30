package com.dailu.nioclient.controller;

import com.dailu.nioclient.client.NioClient;
import com.dailu.nioclient.entity.MessageEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
public class MessageController {

    @Resource
    NioClient nioClient;

    @GetMapping("/send")
    public String send(String msg) {
        MessageEntity message = MessageEntity.builder().id(UUID.randomUUID().toString())
                .content(msg)
                .createTime(LocalDateTime.now())
                .build();
        String response = nioClient.push(message);
        return response;
    }

}
