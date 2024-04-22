package com.faten.rest;

import cn.hutool.json.JSONUtil;
import com.faten.service.ProcessDefService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 请求
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2022/10/25
 */
@Slf4j
@RestController
@RequestMapping("proc/def")
public class ProcessDefController {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private ProcessDefService processDefService;

    /**
     * 发布流程定义
     *
     * @param file bpmn2定义文件
     * @return 发布id
     */
    @PostMapping()
    @SneakyThrows
    public String processDef(@RequestParam MultipartFile file) {
        return processDefService.processDef(file.getInputStream(), file.getOriginalFilename());
    }

    /**
     * 查询流程定义
     *
     * @return 流程定义列表
     */
    @GetMapping("list")
    public String queryProcessDef() {
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();
        return JSONUtil.toJsonStr(list);
    }

    /**
     * 根据流程定义id，获取流程图片
     *
     * @param procDefId 流程定义id
     * @param response  结果
     */
    @GetMapping("diagram")
    public void queryProcessDef(@RequestParam String procDefId, HttpServletResponse response) {
        processDefService.queryProcessDef(procDefId, response);
    }

    /**
     * 流程定义挂起
     *
     * @param procDefId 流程定义id
     * @return {@link Boolean}
     */
    @PostMapping("suspend")
    public Boolean processDefSuspend(@RequestParam String procDefId) {
        return processDefService.processDefSuspend(procDefId);
    }

    /**
     * 流程定义激活
     *
     * @param procDefId 流程定义id
     * @return {@link Boolean}
     */
    @PostMapping("activate")
    public Boolean processDefActivate(String procDefId) {
        return processDefService.processDefActivate(procDefId);
    }
}
