package edu.alibaba.mpc4j.s2pc.pcg.ct;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory.CoinTossType;
import edu.alibaba.mpc4j.s2pc.pcg.ct.blum82.Blum82CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.direct.DirectCoinTossConfig;
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
 * coin-tossing protocol test.
 *
 * @author Weiran Liu
 * @date 2023/5/5
 */
@RunWith(Parameterized.class)
public class CoinTossTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoinTossTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * default bit length
     */
    private static final int DEFAULT_BIT_LENGTH = 40;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BLUM82
        configurations.add(new Object[]{
            CoinTossType.BLUM82.name(), new Blum82CoinTossConfig.Builder().build(),
        });
        // DIRECT
        configurations.add(new Object[]{
            CoinTossType.DIRECT.name(), new DirectCoinTossConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * protocol config
     */
    private final CoinTossConfig config;

    public CoinTossTest(String name, CoinTossConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(1, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void test2Num() {
        testPto(2, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_NUM, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void test1BitLength() {
        testPto(DEFAULT_NUM, 1, false);
    }

    @Test
    public void test7BitLength() {
        testPto(DEFAULT_NUM, 7, false);
    }

    @Test
    public void test9BitLength() {
        testPto(DEFAULT_NUM, 9, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, DEFAULT_BIT_LENGTH, true);
    }

    private void testPto(int num, int bitLength, boolean parallel) {
        CoinTossParty sender = CoinTossFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        CoinTossParty receiver = CoinTossFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            CoinTossPartyThread senderThread = new CoinTossPartyThread(sender, num, bitLength);
            CoinTossPartyThread receiverThread = new CoinTossPartyThread(receiver, num, bitLength);
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            assertOutput(num, bitLength, senderThread.getPartyOutput(), receiverThread.getPartyOutput());
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    private void assertOutput(int num, int bitLength, byte[][] senderCoins, byte[][] receiverCoins) {
        int byteLength = CommonUtils.getByteLength(bitLength);
        Assert.assertEquals(num, senderCoins.length);
        Assert.assertEquals(num, receiverCoins.length);
        IntStream.range(0, num).forEach(index -> {
            byte[] senderCoin = senderCoins[index];
            Assert.assertTrue(BytesUtils.isFixedReduceByteArray(senderCoin, byteLength, bitLength));
            byte[] receiverCoin = receiverCoins[index];
            Assert.assertTrue(BytesUtils.isFixedReduceByteArray(receiverCoin, byteLength, bitLength));
            Assert.assertArrayEquals(senderCoin, receiverCoin);
        });
    }
}