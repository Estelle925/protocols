package com.example.protocols.mqtt;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

/**
 * @author chenhaiming
 */
@Slf4j
@ChannelHandler.Sharable
public class MqttPluginHandler extends ChannelInboundHandlerAdapter {
    private static final Collection<Channel> clientList = new HashSet();
    private static final Map<String, Object> msgMap = new HashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof MqttMessage) {
            Channel channel = ctx.channel();
            MqttMessage message = (MqttMessage) msg;

            MqttMessageType messageType = message.fixedHeader().messageType();
            log.info("MQTT接收到的发送类型===》{}", messageType);

            switch (messageType) {
                // 建立连接
                case CONNECT:
                    try {
                        this.connect(channel, (MqttConnectMessage) message);
                    } catch (Exception e) {
                        //如果用户密码，客户端ID校验不成功，会二次建立CONNECT类型连接
                        //但是没有实际意义
                    }
                    break;
                // 发布消息
                case PUBLISH:
                    this.publish(channel, (MqttPublishMessage) message);
                    break;
                // 订阅主题
                case SUBSCRIBE:
                    this.subscribe(channel, (MqttSubscribeMessage) message);
                    break;
                // 退订主题
                case UNSUBSCRIBE:
                    this.unSubscribe(channel, (MqttUnsubscribeMessage) message);
                    break;
                // 心跳包
                case PINGREQ:
                    this.pingReq(channel, message);
                    break;
                // 断开连接
                case DISCONNECT:
                    this.disConnect(channel, message);
                    break;
                // 确认收到响应报文,用于服务器向客户端推送qos1/qos2后，客户端返回服务器的响应
                case PUBACK:
                    this.puback(channel, message);
                    break;
                // qos2类型，发布收到
                case PUBREC:
                    this.pubrec(channel, message);
                    break;
                // qos2类型，发布释放响应
                case PUBREL:
                    this.pubrel(channel, message);
                    break;
                // qos2类型，发布完成
                case PUBCOMP:
                    this.pubcomp(channel, message);
                    break;
                default:
                    if (log.isDebugEnabled()) {
                        log.debug("Nonsupport server message  type of '{}'.", messageType);
                    }
                    break;
            }
        }
    }

    /**
     * 创建连接时，需要响应对应的ACK包
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/13 23:12
     */
    public void connect(Channel channel, MqttConnectMessage msg) {
        //连接需要答复
        MqttConnAckMessage okResp = (MqttConnAckMessage) MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.CONNACK, false, AT_LEAST_ONCE, false, 0), new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, true), null);
        channel.writeAndFlush(okResp);
        clientList.add(channel);
    }

    /**
     * 响应ping心跳ACK包
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/13 23:13
     */
    public void pingReq(Channel channel, MqttMessage msg) {
        if (log.isDebugEnabled()) {
            log.debug("MQTT pingReq received.");
        }
        MqttMessage pingResp = new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP, false, AT_LEAST_ONCE, false, 0));
        channel.writeAndFlush(pingResp);
    }

    /**
     * 服务端主动断开连接
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/13 23:13
     */
    public void disConnect(Channel channel, MqttMessage msg) {
        clientList.remove(channel);
        if (channel.isActive()) {
            channel.close();
            if (log.isDebugEnabled()) {
                log.debug("MQTT channel '{}' was closed.", channel.id().asShortText());
            }
        }
    }

    /**
     * qos2中使用,发布确认
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/13 23:14
     */
    public void puback(Channel channel, MqttMessage msg) {

//        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = msg.variableHeader();
    }

    /**
     * qos2中发布释放ACK包
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/13 23:13
     */
    public void pubrel(Channel channel, MqttMessage msg) {
        Object mqttMessageIdVariableHeader = msg.variableHeader();
        if (mqttMessageIdVariableHeader instanceof MqttPubReplyMessageVariableHeader) {
            // qos2类型,接收发布者消息
            log.info("qos2客户端返回确认的消息包:{}", msg.payload());
            MqttPubReplyMessageVariableHeader header = (MqttPubReplyMessageVariableHeader) mqttMessageIdVariableHeader;
            MqttMessage mqttMessage = MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.EXACTLY_ONCE, false, 0), MqttMessageIdVariableHeader.from(header.messageId()), 0);
            channel.writeAndFlush(mqttMessage);
            for (Channel channel1 : clientList) {
                try {
                    send(channel1, "aaa", MqttQoS.EXACTLY_ONCE, "我收到那");
                } catch (InterruptedException e) {
                    log.error("该通道推送消息失败,可加入容错机制,channel:{}", channel1);
                }
            }
        }
    }

    /**
     * qos2:发布收到ACK包
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/14 0:09
     */
    public void pubrec(Channel channel, MqttMessage msg) {
        Object mqttMessageIdVariableHeader = msg.variableHeader();
        if (mqttMessageIdVariableHeader instanceof MqttPubReplyMessageVariableHeader) {
            // qos2类型,接收发布者消息
            log.info("qos2客户端返回确认的消息包:{}", msg.payload());
            MqttPubReplyMessageVariableHeader header = (MqttPubReplyMessageVariableHeader) mqttMessageIdVariableHeader;
            MqttMessage mqttMessage = MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.EXACTLY_ONCE, false, 0), MqttMessageIdVariableHeader.from(header.messageId()), 0);
            channel.writeAndFlush(mqttMessage);
        }
    }

    /**
     * qos2发布完成
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/14 0:11
     */
    public void pubcomp(Channel channel, MqttMessage msg) {
        Object mqttMessageIdVariableHeader = msg.variableHeader();
        if (mqttMessageIdVariableHeader instanceof MqttPubReplyMessageVariableHeader) {
            // qos2类型,接收发布者消息
//            log.info("qos2客户端返回确认的消息包:{}",msg.payload());
//            MqttPubReplyMessageVariableHeader header = (MqttPubReplyMessageVariableHeader) mqttMessageIdVariableHeader;
//            MqttMessage mqttMessage = MqttMessageFactory.newMessage(
//                    new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.EXACTLY_ONCE, false, 0),
//                    MqttMessageIdVariableHeader.from(header.messageId()),
//                    0);
//            channel.writeAndFlush(mqttMessage);
        }
    }


    /**
     * 客户端发布消息时使用
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/13 23:14
     */
    public void publish(Channel channel, MqttPublishMessage msg) {

        log.info("qos类型是{}", msg.fixedHeader().qosLevel());
        String topic = msg.variableHeader().topicName();
        log.info("订阅主题:{}", topic);
        ByteBuf buf = msg.content().duplicate();
        byte[] tmp = new byte[buf.readableBytes()];
        buf.readBytes(tmp);
        String content = null;
        try {
            content = new String(tmp, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //校验传入的数据是否符合要求
        if (StringUtils.isBlank(content)) {
            log.error("MQTT接收到的数据包为空===》{}", content);
            puback(channel, msg, "MQTT接收到的数据包为空");
            return;
        }
        log.info("MQTT读取到的客户端发送信息===>{}", content);
        // 如果是qos1或者qos2类型都需要响应
        puback(channel, msg, content);
        // 推送主题消息
        log.info("推送客户端客户端消息：{}", content);
        if (AT_LEAST_ONCE == msg.fixedHeader().qosLevel() || AT_MOST_ONCE == msg.fixedHeader().qosLevel()) {
            for (Channel channel1 : clientList) {
                try {
                    send(channel1, topic, msg.fixedHeader().qosLevel(), content);
                } catch (InterruptedException e) {
                    log.error("该通道推送消息失败,可加入容错机制,channel:{}", channel1);
                }
            }
        }
    }

    /**
     * 客户端订阅消息ACK包
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/13 23:14
     */
    public void subscribe(Channel channel, MqttSubscribeMessage msg) {
        MqttQoS mqttQoS = msg.fixedHeader().qosLevel();
//        mqttQoS = MqttQoS.EXACTLY_ONCE;
        MqttSubAckMessage subAckMessage = (MqttSubAckMessage) MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.SUBACK, false, mqttQoS, false, 0), MqttMessageIdVariableHeader.from(msg.variableHeader().messageId()), new MqttSubAckPayload(0));
        channel.writeAndFlush(subAckMessage);
    }

    /**
     * 客户端取消订阅ACK包
     *
     * @param channel:
     * @param msg:
     * @since 2022/3/13 23:15
     */
    public void unSubscribe(Channel channel, MqttUnsubscribeMessage msg) {

        MqttUnsubAckMessage unSubAckMessage = (MqttUnsubAckMessage) MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.UNSUBACK, false, AT_LEAST_ONCE, false, 0), MqttMessageIdVariableHeader.from(msg.variableHeader().messageId()), null);
        channel.writeAndFlush(unSubAckMessage);
        disConnect(channel, msg);
    }

    /**
     * 捕获异常状态，客户端断开钩子函数
     *
     * @param ctx:
     * @param cause:
     * @since 2022/3/13 23:15
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("MQTT客户端被强制关闭:{}:{}", ctx.channel().id().asShortText(), cause);
        if (ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            ctx.channel().close();
        }
    }

    /**
     * qos1中响应客户端ACK包
     *
     * @param channel:
     * @param msg:
     * @param payLoad:
     * @since 2022/3/13 23:16
     */
    // 客户端QOS1消息类型( MqttQoS.AT_LEAST_ONCE = qos1)，需要服务器响应包
    private void puback(Channel channel, MqttPublishMessage msg, String payLoad) {

        if (MqttQoS.AT_MOST_ONCE == msg.fixedHeader().qosLevel()) {
            // qos0消息类型,不需要ACK客户端
            return;
        }
        if (MqttQoS.AT_LEAST_ONCE == msg.fixedHeader().qosLevel()) {
            // qos1消息类型,需要向客户端返回MqttMessageType.PUBACK 类型ACK应答
            MqttPubAckMessage sendMessage = (MqttPubAckMessage) MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_LEAST_ONCE, false, 0), MqttMessageIdVariableHeader.from(msg.variableHeader().packetId()), payLoad);
            channel.writeAndFlush(sendMessage);
            return;
        }
        if (MqttQoS.EXACTLY_ONCE == msg.fixedHeader().qosLevel()) {
            // qos2消息类型
            MqttMessage mqttMessage = MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.EXACTLY_ONCE, false, 0), MqttMessageIdVariableHeader.from(msg.variableHeader().packetId()), payLoad);

            channel.writeAndFlush(mqttMessage);
        }
    }

    /**
     * 向客户端发布订阅主题消息
     *
     * @param channel:
     * @param topic:
     * @param qos:
     * @param sendMessage:
     * @return
     * @since 2022/3/13 23:16
     */
    public ChannelFuture send(Channel channel, String topic, MqttQoS qos, String sendMessage) throws InterruptedException {
        MqttPluginRequest request = new MqttPluginRequest((sendMessage.getBytes()));
        MqttPublishMessage pubMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.PUBLISH, request.isDup(), qos, request.isRetained(), 0), new MqttPublishVariableHeader(topic, 0), Unpooled.buffer().writeBytes(request.getPayload()));
        msgMap.put(pubMessage.variableHeader().messageId() + "", pubMessage.variableHeader().messageId() + "");
        // 超过高水位，则采取同步模式
        if (channel.isWritable()) {
            return channel.writeAndFlush(pubMessage);
        }
        return channel.writeAndFlush(pubMessage).sync();
    }
}
