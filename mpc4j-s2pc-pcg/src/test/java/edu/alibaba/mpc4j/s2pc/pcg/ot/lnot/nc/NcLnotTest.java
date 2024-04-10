package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotFactory.NcLnotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.cot.CotNcLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.direct.DirectNcLnotConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * no-choice 1-out-of-n (with n = 2^l) test.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
@RunWith(Parameterized.class)
public class NcLnotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(NcLnotTest.class);
    /**
     * default l
     */
    private static final int DEFAULT_L = 5;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * default round
     */
    private static final int DEFAULT_ROUND = 2;
    /**
     * small l
     */
    private static final int SMALL_L = 1;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * large round
     */
    private static final int LARGE_ROUND = 5;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // COT
        configurations.add(new Object[] {
            NcLnotType.COT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new CotNcLnotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[] {
            NcLnotType.COT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new CotNcLnotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // DIRECT
        configurations.add(new Object[] {
            NcLnotType.DIRECT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectNcLnotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[] {
            NcLnotType.DIRECT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectNcLnotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final NcLnotConfig config;

    public NcLnotTest(String name, NcLnotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Round1Num() {
        testPto(DEFAULT_L, 1, 1, false);
    }

    @Test
    public void test2Round2Num() {
        testPto(DEFAULT_L, 2, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_L, DEFAULT_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_L, DEFAULT_NUM, DEFAULT_ROUND, true);
    }

    @Test
    public void testSmallL() {
        testPto(SMALL_L, DEFAULT_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testLog12Num() {
        testPto(DEFAULT_L, 1 << 12, DEFAULT_ROUND, false);
    }

    @Test
    public void testLog16Num() {
        testPto(DEFAULT_L, 1 << 16, DEFAULT_ROUND, false);
    }

    @Test
    public void testLargeRound() {
        testPto(DEFAULT_L, DEFAULT_NUM, LARGE_ROUND, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, DEFAULT_ROUND, true);
    }

    private void testPto(int l, int num, int round, boolean parallel) {
        NcLnotSender sender = NcLnotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        NcLnotReceiver receiver = NcLnotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            NcLnotSenderThread senderThread = new NcLnotSenderThread(sender, l, num, round);
            NcLnotReceiverThread receiverThread = new NcLnotReceiverThread(receiver, l, num, round);
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
            LnotSenderOutput senderOutput = senderThread.getSenderOutput();
            LnotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            LnotTestUtils.assertOutput(l, num * round, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
