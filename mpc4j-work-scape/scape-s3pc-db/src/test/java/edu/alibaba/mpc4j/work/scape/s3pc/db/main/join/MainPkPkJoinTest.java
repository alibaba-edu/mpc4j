package edu.alibaba.mpc4j.work.scape.s3pc.db.main.join;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.MainAbb3PartyThread;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFactory.PkPkJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.join.pkpk.PkPkJoinMain;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.IntStream;

/**
 * pk pk join main test
 *
 * @author Feng Han
 * @date 2025/3/3
 */
@RunWith(Parameterized.class)
public class MainPkPkJoinTest extends AbstractThreePartyMemoryRpcPto {
    /**
     * party names
     */
    private static final String[] PARTY_NAMES = {"first", "second", "third"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false, false, false});
        for (PkPkJoinPtoType type : PkPkJoinPtoType.values()) {
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

    public MainPkPkJoinTest(String typeName, boolean isMalicious, boolean verifyWithMac, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.isMalicious = isMalicious;
        this.verifyWithMac = verifyWithMac;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_pk_pk_join_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), PkPkJoinMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(PkPkJoinMain.PTO_NAME_KEY), "");
        properties.setProperty(PkPkJoinMain.PTO_NAME_KEY, typeName);
        properties.setProperty(PkPkJoinMain.IS_MALICIOUS, String.valueOf(isMalicious));
        properties.setProperty(PkPkJoinMain.VERIFY_WITH_MAC, String.valueOf(verifyWithMac));
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        PkPkJoinMain[] mains = Arrays.stream(PARTY_NAMES)
            .map(name -> new PkPkJoinMain(properties, name))
            .toArray(PkPkJoinMain[]::new);
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
