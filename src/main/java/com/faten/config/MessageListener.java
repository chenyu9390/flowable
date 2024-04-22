package com.faten.config;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 事件监听
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2022/12/3
 */
@Component
public class MessageListener {

    @EventListener
    public void messageListen(MessageEvent messageEvent) {
        System.out.println("##################" + messageEvent.getMessage());
    }
}
