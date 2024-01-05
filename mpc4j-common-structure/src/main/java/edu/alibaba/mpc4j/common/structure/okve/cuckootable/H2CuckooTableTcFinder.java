package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 2哈希-布谷鸟表2-core图寻找工具类。此方法要比寻找Singleton的方法快，但只适用于2哈希-布谷鸟表。
 *
 * @author Weiran Liu
 * @date 2021/09/08
 */
public class H2CuckooTableTcFinder<T> implements CuckooTableTcFinder<T> {
    /**
     * 2哈希-布谷鸟表
     */
    private H2CuckooTable<T> h2CuckooTable;
    /**
     * 2哈希-布谷鸟图
     */
    private ArrayList<Set<T>> h2CuckooGraph;
    /**
     * 2哈希-布谷鸟图顶点集合
     */
    private TIntSet h2CuckooGraphVertexSet;
    /**
     * 删除数据的栈
     */
    private Stack<T> removedDataStack;
    /**
     * 遗留的数据集合
     */
    private Set<T> remainedDataSet;
    /**
     * 删除数据对应顶点的栈
     */
    private Stack<int[]> removedDataVertices;
    /**
     * 2-core图
     */
    private ArrayList<Set<T>> coreCuckooGraph;
    /**
     * 2-core图顶点集合
     */
    private TIntSet coreGraphVertexSet;

    @Override
    public void findTwoCore(CuckooTable<T> cuckooTable) {
        assert (cuckooTable instanceof H2CuckooTable);
        h2CuckooTable = (H2CuckooTable<T>)cuckooTable;
        h2CuckooGraph = h2CuckooTable.getCuckooGraph();
        int numOfVertices = h2CuckooTable.getNumOfVertices();
        int[] vertexArray = IntStream.range(0, numOfVertices).toArray();
        h2CuckooGraphVertexSet = new TIntHashSet(vertexArray);
        coreCuckooGraph = new ArrayList<>();
        IntStream.range(0, numOfVertices).forEach(vertex -> {
            // 只添加度大于等于1的顶点
            Set<T> cuckooGraphVertexDataSet = h2CuckooGraph.get(vertex);
            Set<T> coreCuckooGraphVertexDataSet;
            if (cuckooGraphVertexDataSet.size() > 0) {
                // 复制一份
                coreCuckooGraphVertexDataSet = new HashSet<>(cuckooGraphVertexDataSet);
            } else {
                // 创建个空的集合，提前拿掉已经为空的节点
                coreCuckooGraphVertexDataSet = new HashSet<>(0);
                h2CuckooGraphVertexSet.remove(vertex);
            }
            coreCuckooGraph.add(coreCuckooGraphVertexDataSet);
        });
        coreGraphVertexSet = new TIntHashSet(h2CuckooGraphVertexSet);
        // 构建删除点的栈和集合
        removedDataStack = new Stack<>();
        removedDataVertices = new Stack<>();
        remainedDataSet = new HashSet<>(h2CuckooTable.getDataSet());
        // 应用dfs迭代删除
        findTwoCoreGraph();
    }

    private void findTwoCoreGraph() {
        // 构造标记映射
        TIntObjectMap<Boolean> h2CuckooGraphVertexMarkMap = new TIntObjectHashMap<>(h2CuckooGraphVertexSet.size());
        for (int vertex : h2CuckooGraphVertexSet.toArray()) {
            h2CuckooGraphVertexMarkMap.put(vertex, Boolean.FALSE);
        }
        for (int vertex : h2CuckooGraphVertexSet.toArray()) {
            // 由于处理前面顶点的过程中可能会删除后面的顶点，因此当该顶点未被处理过且当前2core图仍包含此顶点时，才需要再处理此顶点
            if (!h2CuckooGraphVertexMarkMap.get(vertex) && coreGraphVertexSet.contains(vertex)) {
                findTwoCoreGraph(h2CuckooGraphVertexMarkMap, vertex);
            }
        }
    }

    private void findTwoCoreGraph(TIntObjectMap<Boolean> h2CuckooGraphVertexMarkMap, int vertex) {
        if (!h2CuckooGraphVertexMarkMap.get(vertex)) {
            h2CuckooGraphVertexMarkMap.put(vertex, Boolean.TRUE);
            Set<T> coreVertexDataSet = coreCuckooGraph.get(vertex);
            // 如果顶点的度小于1，则把自己删除
            if (coreCuckooGraph.get(vertex).size() <= 1) {
                processSelfVertex(vertex);
            } else {
                // 如果度大于1，则迭代处理，因为后面迭代算法会动态删除，会导致边动态变化，因此将边复制一份出来
                Set<T> copiedCoreVertexDataSet = new HashSet<>(coreVertexDataSet);
                for (T data : copiedCoreVertexDataSet) {
                    if (remainedDataSet.contains(data)) {
                        // 如果这条边没被迭代过程删除，则继续处理
                        int[] vertices = h2CuckooTable.getVertices(data);
                        int target = vertex == vertices[0] ? vertices[1] : vertices[0];
                        // 如果下一节点还没有处理，则处理下一节点
                        if (!h2CuckooGraphVertexMarkMap.get(target)) {
                            findTwoCoreGraph(h2CuckooGraphVertexMarkMap, target);
                        }
                    }
                }
                // 所有临边都处理完毕后，再看自己是不是变成了度小于1的顶点，如果是，则处理自己
                if (coreVertexDataSet.size() <= 1) {
                    processSelfVertex(vertex);
                }
            }
        }
    }

    private void processSelfVertex(int targetVertex) {
        Set<T> coreVertexDataSet = coreCuckooGraph.get(targetVertex);
        // 如果有边，把边拿出来
        if (coreVertexDataSet.size() == 1) {
            T data = null;
            for (T containedData : coreVertexDataSet) {
                data = containedData;
            }
            remainedDataSet.remove(data);
            removedDataStack.push(data);
            int[] vertices = h2CuckooTable.getVertices(data);
            removedDataVertices.push(vertices);
            for (int vertex : vertices) {
                coreCuckooGraph.get(vertex).remove(data);
            }
        }
        // 删除顶点
        coreGraphVertexSet.remove(targetVertex);
    }

    @Override
    public Set<T> getRemainedDataSet() {
        return remainedDataSet;
    }

    @Override
    public Stack<T> getRemovedDataStack() {
        return removedDataStack;
    }

    @Override
    public Stack<int[]> getRemovedDataVertices() {
        return removedDataVertices;
    }
}
