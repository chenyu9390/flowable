package com.faten.listener;

import cn.hutool.extra.spring.SpringUtil;
import org.flowable.common.engine.api.delegate.event.*;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.event.impl.FlowableEntityWithVariablesEventImpl;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务完成后下一个执行的任务信息
 * @author albert.chen
 * @date 2024/01/30
 */
@Component
public class TaskListener extends AbstractFlowableEventListener {

    @Override
    public void onEvent(FlowableEvent flowableEvent) {
        FlowableEntityWithVariablesEventImpl event = (FlowableEntityWithVariablesEventImpl) flowableEvent;
        List<Task> tasks = SpringUtil.getBean(TaskService.class).createTaskQuery().processInstanceId(event.getProcessInstanceId()).list();
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    //该isFireOnTransactionLifecycleEvent()方法确定此事件侦听器是在事件发生时立即触发还是由
    // getOnTransaction()方法确定的事务生命周期事件触发。
    //支持的事务生命周期事件的值是：COMMITTED，ROLLED_BACK，COMMITTING，ROLLINGBACK。
    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return Boolean.TRUE;
    }

    @Override
    public String getOnTransaction() {
        return "COMMITTED";
    }
}
