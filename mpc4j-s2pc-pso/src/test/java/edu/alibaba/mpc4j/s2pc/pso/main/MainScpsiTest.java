package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiFactory.ScpsiType;
import edu.alibaba.mpc4j.s2pc.pso.main.scpsi.ScpsiMain;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * SCPSI main tests.
 *
 * @author Feng Han
 * @date 2023/10/10
 */
@RunWith(Parameterized.class)
public class MainScpsiTest extends AbstractTwoPartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false});
        for (ScpsiType type : ScpsiType.values()) {
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

    public MainScpsiTest(String typeName, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_scpsi_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), ScpsiMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(ScpsiMain.PTO_NAME_KEY), "");
        properties.setProperty(ScpsiMain.PTO_NAME_KEY, typeName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        ScpsiMain serverMain = new ScpsiMain(properties, "server");
        ScpsiMain clientMain = new ScpsiMain(properties, "client");
        runMain(serverMain, clientMain);
    }
}
