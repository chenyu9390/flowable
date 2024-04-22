package com.faten.service;

import cn.hutool.core.io.IoUtil;
import lombok.SneakyThrows;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Date;

/**
 * 服务
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2023/2/20
 */
@Service
public class ProcessDefService {

    @Resource
    private RepositoryService repositoryService;

    /**
     * 发布流程定义
     *
     * @param inputStream bpmn2定义文件输入流
     * @param fileName 文件名
     * @return 发布id
     */
    public String processDef(InputStream inputStream, String fileName) {
        Deployment deploy = repositoryService.createDeployment()
                .addInputStream(fileName, inputStream)
                .name(fileName.split("\\.")[0]).deploy();

        // 根据发布id查询流程定义
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploy.getId()).singleResult();
        return processDefinition.getId();
    }

    /**
     * 根据流程定义id，获取流程图片
     *
     * @param procDefId 流程定义id
     * @param response  结果
     */
    @SneakyThrows
    public void queryProcessDef(String procDefId, HttpServletResponse response) {
        InputStream processDiagram = repositoryService.getProcessDiagram(procDefId);
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        IoUtil.copy(processDiagram, response.getOutputStream());
    }

    /**
     * 流程定义挂起
     *
     * @param procDefId 流程定义id
     * @return {@link Boolean}
     */
    public Boolean processDefSuspend(String procDefId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(procDefId)
                .singleResult();
        if (!processDefinition.isSuspended()) {
            repositoryService.suspendProcessDefinitionById(procDefId, true, new Date());
        }
        return true;
    }

    /**
     * 流程定义激活
     *
     * @param procDefId 流程定义id
     * @return {@link Boolean}
     */
    public Boolean processDefActivate(String procDefId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(procDefId)
                .singleResult();
        if (processDefinition.isSuspended()) {
            repositoryService.activateProcessDefinitionById(procDefId, true, new Date());
        }
        return true;
    }
}
