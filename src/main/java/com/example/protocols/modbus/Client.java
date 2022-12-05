package com.example.protocols.modbus;


import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chenhaiming
 */
public class Client {
    private static final Map<String, Client> DEV_CODE_2_CLIENT;

    static {
        DEV_CODE_2_CLIENT = new ConcurrentHashMap<>();
    }

    private static final Map<Channel, String> CHANNEL_2_DEV_CODE = new ConcurrentHashMap<>();


    public static void disconnect(Channel channel) {

        if (CHANNEL_2_DEV_CODE.containsKey(channel)) {
            String devCode = CHANNEL_2_DEV_CODE.get(channel);
            DEV_CODE_2_CLIENT.remove(devCode);
            CHANNEL_2_DEV_CODE.remove(channel);
        }
    }

}

