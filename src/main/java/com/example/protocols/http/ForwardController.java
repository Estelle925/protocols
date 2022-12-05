package com.example.protocols.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author chenhaiming
 */
@Slf4j
@RestController
public class ForwardController {

    @PostMapping("/api/postMessage")
    public String postMessage(@RequestBody ParamEntity values) {
        log.info("postMessage: {}", JSONObject.toJSONString(values));
        return "success";
    }

}
