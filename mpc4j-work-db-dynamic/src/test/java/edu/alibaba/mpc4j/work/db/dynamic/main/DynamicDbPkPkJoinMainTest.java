package edu.alibaba.mpc4j.work.db.dynamic.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.db.dynamic.main.join.pkpk.DynamicDbPkPkJoinMain;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;
import java.util.Properties;

/**
 * pk-pk join main test
 */
public class DynamicDbPkPkJoinMainTest extends AbstractTwoPartyMemoryRpcPto {

    public DynamicDbPkPkJoinMainTest() {
        super("default pk pk join");
    }

    @Test
    public void testMain() throws InterruptedException {
        String path = "conf_pk_pk_join_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        Properties properties = PropertiesUtils.loadProperties(configPath);
        Assert.assertEquals(DynamicDbPkPkJoinMain.PTO_TYPE_NAME, properties.get(MainPtoConfigUtils.PTO_TYPE_KEY));
        runMain(properties);
    }

    private void runMain(Properties properties) throws InterruptedException {
        DynamicDbPkPkJoinMain serverMain = new DynamicDbPkPkJoinMain(properties, "server");
        DynamicDbPkPkJoinMain clientMain = new DynamicDbPkPkJoinMain(properties, "client");
        runMain(serverMain, clientMain);
    }
}
