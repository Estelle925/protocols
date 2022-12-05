package com.example.protocols.mqtt;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author chenhaiming
 */
@Slf4j
public class MqttPluginServer {

    public void run() {
        // 监听端口号
        int port = 1883;
        // 构建主线程-用于分发socket请求
        EventLoopGroup boosGroup = new NioEventLoopGroup(1);
        // 构建工作线程-用于处理请求处理
        EventLoopGroup workGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boosGroup, workGroup).channel(NioServerSocketChannel.class).childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.TCP_NODELAY, true)
//                    .childOption(ChannelOption.SO_BACKLOG,1024)     //等待队列
                    //快速复用
                    .childOption(ChannelOption.SO_REUSEADDR, true).childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 这个地方注意，如果客户端发送请求体超过此设置值，会抛异常
                            socketChannel.pipeline().addLast(new MqttDecoder(1024 * 1024));
                            socketChannel.pipeline().addLast(MqttEncoder.INSTANCE);
                            // 加载MQTT编解码协议，包含业务逻辑对象
                            socketChannel.pipeline().addLast(new MqttPluginHandler());
                        }
                    });
            serverBootstrap.bind(port).addListener(future -> {
                log.info("服务端成功绑定端口号={}", port);
            });
        } catch (Exception e) {
            boosGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            log.error("mqttServer启动失败:", e);
        }
    }
}