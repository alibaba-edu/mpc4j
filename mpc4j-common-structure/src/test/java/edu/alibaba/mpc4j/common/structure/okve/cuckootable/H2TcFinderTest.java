package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * 2哈希-布谷鸟图查找器测试。
 *
 * @author Weiran Liu
 * @date 2021/09/08
 */
@RunWith(Parameterized.class)
public class H2TcFinderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // Self-Loop
        H2CuckooTable<String> selfLoopH2CuckooTable = new H2CuckooTable<>(1);
        selfLoopH2CuckooTable.addData(new int[] {0, 0}, "Self-Loop Node 1");
        selfLoopH2CuckooTable.addData(new int[] {0, 0}, "Self-Loop Node 2");
        configurationParams.add(new Object[] {"Self Loop", selfLoopH2CuckooTable, 2});
        // Tree
        H2CuckooTable<String> treeH2CuckooTable = new H2CuckooTable<>(5);
        treeH2CuckooTable.addData(new int[] {0, 1}, "Tree Node 1");
        treeH2CuckooTable.addData(new int[] {1, 2}, "Tree Node 2");
        treeH2CuckooTable.addData(new int[] {3, 0}, "Tree Node 3");
        treeH2CuckooTable.addData(new int[] {2, 4}, "Tree Node 4");
        configurationParams.add(new Object[] {"Tree", treeH2CuckooTable, 0});
        // Ring
        H2CuckooTable<String> ringH2CuckooTable = new H2CuckooTable<>(4);
        ringH2CuckooTable.addData(new int[] {0, 1}, "Cycle Node 1");
        ringH2CuckooTable.addData(new int[] {1, 2}, "Cycle Node 2");
        ringH2CuckooTable.addData(new int[] {2, 3}, "Cycle Node 3");
        ringH2CuckooTable.addData(new int[] {0, 2}, "Cycle Node 4");
        ringH2CuckooTable.addData(new int[] {0, 1}, "Cycle Node 5");
        ringH2CuckooTable.addData(new int[] {1, 3}, "Cycle Node 6");
        configurationParams.add(new Object[] {"Ring", ringH2CuckooTable, 6});
        // Self Loop with Cycle
        H2CuckooTable<String> selfCycleH2CuckooTable = new H2CuckooTable<>(3);
        selfCycleH2CuckooTable.addData(new int[] {0, 0}, "Self Loop with Cycle Node 1");
        selfCycleH2CuckooTable.addData(new int[] {0, 1}, "Self Loop with Cycle Node 2");
        selfCycleH2CuckooTable.addData(new int[] {1, 1}, "Self Loop with Cycle Node 3");
        configurationParams.add(new Object[] {"Self Loop with Cycle", selfCycleH2CuckooTable, 3});

        return configurationParams;
    }

    /**
     * 2哈希-布谷鸟图
     */
    private final H2CuckooTable<String> h2CuckooTable;
    /**
     * 剩余边数量
     */
    private final int remainedDataSetSize;

    public H2TcFinderTest(String name, H2CuckooTable<String> h2CuckooTable, int remainedDataSetSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.h2CuckooTable = h2CuckooTable;
        this.remainedDataSetSize = remainedDataSetSize;
    }

    @Test
    public void testH2TcFinder() {
        H2CuckooTableTcFinder<String> tcFinder = new H2CuckooTableTcFinder<>();
        tcFinder.findTwoCore(h2CuckooTable);
        Set<String> remainedDataSet = tcFinder.getRemainedDataSet();
        Assert.assertEquals(remainedDataSetSize, remainedDataSet.size());
    }

    @Test
    public void testH2SingletonTcFinder() {
        CuckooTableSingletonTcFinder<String> singletonFinder = new CuckooTableSingletonTcFinder<>();
        singletonFinder.findTwoCore(h2CuckooTable);
        Set<String> remainedDataSet = singletonFinder.getRemainedDataSet();
        Assert.assertEquals(remainedDataSetSize, remainedDataSet.size());
    }
}
