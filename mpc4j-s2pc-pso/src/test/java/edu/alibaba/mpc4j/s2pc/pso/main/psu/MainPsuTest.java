package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.junit.Test;

import java.util.Objects;
import java.util.Properties;

/**
 * PSU main test.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
public class MainPsuTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    public MainPsuTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testGmr21() throws Exception {
        Properties properties = readConfig("psu/conf_psu_gmr21.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testGmr21SilentTrue() throws Exception {
        Properties properties = readConfig("psu/conf_psu_gmr21_silent_true.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testJsz22Sfc() throws Exception {
        Properties properties = readConfig("psu/conf_psu_jsz22_sfc.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testJsz22SfcSilentTrue() throws Exception {
        Properties properties = readConfig("psu/conf_psu_jsz22_sfc_silent_true.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testJsz22Sfs() throws Exception {
        Properties properties = readConfig("psu/conf_psu_jsz22_sfs.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testJsz22SfsSilentTrue() throws Exception {
        Properties properties = readConfig("psu/conf_psu_jsz22_sfs_silent_true.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testKrtw19Opt() throws Exception {
        Properties properties = readConfig("psu/conf_psu_krtw19_opt.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testKrtw19Ori() throws Exception {
        Properties properties = readConfig("psu/conf_psu_krtw19_ori.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testZcl22Pke() throws Exception {
        Properties properties = readConfig("psu/conf_psu_zcl22_pke.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testZcl22PkeCompressFalse() throws Exception {
        Properties properties = readConfig("psu/conf_psu_zcl22_pke_compress_false.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testZcl22Ske() throws Exception {
        Properties properties = readConfig("psu/conf_psu_zcl22_ske.txt");
        runTest(new PsuMain(properties));
    }

    @Test
    public void testZcl22PkeOfflineFalse() throws Exception {
        Properties properties = readConfig("psu/conf_psu_zcl22_ske_offline_false.txt");
        runTest(new PsuMain(properties));
    }

    private void runTest(PsuMain psuMain) throws InterruptedException {
        MainPsuServerThread serverThread = new MainPsuServerThread(firstRpc, secondRpc.ownParty(), psuMain);
        MainPsuClientThread clientThread = new MainPsuClientThread(secondRpc, firstRpc.ownParty(), psuMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainPsuTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
