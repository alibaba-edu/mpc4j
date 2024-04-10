package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17.Oos17PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.psz14.Psz14PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20.Cm20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22.Rr22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21.Rs21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22.Czz22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21.Gmr21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13.Dcw13PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16.Kkrt16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19FastPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19LowPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20.Prty20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16.Rr16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17.Rr17DePsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17.Rr17EcPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.Hfh99ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.Hfh99EccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21.Rt21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17EccPsiConfig;
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
 * PSI tests.
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
@RunWith(Parameterized.class)
public class PsiTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsiTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 14;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RR22
        configurations.add(new Object[]{
            PsiType.RR22.name() + "(" + SecurityModel.SEMI_HONEST + ")",
            new Rr22PsiConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        configurations.add(new Object[]{
            PsiType.RR22.name() + "(" + SecurityModel.SEMI_HONEST + ", " + Gf2kDokvsType.H3_CLUSTER_BINARY_BLAZE_GCT + ")",
            new Rr22PsiConfig.Builder(SecurityModel.SEMI_HONEST, Gf2kDokvsType.H3_CLUSTER_BINARY_BLAZE_GCT).build(),
        });
        configurations.add(new Object[]{
            PsiType.RR22.name() + "(" + SecurityModel.MALICIOUS + ")",
            new Rr22PsiConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            PsiType.RR22.name() + "(" + SecurityModel.MALICIOUS + ", " + Gf2kDokvsType.H3_CLUSTER_BINARY_BLAZE_GCT + ")",
            new Rr22PsiConfig.Builder(SecurityModel.MALICIOUS, Gf2kDokvsType.H3_CLUSTER_BINARY_BLAZE_GCT).build(),
        });
        // RS21
        configurations.add(new Object[]{
            PsiType.RS21.name() + "(" + SecurityModel.SEMI_HONEST + ")",
            new Rs21PsiConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        configurations.add(new Object[]{
            PsiType.RS21.name() + "(" + SecurityModel.MALICIOUS + ")",
            new Rs21PsiConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // OOS17
        configurations.add(new Object[]{
            PsiType.OOS17.name(), new Oos17PsiConfig.Builder().build(),
        });
        // PSZ14
        configurations.add(new Object[]{
            PsiType.PSZ14.name(), new Psz14PsiConfig.Builder().build(),
        });
        // DCW13
        configurations.add(new Object[]{
            PsiType.DCW13.name(), new Dcw13PsiConfig.Builder().build(),
        });
        // RT21
        configurations.add(new Object[]{
            PsiType.RT21.name(), new Rt21PsiConfig.Builder().build(),
        });
        // CM20
        configurations.add(new Object[]{
            PsiType.CM20.name(), new Cm20PsiConfig.Builder().build(),
        });
        // CZZ22
        configurations.add(new Object[]{
            PsiType.CZZ22.name(), new Czz22PsiConfig.Builder().build(),
        });
        // GMR21
        configurations.add(new Object[]{
            PsiType.GMR21.name() + "(no silent)", new Gmr21PsiConfig.Builder(false).build(),
        });
        configurations.add(new Object[]{
            PsiType.GMR21.name() + "(silent)", new Gmr21PsiConfig.Builder(true).build(),
        });
        // PRTY20
        configurations.add(new Object[]{
            PsiType.PRTY20.name() + "(" + SecurityModel.SEMI_HONEST + ")",
            new Prty20PsiConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        configurations.add(new Object[]{
            PsiType.PRTY20.name() + "(" + SecurityModel.MALICIOUS + ")",
            new Prty20PsiConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            PsiType.PRTY20.name() + " (" + Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT + ")",
            new Prty20PsiConfig.Builder(SecurityModel.SEMI_HONEST)
                .setPaxosType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT)
                .build(),
        });
        // RA17_ECC
        configurations.add(new Object[]{
            PsiType.RA17_ECC.name(), new Ra17EccPsiConfig.Builder().build(),
        });
        // RA17_BYTE_ECC
        configurations.add(new Object[]{
            PsiType.RA17_BYTE_ECC.name(), new Ra17ByteEccPsiConfig.Builder().build(),
        });
        // PRTY19_FAST
        configurations.add(new Object[]{
            PsiType.PRTY19_FAST.name(), new Prty19FastPsiConfig.Builder().build(),
        });
        // PRTY19_LOW
        configurations.add(new Object[]{
            PsiType.PRTY19_LOW.name(), new Prty19LowPsiConfig.Builder().build(),
        });
        // KKRT16 (no-stash)
        configurations.add(new Object[]{
            PsiFactory.PsiType.KKRT16.name() + " (no-stash)",
            new Kkrt16PsiConfig.Builder().setCuckooHashBinType(CuckooHashBinType.NO_STASH_NAIVE).build(),
        });
        // KKRT16 (4 hash)
        configurations.add(new Object[]{
            PsiFactory.PsiType.KKRT16.name() + " (4 hash)",
            new Kkrt16PsiConfig.Builder().setCuckooHashBinType(CuckooHashBinType.NAIVE_4_HASH).build(),
        });
        // KKRT16 (3 hash)
        configurations.add(new Object[]{
            PsiFactory.PsiType.KKRT16.name() + "(3 hash)", new Kkrt16PsiConfig.Builder().build(),
        });
        // HFH99_BYTE_ECC
        configurations.add(new Object[]{
            PsiFactory.PsiType.HFH99_BYTE_ECC.name(), new Hfh99ByteEccPsiConfig.Builder().build(),
        });
        // HFH99_ECC (compress)
        configurations.add(new Object[]{
            PsiFactory.PsiType.HFH99_ECC.name() + " (compress)",
            new Hfh99EccPsiConfig.Builder().setCompressEncode(true).build(),
        });
        // HFH99_ECC (uncompress)
        configurations.add(new Object[]{
            PsiFactory.PsiType.HFH99_ECC.name() + " (uncompress)",
            new Hfh99EccPsiConfig.Builder().setCompressEncode(false).build(),
        });
        // RR17_DE LAN
        configurations.add(new Object[]{
            PsiFactory.PsiType.RR17_DE.name() + " divParam = 4", new Rr17DePsiConfig.Builder().build(),
        });
        // RR17_DE WAN
        configurations.add(new Object[]{
            PsiFactory.PsiType.RR17_DE.name() + " divParam = 10", new Rr17DePsiConfig.Builder().setDivParam(10).build(),
        });
        // RR17_EC LAN
        configurations.add(new Object[]{
            PsiFactory.PsiType.RR17_EC.name() + " divParam = 4", new Rr17EcPsiConfig.Builder().build(),
        });
        // RR17_EC WAN
        configurations.add(new Object[]{
            PsiFactory.PsiType.RR17_EC.name() + " divParam = 10", new Rr17EcPsiConfig.Builder().setDivParam(10).build(),
        });
        // RR16
        configurations.add(new Object[]{
            PsiFactory.PsiType.RR16.name(), new Rr16PsiConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PsiConfig config;

    public PsiTest(String name, PsiConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1() {
        testPto(1, 1, false);
    }

    @Test
    public void test2() {
        testPto(2, 2, false);
    }

    @Test
    public void test10() {
        testPto(10, 10, false);
    }

    @Test
    public void testLargeServerSize() {
        testPto(DEFAULT_SIZE, 10, false);
    }

    @Test
    public void testLargeClientSize() {
        testPto(10, DEFAULT_SIZE, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // generate sets
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            PsiServerThread serverThread = new PsiServerThread(server, serverSet, clientSet.size());
            PsiClientThread clientThread = new PsiClientThread(client, clientSet, serverSet.size());
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
            assertOutput(serverSet, clientSet, clientThread.getIntersectionSet());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, Set<ByteBuffer> outputIntersectionSet) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverSet);
        expectIntersectionSet.retainAll(clientSet);
        Assert.assertTrue(outputIntersectionSet.containsAll(expectIntersectionSet));
        Assert.assertTrue(expectIntersectionSet.containsAll(outputIntersectionSet));
    }
}
