package com.faten.util;

import cn.hutool.core.collection.CollUtil;
import com.faten.entity.TaskSequenceFlow;
import org.flowable.bpmn.model.*;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

/**
 * bpmn工具类
 *
 * @author faten zhang
 * @version 1.0.0
 * @date 2022/12/3
 */
public class BpmnUtils {

    /**
     * 查询两个节点间的节点，从当前节点向上查找，用于驳回；只支持区间不含并行、包含网关的情况
     *
     * @param sourceId   当前节点
     * @param targetId   驳回节点
     * @param oldNodeIds 存储查找过得节点
     */
    public static void findBetweenNodesForUp(BpmnModel bpmnModel, String sourceId, String targetId, Set<String> oldNodeIds) {
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

            findBetweenNodesForUp(bpmnModel, flow.getSourceRef(), targetId, oldNodeIds);
        }
    }

    /**
     * @param bpmnModel
     * @param sourceId
     * @param targetId
     * @return {@link TaskSequenceFlow}
     */
    public static List<List<String>> flowPath(BpmnModel bpmnModel, String sourceId, String targetId){
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Collection<FlowElement> flowElements = bpmnModel.getProcesses().get(0).getFlowElements();
        for (Map.Entry<String, GraphicInfo> entry : bpmnModel.getLocationMap().entrySet()) {
            graph.addVertex(entry.getKey());
        }
        for (Map.Entry<String, List<GraphicInfo>> entry : bpmnModel.getFlowLocationMap().entrySet()) {
            graph.addVertex(entry.getKey());
        }
        flowElements.stream().filter(f -> f instanceof SequenceFlow).forEach(f -> {
            SequenceFlow flow = (SequenceFlow)f;
            graph.addEdge(flow.getSourceRef(),flow.getId());
            graph.addEdge(flow.getId(),flow.getTargetRef());
        });
        AllDirectedPaths<String, DefaultEdge> allDirectedPaths = new AllDirectedPaths<>(graph);
        List<GraphPath<String, DefaultEdge>> paths = allDirectedPaths.getAllPaths(sourceId, targetId, true, null);
        List<List<String>> nodeList = new ArrayList<>();
        for (GraphPath<String, DefaultEdge> path : paths) {
            nodeList.add(path.getVertexList());
        }
        return nodeList;
    }



    /**
     * 查询在当前节点到驳回节点间是否有并行网关或包含网关(true不包含)
     *
     * @param bpmnModel  流程定义
     * @param sourceId   当前节点
     * @param targetId   驳回节点
     * @param oldNodeIds 存储查找过得节点，用于处理形成环形的情况
     * @return 校验结果
     */
    public static boolean checkNotHasGatewayForMiddle(BpmnModel bpmnModel, String sourceId, String targetId, Set<String> oldNodeIds) {
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

            if (checkNotHasGatewayForMiddle(bpmnModel, flow.getSourceRef(), targetId, oldNodeIds)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 查询当前节点向上是否有并行网关或包含网关，且并行条数大于1
     *
     * @param bpmnModel  流程定义
     * @param sourceId   当前节点
     * @param oldNodeIds 存储查找过得节点，用于处理形成环形的情况
     * @return 校验结果
     */
    public static boolean checkHasGatewayForUp(BpmnModel bpmnModel, String sourceId, Set<String> oldNodeIds) {
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
     * @param bpmnModel  流程定义
     * @param targetId   驳回节点
     * @param oldNodeIds 存储查找过的节点，用于处理形成环形的情况
     * @return 校验结果
     */
    public static boolean checkHasGatewayForDown(BpmnModel bpmnModel, String targetId, Set<String> oldNodeIds) {
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
}
