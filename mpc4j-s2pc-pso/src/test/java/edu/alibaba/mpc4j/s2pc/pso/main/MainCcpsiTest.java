package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;
import edu.alibaba.mpc4j.s2pc.pso.main.ccpsi.CcpsiMain;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * client circuit PSI main tests.
 *
 * @author Feng Han
 * @date 2023/10/10
 */
@RunWith(Parameterized.class)
public class MainCcpsiTest extends AbstractTwoPartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false});
        for (CcpsiType type : CcpsiType.values()) {
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

    public MainCcpsiTest(String typeName, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_ccpsi_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), CcpsiMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(CcpsiMain.PTO_NAME_KEY), "");
        properties.setProperty(CcpsiMain.PTO_NAME_KEY, typeName);
        if (correct) {
            testMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> testMain(properties));
        }
    }

    private void testMain(Properties properties) throws InterruptedException {
        CcpsiMain serverMain = new CcpsiMain(properties, "server");
        CcpsiMain clientMain = new CcpsiMain(properties, "client");
        runMain(serverMain, clientMain);
    }
}
