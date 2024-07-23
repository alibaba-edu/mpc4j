package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.psi.PsiMain;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * PSI main tests.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
@RunWith(Parameterized.class)
public class MainPsiTest extends AbstractTwoPartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"INVALID", false});
        for (PsiType type : PsiType.values()) {
            // ignore AID_PSI
            if (!type.equals(PsiType.AID_KMRS14)) {
                configurations.add(new Object[]{type.name(), true});
            }
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

    public MainPsiTest(String typeName, boolean correct) {
        super(typeName);
        this.typeName = typeName;
        this.correct = correct;
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_psi_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(properties.get(MainPtoConfigUtils.PTO_TYPE_KEY), PsiMain.PTO_TYPE_NAME);
        Assert.assertEquals(properties.get(PsiMain.PTO_NAME_KEY), "");
        properties.setProperty(PsiMain.PTO_NAME_KEY, typeName);
        if (correct) {
            runMain(properties);
        } else {
            Assert.assertThrows(IllegalArgumentException.class, () -> runMain(properties));
        }
    }

    private void runMain(Properties properties) throws InterruptedException {
        PsiMain serverMain = new PsiMain(properties, "server");
        PsiMain clientMain = new PsiMain(properties, "client");
        runMain(serverMain, clientMain);
    }
}
