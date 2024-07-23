package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuMain;
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
public class MainPsuTest extends AbstractTwoPartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false});
        for (PsuType type : PsuType.values()) {
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

    public MainPsuTest(String typeName, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_psu_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), PsuMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(PsuMain.PTO_NAME_KEY), "");
        properties.setProperty(PsuMain.PTO_NAME_KEY, typeName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        PsuMain serverPsuMain = new PsuMain(properties, "server");
        PsuMain clientPsuMain = new PsuMain(properties, "client");
        runMain(serverPsuMain, clientPsuMain);
    }
}
