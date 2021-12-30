package com.dailu.nioserver.server;

import com.dailu.nioserver.exception.CustomRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.swing.undo.CannotUndoException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NioServer {

    private Map<String, SocketChannel> channelMap = new ConcurrentHashMap<>();

    ServerSocketChannel serverSocketChannel = null;
    Selector selector;

    private static volatile boolean stop = false;

    @PostConstruct
    public void server() throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(9999));
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
        log.debug("nio server started at " + LocalDateTime.now());
        new Thread(() -> {
            try {
                start();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }).start();
    }

    public boolean stop() {
        stop = true;
        return stop;
    }

    public void start() throws IOException {
        while (!stop) {
            int readyChannel = selector.select(100);
            if (readyChannel == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                try {
                    handleConnect(key);
                } catch (Exception e) {
                    removeChannel(key, (SocketChannel) key.channel());
//                    throw new CustomRuntimeException(e.getMessage(), e);
                    log.error(e.getMessage(),e);
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
    }

    public void push(String address, String msg) {
        SocketChannel socketChannel = channelMap.get(address);
        if (socketChannel == null) {
            throw new CustomRuntimeException("channel not exist!");
        }
        try {
            write(socketChannel, msg);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new CustomRuntimeException(e.getMessage(), e);
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        ByteBuffer readBuff = ByteBuffer.allocate(1024);
        if (key.isAcceptable()) {
            log.debug("isAcceptable");
            SocketChannel clientChannel = serverSocketChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(), SelectionKey.OP_READ);
            log.debug("remote channel added:" + clientChannel.getRemoteAddress().toString());
            channelMap.put(clientChannel.getRemoteAddress().toString(), clientChannel);
        } else if (key.isConnectable()) {
            log.debug("isConnectable");
        } else if (key.isReadable()) {
            log.debug("start read message.........");
            String readMessage;
            SocketChannel channel = (SocketChannel) key.channel();
            try {
                readMessage = read(key, channel, readBuff);
            } catch (Exception e) {
                throw new CustomRuntimeException(e.getMessage(), e);
            }
            log.debug("receive message from " + channel.getRemoteAddress().toString() + " client：" + readMessage);
            write(channel, "消息已收到！" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else if (key.isWritable()) {
            log.debug("change to writeable.......");
        }
    }

    private void removeChannel(SelectionKey key, SocketChannel channel) throws IOException {
        channelMap.remove(channel.getRemoteAddress().toString());
        key.cancel();
        channel.socket().close();
        channel.close();
    }

    private String read(SelectionKey key, SocketChannel channel, ByteBuffer readBuff) throws IOException {
        channel.configureBlocking(false);
        readBuff.clear();
        int read = channel.read(readBuff);
        if (read == -1) {
            key.cancel();
            channel.socket().close();
            channel.close();
            return null;
        }
        readBuff.flip();
        return StandardCharsets.UTF_8.decode(readBuff).toString();
    }

    private void write(SocketChannel channel, String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuffer wrap = ByteBuffer.allocate(bytes.length);
        wrap.put(bytes);
        wrap.flip();
        log.debug("发送消息：" + msg);
        channel.write(wrap);
    }
}
