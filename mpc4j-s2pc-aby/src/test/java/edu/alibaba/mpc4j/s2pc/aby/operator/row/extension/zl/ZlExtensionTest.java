package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.g24.G24ZlExtensionConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.rrgg21.Rrgg21ZlExtensionConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Zl value extension protocol test.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
@RunWith(Parameterized.class)
public class ZlExtensionTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlExtensionTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * default input l
     */
    private static final int DEFAULT_INPUT_L = 16;
    /**
     * default output l
     */
    private static final int DEFAULT_OUTPUT_L = 32;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRGG21 (SIRNN)
        configurations.add(new Object[]{
            ZlExtensionFactory.ZlExtensionType.RRGG21 + " (" + SecurityModel.SEMI_HONEST + ")",
            new Rrgg21ZlExtensionConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        // G24
        configurations.add(new Object[]{
            ZlExtensionFactory.ZlExtensionType.G24 + " (" + SecurityModel.SEMI_HONEST + ")",
            new G24ZlExtensionConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlExtensionConfig config;

    public ZlExtensionTest(String name, ZlExtensionConfig config) {
        super(name);
        this.config = config;
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
    public void test4Num() {
        testPto(4, false);
    }

    @Test
    public void test8Num() {
        testPto(8, false);
    }

    @Test
    public void test7Num() {
        testPto(7, false);
    }

    @Test
    public void test9Num() {
        testPto(9, false);
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
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        // create inputs
        int validInputLen = config.getPtoType().equals(ZlExtensionFactory.ZlExtensionType.G24) ? DEFAULT_INPUT_L - 2 : DEFAULT_INPUT_L;

        BigInteger[] plainInputs = genPlainInput(validInputLen, num, SECURE_RANDOM);
        SquareZlVector xs = genSenderInputArray(DEFAULT_INPUT_L, num, SECURE_RANDOM);
        SquareZlVector ys = genReceiverInputArray(DEFAULT_INPUT_L, plainInputs, xs);
        // init the protocol
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
        Z2cParty z2cSender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), z2cConfig);
        Z2cParty z2cReceiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), z2cConfig);
        ZlExtensionParty sender = ZlExtensionFactory.createSender(z2cSender, secondRpc.ownParty(), config);
        ZlExtensionParty receiver = ZlExtensionFactory.createReceiver(z2cReceiver, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlExtensionPartyThread senderThread = new ZlExtensionPartyThread(
                sender, z2cSender, ZlExtensionTest.DEFAULT_INPUT_L, ZlExtensionTest.DEFAULT_OUTPUT_L, xs
            );
            ZlExtensionPartyThread receiverThread = new ZlExtensionPartyThread(
                receiver, z2cReceiver, ZlExtensionTest.DEFAULT_INPUT_L, ZlExtensionTest.DEFAULT_OUTPUT_L, ys
            );
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
            SquareZlVector z0 = senderThread.getZi();
            SquareZlVector z1 = receiverThread.getZi();
            ZlVector z = z0.getZlVector().add(z1.getZlVector());
            ZlVector expectZ = xs.getZlVector().add(ys.getZlVector());
            IntStream.range(0, num).forEach(i -> Assert.assertEquals(z.getElement(i), expectZ.getElement(i)));
            //assertOutput(l, num, xs, ys, z);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static BigInteger[] genPlainInput(int validL, int num, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(validL);
        return IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, validL, secureRandom))
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
    }

//    public SquareZlVector genInputArray(int l, int num, SecureRandom secureRandom) {
//        int byteL = CommonUtils.getByteLength(l);
//        BigInteger[] inputs =  IntStream.range(0, num)
//            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l-2, secureRandom))
//            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
//            .toArray(BigInteger[]::new);
//        return SquareZlVector.create(ZlFactory.createInstance(EnvType.STANDARD, l), inputs, false);
//    }

    static SquareZlVector genSenderInputArray(int l, int num, SecureRandom secureRandom) {
        int byteL = CommonUtils.getByteLength(l);
        BigInteger[] r = IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, secureRandom))
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        return SquareZlVector.create(ZlFactory.createInstance(EnvType.STANDARD, l), r, false);
    }

    static SquareZlVector genReceiverInputArray(int l, BigInteger[] plainInputs, SquareZlVector senderInputs) {
        ZlVector inputVector = ZlVector.create(ZlFactory.createInstance(EnvType.STANDARD, l), plainInputs);
        return SquareZlVector.create(inputVector.sub(senderInputs.getZlVector()), false);
    }
}