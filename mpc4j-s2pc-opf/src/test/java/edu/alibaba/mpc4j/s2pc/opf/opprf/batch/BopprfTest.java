package edu.alibaba.mpc4j.s2pc.opf.opprf.batch;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.OpprfTestUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory.BopprfType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs.OkvsBopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory.OprfType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Batch OPPRF test.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
@RunWith(Parameterized.class)
public class BopprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(BopprfTest.class);
    /**
     * default l
     */
    private static final int DEFAULT_L = 64;
    /**
     * default batch size
     */
    private static final int DEFAULT_BATCH_NUM = 1000;
    /**
     * large batch size
     */
    private static final int LARGE_BATCH_NUM = 1 << 16;
    /**
     * default point num
     */
    private static final int DEFAULT_POINT_NUM = DEFAULT_BATCH_NUM * 3;
    /**
     * large point num
     */
    private static final int LARGE_POINT_NUM = LARGE_BATCH_NUM * 3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            BopprfType.OKVS.name() + "(" + Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT + ", " + OprfType.RS21.name() + ")",
            new OkvsBopprfConfig.Builder()
                .setOkvsType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT)
                .setOprfConfig(new Rs21MpOprfConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build(),
        });
        configurations.add(new Object[]{
            BopprfType.OKVS.name() + "(" + Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT + ")",
            new OkvsBopprfConfig.Builder().setOkvsType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT).build(),
        });
        configurations.add(new Object[]{
            BopprfType.OKVS.name() + "(" + Gf2eDokvsType.H3_SINGLETON_GCT + ")",
            new OkvsBopprfConfig.Builder().setOkvsType(Gf2eDokvsType.H3_SINGLETON_GCT).build(),
        });
        configurations.add(new Object[]{
            BopprfType.OKVS.name() + "(" + Gf2eDokvsType.H2_SINGLETON_GCT + ")",
            new OkvsBopprfConfig.Builder().setOkvsType(Gf2eDokvsType.H2_SINGLETON_GCT).build(),
        });
        configurations.add(new Object[]{
            BopprfType.OKVS.name() + "(" + Gf2eDokvsType.DISTINCT_GBF + ")",
            new OkvsBopprfConfig.Builder().setOkvsType(Gf2eDokvsType.DISTINCT_GBF).build(),
        });
        configurations.add(new Object[]{
            BopprfType.OKVS.name() + "(" + Gf2eDokvsType.MEGA_BIN + ")",
            new OkvsBopprfConfig.Builder().setOkvsType(Gf2eDokvsType.MEGA_BIN).build(),
        });

        return configurations;
    }

    /**
     * the config
     */
    private final BopprfConfig config;

    public BopprfTest(String name, BopprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2Batch() {
        testPto(DEFAULT_L, 2, DEFAULT_POINT_NUM, false);
    }

    @Test
    public void test1Point() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, 1, false);
    }

    @Test
    public void test2Point() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, false);
    }

    @Test
    public void testSpecialL() {
        testPto(DEFAULT_L + 5, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, true);
    }

    @Test
    public void testLarge() {
        testPto(DEFAULT_L, LARGE_BATCH_NUM, LARGE_POINT_NUM, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(DEFAULT_L, LARGE_BATCH_NUM, LARGE_POINT_NUM, true);
    }

    private void testPto(int l, int batchNum, int pointNum, boolean parallel) {
        testPto(l, batchNum, pointNum, parallel, true);
        testPto(l, batchNum, pointNum, parallel, false);
    }

    private void testPto(int l, int batchNum, int pointNum, boolean parallel, boolean equalTarget) {
        // create the sender and the receiver
        BopprfSender sender = BopprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BopprfReceiver receiver = BopprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info(
                "-----test {}, l = {}, batch_num = {}, point_num = {}, parallel = {}-----",
                sender.getPtoDesc().getPtoName(), l, batchNum, pointNum, parallel
            );
            // generate the sender input
            byte[][][] senderInputArrays = OpprfTestUtils.generateSenderInputArrays(batchNum, pointNum, SECURE_RANDOM);
            byte[][][] senderTargetArrays = equalTarget
                ? OpprfTestUtils.generateEqualSenderTargetArrays(l, senderInputArrays, SECURE_RANDOM)
                : OpprfTestUtils.generateDistinctSenderTargetArrays(l, senderInputArrays, SECURE_RANDOM);
            // generate the receiver input
            byte[][] receiverInputArray = OpprfTestUtils.generateReceiverInputArray(l, senderInputArrays, SECURE_RANDOM);
            BopprfSenderThread senderThread = new BopprfSenderThread(sender, l, senderInputArrays, senderTargetArrays);
            BopprfReceiverThread receiverThread = new BopprfReceiverThread(receiver, l, receiverInputArray, pointNum);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            byte[][] receiverTargetArray = receiverThread.getTargetArray();
            assertOutput(l, senderInputArrays, senderTargetArrays, receiverInputArray, receiverTargetArray);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int l, byte[][][] senderInputArrays, byte[][][] senderTargetArrays,
                              byte[][] receiverInputArray, byte[][] receiverTargetArray) {
        int byteL = CommonUtils.getByteLength(l);
        int batchNum = senderInputArrays.length;
        Assert.assertEquals(batchNum, senderTargetArrays.length);
        Assert.assertEquals(batchNum, receiverInputArray.length);
        Assert.assertEquals(batchNum, receiverTargetArray.length);
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            int batchPointNum = senderInputArrays[batchIndex].length;
            Assert.assertEquals(batchPointNum, senderTargetArrays[batchIndex].length);
            byte[][] senderInputArray = senderInputArrays[batchIndex];
            byte[][] senderTargetArray = senderTargetArrays[batchIndex];
            byte[] receiverInput = receiverInputArray[batchIndex];
            // the receiver output must have l-bit length
            byte[] receiverTarget = receiverTargetArray[batchIndex];
            Assert.assertTrue(BytesUtils.isFixedReduceByteArray(receiverTarget, byteL, l));
            for (int index = 0; index < batchPointNum; index++) {
                // the sender target must have l-bit length
                byte[] senderTarget = senderTargetArray[index];
                Assert.assertTrue(BytesUtils.isFixedReduceByteArray(senderTarget, byteL, l));
            }
            // if receiver input belongs to one of the sender input, then check equal target
            boolean contain = false;
            int containIndex = -1;
            for (int index = 0; index < batchPointNum; index++) {
                byte[] senderInput = senderInputArray[index];
                if (Arrays.equals(senderInput, receiverInput)) {
                    contain = true;
                    containIndex = index;
                }
            }
            if (contain) {
                Assert.assertEquals(ByteBuffer.wrap(receiverTarget), ByteBuffer.wrap(senderTargetArray[containIndex]));
            } else {
                for (int index = 0; index < batchPointNum; index++) {
                    Assert.assertNotEquals(ByteBuffer.wrap(receiverTarget), ByteBuffer.wrap(senderTargetArray[index]));
                }
            }
        });
    }
}
