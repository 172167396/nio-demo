package com.dailu.nioclient.client;

import com.dailu.nioclient.entity.MessageEntity;
import com.dailu.nioclient.exception.CustomRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Slf4j
@Component
public class NioClient {

    private static SocketChannel socketChannel;
    private static Selector selector;
    private volatile boolean stop = false;

    @PostConstruct
    public void connect() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("127.0.0.1", 9999));
        socketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        NioClient.socketChannel = socketChannel;
        NioClient.selector = selector;
        log.debug("nio client connect successful.....");
        new Thread(() -> {
            try {
                start();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                removeChannel();
            }
        }).start();
    }


    public void start() throws IOException {
        //这种写法不知道为什么不生效
        while (!stop) {
            int readyChannelNum = selector.select();
            if (readyChannelNum > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectedKey = iterator.next();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    if (selectedKey.isReadable()) {
                        int read = read(selectedKey, byteBuffer);
                        if (read == 0 || read == -1) continue;
                    }
                    iterator.remove();
                }
            }
        }

        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
//        while (!stop) {
//            byteBuffer.clear();
//            int read = socketChannel.read(byteBuffer);
//            if (read == 0 || read == -1) {
//                continue;
//            }
//            byte[] b = new byte[byteBuffer.position()];
//            byteBuffer.flip();
//            byteBuffer.get(b);
//            log.debug("receive message:" + new String(b));
//        }
    }

    private void removeChannel() {
        socketChannel.keyFor(selector).cancel();
        try {
            socketChannel.socket().close();
            socketChannel.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private int read(SelectionKey key, ByteBuffer readBuff) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        readBuff.clear();
        int read = channel.read(readBuff);
        if (read == -1) {
            channel.socket().close();
            channel.close();
            key.cancel();
        }
        return read;
    }


    public void stop() {
        this.stop = true;
    }

    public String push(MessageEntity message) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String msg;
        try {
            msg = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new CustomRuntimeException(e.getMessage(), e);
        }
        ByteBuffer buff = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
        try {
            socketChannel.write(buff);
            log.debug("发送消息：" + msg);
            String read = read();
            log.debug("服务返回消息：" + read);
            return read;
        } catch (IOException e) {
            throw new CustomRuntimeException(e.getMessage(), e);
        }
    }

    public String read() throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int read = socketChannel.read(readBuffer);
        byte[] b = new byte[readBuffer.position()];
        if (read == -1) {
            socketChannel.socket().close();
            socketChannel.close();
            selector.close();
            return null;
        }
        readBuffer.flip();
        readBuffer.get(b);
        String response = new String(b);
        return response;
    }

}
