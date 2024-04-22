package com.faten.entity;

import lombok.Data;

/**
 * 流程实例任务
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2023/2/21
 */
@Data
public class ProcessTask {

    /**
     * 任务id
     */
    private String id;

    /**
     * 流程实例id
     */
    private String procInstId;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务受理人
     */
    private String assignee;
}
