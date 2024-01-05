package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 布谷鸟表抽象类。
 *
 * @author Weiran Liu
 * @date 2021/09/08
 */
abstract class AbstractCuckooTable<T> implements CuckooTable<T> {
    /**
     * 哈希函数数量
     */
    private final int hashNum;
    /**
     * 布谷鸟图顶点的数量，即哈希函数输出范围
     */
    private final int numOfVertices;
    /**
     * 数据与数据字节的映射
     */
    private final Map<T, ByteBuffer> dataBytesMap;
    /**
     * 数据与边的集合
     */
    private final Map<T, int[]> dataVertexMap;
    /**
     * 布谷鸟图
     */
    private final ArrayList<Set<T>> cuckooGraph;

    /**
     * 布谷鸟哈希图构造函数。
     *
     * @param numOfVertices 顶点总数量。
     * @param hashNum       哈希函数数量。
     */
    AbstractCuckooTable(int numOfVertices, int hashNum) {
        assert numOfVertices > 0;
        this.numOfVertices = numOfVertices;
        this.hashNum = hashNum;
        dataBytesMap = new HashMap<>(numOfVertices);
        cuckooGraph = IntStream.range(0, numOfVertices)
            .mapToObj(vertex -> new HashSet<T>(hashNum))
            .collect(Collectors.toCollection(ArrayList::new));
        // 构造数据集合
        dataVertexMap = new HashMap<>(numOfVertices);
    }

    @Override
    public int getNumOfVertices() {
        return numOfVertices;
    }

    @Override
    public void addData(int[] vertices, T data) {
        assert vertices.length == hashNum;
        // 不需要验证vertices是否包含相同的顶点索引值，因为2哈希-布谷鸟图允许重复的定点索引值
        for (int vertex : vertices) {
            assert vertex >= 0 && vertex < numOfVertices;
        }
        if (dataBytesMap.containsKey(data)) {
            throw new IllegalArgumentException("Inserted items contain duplicate item: " + data);
        }
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer dataByteBuffer = ByteBuffer.wrap(dataBytes);
        int[] sortedVertices = Arrays.stream(vertices).sorted().toArray();
        // 向顶点的数据集合插入元素
        Arrays.stream(sortedVertices).forEach(vertex -> cuckooGraph.get(vertex).add(data));
        // 将输入添加到本地缓存中
        dataBytesMap.put(data, dataByteBuffer);
        dataVertexMap.put(data, sortedVertices);
    }

    @Override
    public int getDataNum() {
        return dataBytesMap.size();
    }

    @Override
    public Set<T> getDataSet() {
        return dataBytesMap.keySet();
    }

    @Override
    public Set<T> getDataSet(int[] vertices) {
        assert vertices.length == this.hashNum;
        int[] sortedVertices = Arrays.stream(vertices).sorted().toArray();
        int source = sortedVertices[0];
        // 复制一份起点所包含的所有元素，依次拿掉终点不是指定顶点的元素
        return new HashSet<>(cuckooGraph.get(source)).stream()
            .filter(candidateData -> {
                int[] candidateDataVertices = getVertices(candidateData);
                return Arrays.equals(sortedVertices, candidateDataVertices);
            })
            .collect(Collectors.toSet());
    }

    @Override
    public byte[] getDataBytes(T data) {
        return dataBytesMap.get(data).array();
    }

    @Override
    public int[] getVertices(T data) {
        return dataVertexMap.get(data);
    }

    @Override
    public String getEdgeString(T data) {
        return Arrays.toString(getVertices(data)) + ": " + data.toString();
    }

    @Override
    public ArrayList<Set<T>> getCuckooGraph() {
        return cuckooGraph;
    }
}
