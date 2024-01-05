package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * 布谷鸟图测试类。
 *
 * @author Weiran Liu
 * @date 2021/09/05
 */
public class H3CuckooTableTest {
    /**
     * 测试顶点数量
     */
    private static final int VERTICES_NUM = 10;

    @Test
    public void testH3CuckooTable() {
        H3CuckooTable<String> h3CuckooTable = new H3CuckooTable<>(VERTICES_NUM);
        Assert.assertEquals(h3CuckooTable.getNumOfVertices(), VERTICES_NUM);
        Assert.assertEquals(h3CuckooTable.getDataNum(), 0);
        // 创建不同种类的边
        String[] edgeStrings = IntStream.range(0, 6).mapToObj(index -> "Edge String" + index).toArray(String[]::new);
        int dataNum = 0;
        // 插入(0, 0, 0)边，应该可以添加成功
        h3CuckooTable.addData(new int[]{0, 0, 0}, edgeStrings[dataNum]);
        dataNum++;
        Assert.assertEquals(h3CuckooTable.getDataNum(), dataNum);
        // 插入(1, 1, 2)边，应该可以添加成功
        h3CuckooTable.addData(new int[]{1, 1, 2}, edgeStrings[dataNum]);
        dataNum++;
        Assert.assertEquals(h3CuckooTable.getDataNum(), dataNum);
        // 插入(1, 2, 1)边，应该可以添加成功
        h3CuckooTable.addData(new int[]{1, 2, 1}, edgeStrings[dataNum]);
        dataNum++;
        Assert.assertEquals(h3CuckooTable.getDataNum(), dataNum);
        // 插入(2, 1, 1)边，应该可以添加成功
        h3CuckooTable.addData(new int[]{2, 1, 1}, edgeStrings[dataNum]);
        dataNum++;
        Assert.assertEquals(h3CuckooTable.getDataNum(), dataNum);
        // 插入(3, 4, 5)边，应该可以添加成功
        h3CuckooTable.addData(new int[]{3, 4, 5}, edgeStrings[dataNum]);
        dataNum++;
        Assert.assertEquals(h3CuckooTable.getDataNum(), dataNum);
        // 插入(3, 4, 5)边，数据不同，应该可以添加成功
        h3CuckooTable.addData(new int[]{3, 4, 5}, edgeStrings[dataNum]);
        dataNum++;
        Assert.assertEquals(h3CuckooTable.getDataNum(), dataNum);
        // 插入相同的边，数据相同，应该失败
        try {
            h3CuckooTable.addData(new int[]{3, 4, 5}, edgeStrings[dataNum - 1]);
            Assert.fail("ERROR: Successfully insert duplicate item");
        } catch (IllegalArgumentException ignored) {

        }
        // 插入顺序不同但排序相同的边，数据相同，应该失败
        try {
            h3CuckooTable.addData(new int[]{3, 5, 4}, edgeStrings[dataNum - 1]);
            Assert.fail("ERROR: Successfully insert duplicate item");
        } catch (IllegalArgumentException ignored) {

        }
    }
}
