package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.AcPermuteRes;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.BcPermuteRes;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.ahi22.Ahi22PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.ahi22.Ahi22PermutePtoDesc;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * oblivious permutation test.
 *
 * @author Feng Han
 * @date 2024/02/28
 */
@RunWith(Parameterized.class)
public class PermutationTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutationTest.class);

    /**
     * operations to be tested
     */
    public static final PermuteOp[] opAll = new PermuteOp[]{
        PermuteOp.COMPOSE_A_A,
        PermuteOp.COMPOSE_B_B,
        PermuteOp.APPLY_INV_A_A,
        PermuteOp.APPLY_INV_B_B,
        PermuteOp.APPLY_INV_A_B
    };

    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * input dimension
     */
    private static final int BATCH_NUM = 2;
    /**
     * small test size
     */
    private static final int SMALL_SIZE = 1 << 2;
    /**
     * middle test size
     */
    private static final int MIDDLE_SIZE = 1 << 10;
    /**
     * large test size
     */
    private static final int LARGE_SIZE = 1 << 15;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Ahi22PermutePtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Ahi22PermuteConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Ahi22PermutePtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new Ahi22PermuteConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Ahi22PermutePtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Ahi22PermuteConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * permute config
     */
    private final PermuteConfig config;
    /**
     * use mac to verify or not
     */
    private final boolean baseUseMac;

    public PermutationTest(String name, PermuteConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testAllSmallSize() {
        testOpi(false, opAll, SMALL_SIZE, BATCH_NUM);
    }

    @Test
    public void testEachSmallSize() {
        for (PermuteOp op : opAll) {
            PermuteOp[] single = new PermuteOp[]{op};
            testOpi(false, single, SMALL_SIZE, BATCH_NUM);
        }
    }

    @Test
    public void testAllMiddleSize() {
        testOpi(false, opAll, MIDDLE_SIZE, BATCH_NUM);
    }

    @Test
    public void testEachMiddleSize() {
        for (PermuteOp op : opAll) {
            PermuteOp[] single = new PermuteOp[]{op};
            testOpi(false, single, MIDDLE_SIZE, BATCH_NUM);
        }
    }

    @Test
    public void testAllLargeSize() {
        testOpi(true, opAll, LARGE_SIZE, BATCH_NUM);
    }
    @Test
    public void testEachLargeSize() {
        for(PermuteOp op : opAll){
            PermuteOp[] single = new PermuteOp[]{op};
            testOpi(true, single, LARGE_SIZE, BATCH_NUM);
        }
    }

    private PermuteParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);

        Abb3RpConfig abb3RpConfig = (isMalicious && USE_MT_TEST_MODE)
            ? new Abb3RpConfig.Builder(isMalicious, baseUseMac)
            .setTripletProviderConfig(new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode())
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode())
                .build()).build()
            : new Abb3RpConfig.Builder(isMalicious, baseUseMac).build();

        Abb3Party[] parties = IntStream.range(0, 3).mapToObj(i ->
                new Abb3RpParty(rpcAll[i], abb3RpConfig))
            .toArray(Abb3RpParty[]::new);

        PermuteParty[] permuteParties = Arrays.stream(parties).map(each ->
            PermuteFactory.createParty(each, config)).toArray(PermuteParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(permuteParties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return permuteParties;
    }

    private void testAcRes(PermuteOp op, AcPermuteRes x) throws MpcAbortException {
        LOGGER.info("verifying " + op.name());
        LongVector[] input = x.input;
        LongVector[] output = x.output;
        if(op.name().startsWith("COMPOSE")){
            LongVector[] tmp = ShuffleUtils.applyPermutationToRows(input, x.pai);
            Assert.assertArrayEquals(tmp, output);
        }else{
            LongVector[] tmp = ShuffleUtils.applyPermutationToRows(output, x.pai);
            Assert.assertArrayEquals(tmp, input);
        }
    }

    private void testBcRes(PermuteOp op, BcPermuteRes x) throws MpcAbortException {
        LOGGER.info("verifying " + op.name());
        BitVector[] input = x.input;
        BitVector[] output = x.output;
        if(op.name().startsWith("COMPOSE")){
            BitVector[] tmp = ShuffleUtils.applyPermutationToRows(input, x.pai);
            Assert.assertArrayEquals(tmp, output);
        }else{
            BitVector[] tmp = ShuffleUtils.applyPermutationToRows(output, x.pai);
            Assert.assertArrayEquals(tmp, input);
        }
    }

    private void testOpi(boolean parallel, PermuteOp[] ops, int dataNum, int dataDim) {
        PermuteParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, (dataNum = {}, dataDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), dataNum, dataDim);
            PermutationPartyThread[] threads = Arrays.stream(parties).map(p ->
                new PermutationPartyThread(p, dataNum, dataDim, ops)).toArray(PermutationPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (PermutationPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            // verify
            for(PermuteOp op : ops){
                if(op.name().endsWith("_B")){
                    BcPermuteRes tmp = threads[0].getBcPermuteRes(op);
                    testBcRes(op, tmp);
                }else{
                    AcPermuteRes tmp = threads[0].getAcPermuteRes(op);
                    testAcRes(op, tmp);
                }
            }

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (dataNum = {}, dataDim = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                dataNum, dataDim, time);
            LOGGER.info("op:[{}] test pass", Arrays.toString(ops));
        } catch (MpcAbortException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
