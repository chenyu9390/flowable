package com.faten.task;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * 任务拒绝后操作
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2022/10/24
 */
public class SendRejectMail implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        System.out.println(execution.getEventName() + ":请假被拒绝，好好工作!");
    }
}
