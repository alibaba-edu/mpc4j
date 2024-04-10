package edu.alibaba.mpc4j.s2pc.pjc.pid;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20ByteEccPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20EccPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21MpPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21SloppyPidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * PID protocol test.
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
@RunWith(Parameterized.class)
public class PidTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PidTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = (1 << 10) - 2;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // GMR21_MP
        configurations.add(new Object[] {
            PidFactory.PidType.GMR21_MP.name(),
            new Gmr21MpPidConfig.Builder().build(),
        });
        // GMR21_MP (JSZ22_SFC_PSU)
        configurations.add(new Object[] {
            PidFactory.PidType.GMR21_MP.name() + " (" + PsuType.JSZ22_SFC + ")",
            new Gmr21MpPidConfig.Builder().setPsuConfig(new Jsz22SfcPsuConfig.Builder(false).build()).build(),
        });
        // GMR21_SLOPPY (MEGA_BIN)
        configurations.add(new Object[] {
            PidFactory.PidType.GMR21_SLOPPY.name() + " (" + Gf2eDokvsType.MEGA_BIN + ")",
            new Gmr21SloppyPidConfig.Builder().setSloppyOkvsType(Gf2eDokvsType.MEGA_BIN).build(),
        });
        // GMR21_SLOPPY (H3_SINGLETON_GCT)
        configurations.add(new Object[] {
            PidFactory.PidType.GMR21_SLOPPY.name() + " (" + Gf2eDokvsType.H3_SINGLETON_GCT + ")",
            new Gmr21SloppyPidConfig.Builder().setSloppyOkvsType(Gf2eDokvsType.H3_SINGLETON_GCT).build(),
        });
        // GMR21_SLOPPY (JSZ22_SFC_PSU)
        configurations.add(new Object[] {
            PidFactory.PidType.GMR21_SLOPPY.name() + " (" + PsuType.JSZ22_SFC + ")",
            new Gmr21SloppyPidConfig.Builder().setPsuConfig(new Jsz22SfcPsuConfig.Builder(false).build()).build(),
        });
        // BKMS20_BYTE_ECC
        configurations.add(new Object[] {
            PidFactory.PidType.BKMS20_BYTE_ECC.name(), new Bkms20ByteEccPidConfig.Builder().build(),
        });
        // BKMS20_ECC (compress)
        configurations.add(new Object[] {
            PidFactory.PidType.BKMS20_ECC.name() + " (compress)",
            new Bkms20EccPidConfig.Builder().setCompressEncode(true).build(),
        });
        // BKMS20_ECC (uncompress)
        configurations.add(new Object[] {
            PidFactory.PidType.BKMS20_ECC.name() + " (uncompress)",
            new Bkms20EccPidConfig.Builder().setCompressEncode(false).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PidConfig config;

    public PidTest(String name, PidConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2() {
        testPid(2, 2, false);
    }

    @Test
    public void test10() {
        testPid(10, 10, false);
    }

    @Test
    public void testDefault() {
        testPid(DEFAULT_SIZE, DEFAULT_SIZE, false);
    }

    @Test
    public void testParallelDefault() {
        testPid(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testSmallServerSize() {
        testPid(10, DEFAULT_SIZE, false);
    }

    @Test
    public void testLargeClientSize() {
        testPid(DEFAULT_SIZE, 10, false);
    }

    @Test
    public void testLarge() {
        testPid(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPid(LARGE_SIZE, LARGE_SIZE, true);
    }

    private void testPid(int serverSize, int clientSize, boolean parallel) {
        PidParty<String> server = PidFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PidParty<String> client = PidFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info(
                "-----test {}，server size = {}, client size = {}-----",
                server.getPtoDesc().getPtoName(), serverSize, clientSize
            );
            // generate sets
            ArrayList<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSize, clientSize);
            Set<String> serverSet = sets.get(0);
            Set<String> clientSet = sets.get(1);
            PidPartyThread serverThread = new PidPartyThread(server, serverSet, clientSet.size());
            PidPartyThread clientThread = new PidPartyThread(client, clientSet, serverSet.size());
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            assertOutput(serverSet, clientSet, serverThread.getPidOutput(), clientThread.getPidOutput());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<String> serverSet, Set<String> clientSet,
        PidPartyOutput<String> serverOutput, PidPartyOutput<String> clientOutput) {
        Assert.assertEquals(serverOutput.getPidByteLength(), clientOutput.getPidByteLength());
        // compute the intersection
        Set<String> intersection = new HashSet<>();
        serverSet.forEach(serverElement -> {
            if (clientSet.contains(serverElement)) {
                intersection.add(serverElement);
            }
        });
        // compute the union
        Set<String> union = new HashSet<>(serverSet);
        union.addAll(clientSet);
        // get PIDs
        Set<ByteBuffer> serverPidSet = serverOutput.getPidSet();
        Set<ByteBuffer> clientPidSet = clientOutput.getPidSet();
        // verify PID num
        Assert.assertEquals(union.size(), serverPidSet.size());
        Assert.assertEquals(union.size(), clientPidSet.size());
        // verify PID map num
        Assert.assertEquals(serverSet.size(), serverOutput.getIdSet().size());
        Assert.assertEquals(clientSet.size(), clientOutput.getIdSet().size());
        // verify PID
        Assert.assertTrue(serverPidSet.containsAll(clientPidSet));
        Assert.assertTrue(clientPidSet.containsAll(serverPidSet));
        // compute PID intersection
        Set<String> intersectionSet = new HashSet<>();
        serverPidSet.forEach(pid -> {
            String serverId = serverOutput.getId(pid);
            String clientId = clientOutput.getId(pid);
            if (serverId != null && clientId != null) {
                Assert.assertEquals(serverId, clientId);
                intersectionSet.add(serverId);
            }
        });
        Assert.assertTrue(intersectionSet.containsAll(intersection));
        Assert.assertTrue(intersection.containsAll(intersectionSet));
    }
}