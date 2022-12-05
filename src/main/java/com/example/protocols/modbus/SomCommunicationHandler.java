package com.example.protocols.modbus;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 请求：
 * 00 00   00 00   00 06      FF          03      00 01       00 00
 * | 事务 |协议标识| 长度 | 单元标识 |功能码|起始地址|寄存器地址|
 * <p>
 * 响应：
 * 00 00   00 00   00 06       FF           03     00 01      00 00
 * | 事务 |协议标识| 长度 | 单元标识 |功能码|字节个数|请求的数据|
 *
 * @author chenhaiming
 */
public class SomCommunicationHandler extends ChannelInboundHandlerAdapter {
    /**
     * 请求指令的长度，此协议指定12个字节长度
     */
    private static final int COMMAND_SIZE = 12;
    private ByteBuf readBuf;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        ByteBuf m = (ByteBuf) msg;
        Consts.logger.info("SomCommunicationHandler收到SOM的二进制形式指令： " + ByteBufUtil.hexDump(m));
        Consts.logger.info("SomCommunicationHandler收到SOM的ASCII形式指令： " + m.toString(StandardCharsets.US_ASCII));
        readBuf.writeBytes(m);
        m.release();
        byte[] bytes = handleData();
        if (bytes == null) {
            return;
        }
        ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(bytes)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener((ChannelFutureListener) channelFuture -> Consts.logger.info("SomCommunicationHandler " + this.getClass().getName() + "成功发送指令:" + Hex.encodeHexString(bytes)));
    }

    private byte[] handleData() {
        while (readBuf.readableBytes() >= COMMAND_SIZE) {
            List<Byte> result = new ArrayList<>();
            byte[] bytes = new byte[COMMAND_SIZE];
            readBuf.readBytes(bytes);
            // 事务
            result.add(bytes[0]);
            result.add(bytes[1]);
            // 协议标识
            result.add(bytes[2]);
            result.add(bytes[3]);
            // 起始地址
            float startIndex = ByteBuffer.wrap(bytes, 8, 2).getShort();
            // 寄存器个数
            float numberRegister = ByteBuffer.wrap(bytes, 10, 2).getShort();

            // 发送指令
            try {
                // 数据包 AI + DI
                byte[] dataBytes = getData((int) startIndex, (int) numberRegister);
                // Length 长度:长度字段以字节计数，包括单元标识和数据字段。
                int length = 3 + (dataBytes != null ? dataBytes.length : 0);
                byte[] len = numToBytes(length);
                addBytes(result, len);
                //Unit Identifier 单元标识
                result.add((byte) 0xFF);
                // Function Code 功能码
                result.add((byte) 0x04);
                //Byte Count 字节个数
                //result.add(numToBytes(dataBytes != null ? dataBytes.length : 0));

                byte[] len2 = numToHex8((dataBytes != null ? dataBytes.length : 0));
                addBytes(result, len2);

                addBytes(result, dataBytes);

                return list2byte(result);
            } catch (UnsupportedEncodingException e) {
                Consts.logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private byte[] list2byte(List<Byte> list) {
        if (list == null) {
            return null;
        }
        byte[] bytes = new byte[list.size()];
        int i = 0;
        for (Byte aList : list) {
            bytes[i] = aList;
            i++;
        }
        return bytes;
    }

    /**
     * @param startIndex 起始地址是指从数据的第几个字段开始, 1：系统报警， 2。。。12 数据
     * @param size       寄存器的个数 = DI + AI， DI有1个，AI有11个
     */
    private byte[] getData(int startIndex, int size) throws UnsupportedEncodingException {
        // TODO: demo数据。
        ModBusSample sample = new ModBusSample(20, 70, 50, 12, 1, 25, 70, 20, 18, 40, 10);
        //                                                  // 寄存器地址
        // DI：系统报警值，0,1,2,3,4， 1为正常
        byte[] di = numToBytes(1);
        // AI 请求的数据data，
        List<Byte> bytes = new ArrayList<>();
        addBytes(bytes, numToBytes(sample.getAvgTemp()));
        addBytes(bytes, numToBytes(sample.getAvgHumid()));
        addBytes(bytes, numToBytes(sample.getAvgPm10()));
        addBytes(bytes, numToBytes(sample.getAvgCo2()));
        addBytes(bytes, numToBytes(sample.getInModBus()));
        addBytes(bytes, numToBytes(sample.getInTemp()));
        addBytes(bytes, numToBytes(sample.getInPm10()));
        addBytes(bytes, numToBytes(sample.getInCo2()));
        addBytes(bytes, numToBytes(sample.getOutTemp()));
        addBytes(bytes, numToBytes(sample.getOutPm10()));
        addBytes(bytes, numToBytes(sample.getOutCo2()));

        // 处理一下startIndex, startIndex默认在区间[1,12]，1为DI，AI[2,12]
        if (startIndex == 1) {
            startIndex = 0;
        } else if (startIndex >= 2 && startIndex <= 12) {
            startIndex = (startIndex - 2) * 2;
        } else {
            Consts.logger.error("Data (as requested)获取请求数据失败， Starting Address 起始地址小于2或大于12，为：" + size);
            return null;
        }

        // 寄存器个数
        if (size < 0 || size > bytes.size()) {
            Consts.logger.error("Data (as requested)获取请求数据失败， Number of Registers 寄存器个数错误，个数为：" + size);
            return null;
        }

        bytes = bytes.subList(startIndex, (size - 1) * 2);

        List<Byte> result = new ArrayList<>();
        addBytes(result, di);
        result.addAll(bytes);
        return list2byte(result);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Consts.logger.error("DTUHandler exception: " + cause.toString());
        ctx.close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        readBuf = ctx.alloc().buffer();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        readBuf.release();
        readBuf = null;
        Client.disconnect(ctx.channel());
    }

    private void addBytes(List<Byte> list, byte[] arr) {
        if (arr == null || arr.length == 0) {
            return;
        }
        for (byte b : arr) {
            list.add(b);
        }
    }

    private static byte[] numToHex8(int b) {
        return hexStringToBytes(String.format("%02x", b));
    }

    private static byte[] numToBytes(int b) {
        String hex = String.format("%04x", b);
        return hexStringToBytes(hex);
    }

    private static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || "".equals(hexString)) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}

