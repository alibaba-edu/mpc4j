package edu.alibaba.mpc4j.s2pc.pso.main.ccpsi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;
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
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    private static final CuckooHashBinType[] CUCKOO_HASH_BIN_TYPES = new CuckooHashBinType[] {
        CuckooHashBinType.NO_STASH_PSZ18_3_HASH,
        CuckooHashBinType.NAIVE_2_HASH,
        CuckooHashBinType.NAIVE_4_HASH,
    };

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        String[] silentChoices = new String[]{"_silent_", "_no_silent_"};
        for (CuckooHashBinType type : CUCKOO_HASH_BIN_TYPES) {
            for(String silentChoice : silentChoices){
                // CGS22
                configurations.add(new Object[]{
                    CcpsiType.CGS22.name() + silentChoice + type.name(), "ccpsi/cgs22" + silentChoice + type.name().toLowerCase() + ".txt",
                });
                // PSTY19
                configurations.add(new Object[]{
                    CcpsiType.PSTY19.name() + silentChoice + type.name(), "ccpsi/psty19" + silentChoice + type.name().toLowerCase() + ".txt",
                });
                // RS21
                configurations.add(new Object[]{
                    CcpsiType.RS21.name() + silentChoice + type.name(), "ccpsi/rs21" + silentChoice + type.name().toLowerCase() + ".txt",
                });
            }
        }

        return configurations;
    }

    /**
     * file name
     */
    private final String filePath;

    public MainCcpsiTest(String name, String filePath) {
        super(name);
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
        this.filePath = filePath;
    }

    @Test
    public void testPsi() throws InterruptedException {
        Properties properties = readConfig(filePath);
        runTest(new CcpsiMain(properties));
    }

    private void runTest(CcpsiMain ccpsiMain) throws InterruptedException {
        MainCcpsiServerThread serverThread = new MainCcpsiServerThread(firstRpc, secondRpc.ownParty(), ccpsiMain);
        MainCcpsiClientThread clientThread = new MainCcpsiClientThread(secondRpc, firstRpc.ownParty(), ccpsiMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
        Assert.assertTrue(serverThread.getSuccess());
        Assert.assertTrue(clientThread.getSuccess());
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainCcpsiTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
