package com.faten;

import cn.hutool.core.collection.CollUtil;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * 包含网关驳回
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2022/12/3
 */
public class FlowableBackTest {

    public static ProcessEngineConfiguration configuration = null;

    @Before
    public void config() {
        //1、创建ProcessEngineConfiguration实例,该实例可以配置与调整流程引擎的设置
        configuration = new StandaloneProcessEngineConfiguration()
                //2、通常采用xml配置文件创建ProcessEngineConfiguration，这里直接采用代码的方式
                //3、配置数据库相关参数
                .setJdbcUrl("jdbc:mysql://localhost:3306/flowable?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8&nullCatalogMeansCurrent=true")
                .setJdbcUsername("root")
                .setJdbcPassword("root")
                .setJdbcDriver("com.mysql.cj.jdbc.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
    }

    /**
     * 部署定义流程
     */
    @Test
    public void deployment() {
        // 获取引擎
        ProcessEngine processEngine = configuration.buildProcessEngine();

        // 部署流程，获取仓库服务
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("faten-back.bpmn20.xml")
                .name("faten-back").deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploy.getId()).singleResult();
        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
        System.out.println(processDefinition.getId());
    }



    /**
     * 启动流程实例
     */
    @Test
    public void runProcess() {

        RuntimeService runtimeService = configuration.buildProcessEngine().getRuntimeService();
        // 构建参数，并启动流程
        Map<String, Object> variables = new HashMap<>();
        // 请几天假
        variables.put("num", 4);

        // 启动流程实例，第一个参数是流程定义的key
        ProcessInstance processInstance = runtimeService.startProcessInstanceById("faten-back:1:12504", variables);
        TaskService taskService = configuration.buildProcessEngine().getTaskService();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        System.out.println(processInstance.getDeploymentId());
        System.out.println(processInstance.getProcessDefinitionId());
        System.out.println(processInstance.getId());
        System.out.println(processInstance.getActivityId());
    }

    @Test
    public void completeTask() {
        TaskService taskService = configuration.buildProcessEngine().getTaskService();

        // 获取当前任务
        List<Task> tasks = taskService.createTaskQuery().processInstanceId("22501")
                .taskAssignee("lisi")
                .list();

        // 构建参数;同意请假
        Map<String, Object> map = new HashMap<>();
        map.put("approve", true);
        map.put("num", 5);

        for (Task task : tasks) {
            // 完成任务
            taskService.complete(task.getId(), map);
        }
    }

    /**
     * 驳回
     */
    @Test
    public void back() {
        RuntimeService runtimeService = configuration.buildProcessEngine().getRuntimeService();
        List<Execution> executions = runtimeService.createExecutionQuery().parentId("125001").list();

        List<String> executionIds = new ArrayList<>();
        executionIds.add("127504");
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionsToSingleActivityId(executionIds, "sid-CA559BB4-F66E-486B-8D5D-FF6BEB0DC77B")
//            .localVariable(disAllowanceDto.getDistFlowElementId(), TaskConstants.ASSIGNEE_USER, userId)
            .changeState();

        executions.forEach(r -> System.out.println(r.getId()));
    }

    /**
     * 驳回
     */
    @Test
    public void findAllNode() {
        RepositoryService repositoryService = configuration.buildProcessEngine().getRepositoryService();
        BpmnModel bpmnModel = repositoryService.getBpmnModel("faten-back-test:2:142504");

        String sourceId = "sid-89AF4F63-0F42-47C4-A6AF-30DD83F0864C";
        String targetId = "sid-C53449C5-F8E9-4D2C-B5FB-AB1C390BD439";

        Set<String> oldNodeIds = new HashSet<>();
        boolean check = checkHasGatewayForMiddle(bpmnModel, sourceId, targetId, oldNodeIds);

        System.out.println(check);

        oldNodeIds.clear();
        boolean checkUp = checkHasGatewayForUp(bpmnModel, targetId, oldNodeIds);
        System.out.println(checkUp);

        oldNodeIds.clear();
        boolean checkDown = checkHasGatewayForDown(bpmnModel, sourceId, oldNodeIds);
        System.out.println(checkDown);

        if (check && checkUp && checkDown) {
            oldNodeIds.clear();
            findBetweenNodes(bpmnModel, sourceId, targetId, oldNodeIds);
            oldNodeIds.forEach(System.out::println);
        }

    }

