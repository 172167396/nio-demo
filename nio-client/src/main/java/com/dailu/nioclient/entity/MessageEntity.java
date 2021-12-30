package com.dailu.nioclient.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MessageEntity {
    private String id;
    private String content;
    private LocalDateTime createTime;
}
