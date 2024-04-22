package com.faten;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2022/10/24
 */
public class FlowableDemoTest {

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
                .addClasspathResource("请假流程.bpmn20.xml")
                .name("请假流程").deploy();

        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
    }

    /**
     * 查询部署定义的流程
     */
    @Test
    public void queryDeployment() {
        ProcessEngine processEngine = configuration.buildProcessEngine();

        RepositoryService repositoryService = processEngine.getRepositoryService();

        // 查询流程定义
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        ProcessDefinition processDefinition = processDefinitionQuery.deploymentId("40001").singleResult();

        System.out.println(processDefinition.getDeploymentId());
        System.out.println(processDefinition.getId());
        System.out.println(processDefinition.getName());
        System.out.println(processDefinition.getDescription());
    }

    /**
     * 级联删除部署的流程
     */
    @Test
    public void deleteDeployment() {
        configuration.buildProcessEngine().getRepositoryService().deleteDeployment("57501", true);
    }

    /**
     * 启动流程实例
     */
    @Test
    public void runProcess() {
//        // 设置任务发起人
//        Authentication.setAuthenticatedUserId("zhangsan");

        RuntimeService runtimeService = configuration.buildProcessEngine().getRuntimeService();
        // 构建参数，并启动流程
        Map<String, Object> variables = new HashMap<>();
        // 谁申请请假
        variables.put("employee", "张三");
        // 请几天假
        variables.put("nrOfHolidays", 3);
        // 请假的原因
        variables.put("description", "有事");
        // 通过变量形式设置受理人
        variables.put("manager", "zhangsan");

        // 启动流程实例，第一个参数是流程定义的key
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("holidayRequest", variables);
        System.out.println(processInstance.getDeploymentId());
        System.out.println(processInstance.getProcessDefinitionId());
        System.out.println(processInstance.getId());
        System.out.println(processInstance.getActivityId());
    }

    /**
     * 删除流程实例
     */
    @Test
    public void deleteProcessInstance() {
        RuntimeService runtimeService = configuration.buildProcessEngine().getRuntimeService();
        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().list();
        for (ProcessInstance p :list) {
            runtimeService.deleteProcessInstance(p.getProcessInstanceId(), null);
        }
    }

    /**
     * 查询当前流程的任务(在运行的节点)
     */
    @Test
    public void queryTask() {
        TaskService taskService = configuration.buildProcessEngine().getTaskService();
        List<Task> tasks = taskService.createTaskQuery()
                .processDefinitionKey("holidayRequest")
                .list();
        for (Task task : tasks) {
            System.out.println(task.getProcessDefinitionId());
            System.out.println(task.getId());
            System.out.println(task.getAssignee());
            System.out.println(task.getName());
        }
    }

    @Test
    public void completeTask() {
        TaskService taskService = configuration.buildProcessEngine().getTaskService();

        // 获取当前任务
        Task task = taskService.createTaskQuery().processDefinitionKey("holidayRequest")
                .taskAssignee("lisi")
                .singleResult();

        // 构建参数;同意请假
        Map<String, Object> map = new HashMap<>();
        map.put("approve", true);

        // 完成任务
        taskService.complete(task.getId(), map);
    }

    /**
     * 查询流程任务处理历史
     */
    @Test
    public void queryHistory() {
        HistoryService historyService = configuration.buildProcessEngine().getHistoryService();
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                .processDefinitionId("holidayRequest:1:67504")
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .list();

        for (HistoricActivityInstance historicActivityInstance : list) {
            System.out.println(historicActivityInstance.getActivityId() + " took "
                    + historicActivityInstance.getDurationInMillis() + " milliseconds");
        }
    }
}
