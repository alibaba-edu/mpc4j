package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.OoPsuConfigUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.OoPsuMain;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * PSU main test.
 *
 * @author Weiran Liu
 * @date 2024/4/28
 */
@RunWith(Parameterized.class)
public class MainOoPsuTest extends AbstractTwoPartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", "LLL24_FLAT_NET", false});
        configurations.add(new Object[]{PsuType.JSZ22_SFC.name(), "LLL24_FLAT_NET", true});
        configurations.add(new Object[]{PsuType.JSZ22_SFS.name(), "LLL24_FLAT_NET", true});
        configurations.add(new Object[]{PsuType.GMR21.name(), "LLL24_FLAT_NET", true});
        configurations.add(new Object[]{PsuType.JSZ22_SFC.name(), "GMR21_NET", true});
        configurations.add(new Object[]{PsuType.JSZ22_SFS.name(), "GMR21_NET", true});
        configurations.add(new Object[]{PsuType.GMR21.name(), "GMR21_NET", true});

        return configurations;
    }

    /**
     * type name
     */
    private final String typeName;
    /**
     * type name
     */
    private final String rosnName;
    /**
     * correct
     */
    private final boolean correct;

    public MainOoPsuTest(String typeName, String rosnName, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.rosnName = rosnName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_oo_psu_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), OoPsuMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(OoPsuMain.PTO_NAME_KEY), "");
        properties.setProperty(OoPsuMain.PTO_NAME_KEY, typeName);
        properties.setProperty(OoPsuConfigUtils.ROSN_TYPE, rosnName);
        properties.setProperty("append_string", rosnName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        OoPsuMain serverPsuMain = new OoPsuMain(properties, "server");
        OoPsuMain clientPsuMain = new OoPsuMain(properties, "client");
        runMain(serverPsuMain, clientPsuMain);
    }
}
