package com.faten.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.assertj.core.util.Lists;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TaskSequenceFlow {

    private String taskDefinitionId;

    private String name;

    private String taskId;

    private String procInstId;

    /**
     * 节点key（activityId）
     */
    private String taskDefKey;

    private List<TaskSequenceFlow> upTask = Lists.newArrayList();

    public TaskSequenceFlow(String taskDefinitionId,String name) {
        this.taskDefinitionId = taskDefinitionId;
        this.name = name;
    }
}
