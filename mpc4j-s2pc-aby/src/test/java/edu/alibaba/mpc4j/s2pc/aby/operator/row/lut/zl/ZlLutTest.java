package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21.Rrgg21ZlLutConfig;
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
 * Zl lookup table protocol test.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
@RunWith(Parameterized.class)
public class ZlLutTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlLutTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * default m
     */
    private static final int DEFAULT_M = 4;
    /**
     * default n
     */
    private static final int DEFAULT_N = 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRGG21 (SIRNN)
        configurations.add(new Object[]{
            ZlLutFactory.ZlLutType.RRGG21.name(),
            new Rrgg21ZlLutConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlLutConfig config;

    public ZlLutTest(String name, ZlLutConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(DEFAULT_M, 1, false);
    }

    @Test
    public void test2Num() {
        testPto(DEFAULT_M, 2, false);
    }

    @Test
    public void test4Num() {
        testPto(DEFAULT_M, 4, false);
    }

    @Test
    public void test8Num() {
        testPto(DEFAULT_M, 8, false);
    }

    @Test
    public void test7Num() {
        testPto(DEFAULT_M, 7, false);
    }

    @Test
    public void test9Num() {
        testPto(DEFAULT_M, 9, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_M, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_M, DEFAULT_NUM, true);
    }

    @Test
    public void test1M() {
        testPto(1, DEFAULT_NUM, false);
    }

    @Test
    public void test7M() {
        testPto(7, DEFAULT_NUM, false);
    }

    @Test
    public void test9M() {
        testPto(9, DEFAULT_NUM, false);
    }

    @Test
    public void test13M() {
        testPto(13, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_M, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_M, LARGE_NUM, true);
    }

    private void testPto(int m, int num, boolean parallel) {
        // create tables
        byte[][][] tables = genTable(m, num);
        // create inputs
        byte[][] inputs = genInputArray(m, num);
        // init the protocol
        ZlLutSender sender = ZlLutFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlLutReceiver receiver = ZlLutFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlLutSenderThread senderThread = new ZlLutSenderThread(sender, tables, m, ZlLutTest.DEFAULT_N);
            ZlLutReceiverThread receiverThread = new ZlLutReceiverThread(receiver, inputs, m, ZlLutTest.DEFAULT_N);
            StopWatch stopWatch = new StopWatch();
            // execute the protocol
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            byte[][] actualResult = receiverThread.getOutputs();
            IntStream.range(0, num).forEach(i -> {
                int choice = IntUtils.fixedByteArrayToNonNegInt(inputs[i]);
                Assert.assertTrue(BytesUtils.equals(tables[i][choice], actualResult[i]));
            });
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    static byte[][][] genTable(int m, int num) {
        int byteN = CommonUtils.getByteLength(ZlLutTest.DEFAULT_N);
        byte[][][] table = new byte[num][1 << m][byteN];
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < 1 << m; j++) {
                table[i][j] = BytesUtils.randomByteArray(byteN, ZlLutTest.DEFAULT_N, SECURE_RANDOM);
            }
        }
        return table;
    }

    static byte[][] genInputArray(int m, int num) {
        int byteM = CommonUtils.getByteLength(m);
        return IntStream.range(0, num)
            .mapToObj(index -> BytesUtils.randomByteArray(byteM, m, SECURE_RANDOM))
            .toArray(byte[][]::new);
    }
}