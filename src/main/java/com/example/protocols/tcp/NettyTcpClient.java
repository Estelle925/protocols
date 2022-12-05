package com.example.protocols.tcp;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

public class NettyTcpClient {
    public static void main(String[] args) {
        //处理连接请求的NioEventLoopGroup
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        //服务对象
        Bootstrap bootstrap = new Bootstrap();
        //配置服务参数
        bootstrap.group(nioEventLoopGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(new NettyTcpClientHandler());
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                super.exceptionCaught(ctx, cause);
            }

            @Override
            public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                super.handlerAdded(ctx);
            }
        });
        //连接服务器端
        ChannelFuture connect = bootstrap.connect("192.170.98.204", 8999);
        try {
            //关闭连接
            connect.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //关闭连接
            nioEventLoopGroup.shutdownGracefully();
        }
    }

    /**
     * 处理器
     */
    static class NettyTcpClientHandler extends ChannelInboundHandlerAdapter {

        /**
         * 管道就绪则处理该方法
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush(Unpooled.copiedBuffer("Hello World,I am Client.", CharsetUtil.UTF_8));
        }

        /**
         * 通道读取事件时会触发
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            System.out.println(byteBuf.toString(CharsetUtil.UTF_8));
            System.out.println(ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}

