package com.faten.config;

import com.faten.listener.TaskListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 流程配置
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2023/2/20
 */
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    /**
     * 解决流程图汉字乱码
     */
    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        engineConfiguration.setActivityFontName("宋体");
        engineConfiguration.setLabelFontName("宋体");
        engineConfiguration.setAnnotationFontName("宋体");
        engineConfiguration.initEventDispatcher();
        FlowableEventDispatcher dispatcher = engineConfiguration.getEventDispatcher();
        dispatcher.addEventListener(new TaskListener(), FlowableEngineEventType.TASK_COMPLETED);
    }
}
