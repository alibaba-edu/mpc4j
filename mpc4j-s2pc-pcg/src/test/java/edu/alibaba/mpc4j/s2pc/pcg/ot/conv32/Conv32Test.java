package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.ccot.CcotConv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot.ScotConv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode.SvodeConv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole.SvoleConv32Config;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * F_3 -> F_2 modulus conversion test.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
@RunWith(Parameterized.class)
public class Conv32Test extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Conv32Test.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = (1 << 5) + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CCOT
        configurations.add(new Object[] {
            Conv32Type.CCOT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new CcotConv32Config.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // SVODE
        configurations.add(new Object[] {
            Conv32Type.SVODE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new SvodeConv32Config.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // SVOLE
        configurations.add(new Object[] {
            Conv32Type.SVOLE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new SvoleConv32Config.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // SCOT
        configurations.add(new Object[] {
            Conv32Type.SCOT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new ScotConv32Config.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }
    /**
     * config
     */
    private final Conv32Config config;
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;

    public Conv32Test(String name, Conv32Config config) {
        super(name);
        this.config = config;
        z3Field = new Z3ByteField();
    }

    @Test
    public void testConstant() {
        int num = DEFAULT_NUM;
        byte[] w0, w1;
        // (0,?)
        w0 = z3Field.createZeros(num);
        // (0,0)
        w1 = z3Field.createZeros(num);
        testPto(w0, w1, false);
        // (0,1)
        w1 = z3Field.createOnes(num);
        testPto(w0, w1, false);
        // (0,2)
        w1 = z3Field.createTwos(num);
        testPto(w0, w1, false);

        // (1,?)
        w0 = z3Field.createOnes(num);
        // (1,0)
        w1 = z3Field.createZeros(num);
        testPto(w0, w1, false);
        // (0,1)
        w1 = z3Field.createOnes(num);
        testPto(w0, w1, false);
        // (0,2)
        w1 = z3Field.createTwos(num);
        testPto(w0, w1, false);

        // (2,?)
        w0 = z3Field.createTwos(num);
        // (1,0)
        w1 = z3Field.createZeros(num);
        testPto(w0, w1, false);
        // (0,1)
        w1 = z3Field.createOnes(num);
        testPto(w0, w1, false);
        // (0,2)
        w1 = z3Field.createTwos(num);
        testPto(w0, w1, false);
    }

    @Test
    public void test1Num() {
        testPto(1, false);
    }

    @Test
    public void test2Num() {
        testPto(2, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto((1 << 20) + 1, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto((1 << 20) + 1, true);
    }

    private void testPto(int num, boolean parallel) {
        byte[] w0 = z3Field.createRandoms(num, SECURE_RANDOM);
        byte[] w1 = z3Field.createRandoms(num, SECURE_RANDOM);
        testPto(w0, w1, parallel);
    }

    private void testPto(byte[] w0, byte[] w1, boolean parallel) {
        int num = w0.length;
        Conv32Party sender = Conv32Factory.createSender(firstRpc, secondRpc.ownParty(), config);
        Conv32Party receiver = Conv32Factory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}-----", sender.getPtoDesc().getPtoName());
            Conv32PartyThread senderThread = new Conv32PartyThread(sender, w0);
            Conv32PartyThread receiverThread = new Conv32PartyThread(receiver, w1);
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
            byte[] v0 = senderThread.getPartyOutput();
            byte[] v1 = receiverThread.getPartyOutput();
            assertOutput(num, w0, w1, v0, v1);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultipleRound() {
        int eachNum = DEFAULT_NUM;
        int num = eachNum * 5;
        Conv32Party sender = Conv32Factory.createSender(firstRpc, secondRpc.ownParty(), config);
        Conv32Party receiver = Conv32Factory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            byte[] w0 = z3Field.createRandoms(num, SECURE_RANDOM);
            byte[] w1 = z3Field.createRandoms(num, SECURE_RANDOM);
            LOGGER.info("-----test {} (multiple round) start-----", sender.getPtoDesc().getPtoName());
            Conv32PartyThread senderThread = new Conv32PartyThread(sender, eachNum, w0);
            Conv32PartyThread receiverThread = new Conv32PartyThread(receiver, eachNum, w1);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            byte[] v0 = senderThread.getPartyOutput();
            byte[] v1 = receiverThread.getPartyOutput();
            assertOutput(num, w0, w1, v0, v1);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (multiple round) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int num, byte[] w0, byte[] w1, byte[] v0, byte[] v1) {
        Assert.assertEquals(num, w0.length);
        Assert.assertEquals(num, w1.length);
        Assert.assertTrue(BytesUtils.isReduceByteArray(v0, num));
        Assert.assertTrue(BytesUtils.isReduceByteArray(v1, num));
        BitVector w = BitVectorFactory.createZeros(num);
        for (int i = 0; i < num; i++) {
            w.set(i, (z3Field.add(w0[i], w1[i]) & 0b00000001) == 1);
        }
        BitVector v = BitVectorFactory.create(num, BytesUtils.xor(v0, v1));
        Assert.assertEquals(w, v);
    }
}
