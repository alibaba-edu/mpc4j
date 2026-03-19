package edu.alibaba.mpc4j.work.db.dynamic.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.db.dynamic.orderby.DynamicDbOrderByCircuitFactory.DynamicDbOrderByCircuitType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * pk-pk join main test
 */
@RunWith(Parameterized.class)
public class DynamicDbOrderByMainTest extends AbstractTwoPartyMemoryRpcPto {
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
     * type name
     */
    private final String typeName;
    /**
     * correct
     */
    private final boolean correct;

    public DynamicDbOrderByMainTest(String typeName, boolean correct) {
        super("default group by");
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_order_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(DynamicDbOrderByMain.PTO_TYPE_NAME, properties.get(MainPtoConfigUtils.PTO_TYPE_KEY));
        properties.setProperty(DynamicDbCircuitUtils.ORDER_BY_TYPE, typeName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        DynamicDbOrderByMain serverMain = new DynamicDbOrderByMain(properties, "server");
        DynamicDbOrderByMain clientMain = new DynamicDbOrderByMain(properties, "client");
        runMain(serverMain, clientMain);
    }
}
