package edu.alibaba.mpc4j.s2pc.pir.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.index.CpIdxPirMain;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * single Client-Preprocessing PIR main tests.
 *
 * @author Weiran Liu
 * @date 2024/5/3
 */
@RunWith(Parameterized.class)
public class CpIdxPirMainTest extends AbstractTwoPartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false});
        for (CpIdxPirType type : CpIdxPirType.values()) {
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

    public CpIdxPirMainTest(String typeName, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_cp_idx_pir_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), CpIdxPirMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(CpIdxPirMain.PTO_NAME_KEY), "");
        properties.setProperty(CpIdxPirMain.PTO_NAME_KEY, typeName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        CpIdxPirMain serverMain = new CpIdxPirMain(properties, "server");
        CpIdxPirMain clientMain = new CpIdxPirMain(properties, "client");
        runMain(serverMain, clientMain);
    }
}
