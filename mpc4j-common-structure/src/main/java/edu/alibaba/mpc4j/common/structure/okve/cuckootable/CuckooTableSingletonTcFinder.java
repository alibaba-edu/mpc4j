package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 布谷鸟图单例图查找方法。
 *
 * @author Weiran Liu
 * @date 2021/09/08
 */
public class CuckooTableSingletonTcFinder<T> implements CuckooTableTcFinder<T> {
    /**
     * 布谷鸟图
     */
    private CuckooTable<T> cuckooTable;
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
     * core图
     */
    private ArrayList<Set<T>> coreCuckooGraph;

    @Override
    public void findTwoCore(CuckooTable<T> cuckooTable) {
        this.cuckooTable = cuckooTable;
        int numOfVertices = cuckooTable.getNumOfVertices();
        coreCuckooGraph = new ArrayList<>();
        // 构建搜索节点集合
        TIntSet twoCoreVertexSet = new TIntHashSet(IntStream.range(0, numOfVertices).toArray());
        ArrayList<Set<T>> cuckooGraph = cuckooTable.getCuckooGraph();
        IntStream.range(0, numOfVertices).forEach(vertex -> {
            // 只添加度大于等于1的顶点
            Set<T> cuckooGraphVertexDataSet = cuckooGraph.get(vertex);
            Set<T> coreCuckooGraphVertexDataSet;
            if (cuckooGraphVertexDataSet.size() > 0) {
                // 复制一份
                coreCuckooGraphVertexDataSet = new HashSet<>(cuckooGraphVertexDataSet);
            } else {
                // 创建个空的集合，提前拿掉已经为空的节点
                coreCuckooGraphVertexDataSet = new HashSet<>(0);
                twoCoreVertexSet.remove(vertex);
            }
            coreCuckooGraph.add(coreCuckooGraphVertexDataSet);
        });
        // 构建删除点的栈和集合
        removedDataStack = new Stack<>();
        removedDataVertices = new Stack<>();
        remainedDataSet = new HashSet<>(this.cuckooTable.getDataSet());
        findSingletons(twoCoreVertexSet);
    }

    private void findSingletons(TIntSet twoCoreVertexSet) {
        TIntLinkedList singletonQueue = new TIntLinkedList();
        // 先扫描一遍所有可能的顶点，把Singleton的加进去
        for (int vertex : twoCoreVertexSet.toArray()) {
            Set<T> vertexDataSet = coreCuckooGraph.get(vertex);
            if (vertexDataSet.size() == 1) {
                singletonQueue.add(vertex);
            }
        }
        // 开始遍历节点
        while (singletonQueue.size() > 0) {
            // 仍然存在Singleton，提取出来一个删掉
            int singletonVertex = singletonQueue.get(0);
            singletonQueue.removeAt(0);
            Set<T> singletonVertexDataSet = coreCuckooGraph.get(singletonVertex);
            // 有可能出现等于0的情况，这是因为最后一次删除的时候把两个节点都删空了，因此这里再判断一下
            if (singletonVertexDataSet.size() == 1) {
                // 此节点只有一个数据，移除数据和节点，这里虽然用的迭代器，但其实只能拿出一个节点
                T data = null;
                for (T containedData : singletonVertexDataSet) {
                    data = containedData;
                }
                assert (data != null);
                int[] vertices = cuckooTable.getVertices(data);
                // 从所有集合中删除数据
                for (int vertex : vertices) {
                    if (twoCoreVertexSet.contains(vertex)) {
                        Set<T> vertexDataSet = coreCuckooGraph.get(vertex);
                        vertexDataSet.remove(data);
                        if (vertexDataSet.size() == 1) {
                            singletonQueue.add(vertex);
                        } else if (vertexDataSet.isEmpty()) {
                            twoCoreVertexSet.remove(vertex);
                        }
                    }
                }
                // 移除此节点
                twoCoreVertexSet.remove(singletonVertex);
                // 把此数据标记为删除数据
                removedDataStack.push(data);
                removedDataVertices.push(vertices);
                remainedDataSet.remove(data);
            }
        }
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
