package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import java.util.ArrayList;
import java.util.Set;

/**
 * 布谷鸟表接口。
 *
 * @author Weiran Liu
 * @date 2021/09/08
 */
public interface CuckooTable<T> {

    /**
     * 返回顶点总数量。
     *
     * @return 顶点总数量。
     */
    int getNumOfVertices();

    /**
     * 插入一条数据。
     *
     * @param vertices 涉及顶点。
     * @param data     插入边所对应的数据。
     */
    void addData(int[] vertices, T data);

    /**
     * 返回插入的数据量。
     *
     * @return 插入的数据量。
     */
    int getDataNum();

    /**
     * 返回数据集合。
     *
     * @return 数据集合。
     */
    Set<T> getDataSet();

    /**
     * 返回给定顶点中的所有数据。
     *
     * @param vertices 顶点。
     * @return 顶点中的所有数据。
     */
    Set<T> getDataSet(int[] vertices);

    /**
     * 返回数据对应的字节数组。
     *
     * @param data 数据。
     * @return 数据对应的字节数组。
     */
    byte[] getDataBytes(T data);

    /**
     * 返回给定数据所对应的顶点。
     *
     * @param data 数据。
     * @return 数据所对应的顶点，如果尚未插入此数据，则返回{@code null}。
     */
    int[] getVertices(T data);

    /**
     * 返回边。
     *
     * @param data 数据。
     * @return 数据的边。
     */
    String getEdgeString(T data);

    /**
     * 返回布谷鸟图。
     *
     * @return 布谷鸟图。
     */
    ArrayList<Set<T>> getCuckooGraph();
}
