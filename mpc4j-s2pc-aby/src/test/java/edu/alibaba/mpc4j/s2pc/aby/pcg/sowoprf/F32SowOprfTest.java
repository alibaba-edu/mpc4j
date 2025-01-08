package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfFactory.F32SowOprfType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F32SowOprfConfig.Builder;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
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
 * (F3, F2)-sowOPRF tests.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
@RunWith(Parameterized.class)
public class F32SowOprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(F32SowOprfTest.class);
    /**
     * default batch size
     */
    private static final int DEFAULT_BATCH_SIZE = (1 << 6) + 1;
    /**
     * large batch size
     */
    private static final int LARGE_BATCH_SIZE = (1 << 14) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // APRP24
        configurations.add(new Object[]{
            F32SowOprfType.APRR24.name() + " (" + Conv32Type.CCOT + ")",
            new Builder(Conv32Type.CCOT).build(),
        });
        configurations.add(new Object[]{
            F32SowOprfType.APRR24.name() + " (" + Conv32Type.SVODE + ")",
            new Builder(Conv32Type.SVODE).build(),
        });
        configurations.add(new Object[]{
            F32SowOprfType.APRR24.name() + " (" + Conv32Type.SCOT + ")",
            new Builder(Conv32Type.SCOT).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final F32SowOprfConfig config;
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;

    public F32SowOprfTest(String name, F32SowOprfConfig config) {
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

    private void testPto(int batchSize, boolean parallel) {
        F32SowOprfSender sender = F32SowOprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        F32SowOprfReceiver receiver = F32SowOprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, batch_size = {}-----", sender.getPtoDesc().getPtoName(), batchSize);
            byte[][] inputs = IntStream.range(0, batchSize)
                .mapToObj(index -> z3Field.createRandoms(F32Wprf.getInputLength(), SECURE_RANDOM))
                .toArray(byte[][]::new);
            F32SowOprfSenderThread senderThread = new F32SowOprfSenderThread(sender, batchSize);
            F32SowOprfReceiverThread receiverThread = new F32SowOprfReceiverThread(receiver, inputs);
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
        IntStream.range(0, batchSize).forEach(index -> {
            byte[] actualPrf = BytesUtils.xor(senderOutputs[index], receiverOutputs[index]);
            Assert.assertArrayEquals(expectPrfs[index], actualPrf);
        });
    }
}
