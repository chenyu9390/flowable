package com.faten;

import com.faten.service.ProcessInstService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ActivityInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 进程实例测试
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2023/3/8
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FlowableDemoApplication.class})
public class ProcessInstServiceTest {

    @Resource
    private ProcessInstService processInstService;

    @Resource
    private RuntimeService runtimeService;

    @Test
    public void deleteActivityAndTask() {
        List<ActivityInstance> list1 = runtimeService.createActivityInstanceQuery().processInstanceId("95278082-bd84-11ed-a445-00be43b83910").list();
        List<String> ids = Arrays.asList("ee71ee6a-bd84-11ed-a445-00be43b83910", "d8b52c6d-bd87-11ed-81bb-00be43b83910");
        List<ActivityInstance> list = list1.stream().filter(a -> ids.contains(a.getExecutionId())).collect(Collectors.toList());
        processInstService.deleteActivityAndTask(list);
    }
}
