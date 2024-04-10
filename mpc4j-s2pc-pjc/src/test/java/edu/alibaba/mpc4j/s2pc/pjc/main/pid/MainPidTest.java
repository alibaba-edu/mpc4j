package edu.alibaba.mpc4j.s2pc.pjc.main.pid;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.junit.Test;

import java.util.Objects;
import java.util.Properties;

/**
 * PID main test.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
public class MainPidTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    public MainPidTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testBkms20ByteEcc() throws Exception {
        Properties properties = readConfig("pid/conf_pid_bkms20_byte_ecc.txt");
        runTest(new PidMain(properties));
    }

    @Test
    public void testBkms20EccCompressFalse() throws Exception {
        Properties properties = readConfig("pid/conf_pid_bkms20_ecc_compress_false.txt");
        runTest(new PidMain(properties));
    }

    @Test
    public void testBkms20EccCompressTrue() throws Exception {
        Properties properties = readConfig("pid/conf_pid_bkms20_ecc.txt");
        runTest(new PidMain(properties));
    }

    @Test
    public void testGmr21MpGmr21() throws Exception {
        Properties properties = readConfig("pid/conf_pid_gmr21_mp_gmr21.txt");
        runTest(new PidMain(properties));
    }

    @Test
    public void testGmr21MpKrtw19() throws Exception {
        Properties properties = readConfig("pid/conf_pid_gmr21_mp_krtw19.txt");
        runTest(new PidMain(properties));
    }

    @Test
    public void testGmr21MpZcl22Pke() throws Exception {
        Properties properties = readConfig("pid/conf_pid_gmr21_mp_zcl23_pke.txt");
        runTest(new PidMain(properties));
    }

    @Test
    public void testGmr21SloppyGmr21() throws Exception {
        Properties properties = readConfig("pid/conf_pid_gmr21_sloppy_gmr21.txt");
        runTest(new PidMain(properties));
    }

    @Test
    public void testGmr21SloppyKrtw19() throws Exception {
        Properties properties = readConfig("pid/conf_pid_gmr21_sloppy_krtw19.txt");
        runTest(new PidMain(properties));
    }

    @Test
    public void testGmr21SloppyZcl22Pke() throws Exception {
        Properties properties = readConfig("pid/conf_pid_gmr21_sloppy_zcl23_pke.txt");
        runTest(new PidMain(properties));
    }

    private void runTest(PidMain pidMain) throws InterruptedException {
        MainPidServerThread serverThread = new MainPidServerThread(firstRpc, secondRpc.ownParty(), pidMain);
        MainPidClientThread clientThread = new MainPidClientThread(secondRpc, firstRpc.ownParty(), pidMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainPidTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
