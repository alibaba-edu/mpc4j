package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi.Cmg21BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory.OkvrType;
import edu.alibaba.mpc4j.s2pc.upso.okvr.kw.KwOkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.okvs.OkvsOkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.pir.PirOkvrConfig;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OKVR test.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
@RunWith(Parameterized.class)
public class OkvrTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OkvrTest.class);
    /**
     * default l
     */
    private static final int DEFAULT_L = 64;
    /**
     * default retrieval size
     */
    private static final int DEFAULT_RETRIEVAL_SIZE = 64;
    /**
     * large retrieval size
     */
    private static final int LARGE_RETRIEVAL_SIZE = 512;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1 << 10;
    /**
     * large num
     */
    private static final int LARGE_NUM = DEFAULT_NUM * 3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Keyword PIR
        configurations.add(new Object[]{
            OkvrType.KW.name() + "(" + Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT + ")",
            new KwOkvrConfig.Builder().setSparseOkvsType(Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT).build(),
        });
        // PIR
        configurations.add(new Object[]{
            OkvrType.PIR.name() + "(" + Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT + ")",
            new PirOkvrConfig.Builder().setSparseOkvsType(Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT).build(),
        });
        configurations.add(new Object[]{
            OkvrType.PIR.name() + "(" + Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT + ")",
            new PirOkvrConfig.Builder().setSparseOkvsType(Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT).build(),
        });
        configurations.add(new Object[]{
            OkvrType.PIR.name() + "(" + BatchIndexPirFactory.BatchIndexPirType.LABEL_PSI + " " + Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT + ")",
            new PirOkvrConfig.Builder()
                .setBatchIndexPirConfig(new Cmg21BatchIndexPirConfig.Builder().build())
                .setSparseOkvsType(Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT)
                .build(),
        });
        configurations.add(new Object[]{
            OkvrType.PIR.name() + "(" + BatchIndexPirFactory.BatchIndexPirType.LABEL_PSI + " " + Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT + ")",
            new PirOkvrConfig.Builder()
                .setBatchIndexPirConfig(new Cmg21BatchIndexPirConfig.Builder().build())
                .setSparseOkvsType(Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT)
                .build(),
        });
        // OKVS
        configurations.add(new Object[]{
            OkvrType.OKVS.name() + "(" + Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT + ")",
            new OkvsOkvrConfig.Builder().setOkvsType(Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT).build(),
        });
        configurations.add(new Object[]{
            OkvrType.OKVS.name() + "(" + Gf2eDokvsType.H2_SINGLETON_GCT + ")",
            new OkvsOkvrConfig.Builder().setOkvsType(Gf2eDokvsType.H2_SINGLETON_GCT).build(),
        });
        configurations.add(new Object[]{
            OkvrType.OKVS.name() + "(" + Gf2eDokvsType.DISTINCT_GBF + ")",
            new OkvsOkvrConfig.Builder().setOkvsType(Gf2eDokvsType.DISTINCT_GBF).build(),
        });
        configurations.add(new Object[]{
            OkvrType.OKVS.name() + "(" + Gf2eDokvsType.MEGA_BIN + ")",
            new OkvsOkvrConfig.Builder().setOkvsType(Gf2eDokvsType.MEGA_BIN).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final OkvrConfig config;

    public OkvrTest(String name, OkvrConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2Batch() {
        testPto(DEFAULT_L, 2, DEFAULT_NUM, false);
    }

    @Test
    public void test1Point() {
        testPto(DEFAULT_L, DEFAULT_RETRIEVAL_SIZE, 1, false);
    }

    @Test
    public void test2Point() {
        testPto(DEFAULT_L, DEFAULT_RETRIEVAL_SIZE, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_L, DEFAULT_RETRIEVAL_SIZE, DEFAULT_NUM, false);
    }

    @Test
    public void testSpecialL() {
        testPto(DEFAULT_L + 1, DEFAULT_RETRIEVAL_SIZE, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_L, DEFAULT_RETRIEVAL_SIZE, DEFAULT_NUM, true);
    }

    @Test
    public void testLarge() {
        testPto(DEFAULT_L, LARGE_RETRIEVAL_SIZE, LARGE_NUM, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(DEFAULT_L, LARGE_RETRIEVAL_SIZE, LARGE_NUM, true);
    }

    private void testPto(int l, int batchNum, int pointNum, boolean parallel) {
        testPto(l, batchNum, pointNum, parallel, true);
        testPto(l, batchNum, pointNum, parallel, false);
    }

    private void testPto(int l, int retrievalSize, int num, boolean parallel, boolean equalTarget) {
        // create the sender and the receiver
        OkvrSender sender = OkvrFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        OkvrReceiver receiver = OkvrFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info(
                "-----test {}, l = {}, batch_num = {}, point_num = {}, parallel = {}, equal_target = {}-----",
                sender.getPtoDesc().getPtoName(), l, retrievalSize, num, parallel, equalTarget
            );
            byte[][] simpleHashKeys = CommonUtils.generateRandomKeys(1, SECURE_RANDOM);
            Pair<Map<ByteBuffer, byte[]>, Set<ByteBuffer>> inputs = OkvrTestUtils.generateInputs(
                num, l, retrievalSize, equalTarget, simpleHashKeys, SECURE_RANDOM
            );
            Map<ByteBuffer, byte[]> senderInput = inputs.getFirst();
            Set<ByteBuffer> receiverInput = inputs.getSecond();
            OkvrSenderThread senderThread = new OkvrSenderThread(sender, senderInput, l, retrievalSize);
            OkvrReceiverThread receiverThread = new OkvrReceiverThread(receiver, num, l, receiverInput);
            // start
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Map<ByteBuffer, byte[]> receiverOutputs = receiverThread.getKeyValueMap();
            // verify
            assertOutput(senderInput, receiverOutputs);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Map<ByteBuffer, byte[]> senderInputs, Map<ByteBuffer, byte[]> receiverOutputs) {
        Set<ByteBuffer> values = senderInputs.values().stream().map(ByteBuffer::wrap).collect(Collectors.toSet());
        receiverOutputs.forEach((key, value) -> {
            if (senderInputs.containsKey(key)) {
                Assert.assertArrayEquals(senderInputs.get(key), value);
            } else {
                Assert.assertFalse(values.contains(ByteBuffer.wrap(value)));
            }
        });
    }
}
