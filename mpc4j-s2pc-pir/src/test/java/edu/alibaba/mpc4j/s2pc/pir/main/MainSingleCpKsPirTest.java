package edu.alibaba.mpc4j.s2pc.pir.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory.CpKsPirType;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword.SingleCpKsPirMain;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * single Client-Preprocessing Keyword Symmetric PIR main tests.
 *
 * @author Weiran Liu
 * @date 2024/5/3
 */
@RunWith(Parameterized.class)
public class MainSingleCpKsPirTest extends AbstractTwoPartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false});
        for (CpKsPirType type : CpKsPirType.values()) {
            configurations.add(new Object[]{type.name(), true});
        }

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

    public MainSingleCpKsPirTest(String typeName, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_single_cp_ks_pir_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), SingleCpKsPirMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(SingleCpKsPirMain.PTO_NAME_KEY), "");
        properties.setProperty(SingleCpKsPirMain.PTO_NAME_KEY, typeName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        SingleCpKsPirMain serverMain = new SingleCpKsPirMain(properties, "server");
        SingleCpKsPirMain clientMain = new SingleCpKsPirMain(properties, "client");
        runMain(serverMain, clientMain);
    }
}
