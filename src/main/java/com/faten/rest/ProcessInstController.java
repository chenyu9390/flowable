package com.faten.rest;

import com.faten.entity.ProcessHiTask;
import com.faten.entity.ProcessTask;
import com.faten.service.ProcessInstService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 流程实例
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2023/2/21
 */
@Slf4j
@RestController
@RequestMapping("proc/inst")
public class ProcessInstController {

    @Resource
    private ProcessInstService processInstService;

    /**
     * 流程实例发起
     *
     * @param variables 参数(流程定义id)
     * @return 流程实例id
     */
    @PostMapping("start")
    public String processInstStart(@RequestBody Map<String, Object> variables) {
        return processInstService.processInstStart(variables);
    }

    /**
     * 查询流程实例
     *
     * @param procInstId 流程实体id
     */
    @GetMapping("query")
    public void queryProcessInst(@RequestParam String procInstId, HttpServletResponse response) {
        processInstService.queryProcessInst(procInstId, response);
    }

    /**
     * 查询流程实例任务
     *
     * @param procInstId 流程实例id
     * @return 任务列表
     */
    @GetMapping("task/query")
    public List<ProcessTask> queryProcessInstTask(@RequestParam String procInstId) {
        return processInstService.queryProcessInstTask(procInstId);
    }

    /**
     * 查询流程实例任务
     *
     * @param procInstId 流程实例id
     * @return 任务列表
     */
    @GetMapping("/hi/task/query")
    public List<ProcessHiTask> queryProcessInstHiTask(@RequestParam String procInstId) {
        return processInstService.queryProcessInstHiTask(procInstId);
    }

    /**
     * 完成流程实例任务
     *
     * @param variables 参数(任务id)
     * @return {@link Boolean}
     */
    @PostMapping("task/complete")
    public Boolean completeProcessInstTask(@RequestBody Map<String, Object> variables) {
        return processInstService.completeProcessInstTask(variables);
    }

    /**
     * 驳回流程实例任务
     *
     * @param variables 参数(任务id，驳回任务id)
     * @return {@link Boolean}
     */
    @PostMapping("task/reject")
    public Boolean rejectProcessInstTask(@RequestBody Map<String, Object> variables) {
        return processInstService.reject(variables);
    }

    /**
     * 转交当前用户任务
     *
     * @return variables 参数(任务id，被转交人)
     * @return {@link Boolean}
     */
    @PostMapping("task/hand/over")
    public Boolean handOverProcessInstTask(@RequestBody Map<String, Object> variables) {
        return processInstService.handOverProcessInstTask(variables);
    }

    /**
     * 终止审批
     *
     * @param procInstId 流程实例id
     * @return {@link Boolean}
     */
    @PostMapping("stop")
    public Boolean stopProcessInst(@RequestParam String procInstId) {
        return processInstService.stopProcessInst(procInstId);
    }

    /**
     * 删除审批
     *
     * @param procInstId 流程实例id
     * @return {@link Boolean}
     */
    @PostMapping("delete")
    public Boolean deleteProcessInst(@RequestParam String procInstId) {
        return processInstService.deleteProcessInst(procInstId);
    }

    @PostMapping("suspend")
    public Boolean suspendProcessInst(@RequestParam String procInstId) {
        return processInstService.suspendProcessInst(procInstId);
    }

    /**
     * 流程定义激活
     *
     * @param procInstId 流程定义id
     * @return {@link Boolean}
     */
    @PostMapping("activate")
    public Boolean activateProcessInst(@RequestParam String procInstId) {
        return processInstService.activateProcessInst(procInstId);
    }

    /**
     * 流程定义复制
     *
     * @param originalInstanceId 要复制的原流程实例ID
     * @return {@link Boolean} 新创建的流程实例ID
     */
    @PostMapping("copy")
    public String copyProcessInst(@RequestParam String originalInstanceId,@RequestParam String processDefinitionKey) {
        return processInstService.copyProcessInst(originalInstanceId,processDefinitionKey);
    }
}
