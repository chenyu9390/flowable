package com.faten.rest;

import com.faten.config.MessageEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 消息
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2022/12/3
 */
@RestController
@RequestMapping("message")
public class MessageController {

    @Resource
    private ApplicationContext applicationContext;

    @GetMapping("event")
    public void event(@RequestParam String message) {
        MessageEvent event = new MessageEvent();
        event.setMessage(message);
        applicationContext.publishEvent(event);
    }
}
