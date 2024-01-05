package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * 2哈希-布谷鸟表测试类。
 *
 * @author Weiran Liu
 * @date 2020/06/18
 */
public class H2CuckooTableTest {
    /**
     * 测试顶点数量
     */
    private static final int VERTICES_NUM = 10;

    @Test
    public void testH2CuckooTable() {
        H2CuckooTable<String> h2CuckooTable = new H2CuckooTable<>(VERTICES_NUM);
        Assert.assertEquals(h2CuckooTable.getNumOfVertices(), VERTICES_NUM);
        Assert.assertEquals(h2CuckooTable.getDataSet().size(), 0);
        String edgeString1 = "Edge String 1";
        String edgeString2 = "Edge String 2";
        // 插入self-loop边
        h2CuckooTable.addData(new int[] {0, 0}, edgeString1);
        Assert.assertEquals(h2CuckooTable.getDataSet().size(), 1);
        // 插入另一条self-loop边，但数据不同，理论上可以添加成功
        h2CuckooTable.addData(new int[] {0, 0}, edgeString2);
        Assert.assertEquals(h2CuckooTable.getDataSet().size(), 2);
        // 查看起点到终点是否包含两个元素
        int[] vertices = new int[] {0, 0};
        Set<String> verticesDataSet = h2CuckooTable.getDataSet(vertices);
        Assert.assertEquals(2, verticesDataSet.size());
        // 插入self-loop边，数据相同，应该添加失败
        try {
            h2CuckooTable.addData(new int[] {0, 0}, edgeString1);
            Assert.fail("ERROR: Successfully insert duplicate item");
        } catch (IllegalArgumentException ignored) {

        }
        // 插入正常边，并查看是否可以得到插入的边
        String edgeString3 = "Edge String 3";
        h2CuckooTable.addData(new int[] {0, 1}, edgeString3);
        Assert.assertEquals(h2CuckooTable.getDataSet().size(), 3);
    }
}
