package com.faten;

import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关测试
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2022/11/4
 */
public class FlowableGatewayTest {

    public static ProcessEngineConfiguration configuration = null;

    @Before
    public void config() {
        //1、创建ProcessEngineConfiguration实例,该实例可以配置与调整流程引擎的设置
        configuration = new StandaloneProcessEngineConfiguration()
                //2、通常采用xml配置文件创建ProcessEngineConfiguration，这里直接采用代码的方式
                //3、配置数据库相关参数
                .setJdbcUrl("jdbc:mysql://localhost:3306/flowable_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8&nullCatalogMeansCurrent=true")
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
                .addClasspathResource("请假流程网关.bpmn20.xml")
                .name("请假流程网关").deploy();

        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
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
        variables.put("num", 3);

        // 启动流程实例，第一个参数是流程定义的key
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("qingjiawangguan", variables);
        System.out.println(processInstance.getDeploymentId());
        System.out.println(processInstance.getProcessDefinitionId());
        System.out.println(processInstance.getId());
        System.out.println(processInstance.getActivityId());
    }

    @Test
    public void completeTask() {
        TaskService taskService = configuration.buildProcessEngine().getTaskService();

        // 获取当前任务
        Task task = taskService.createTaskQuery().processDefinitionKey("qingjiawangguan")
//                .taskAssignee("lisi")
                .singleResult();

        // 构建参数;同意请假
        Map<String, Object> map = new HashMap<>();
        map.put("approve", true);

        // 完成任务
        taskService.complete(task.getId(), map);
    }
}
