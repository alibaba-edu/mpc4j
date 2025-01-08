package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.stream.IntStream;

/**
 * 2哈希-布谷鸟表测试类。
 *
 * @author Weiran Liu
 * @date 2020/06/18
 */
public class CuckooTableTest {
    /**
     * 测试顶点数量
     */
    private static final int VERTICES_NUM = 10;

    @Test
    public void testH2CuckooTable() {
        H2CuckooTable<String> cuckooTable = new H2CuckooTable<>(VERTICES_NUM);
        Assert.assertEquals(cuckooTable.getNumOfVertices(), VERTICES_NUM);
        Assert.assertEquals(cuckooTable.getDataSet().size(), 0);
        // create edge data
        String[] edgeStrings = IntStream.range(0, 3).mapToObj(index -> "Edge String" + index).toArray(String[]::new);
        // add invalid edge
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{0,}, edgeStrings[0]));
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{0, 1, 2}, edgeStrings[0]));
        // add self-loop edge
        cuckooTable.addData(new int[]{0, 0}, edgeStrings[0]);
        Assert.assertEquals(cuckooTable.getDataSet().size(), 1);
        // add another self-loop with the same vertices but different data
        cuckooTable.addData(new int[]{0, 0}, edgeStrings[1]);
        Assert.assertEquals(cuckooTable.getDataSet().size(), 2);
        // check vertices
        int[] vertices = new int[]{0, 0};
        Set<String> verticesDataSet = cuckooTable.getDataSet(vertices);
        Assert.assertEquals(2, verticesDataSet.size());
        // add illegal self-loop edge with the same data
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{0, 0}, edgeStrings[0]));
        // add a normal edge
        cuckooTable.addData(new int[]{0, 1}, edgeStrings[2]);
        Assert.assertEquals(cuckooTable.getDataSet().size(), 3);
        // add a same edge with same order and same data
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{0, 1}, edgeStrings[2]));
        // add a same edge with different order and same data
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{1, 0}, edgeStrings[2]));
    }

    @Test
    public void testH3CuckooTable() {
        H3CuckooTable<String> cuckooTable = new H3CuckooTable<>(VERTICES_NUM);
        Assert.assertEquals(cuckooTable.getNumOfVertices(), VERTICES_NUM);
        Assert.assertEquals(cuckooTable.getDataNum(), 0);
        // create edge data
        String[] edgeStrings = IntStream.range(0, 6).mapToObj(index -> "Edge String" + index).toArray(String[]::new);
        // add invalid edge
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{0, 1,}, edgeStrings[0]));
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{0, 1, 2, 3}, edgeStrings[0]));
        // add (0, 0, 0) edge
        cuckooTable.addData(new int[]{0, 0, 0}, edgeStrings[0]);
        Assert.assertEquals(cuckooTable.getDataNum(), 1);
        // add (1, 1, 2) edge
        cuckooTable.addData(new int[]{1, 1, 2}, edgeStrings[1]);
        Assert.assertEquals(cuckooTable.getDataNum(), 2);
        // add (1, 2, 1) edge
        cuckooTable.addData(new int[]{1, 2, 1}, edgeStrings[2]);
        Assert.assertEquals(cuckooTable.getDataNum(), 3);
        // add (2, 1, 1) edge
        cuckooTable.addData(new int[]{2, 1, 1}, edgeStrings[3]);
        Assert.assertEquals(cuckooTable.getDataNum(), 4);
        // add (3, 4, 5) edge
        cuckooTable.addData(new int[]{3, 4, 5}, edgeStrings[4]);
        Assert.assertEquals(cuckooTable.getDataNum(), 5);
        // add (3, 4, 5) edge with different data
        cuckooTable.addData(new int[]{3, 4, 5}, edgeStrings[5]);
        Assert.assertEquals(cuckooTable.getDataNum(), 6);
        // add illegal same edge with same data
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{3, 4, 5}, edgeStrings[5]));
        // add illegal same edge with different order, same data
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{3, 5, 4}, edgeStrings[5]));
    }

    @Test
    public void testH4CuckooTable() {
        H4CuckooTable<String> cuckooTable = new H4CuckooTable<>(VERTICES_NUM);
        Assert.assertEquals(cuckooTable.getNumOfVertices(), VERTICES_NUM);
        Assert.assertEquals(cuckooTable.getDataNum(), 0);
        // create different edges
        String[] edgeStrings = IntStream.range(0, 6).mapToObj(index -> "Edge String" + index).toArray(String[]::new);
        // add invalid edge
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{0, 1, 2}, edgeStrings[0]));
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{0, 1, 2, 3, 4}, edgeStrings[0]));
        // add (0, 0, 0, 0)
        cuckooTable.addData(new int[]{0, 0, 0, 0}, edgeStrings[0]);
        Assert.assertEquals(cuckooTable.getDataNum(), 1);
        // add (1, 1, 1, 2)
        cuckooTable.addData(new int[]{1, 1, 1, 2}, edgeStrings[1]);
        Assert.assertEquals(cuckooTable.getDataNum(), 2);
        // add (1, 2, 1, 1)
        cuckooTable.addData(new int[]{1, 2, 1, 1}, edgeStrings[2]);
        Assert.assertEquals(cuckooTable.getDataNum(), 3);
        // add (2, 1, 1, 1)
        cuckooTable.addData(new int[]{2, 1, 1, 1}, edgeStrings[3]);
        Assert.assertEquals(cuckooTable.getDataNum(), 4);
        // add (3, 4, 5, 6)
        cuckooTable.addData(new int[]{3, 4, 5, 6}, edgeStrings[4]);
        Assert.assertEquals(cuckooTable.getDataNum(), 5);
        // add (3, 4, 5, 6) with different data
        cuckooTable.addData(new int[]{3, 4, 5, 6}, edgeStrings[5]);
        Assert.assertEquals(cuckooTable.getDataNum(), 6);
        // add illegal same edge with same data
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{3, 4, 5, 6}, edgeStrings[5]));
        // add illegal same edge with different order, same data
        Assert.assertThrows(IllegalArgumentException.class, () -> cuckooTable.addData(new int[]{6, 3, 5, 4}, edgeStrings[5]));
    }
}
