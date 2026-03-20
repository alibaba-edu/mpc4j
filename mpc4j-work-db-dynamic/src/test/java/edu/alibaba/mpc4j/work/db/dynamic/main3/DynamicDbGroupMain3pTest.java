package edu.alibaba.mpc4j.work.db.dynamic.main3;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.MainAbb3PartyThread;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.IntStream;

public class DynamicDbGroupMain3pTest extends AbstractThreePartyMemoryRpcPto {

    /**
     * party names
     */
    private static final String[] PARTY_NAMES = {"first", "second", "third"};

    public DynamicDbGroupMain3pTest() {
        super("default group by");
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_group_example_3p.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(DynamicDbGroupMain3p.PTO_TYPE_NAME, properties.get(MainPtoConfigUtils.PTO_TYPE_KEY));
        runMain(properties);
    }

    private void runMain(Properties properties) throws InterruptedException {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        DynamicDbGroupMain3p[] mains = Arrays.stream(PARTY_NAMES)
            .map(name -> new DynamicDbGroupMain3p(properties, name))
            .toArray(DynamicDbGroupMain3p[]::new);
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
