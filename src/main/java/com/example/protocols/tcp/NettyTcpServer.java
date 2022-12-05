package com.example.protocols.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;


public class NettyTcpServer {

    public static void main(String[] args) {
        //boosGroup处理连接请求
        NioEventLoopGroup boosGroup = new NioEventLoopGroup();
        //workGroup处理真正的业务逻辑
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        //服务端启动对象
        ServerBootstrap bootstrap = new ServerBootstrap();
        //配置启动对象参数
        //设置两个线程组
        bootstrap.group(boosGroup, workGroup)
                //channel类型为NioServerSocketChannel
                .channel(NioServerSocketChannel.class)
                //线程队列的连接个数
                .option(ChannelOption.SO_BACKLOG, 128)
                //线程队列的状态
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                //配置子处理器（匿名类）
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    /**
                     * 初始化阶段
                     * @param socketChannel
                     * @throws Exception
                     */
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new NettyTcpServerHandler());
                    }

                    /**
                     * 发生异常处理
                     * @param ctx
                     * @param cause
                     * @throws Exception
                     */
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        super.exceptionCaught(ctx, cause);
                    }

                    /**
                     * 添加新的处理器
                     * @param ctx
                     * @throws Exception
                     */
                    @Override
                    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                        super.handlerAdded(ctx);
                    }
                });
        try {
            //绑定端口
            ChannelFuture channelFuture = bootstrap.bind(8888).sync();
            //关闭通道监听
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //关闭连接
            boosGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    /**
     * 自定义Handler
     */
    static class NettyTcpServerHandler extends ChannelInboundHandlerAdapter {

        /**
         * 管道读取数据
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            System.out.println(byteBuf.toString(StandardCharsets.UTF_8));
            System.out.println(ctx.channel().remoteAddress());
        }

        /**
         * 读取数据完毕
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush(Unpooled.copiedBuffer("Hello World,I am Server.", CharsetUtil.UTF_8));
        }


        /**
         * 异常处理
         *
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            //super.exceptionCaught(ctx, cause);
            ctx.close();
        }
    }
}