    /**
     * 查询在当前节点到驳回节点间是否有并行网关或包含网关
     *
     * @param bpmnModel 流程定义
     * @param sourceId  当前节点
     * @param targetId  驳回节点
     * @param oldNodeIds 存储查找过得节点，用于处理形成环形的情况
     * @return 校验结果
     */
    private boolean checkHasGatewayForMiddle(BpmnModel bpmnModel, String sourceId, String targetId, Set<String> oldNodeIds) {
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(sourceId);
        if (flowNode instanceof InclusiveGateway || flowNode instanceof ParallelGateway) {
            return false;
        }

        oldNodeIds.add(sourceId);

        List<SequenceFlow> incomingFlows = flowNode.getIncomingFlows();
        if (CollUtil.isEmpty(incomingFlows)) {
            return false;
        }
        for (SequenceFlow flow : incomingFlows) {
            if (flow.getSourceRef().equals(targetId)) {
                return true;
            }

            // 防止有环形节点的情况
            if (oldNodeIds.contains(flow.getSourceRef())) {
                continue;
            }

            if (checkHasGatewayForMiddle(bpmnModel, flow.getSourceRef(), targetId, oldNodeIds)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 查询当前节点向上是否有并行网关或包含网关，且并行条数大于1
     *
     * @param bpmnModel 流程定义
     * @param sourceId  当前节点
     * @param oldNodeIds 存储查找过得节点，用于处理形成环形的情况
     * @return 校验结果
     */
    private boolean checkHasGatewayForUp(BpmnModel bpmnModel, String sourceId, Set<String> oldNodeIds) {
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(sourceId);
        if (flowNode instanceof InclusiveGateway || flowNode instanceof ParallelGateway) {
            if (flowNode.getIncomingFlows().size() > 1 || flowNode.getOutgoingFlows().size() > 1) {
                return true;
            }
        }

        oldNodeIds.add(sourceId);

        List<SequenceFlow> incomingFlows = flowNode.getIncomingFlows();
        if (CollUtil.isEmpty(incomingFlows)) {
            return false;
        }

        for (SequenceFlow flow : incomingFlows) {

            // 防止有环形节点的情况
            if (oldNodeIds.contains(flow.getSourceRef())) {
                continue;
            }

            if (checkHasGatewayForUp(bpmnModel, flow.getSourceRef(), oldNodeIds)) {
                return true;
            }
        }

        return false;
    }


    /**
     * 查询驳回节点间向下是否有并行网关或包含网关
     *
     * @param bpmnModel 流程定义
     * @param targetId  驳回节点
     * @param oldNodeIds 存储查找过的节点，用于处理形成环形的情况
     * @return 校验结果
     */
    private boolean checkHasGatewayForDown(BpmnModel bpmnModel, String targetId, Set<String> oldNodeIds) {
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(targetId);
        if (flowNode instanceof InclusiveGateway || flowNode instanceof ParallelGateway) {
            if (flowNode.getIncomingFlows().size() > 1 || flowNode.getOutgoingFlows().size() > 1) {
                return true;
            }
        }

        oldNodeIds.add(targetId);

        List<SequenceFlow> outgoingFlows = flowNode.getOutgoingFlows();
        if (CollUtil.isEmpty(outgoingFlows)) {
            return false;
        }
        for (SequenceFlow flow : outgoingFlows) {
            // 防止有环形节点的情况
            if (oldNodeIds.contains(flow.getTargetRef())) {
                continue;
            }

            if (checkHasGatewayForDown(bpmnModel, flow.getTargetRef(), oldNodeIds)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 查询两个节点间的节点，只支持区间不含并行、包含网关的情况
     *
     * @param sourceId  当前节点
     * @param targetId  驳回节点
     * @param oldNodeIds 存储查找过得节点
     */
    public static void findBetweenNodes(BpmnModel bpmnModel, String sourceId, String targetId, Set<String> oldNodeIds) {
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(sourceId);
        oldNodeIds.add(sourceId);

        List<SequenceFlow> incomingFlows = flowNode.getIncomingFlows();
        if (CollUtil.isEmpty(incomingFlows)) {
            return;
        }

        for (SequenceFlow flow : incomingFlows) {
            oldNodeIds.add(flow.getId());
            if (flow.getSourceRef().equals(targetId)) {
                oldNodeIds.add(targetId);
                return;
            }

            // 防止有环形节点的情况
            if (oldNodeIds.contains(flow.getSourceRef())) {
                continue;
            }

            findBetweenNodes(bpmnModel, flow.getSourceRef(), targetId, oldNodeIds);
        }
    }
}
