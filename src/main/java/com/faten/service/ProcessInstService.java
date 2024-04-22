package com.faten.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.faten.entity.ProcessHiTask;
import com.faten.entity.ProcessTask;
import com.faten.entity.TaskSequenceFlow;
import com.faten.util.BpmnUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.NativeHistoricActivityInstanceQuery;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程实例
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2023/2/21
 */
@Slf4j
@Service
public class ProcessInstService {

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private HistoryService historyService;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private TaskService taskService;

    @Resource
    private ManagementService managementService;

    /**
     * 流程实例发起
     *
     * @param variables 参数(流程定义id)
     * @return 流程实例id
     */
    public String processInstStart(Map<String, Object> variables) {
        String procDefId = (String) variables.remove("procDefId");
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefId, variables);
        return processInstance.getId();
    }

    /**
     * 查询流程实体
     *
     * @param procInstId 流程实体id
     */
    @SneakyThrows
    public void queryProcessInst(String procInstId, HttpServletResponse response) {
        // 获得已经办理的历史节点
        List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(procInstId).orderByHistoricActivityInstanceStartTime().asc().list();
        List<String> activities = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (HistoricActivityInstance activityInstance : activityInstances) {
            if ("sequenceFlow".equals(activityInstance.getActivityType())) {
                // 需要高亮显示的连接线
                flows.add(activityInstance.getActivityId());
            } else {
                // 需要高亮显示的节点
                activities.add(activityInstance.getActivityId());
            }
        }

        // 根据modelId或者BpmnModel
        String procDefId = activityInstances.get(0).getProcessDefinitionId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(procDefId);
        // 获得图片流
        ProcessDiagramGenerator processDiagramGenerator = new DefaultProcessDiagramGenerator();
        String font = "宋体";
        InputStream inputStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", activities, flows,
                font, font, font, null, 1.0, false);
        // 输出图片
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        IoUtil.copy(inputStream, response.getOutputStream());
    }

    /**
     * 查询流程实例任务
     *
     * @param procInstId 流程实例id
     * @return 流程实例任务
     */
    public List<ProcessTask> queryProcessInstTask(String procInstId) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInstId).list();
        return tasks.stream().map(t -> {
            ProcessTask task = new ProcessTask();
            task.setId(t.getId());
            task.setProcInstId(t.getProcessInstanceId());
            task.setName(t.getName());
            task.setAssignee(t.getAssignee());
            task.setDescription(t.getDescription());
            return task;
        }).collect(Collectors.toList());
    }

    /**
     * 查询流程实例历史任务
     *
     * @param procInstId 流程实例id
     * @return 流程实例历史任务
     */
    public List<ProcessHiTask> queryProcessInstHiTask(String procInstId) {
        List<HistoricTaskInstance> hiTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(procInstId).list();
        return hiTasks.stream().map(t -> {
            ProcessHiTask task = new ProcessHiTask();
            task.setId(t.getId());
            task.setProcInstId(t.getProcessInstanceId());
            task.setName(t.getName());
            task.setTaskDefKey(t.getTaskDefinitionKey());
            task.setAssignee(t.getAssignee());
            task.setDescription(t.getDescription());
            task.setStartTime(t.getCreateTime());
            task.setEndTime(t.getEndTime());
            task.setDeleteReason(t.getDeleteReason());
            return task;
        }).collect(Collectors.toList());
    }

    /**
     * 完成流程实例任务
     *
     * @param variables 参数(任务id)
     * @return {@link Boolean}
     */
    public Boolean completeProcessInstTask(Map<String, Object> variables) {
        String taskId = (String) variables.remove("taskId");
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (Objects.isNull(task)) {
            return false;
        }

        // 校验任务处理人
        String assignee = (String) variables.get("assignee");
        if(task.getAssignee() != null){
            if (Objects.isNull(assignee) || !assignee.equals(task.getAssignee())) {
                return false;
            }
        }
        taskService.complete(taskId, variables);
        return true;
    }

    @Transactional(rollbackFor = Throwable.class)
    public Boolean reject(Map<String,Object> variables){
        // 查询当前任务信息
        String taskId = (String) variables.remove("taskId");
        TaskEntity taskEntity = (TaskEntity) taskService.createTaskQuery().taskId(taskId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(taskEntity.getProcessDefinitionId());
        // 驳回节点id（act_hi_taskinst表中TASK_DEF_KEY_ 或 act_ru_actinst表中ACT_ID_）；TASK_DEF_KEY_和ACT_ID_一样
        String rejectId = (String) variables.remove("rejectId");
        String sourceId = taskEntity.getTaskDefinitionKey();
        //从驳回节点到当前节点的所有路径
        List<List<String>> flowPath = BpmnUtils.flowPath(bpmnModel, rejectId, sourceId);
        deleteActivityAndTask(taskEntity,flowPath);
        boolean rejectInGateway = BpmnUtils.checkNotHasGatewayForMiddle(bpmnModel, sourceId, rejectId, new HashSet<>())
                && BpmnUtils.checkHasGatewayForUp(bpmnModel, sourceId, new HashSet<>())
                && BpmnUtils.checkHasGatewayForDown(bpmnModel, rejectId, new HashSet<>());
        // 判断节点是不是子流程内部的节点
        List<String> executionIds;
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(taskEntity.getProcessInstanceId()).list();
        if (rejectInGateway) {
            // 网关内驳回，当前节点的所有数据，注意或签、会签
            executionIds = executions.stream().filter(e -> e.getActivityId().equals(taskEntity.getTaskDefinitionKey()))
                    .map(Execution::getId).collect(Collectors.toList());
        } else {
            // 普通驳回
            executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());
        }
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionsToSingleActivityId(executionIds, rejectId)
                .localVariables(rejectId, variables)
                .changeState();
        return Boolean.TRUE;
    }

    /**
     * 驳回流程实例任务
     *
     * @return variables 参数(任务id，驳回任务id)
     * @return {@link Boolean}
     */
    @Transactional(rollbackFor = Throwable.class)
    public Boolean rejectProcessInstTask(Map<String, Object> variables) {
        // 查询当前任务信息
        String taskId = (String) variables.remove("taskId");
        TaskEntity taskEntity = (TaskEntity) taskService.createTaskQuery().taskId(taskId).singleResult();

        // 驳回节点id（act_hi_taskinst表中TASK_DEF_KEY_ 或 act_ru_actinst表中ACT_ID_）；TASK_DEF_KEY_和ACT_ID_一样
        String rejectId = (String) variables.remove("rejectId");

        // 确定是否为网关内驳回；认为节点中间无并行、包含网关，且向上、向下都具有并行或包含网关；
        BpmnModel bpmnModel = repositoryService.getBpmnModel(taskEntity.getProcessDefinitionId());
        String sourceId = taskEntity.getTaskDefinitionKey();
        boolean rejectInGateway = BpmnUtils.checkNotHasGatewayForMiddle(bpmnModel, sourceId, rejectId, new HashSet<>())
                && BpmnUtils.checkHasGatewayForUp(bpmnModel, sourceId, new HashSet<>())
                && BpmnUtils.checkHasGatewayForDown(bpmnModel, rejectId, new HashSet<>());

        // 删除历史数据；删除ACT_RU_ACTINST、ACT_HI_ACTINST、ACT_HI_TASKINST 表中数据
        deleteActivityAndTask(taskEntity, rejectId, rejectInGateway);

        // 判断节点是不是子流程内部的节点
        List<String> executionIds;
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(taskEntity.getProcessInstanceId()).list();
        if (rejectInGateway) {
            // 网关内驳回，当前节点的所有数据，注意或签、会签
            executionIds = executions.stream().filter(e -> e.getActivityId().equals(taskEntity.getTaskDefinitionKey()))
                    .map(Execution::getId).collect(Collectors.toList());
        } else {
            // 普通驳回
            executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());
        }
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionsToSingleActivityId(executionIds, rejectId)
                .localVariables(rejectId, variables)
                .changeState();
        return true;
    }

    /**
     * 转交当前用户任务
     *
     * @return variables 参数(任务id，被转交人)
     * @return {@link Boolean}
     */
    @Transactional(rollbackFor = Throwable.class)
    public Boolean handOverProcessInstTask(Map<String, Object> variables) {
        // 当前任务id及被转交人
        String taskId = (String) variables.get("taskId");
        String assignee = (String) variables.get("assignee");
        taskService.setAssignee(taskId, assignee);
        return true;
    }

    /**
     * 终止审批
     *
     * @param procInstId 流程实例id
     * @return {@link Boolean}
     */
    @Transactional(rollbackFor = Throwable.class)
    public Boolean stopProcessInst(String procInstId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        if (Objects.isNull(processInstance)) {
            return false;
        }

        // 获取结束节点
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        Process process = bpmnModel.getMainProcess();
        List<EndEvent> endEvents = process.findFlowElementsOfType(EndEvent.class);
        String endId = endEvents.get(0).getId();

        // 执行终止
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(procInstId).list();
        List<String> executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionsToSingleActivityId(executionIds, endId)
                .changeState();
        return true;
    }

    /**
     * 删除审批
     *
     * @param procInstId 流程实例id
     * @return {@link Boolean}
     */
    @Transactional(rollbackFor = Throwable.class)
    public Boolean deleteProcessInst(String procInstId) {
        long count = runtimeService.createActivityInstanceQuery().processInstanceId(procInstId).count();
        if (count > 0) {
            // 删除流程实例，未级联删除关联表
            runtimeService.deleteProcessInstance(procInstId, "删除流程实例");
            return true;
        }

        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        if (Objects.nonNull(instance)) {
            historyService.deleteHistoricProcessInstance(procInstId);
        }
        return true;
    }

    public void deleteActivityAndTask(TaskEntity taskEntity,List<List<String>> activityId){
        String tableName = managementService.getTableName(ActivityInstanceEntity.class);
        StrBuilder builder = new StrBuilder();
        for (List<String> sList : activityId) {
            for (String s : sList) {
                builder.append("'").append(s).append("'").append(",");
            }
        }
        String s = builder.subString(0, builder.length() - 1);
        String sql = "select t.* from " + tableName + " t where t.PROC_INST_ID_=#{processInstanceId} " +
                "and t.ACT_ID_ IN ("+s+") order by t.END_TIME_ ASC";
        String processInstanceId = taskEntity.getProcessInstanceId();
        List<ActivityInstance> disActivities = runtimeService.createNativeActivityInstanceQuery().sql(sql)
                .parameter("processInstanceId", processInstanceId).list();
        deleteActivityAndTask(disActivities);
    }

    /**
     * 删除跳转的历史节点信息
     *
     * @param taskEntity    当前节点
     * @param disActivityId 跳转的节点id
     */
    public void deleteActivityAndTask(TaskEntity taskEntity, String disActivityId, boolean rejectInGateway) {
        String tableName = managementService.getTableName(ActivityInstanceEntity.class);
        String sql = "select t.* from " + tableName + " t where t.PROC_INST_ID_=#{processInstanceId} " +
                "and t.ACT_ID_ = #{disActivityId} order by t.END_TIME_ ASC";
        String processInstanceId = taskEntity.getProcessInstanceId();
        List<ActivityInstance> disActivities = runtimeService.createNativeActivityInstanceQuery().sql(sql)
                .parameter("processInstanceId", processInstanceId)
                .parameter("disActivityId", disActivityId).list();
        if (CollUtil.isEmpty(disActivities)) {
            return;
        }
        // 删除运行时和历史节点信息
        ActivityInstance activityInstance = disActivities.get(0);

        sql = "select t.* from " + tableName + " t where t.PROC_INST_ID_=#{processInstanceId} " +
                "and (t.END_TIME_ >= #{endTime} or t.END_TIME_ is null)";
        List<ActivityInstance> activityInstances = runtimeService.createNativeActivityInstanceQuery().sql(sql)
                .parameter("processInstanceId", processInstanceId)
                .parameter("endTime", activityInstance.getEndTime()).list();

        // 网关内驳回，过滤其他分支的节点
        if (rejectInGateway) {
            // 查询驳回区间的节点
            Set<String> actIds = new HashSet<>();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(taskEntity.getProcessDefinitionId());
            BpmnUtils.findBetweenNodesForUp(bpmnModel, taskEntity.getTaskDefinitionKey(), disActivityId, actIds);
            activityInstances = activityInstances.stream().filter(a -> actIds.contains(a.getActivityId())).collect(Collectors.toList());
        }

        deleteActivityAndTask(activityInstances);
    }

    public void deleteActivityAndTask(List<ActivityInstance> activityInstances) {
        if (CollUtil.isEmpty(activityInstances)) {
            return;
        }

        for (ActivityInstance activityInstance : activityInstances) {
            if (StrUtil.isNotBlank(activityInstance.getTaskId())) {
                historyService.deleteHistoricTaskInstance(activityInstance.getTaskId());
            }

            if (StrUtil.isNotBlank(activityInstance.getId())) {
                runtimeService.createNativeActivityInstanceQuery().sql(buildDeleteSql(ActivityInstanceEntity.class))
                        .parameter("id", activityInstance.getId()).singleResult();

                NativeHistoricActivityInstanceQuery query1 = historyService.createNativeHistoricActivityInstanceQuery();
                query1.sql(buildDeleteSql(HistoricActivityInstanceEntity.class));
                query1.parameter("id", activityInstance.getId()).singleResult();
            }
        }
    }

    private String buildDeleteSql(Class<? extends Entity> entityClass) {
        String tableName = managementService.getTableName(entityClass);
        return "delete from " + tableName + " where ID_ = #{id}";
    }
}
