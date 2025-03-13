package edu.alibaba.mpc4j.work.scape.s3pc.opf.main;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.MainAbb3PartyThread;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFactory.AggPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.main.agg.AggMain;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.IntStream;

/**
 * main agg test
 *
 * @author Feng Han
 * @date 2025/2/28
 */
@RunWith(Parameterized.class)
public class MainAggTest extends AbstractThreePartyMemoryRpcPto {
    /**
     * party names
     */
    private static final String[] PARTY_NAMES = {"first", "second", "third"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false, false, false});
        for (AggPtoType type : AggPtoType.values()) {
            // semi-honest
            configurations.add(new Object[]{type.name(), false, false, true});
            // malicious tuple verification
            configurations.add(new Object[]{type.name(), true, false, true});
            // malicious mac verification
            configurations.add(new Object[]{type.name(), true, true, true});
        }

        return configurations;
    }

    /**
     * type name
     */
    private final String typeName;
    /**
     * type name
     */
    private final boolean isMalicious;
    /**
     * type name
     */
    private final boolean verifyWithMac;
    /**
     * correct
     */
    private final boolean correct;

    public MainAggTest(String typeName, boolean isMalicious, boolean verifyWithMac, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.isMalicious = isMalicious;
        this.verifyWithMac = verifyWithMac;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_agg_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), AggMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(AggMain.PTO_NAME_KEY), "");
        properties.setProperty(AggMain.PTO_NAME_KEY, typeName);
        properties.setProperty(AggMain.IS_MALICIOUS, String.valueOf(isMalicious));
        properties.setProperty(AggMain.VERIFY_WITH_MAC, String.valueOf(verifyWithMac));
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        AggMain[] mains = Arrays.stream(PARTY_NAMES)
            .map(name -> new AggMain(properties, name))
            .toArray(AggMain[]::new);
        MainAbb3PartyThread[] threads = IntStream.range(0, 3)
            .mapToObj(i -> new MainAbb3PartyThread(rpcAll[i], mains[i]))
            .toArray(MainAbb3PartyThread[]::new);
        Arrays.stream(threads).forEach(Thread::start);
        for (MainAbb3PartyThread thread : threads) {
            thread.join();
        }
        for (MainAbb3PartyThread thread : threads) {
            Assert.assertTrue(thread.getSuccess());
        }
    }
}
