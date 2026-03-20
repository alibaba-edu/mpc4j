package edu.alibaba.mpc4j.work.db.dynamic.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;
import java.util.Properties;

/**
 * pk-pk join main test
 */
public class DynamicDbGroupMainTest extends AbstractTwoPartyMemoryRpcPto {

    public DynamicDbGroupMainTest() {
        super("default group by");
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_group_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(DynamicDbGroupMain.PTO_TYPE_NAME, properties.get(MainPtoConfigUtils.PTO_TYPE_KEY));
        runMain(properties);
    }

    private void runMain(Properties properties) throws InterruptedException {
        DynamicDbGroupMain serverMain = new DynamicDbGroupMain(properties, "server");
        DynamicDbGroupMain clientMain = new DynamicDbGroupMain(properties, "client");
        runMain(serverMain, clientMain);
    }
}
