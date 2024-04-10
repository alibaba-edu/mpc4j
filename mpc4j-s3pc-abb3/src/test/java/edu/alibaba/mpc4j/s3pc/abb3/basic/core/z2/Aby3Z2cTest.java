package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cPtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * z2 circuit test.
 *
 * @author Feng Han
 * @date 2024/02/01
 */
@RunWith(Parameterized.class)
public class Aby3Z2cTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aby3Z2cTest.class);

    public enum BcOperator {
        XOR,
        XORI,
        AND,
        ANDI,
        OR,
        NOT,
        NOTI,
    }

    public static final BcOperator[] opAll = new BcOperator[]{
        BcOperator.AND, BcOperator.ANDI, BcOperator.OR, BcOperator.XOR, BcOperator.XORI, BcOperator.NOT, BcOperator.NOTI};
    public static final List<BcOperator> MUL_REQ_OP = Arrays.asList(BcOperator.AND, BcOperator.ANDI, BcOperator.OR);

    private static final int BATCH_NUM = 64;

    private static final int SMALL_SIZE = 1 << 8;

    private static final int MIDDLE_SIZE = 1 << 12;

    private static final int LARGE_SIZE = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Aby3Z2cPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Aby3Z2cConfig.Builder(false).build()
        });
        configurations.add(new Object[]{
            Aby3Z2cPtoDesc.getInstance().getPtoName() + "(malicious)",
            new Aby3Z2cConfig.Builder(true).build()
        });

        return configurations;
    }

    private final Aby3Z2cConfig config;

    public Aby3Z2cTest(String name, Aby3Z2cConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testAllSmallSize() {
        testOpi(false, opAll, SMALL_SIZE);
    }
    @Test
    public void testEachSmallSize() {
        for(BcOperator op : opAll){
            BcOperator[] single = new BcOperator[]{op};
            testOpi(false, single, SMALL_SIZE);
        }
    }
    @Test
    public void testAllMiddleSize() {
        testOpi(true, opAll, MIDDLE_SIZE);
    }
    @Test
    public void testEachMiddleSize() {
        for(BcOperator op : opAll){
            BcOperator[] single = new BcOperator[]{op};
            testOpi(true, single, MIDDLE_SIZE);
        }
    }
    @Test
    public void testAllLargeSize() {
        testOpi(true, opAll, LARGE_SIZE);
    }
    @Test
    public void testEachLargeSize() {
        for(BcOperator op : opAll){
            BcOperator[] single = new BcOperator[]{op};
            testOpi(true, single, LARGE_SIZE);
        }
    }

    private TripletZ2cParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        TripletProvider[] tripletProviders = IntStream.range(0, 3).mapToObj(i ->
                new TripletProvider(rpcAll[i],
                    new TripletProviderConfig.Builder(config.getSecurityModel().equals(SecurityModel.MALICIOUS)).build()))
            .toArray(TripletProvider[]::new);
        TripletZ2cParty[] parties = IntStream.range(0, 3).mapToObj(i ->
            Aby3Z2cFactory.createParty(rpcAll[i], config, tripletProviders[i])).toArray(TripletZ2cParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void verifyRes(BcOperator op, BitVector[][] data) {
        switch (op) {
            case XORI:
            case XOR:
                for (int i = 0; i < data[0].length; i++) {
                    Assert.assertEquals(data[0][i].xor(data[1][i]), data[2][i]);
                }
                break;
            case AND:
            case ANDI:
                for (int i = 0; i < data[0].length; i++) {
                    Assert.assertEquals(data[0][i].and(data[1][i]), data[2][i]);
                }
                break;
            case OR:
                for (int i = 0; i < data[0].length; i++) {
                    Assert.assertEquals(data[0][i].or(data[1][i]), data[2][i]);
                }
                break;
            case NOTI:
            case NOT:
                for (int i = 0; i < data[0].length; i++) {
                    Assert.assertEquals(data[0][i].not(), data[1][i]);
                }
                break;
        }
    }

    private void testOpi(boolean parallel, BcOperator[] ops, int eachBitNum) {
        TripletZ2cParty[] parties = getParties(parallel);
        int[] bitNums = IntStream.range(0, BATCH_NUM).map(i -> SECURE_RANDOM.nextInt(eachBitNum) + 1).toArray();

        try {
            LOGGER.info("-----test {}, (Bit number = {}) start-----", parties[0].getPtoDesc().getPtoName(), eachBitNum);
            Aby3Z2cPartyThread[] threads = Arrays.stream(parties).map(p ->
                new Aby3Z2cPartyThread(p, bitNums, ops)).toArray(Aby3Z2cPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (Aby3Z2cPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            for (BcOperator op : ops) {
                BitVector[][] data = threads[0].getInputAndRes(op);
                verifyRes(op, data);
            }

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (Bit number = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(), eachBitNum, time);
            LOGGER.info("op:[{}] test pass", Arrays.toString(ops));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
