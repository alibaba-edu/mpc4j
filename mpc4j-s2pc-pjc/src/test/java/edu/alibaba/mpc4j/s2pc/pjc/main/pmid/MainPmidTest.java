package edu.alibaba.mpc4j.s2pc.pjc.main.pmid;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.junit.Test;

import java.util.Objects;
import java.util.Properties;

/**
 * PMID main test.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
public class MainPmidTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    public MainPmidTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testZcl22MpGmr21() throws Exception {
        Properties properties = readConfig("pmid/conf_pmid_zcl22_mp_gmr21.txt");
        runTest(new PmidMain(properties));
    }

    @Test
    public void testZcl22MpZcl22Pke() throws Exception {
        Properties properties = readConfig("pmid/conf_pmid_zcl22_mp_zcl23_pke.txt");
        runTest(new PmidMain(properties));
    }

    @Test
    public void testZcl22MpZcl22PkeCompressFalse() throws Exception {
        Properties properties = readConfig("pmid/conf_pmid_zcl22_mp_zcl23_pke_compress_false.txt");
        runTest(new PmidMain(properties));
    }

    @Test
    public void testZcl22SloppyGmr21() throws Exception {
        Properties properties = readConfig("pmid/conf_pmid_zcl22_sloppy_gmr21.txt");
        runTest(new PmidMain(properties));
    }

    @Test
    public void testZcl22SloppyZcl22Pke() throws Exception {
        Properties properties = readConfig("pmid/conf_pmid_zcl22_sloppy_zcl23_pke.txt");
        runTest(new PmidMain(properties));
    }

    @Test
    public void testZcl22SloppyZcl22PkeCompressFalse() throws Exception {
        Properties properties = readConfig("pmid/conf_pmid_zcl22_sloppy_zcl23_pke_compress_false.txt");
        runTest(new PmidMain(properties));
    }

    private void runTest(PmidMain pmidMain) throws InterruptedException {
        MainPmidServerThread serverThread = new MainPmidServerThread(firstRpc, secondRpc.ownParty(), pmidMain);
        MainPmidClientThread clientThread = new MainPmidClientThread(secondRpc, firstRpc.ownParty(), pmidMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainPmidTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
