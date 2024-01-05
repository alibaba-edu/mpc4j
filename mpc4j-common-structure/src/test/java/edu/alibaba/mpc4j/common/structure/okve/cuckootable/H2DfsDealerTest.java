package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import com.google.common.base.Preconditions;
import gnu.trove.map.TIntObjectMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 2哈希-布谷鸟图的深度优先搜索功能测试。
 *
 * @author Weiran Liu
 * @date 2021/09/09
 */
@RunWith(Parameterized.class)
public class H2DfsDealerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // Self-Loop
        H2CuckooTable<String> selfLoopH2CuckooTable = new H2CuckooTable<>(1);
        selfLoopH2CuckooTable.addData(new int[] {0, 0}, "Self-Loop Node 1");
        selfLoopH2CuckooTable.addData(new int[] {0, 0}, "Self-Loop Node 2");
        configurationParams.add(new Object[] {"Self Loop", selfLoopH2CuckooTable, 0, 0});
        // Tree
        H2CuckooTable<String> treeH2CuckooTable = new H2CuckooTable<>(5);
        treeH2CuckooTable.addData(new int[] {0, 1}, "Tree Node 1");
        treeH2CuckooTable.addData(new int[] {1, 2}, "Tree Node 2");
        treeH2CuckooTable.addData(new int[] {3, 0}, "Tree Node 3");
        treeH2CuckooTable.addData(new int[] {2, 4}, "Tree Node 4");
        configurationParams.add(new Object[] {"Tree", treeH2CuckooTable, 4, 4});
        // Ring
        H2CuckooTable<String> ringH2CuckooTable = new H2CuckooTable<>(4);
        ringH2CuckooTable.addData(new int[] {0, 1}, "Cycle Node 1");
        ringH2CuckooTable.addData(new int[] {1, 2}, "Cycle Node 2");
        ringH2CuckooTable.addData(new int[] {2, 3}, "Cycle Node 3");
        ringH2CuckooTable.addData(new int[] {0, 2}, "Cycle Node 4");
        ringH2CuckooTable.addData(new int[] {0, 1}, "Cycle Node 5");
        ringH2CuckooTable.addData(new int[] {1, 3}, "Cycle Node 6");
        configurationParams.add(new Object[] {"Ring", ringH2CuckooTable, 3, 3});
        // Self Loop with Cycle
        H2CuckooTable<String> selfCycleH2CuckooTable = new H2CuckooTable<>(3);
        selfCycleH2CuckooTable.addData(new int[] {0, 0}, "Self Loop with Cycle Node 1");
        selfCycleH2CuckooTable.addData(new int[] {0, 1}, "Self Loop with Cycle Node 2");
        selfCycleH2CuckooTable.addData(new int[] {1, 1}, "Self Loop with Cycle Node 3");
        configurationParams.add(new Object[] {"Self Loop with Cycle", selfCycleH2CuckooTable, 1, 1});

        return configurationParams;
    }

    /**
     * 2哈希-布谷鸟图
     */
    private final H2CuckooTable<String> h2CuckooTable;
    /**
     * 验证顶点
     */
    private final int rootVertex;
    /**
     * 验证顶点的关联边数量
     */
    private final int rootVertexEdgeSize;

    public H2DfsDealerTest(String name, H2CuckooTable<String> h2CuckooTable, int rootVertex, int rootVertexEdgeSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.h2CuckooTable = h2CuckooTable;
        this.rootVertex = rootVertex;
        this.rootVertexEdgeSize = rootVertexEdgeSize;
    }

    @Test
    public void testCorrectness() {
        // 测试正确性
        H2CuckooTableDfsDealer<String> dfsDealer = new H2CuckooTableDfsDealer<>();
        dfsDealer.findCycle(h2CuckooTable);
        TIntObjectMap<ArrayList<String>> firstRootEdgesMap = dfsDealer.getRootTraversalDataMap();
        ArrayList<String> rootVertexEdges = firstRootEdgesMap.get(rootVertex);
        Assert.assertEquals(rootVertexEdgeSize, rootVertexEdges.size());
    }

    @Test
    public void testConsistency() {
        // 第一次搜索
        H2CuckooTableDfsDealer<String> firstDealer = new H2CuckooTableDfsDealer<>();
        firstDealer.findCycle(h2CuckooTable);
        TIntObjectMap<ArrayList<String>> firstRootEdgesMap = firstDealer.getRootTraversalDataMap();
        ArrayList<String> firstRootVertexEdges = firstRootEdgesMap.get(rootVertex);
        // 第二次搜索
        H2CuckooTableDfsDealer<String> secondDealer = new H2CuckooTableDfsDealer<>();
        secondDealer.findCycle(h2CuckooTable);
        TIntObjectMap<ArrayList<String>> secondRootEdgesMap = secondDealer.getRootTraversalDataMap();
        ArrayList<String> secondRootVertexEdges = secondRootEdgesMap.get(rootVertex);
        // 验证一致性
        Assert.assertEquals(firstRootVertexEdges, secondRootVertexEdges);
    }
}
