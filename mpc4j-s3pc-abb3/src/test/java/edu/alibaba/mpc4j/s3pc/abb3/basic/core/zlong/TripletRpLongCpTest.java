package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongCpFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongCpPtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongCpPtoDesc;
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
 * zlong circuit test.
 *
 * @author Feng Han
 * @date 2024/02/01
 */
@RunWith(Parameterized.class)
public class TripletRpLongCpTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripletRpLongCpTest.class);

    public enum AcOperator {
        ADD,
        ADDI,
        SUB,
        SUBI,
        MUL,
        MULI,
        NEG,
        NEGI,
    }

    public static final AcOperator[] opAll = new AcOperator[]{
        AcOperator.ADD, AcOperator.ADDI, AcOperator.SUB, AcOperator.SUBI,
        AcOperator.MUL,
        AcOperator.MULI,
        AcOperator.NEG, AcOperator.NEGI};
    public static final List<AcOperator> MUL_REQ_OP = Collections.singletonList(AcOperator.MUL);

    private static final int BATCH_NUM = 16;

    private static final int SMALL_SIZE = 1 << 8;

    private static final int MIDDLE_SIZE = 1 << 12;

    private static final int LARGE_SIZE = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Aby3LongCpPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Aby3LongConfig.Builder(false).build()
        });
        configurations.add(new Object[]{
            Aby3LongCpPtoDesc.getInstance().getPtoName() + "(malicious)",
            new Aby3LongConfig.Builder(true).build()
        });
        configurations.add(new Object[]{
            Cgh18RpLongCpPtoDesc.getInstance().getPtoName() + "(mac)",
            new Cgh18RpLongConfig.Builder().build()
        });

        return configurations;
    }

    private final TripletRpLongConfig config;

    public TripletRpLongCpTest(String name, TripletRpLongConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testAllSmallSize() {
        testOpi(false, opAll, SMALL_SIZE);
    }
    @Test
    public void testEachSmallSize() {
        for(AcOperator op : opAll){
            AcOperator[] single = new AcOperator[]{op};
            testOpi(false, single, SMALL_SIZE);
        }
    }
    @Test
    public void testAllMiddleSize() {
        testOpi(true, opAll, MIDDLE_SIZE);
    }
    @Test
    public void testEachMiddleSize() {
        for(AcOperator op : opAll){
            AcOperator[] single = new AcOperator[]{op};
            testOpi(true, single, MIDDLE_SIZE);
        }
    }
    @Test
    public void testAllLargeSize() {
        testOpi(true, opAll, LARGE_SIZE);
    }
    @Test
    public void testEachLargeSize() {
        for(AcOperator op : opAll){
            AcOperator[] single = new AcOperator[]{op};
            testOpi(true, single, LARGE_SIZE);
        }
    }

    private TripletLongParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        TripletProvider[] tripletProviders = IntStream.range(0, 3).mapToObj(i ->
                new TripletProvider(rpcAll[i], new TripletProviderConfig.Builder(config.getSecurityModel().equals(SecurityModel.MALICIOUS)).build()))
            .toArray(TripletProvider[]::new);
        TripletLongParty[] parties = IntStream.range(0, 3).mapToObj(i ->
            TripletRpLongCpFactory.createParty(rpcAll[i], config, tripletProviders[i])).toArray(TripletLongParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void verifyRes(AcOperator op, LongVector[][] data) {
        LOGGER.info("verifying {}", op.toString());
        switch (op) {
            case ADD:
            case ADDI:
                for (int i = 0; i < data[0].length; i++) {
                    Assert.assertEquals(data[0][i].add(data[1][i]), data[2][i]);
                }
                break;
            case SUB:
            case SUBI:
                for (int i = 0; i < data[0].length; i++) {
                    Assert.assertEquals(data[0][i].sub(data[1][i]), data[2][i]);
                }
                break;
            case MUL:
            case MULI:
                for (int i = 0; i < data[0].length; i++) {
                    Assert.assertEquals(data[0][i].mul(data[1][i]), data[2][i]);
                }
                break;
            case NEG:
            case NEGI:
                for (int i = 0; i < data[0].length; i++) {
                    Assert.assertEquals(data[0][i].neg(), data[1][i]);
                }
                break;
        }
    }

    private void testOpi(boolean parallel, AcOperator[] ops, int eachBitNum) {
        TripletLongParty[] parties = getParties(parallel);
        int[] dataNums = IntStream.range(0, BATCH_NUM).map(i -> SECURE_RANDOM.nextInt(eachBitNum) + 1).toArray();

        try {
            LOGGER.info("-----test {}, (Data upper bound = {}) start-----", parties[0].getPtoDesc().getPtoName(), eachBitNum);
            TripletRpLongCpThread[] threads = Arrays.stream(parties).map(p ->
                new TripletRpLongCpThread(p, dataNums, ops)).toArray(TripletRpLongCpThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (TripletRpLongCpThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            for (AcOperator op : ops) {
                LongVector[][] data = threads[0].getInputAndRes(op);
                verifyRes(op, data);
            }

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (Data upper bound = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(), eachBitNum, time);
            LOGGER.info("op:[{}] test pass", Arrays.toString(ops));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
