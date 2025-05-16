package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23SowOprfFactory.F23SowOprfType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F23SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * (F2, F3)-sowOPRF tests.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
@RunWith(Parameterized.class)
public class F23SowOprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(F23SowOprfTest.class);
    /**
     * default batch size
     */
    private static final int DEFAULT_BATCH_SIZE = (1 << 6) + 1;
    /**
     * large batch size
     */
    private static final int LARGE_BATCH_SIZE = (1 << 16) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // APRP24
        configurations.add(new Object[]{
            F23SowOprfType.APRR24.name() + " (non-silent)",
            new Aprr24F23SowOprfConfig.Builder(false).build(),
        });
        configurations.add(new Object[]{
            F23SowOprfType.APRR24.name() + " (silent)",
            new Aprr24F23SowOprfConfig.Builder(true).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final F23SowOprfConfig config;
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;

    public F23SowOprfTest(String name, F23SowOprfConfig config) {
        super(name);
        this.config = config;
        z3Field = new Z3ByteField();
    }

    @Test
    public void test1N() {
        testPto(1, false);
    }

    @Test
    public void test2N() {
        testPto(2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_BATCH_SIZE, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_BATCH_SIZE, true);
    }

    @Test
    public void testLargeN() {
        testPto(LARGE_BATCH_SIZE, false);
    }

    @Test
    public void testParallelLargeN() {
        testPto(LARGE_BATCH_SIZE, true);
    }

    @Test
    public void testPrecomputeDefaultN() {
        testPrecompute(DEFAULT_BATCH_SIZE, false);
    }

    @Test
    public void testParallelPrecomputeDefaultN() {
        testPrecompute(DEFAULT_BATCH_SIZE, true);
    }

    @Test
    public void testPrecomputeLargeN() {
        testPrecompute(LARGE_BATCH_SIZE, false);
    }

    @Test
    public void testParallelPrecomputeLargeN() {
        testPrecompute(LARGE_BATCH_SIZE, true);
    }

    private void testPrecompute(int size, boolean parallel) {
        int preCotSize = F23SowOprfFactory.getPreCotNum(size);
        byte[] delta = BlockUtils.randomBlock(SECURE_RANDOM);
        CotSenderOutput preCotSenderOutput = CotSenderOutput.createRandom(preCotSize, delta, SECURE_RANDOM);
        CotReceiverOutput preCotReceiverOutput = CotReceiverOutput.createRandom(preCotSenderOutput, SECURE_RANDOM);
        testPto(size, parallel, preCotSenderOutput, preCotReceiverOutput);
    }

    private void testPto(int batchSize, boolean parallel) {
        testPto(batchSize, parallel, null, null);
    }

    private void testPto(int batchSize, boolean parallel, CotSenderOutput preCotSenderOutput, CotReceiverOutput preCotReceiverOutput) {
        F23SowOprfSender sender = F23SowOprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        F23SowOprfReceiver receiver = F23SowOprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, batch_size = {}-----", sender.getPtoDesc().getPtoName(), batchSize);
            byte[][] inputs = BytesUtils.randomByteArrayVector(batchSize, F23Wprf.N_BYTE_LENGTH, SECURE_RANDOM);
            F23SowOprfSenderThread senderThread = new F23SowOprfSenderThread(sender, batchSize, preCotSenderOutput);
            F23SowOprfReceiverThread receiverThread = new F23SowOprfReceiverThread(receiver, inputs, preCotReceiverOutput);
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
            byte[][] senderOutput = senderThread.getSenderOutput();
            byte[][] receiverOutput = receiverThread.getReceiverOutput();
            byte[][] expectPrfs = IntStream.range(0, batchSize)
                .mapToObj(index -> sender.prf(inputs[index]))
                .toArray(byte[][]::new);
            assertOutput(batchSize, expectPrfs, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int batchSize, byte[][] expectPrfs, byte[][] senderOutputs, byte[][] receiverOutputs) {
        Assert.assertEquals(batchSize, expectPrfs.length);
        Assert.assertEquals(batchSize, senderOutputs.length);
        Assert.assertEquals(batchSize, receiverOutputs.length);
        IntStream.range(0, batchSize).forEach(batchIndex -> {
            byte[] actualPrf = new byte[F23Wprf.T];
            for (int k = 0; k < F23Wprf.T; k++) {
                actualPrf[k] = z3Field.add(senderOutputs[batchIndex][k], receiverOutputs[batchIndex][k]);
            }
            Assert.assertArrayEquals(expectPrfs[batchIndex], actualPrf);
        });
    }
}
