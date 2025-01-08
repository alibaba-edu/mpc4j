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
 * (4 hash) two-core finder test.
 *
 * @author Weiran Liu
 * @date 2024/7/25
 */
@RunWith(Parameterized.class)
public class H4TcFinderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Own Self Loop
        H4CuckooTable<String> ownSelfLoopCuckooTable = new H4CuckooTable<>(2);
        ownSelfLoopCuckooTable.addData(new int[]{0, 0, 0, 0}, "Own-Self Loop Edge 1");
        ownSelfLoopCuckooTable.addData(new int[]{1, 1, 1, 0}, "Own-Self Loop Edge 2");
        configurations.add(new Object[]{"Own-Self Loop", ownSelfLoopCuckooTable, 0});

        // Self Loop
        H4CuckooTable<String> selfLoopCuckooTable = new H4CuckooTable<>(1);
        // 添加2个自指边
        selfLoopCuckooTable.addData(new int[]{0, 0, 0, 0}, "Self-Loop Edge 1");
        selfLoopCuckooTable.addData(new int[]{0, 0, 0, 0}, "Self-Loop Edge 2");
        configurations.add(new Object[]{"Self Loop", selfLoopCuckooTable, 2});

        // Tree
        H4CuckooTable<String> treeCuckooTable = new H4CuckooTable<>(16);
        // add a subtree with right root
        treeCuckooTable.addData(new int[]{0, 1, 2, 3}, "Right Tree Node 1");
        treeCuckooTable.addData(new int[]{1, 2, 3, 4}, "Right Tree Node 2");
        treeCuckooTable.addData(new int[]{3, 0, 4, 5}, "Right Tree Node 3");
        // add a subtree with left root
        treeCuckooTable.addData(new int[]{5, 7, 8, 9}, "Left Tree Node 1");
        treeCuckooTable.addData(new int[]{6, 8, 9, 10}, "Left Tree Node 2");
        treeCuckooTable.addData(new int[]{7, 8, 9, 10}, "Left Tree Node 3");
        // add a large tree
        treeCuckooTable.addData(new int[]{9, 9, 10, 11}, "Large Tree Node 1");
        treeCuckooTable.addData(new int[]{12, 13, 14, 15}, "Large Tree Node 2");
        treeCuckooTable.addData(new int[]{10, 11, 12, 13}, "Large Tree Node 3");
        treeCuckooTable.addData(new int[]{10, 11, 12, 14}, "Large Tree Node 4");
        configurations.add(new Object[]{"Tree", treeCuckooTable, 0});

        // Ring
        H4CuckooTable<String> ringCuckooTable = new H4CuckooTable<>(7);
        ringCuckooTable.addData(new int[]{0, 1, 2, 3}, "Ring Node 1");
        ringCuckooTable.addData(new int[]{3, 4, 5, 6}, "Ring Node 2");
        ringCuckooTable.addData(new int[]{1, 2, 3, 4}, "Ring Node 3");
        ringCuckooTable.addData(new int[]{1, 2, 4, 5}, "Ring Node 4");
        ringCuckooTable.addData(new int[]{0, 4, 5, 6}, "Ring Node 5");
        configurations.add(new Object[]{"Ring", ringCuckooTable, 5});

        // Semi-Self-Loop Tree
        H4CuckooTable<String> semiSelfLoopTreeCuckooTable = new H4CuckooTable<>(6);
        // left semi-self-loop
        semiSelfLoopTreeCuckooTable.addData(new int[]{0, 1, 1, 1}, "Left Semi-Self-Loop Tree Edge 1");
        semiSelfLoopTreeCuckooTable.addData(new int[]{1, 2, 2, 2}, "Left Semi-Self-Loop Tree Edge 2");
        // right semi-self-loop
        semiSelfLoopTreeCuckooTable.addData(new int[]{3, 3, 4, 5}, "Right Semi-Self-Loop Tree Edge 1");
        semiSelfLoopTreeCuckooTable.addData(new int[]{4, 5, 5, 5}, "Right Semi-Self-Loop Tree Edge 2");
        configurations.add(new Object[]{"Semi-Self-Loop Tree", semiSelfLoopTreeCuckooTable, 0});

        // Semi-Self Loop Graph
        H4CuckooTable<String> semiSelfLoopGraphCuckooTable = new H4CuckooTable<>(2);
        semiSelfLoopGraphCuckooTable.addData(new int[]{0, 0, 1, 1}, "Semi-Self-Loop Graph Edge 1");
        semiSelfLoopGraphCuckooTable.addData(new int[]{0, 1, 1, 1}, "Semi-Self-Loop Graph Edge 2");
        configurations.add(new Object[]{"Semi-Self-Loop Graph", semiSelfLoopGraphCuckooTable, 2});

        // Self-Loop Tree
        H4CuckooTable<String> selfLoopTreeH3CuckooTable = new H4CuckooTable<>(3);
        // self-loop with cycle node tree
        selfLoopTreeH3CuckooTable.addData(new int[]{0, 0, 0, 0}, "Self-Loop with Cycle Node Tree 1");
        selfLoopTreeH3CuckooTable.addData(new int[]{1, 1, 1, 1}, "Self-Loop with Cycle Node Tree 2");
        selfLoopTreeH3CuckooTable.addData(new int[]{0, 1, 2, 2}, "Self-Loop with Cycle Node Tree 3");
        configurations.add(new Object[]{"Semi-Self-Loop Graph", selfLoopTreeH3CuckooTable, 0});

        H4CuckooTable<String> selfLoopGraphH3CuckooTable = new H4CuckooTable<>(3);
        // Self-Loop with Cycle Node
        selfLoopGraphH3CuckooTable.addData(new int[]{0, 0, 0, 0}, "Self-Loop with Cycle Node 1");
        selfLoopGraphH3CuckooTable.addData(new int[]{1, 1, 1, 1}, "Self-Loop with Cycle Node 2");
        selfLoopGraphH3CuckooTable.addData(new int[]{1, 1, 2, 2}, "Self-Loop with Cycle Node 3");
        selfLoopGraphH3CuckooTable.addData(new int[]{0, 1, 2, 2}, "Self-Loop with Cycle Node 4");
        configurations.add(new Object[]{"Self-Loop Graph", selfLoopGraphH3CuckooTable, 4});

        return configurations;
    }

    /**
     * cuckoo table
     */
    private final H4CuckooTable<String> cuckooTable;
    /**
     * remained dataset size
     */
    private final int remainedDataSetSize;

    public H4TcFinderTest(String name, H4CuckooTable<String> cuckooTable, int remainedDataSetSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.cuckooTable = cuckooTable;
        this.remainedDataSetSize = remainedDataSetSize;
    }

    @Test
    public void testCorrectness() {
        CuckooTableSingletonTcFinder<String> singletonFinder = new CuckooTableSingletonTcFinder<>();
        singletonFinder.findTwoCore(cuckooTable);
        Set<String> remainedDataSet = singletonFinder.getRemainedDataSet();
        Assert.assertEquals(remainedDataSetSize, remainedDataSet.size());
    }
}
