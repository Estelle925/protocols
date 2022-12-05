package com.example.protocols.mqtt;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


/**
 * @author chenhaiming
 */
@Slf4j
@Component
public class MqttPluginServerInit {

    @PostConstruct
    public void init() {
        // 简单版测试服务
        MqttPluginServer MQTTPluginServer = new MqttPluginServer();
        MQTTPluginServer.run();
    }
}
