package edu.alibaba.mpc4j.work.db.sketch.main;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.MainAbb3PartyThread;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSFactory.CMSPtoType;
import edu.alibaba.mpc4j.work.db.sketch.main.CMS.CMSZ2Main;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.IntStream;

/**
 * CMS protocol main test case
 */
@RunWith(Parameterized.class)
public class CMSZ2MainTest extends AbstractThreePartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false});
        configurations.add(new Object[] {CMSPtoType.CMS_V2.name(), true});
        return configurations;
    }

    /**
     * type name
     */
    private final String typeName;
    /**
     * correct
     */
    private final boolean correct;

    /**
     * party names
     */
    private static final String[] PARTY_NAMES = {"first", "second", "third"};

    public CMSZ2MainTest(String typeName, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_cms_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), CMSZ2Main.PTO_NAME);
        properties.setProperty(CMSZ2Main.PTO_NAME_KEY, typeName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        AbstractMainAbb3PartyPto[] mains = Arrays.stream(PARTY_NAMES)
            .map(name -> new CMSZ2Main(properties, name))
            .toArray(AbstractMainAbb3PartyPto[]::new);
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
