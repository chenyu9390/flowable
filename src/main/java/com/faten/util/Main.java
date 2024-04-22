package com.faten.util;

import org.assertj.core.util.Sets;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;
import java.util.Set;

public class Main {



    public static void main(String[] args) {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        graph.addVertex("1");
        graph.addVertex("2");
        graph.addVertex("3");
        graph.addVertex("4");
        graph.addVertex("5");
        graph.addVertex("6");
        graph.addVertex("7");
        graph.addVertex("8");
        graph.addVertex("9");
        graph.addVertex("10");
        graph.addEdge("1", "2");
        graph.addEdge("2", "3");
        graph.addEdge("2", "4");
        graph.addEdge("2", "5");
        graph.addEdge("3", "6");
        graph.addEdge("4", "7");
        graph.addEdge("5", "8");

        graph.addEdge("6", "9");
        graph.addEdge("7", "9");
        graph.addEdge("8", "9");
        graph.addEdge("9", "10");
        graph.addEdge("10", "3");

        AllDirectedPaths<String, DefaultEdge> allDirectedPaths = new AllDirectedPaths<>(graph);

        // 查询从源顶点到目标顶点的所有路径
        String sourceVertex = "10";
        String targetVertex = "8";
        List<GraphPath<String, DefaultEdge>> paths = allDirectedPaths.getAllPaths(sourceVertex, targetVertex, true, null);
        for (GraphPath<String, DefaultEdge> path : paths) {
            System.out.println(path.getVertexList());
        }
    }
}
