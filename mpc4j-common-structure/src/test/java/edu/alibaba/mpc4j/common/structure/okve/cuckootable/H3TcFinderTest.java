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
 * 3哈希-布谷鸟图的2-core搜寻器测试。
 *
 * @author Weiran Liu
 * @date 2021/09/05
 */
@RunWith(Parameterized.class)
public class H3TcFinderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        // Own Self Loop
        H3CuckooTable<String> ownSelfLoopH3CuckooTable = new H3CuckooTable<>(2);
        ownSelfLoopH3CuckooTable.addData(new int[]{0, 0, 0}, "Own-Self Loop Edge 1");
        ownSelfLoopH3CuckooTable.addData(new int[]{1, 1, 1}, "Own-Self Loop Edge 2");
        configurationParams.add(new Object[]{"Own-Self Loop", ownSelfLoopH3CuckooTable, 0});

        // Self Loop
        H3CuckooTable<String> selfLoopH3CuckooTable = new H3CuckooTable<>(1);
        // 添加2个自指边
        selfLoopH3CuckooTable.addData(new int[]{0, 0, 0}, "Self-Loop Edge 1");
        selfLoopH3CuckooTable.addData(new int[]{0, 0, 0}, "Self-Loop Edge 2");
        configurationParams.add(new Object[]{"Self Loop", selfLoopH3CuckooTable, 2});

        // Tree
        H3CuckooTable<String> treeH3CuckooTable = new H3CuckooTable<>(16);
        // 添加1棵右顶点小树
        treeH3CuckooTable.addData(new int[]{0, 1, 2}, "Right Tree Node 1");
        treeH3CuckooTable.addData(new int[]{1, 2, 3}, "Right Tree Node 2");
        treeH3CuckooTable.addData(new int[]{3, 0, 4}, "Right Tree Node 3");
        // 添加1棵左顶点小树
        treeH3CuckooTable.addData(new int[]{5, 7, 8}, "Left Tree Node 1");
        treeH3CuckooTable.addData(new int[]{6, 8, 9}, "Left Tree Node 2");
        treeH3CuckooTable.addData(new int[]{7, 8, 9}, "Left Tree Node 3");
        // 添加1棵比较大的树
        treeH3CuckooTable.addData(new int[]{10, 11, 12}, "Large Tree Node 1");
        treeH3CuckooTable.addData(new int[]{13, 14, 15}, "Large Tree Node 2");
        treeH3CuckooTable.addData(new int[]{11, 12, 13}, "Large Tree Node 3");
        treeH3CuckooTable.addData(new int[]{11, 12, 14}, "Large Tree Node 4");
        configurationParams.add(new Object[]{"Tree", treeH3CuckooTable, 0});

        // Ring
        H3CuckooTable<String> ringH3CuckooTable = new H3CuckooTable<>(6);
        // 添加一个环
        ringH3CuckooTable.addData(new int[]{0, 1, 2}, "Ring Node 1");
        ringH3CuckooTable.addData(new int[]{3, 4, 5}, "Ring Node 2");
        ringH3CuckooTable.addData(new int[]{1, 2, 3}, "Ring Node 3");
        ringH3CuckooTable.addData(new int[]{1, 2, 4}, "Ring Node 4");
        ringH3CuckooTable.addData(new int[]{0, 4, 5}, "Ring Node 5");
        configurationParams.add(new Object[]{"Ring", ringH3CuckooTable, 5});

        // Semi-Self-Loop Tree
        H3CuckooTable<String> semiSelfLoopTreeH3CuckooTable = new H3CuckooTable<>(6);
        // 添加一个带重复点的树
        semiSelfLoopTreeH3CuckooTable.addData(new int[]{0, 1, 1}, "Left Semi-Self-Loop Tree Edge 1");
        semiSelfLoopTreeH3CuckooTable.addData(new int[]{1, 2, 2}, "Left Semi-Self-Loop Tree Edge 2");
        // 再添加一个带重复点的树
        semiSelfLoopTreeH3CuckooTable.addData(new int[]{3, 3, 4}, "Right Semi-Self-Loop Tree Edge 1");
        semiSelfLoopTreeH3CuckooTable.addData(new int[]{4, 5, 5}, "Right Semi-Self-Loop Tree Edge 2");
        configurationParams.add(new Object[]{"Semi-Self-Loop Tree", semiSelfLoopTreeH3CuckooTable, 0});

        // Semi-Self Loop Graph
        H3CuckooTable<String> semiSelfLoopGraphH3CuckooTable = new H3CuckooTable<>(2);
        // 添加一个带重复点的树
        semiSelfLoopGraphH3CuckooTable.addData(new int[]{0, 0, 1}, "Semi-Self-Loop Graph Edge 1");
        semiSelfLoopGraphH3CuckooTable.addData(new int[]{0, 1, 1}, "Semi-Self-Loop Graph Edge 2");
        configurationParams.add(new Object[]{"Semi-Self-Loop Graph", semiSelfLoopGraphH3CuckooTable, 2});

        // Self-Loop Tree
        H3CuckooTable<String> selfLoopTreeH3CuckooTable = new H3CuckooTable<>(3);
        // 极端情况，添加两个自指边，两个自指边还链接
        selfLoopTreeH3CuckooTable.addData(new int[]{0, 0, 0}, "Self-Loop with Cycle Node Tree 1");
        selfLoopTreeH3CuckooTable.addData(new int[]{1, 1, 1}, "Self-Loop with Cycle Node Tree 2");
        selfLoopTreeH3CuckooTable.addData(new int[]{0, 1, 2}, "Self-Loop with Cycle Node Tree 3");
        configurationParams.add(new Object[]{"Semi-Self-Loop Graph", selfLoopTreeH3CuckooTable, 0});

        H3CuckooTable<String> selfLoopGraphH3CuckooTable = new H3CuckooTable<>(3);
        // 极端情况，添加两个自指边，两个自指边还链接
        selfLoopGraphH3CuckooTable.addData(new int[]{0, 0, 0}, "Self-Loop with Cycle Node 1");
        selfLoopGraphH3CuckooTable.addData(new int[]{1, 1, 1}, "Self-Loop with Cycle Node 2");
        selfLoopGraphH3CuckooTable.addData(new int[]{1, 1, 2}, "Self-Loop with Cycle Node 3");
        selfLoopGraphH3CuckooTable.addData(new int[]{0, 1, 2}, "Self-Loop with Cycle Node 4");
        configurationParams.add(new Object[]{"Self-Loop Graph", selfLoopGraphH3CuckooTable, 4});

        return configurationParams;
    }

    /**
     * 2哈希-布谷鸟图
     */
    private final H3CuckooTable<String> h3CuckooTable;
    /**
     * 剩余边数量
     */
    private final int remainedDataSetSize;

    public H3TcFinderTest(String name, H3CuckooTable<String> h3CuckooTable, int remainedDataSetSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.h3CuckooTable = h3CuckooTable;
        this.remainedDataSetSize = remainedDataSetSize;
    }

    @Test
    public void testCorrectness() {
        CuckooTableSingletonTcFinder<String> singletonFinder = new CuckooTableSingletonTcFinder<>();
        singletonFinder.findTwoCore(h3CuckooTable);
        Set<String> remainedDataSet = singletonFinder.getRemainedDataSet();
        Assert.assertEquals(remainedDataSetSize, remainedDataSet.size());
    }
}
