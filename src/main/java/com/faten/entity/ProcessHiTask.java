package com.faten.entity;

import lombok.Data;

import java.util.Date;

/**
 * 流程实例历史任务
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2023/2/21
 */
@Data
public class ProcessHiTask {

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
     * 节点key（activityId）
     */
    private String taskDefKey;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务受理人
     */
    private String assignee;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 开始时间
     */
    private Date endTime;

    /**
     * 删除原因，不为空时，表示无效的任务
     */
    private String deleteReason;
}
