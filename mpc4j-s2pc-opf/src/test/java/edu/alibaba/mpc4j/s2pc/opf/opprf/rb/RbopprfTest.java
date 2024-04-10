package edu.alibaba.mpc4j.s2pc.opf.opprf.rb;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.OpprfTestUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfFactory.RbopprfType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22.Cgs22RbopprfConfig;
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
 * Related-Batch OPPRF test.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
@RunWith(Parameterized.class)
public class RbopprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(RbopprfTest.class);
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
            RbopprfType.CGS22.name(), new Cgs22RbopprfConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * the config
     */
    private final RbopprfConfig config;

    public RbopprfTest(String name, RbopprfConfig config) {
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
    public void testParallelDefault() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, true);
    }

    @Test
    public void testSpecialL() {
        testPto(DEFAULT_L + 5, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, true);
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
        RbopprfSender sender = RbopprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        RbopprfReceiver receiver = RbopprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
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
            RbopprfSenderThread senderThread = new RbopprfSenderThread(sender, l, senderInputArrays, senderTargetArrays);
            RbopprfReceiverThread receiverThread = new RbopprfReceiverThread(receiver, l, receiverInputArray, pointNum);
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
            byte[][][] receiverTargetArray = receiverThread.getTargetArray();
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
                              byte[][] receiverInputArray, byte[][][] receiverTargetArray) {
        int d = config.getD();
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
            byte[][] receiverTargets = receiverTargetArray[batchIndex];
            Assert.assertEquals(d, receiverTargets.length);
            // the receiver output must have l-bit length
            for (byte[] receiverTarget : receiverTargets) {
                Assert.assertTrue(BytesUtils.isFixedReduceByteArray(receiverTarget, byteL, l));
            }
            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                byte[] senderTarget = senderTargetArray[pointIndex];
                // the sender target must have l-bit length
                Assert.assertTrue(BytesUtils.isFixedReduceByteArray(senderTarget, byteL, l));
            }
            // if receiver input belongs to one of the sender input, then check at most one equal target
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
                byte[] senderTarget = senderTargetArray[containIndex];
                int targetEqualNum = 0;
                for (int b = 0; b < d; b++) {
                    if (Arrays.equals(receiverTargets[b], senderTarget)) {
                        targetEqualNum++;
                    }
                }
                Assert.assertEquals(1, targetEqualNum);
            } else {
                for (int index = 0; index < batchPointNum; index++) {
                    byte[] senderTarget = senderTargetArray[index];
                    for (int b = 0; b < d; b++) {
                        Assert.assertNotEquals(ByteBuffer.wrap(receiverTargets[b]), ByteBuffer.wrap(senderTarget));
                    }
                }
            }
        });
    }
}
