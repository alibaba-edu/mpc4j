package edu.alibaba.mpc4j.work.db.dynamic.main3;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.MainAbb3PartyThread;
import edu.alibaba.mpc4j.work.db.dynamic.main.DynamicDbCircuitUtils;
import edu.alibaba.mpc4j.work.db.dynamic.orderby.DynamicDbOrderByCircuitFactory.DynamicDbOrderByCircuitType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.IntStream;

/**
 * group by main test
 */
@RunWith(Parameterized.class)
public class DynamicDbOrderByMain3pTest extends AbstractThreePartyMemoryRpcPto {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false});
        for (DynamicDbOrderByCircuitType type : DynamicDbOrderByCircuitType.values()) {
            configurations.add(new Object[]{type.name(), true});
        }

        return configurations;
    }
    /**
     * party names
     */
    private static final String[] PARTY_NAMES = {"first", "second", "third"};

    /**
     * type name
     */
    private final String typeName;
    /**
     * correct
     */
    private final boolean correct;

    public DynamicDbOrderByMain3pTest(String typeName, boolean correct) {
        super("default group by");
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_order_example_3p.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(DynamicDbOrderByMain3p.PTO_TYPE_NAME, properties.get(MainPtoConfigUtils.PTO_TYPE_KEY));
        properties.setProperty(DynamicDbCircuitUtils.ORDER_BY_TYPE, typeName);
        properties.setProperty("append_string", typeName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        DynamicDbOrderByMain3p[] mains = Arrays.stream(PARTY_NAMES)
            .map(name -> new DynamicDbOrderByMain3p(properties, name))
            .toArray(DynamicDbOrderByMain3p[]::new);
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
