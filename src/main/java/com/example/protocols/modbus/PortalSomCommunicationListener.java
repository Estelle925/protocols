package com.example.protocols.modbus;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chenhaiming
 */
@Slf4j
public class PortalSomCommunicationListener implements Runnable {
    private final int port;

    public PortalSomCommunicationListener(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        startNettyServer(port);
    }

    private void startNettyServer(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EventExecutorGroup businessGroup = new DefaultEventExecutorGroup(10);
        try {
            // 配置服务器的NIO线程租
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true).childHandler(new ChannelInitializer() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addLast(new FixedLengthFrameDecoder(12));
                    // 设置超时时间，防止连接过多。
                    channel.pipeline().addLast("readtime", new ReadTimeoutHandler(5));
                    channel.pipeline().addLast(businessGroup, "executer", new SomCommunicationHandler());
                }
            });
            // 绑定端口，同步等待成功
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("modbus异常 e=", e);
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        // 异步MODBUS协议
        new Thread(new PortalSomCommunicationListener(Consts.MOD_BUS_PORT)).start();
    }
}


