package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import java.util.Set;
import java.util.Stack;

/**
 * 布谷鸟表2-core查找器接口。
 *
 * @author Weiran Liu
 * @date 2021/09/08
 */
public interface CuckooTableTcFinder<T> {

    /**
     * 寻找给定布谷鸟表的2-core图。
     *
     * @param cuckooTable 布谷鸟表。
     */
    void findTwoCore(CuckooTable<T> cuckooTable);

    /**
     * 返回剩余的数据集合。
     *
     * @return 剩余的数据集合。
     */
    Set<T> getRemainedDataSet();

    /**
     * 返回被删除数据的栈。
     *
     * @return 被删除数据的栈。
     */
    Stack<T> getRemovedDataStack();

    /**
     * 返回被删除数据关联顶点的栈。
     *
     * @return 被删除数据关联顶点的栈。
     */
    Stack<int[]> getRemovedDataVertices();
}
