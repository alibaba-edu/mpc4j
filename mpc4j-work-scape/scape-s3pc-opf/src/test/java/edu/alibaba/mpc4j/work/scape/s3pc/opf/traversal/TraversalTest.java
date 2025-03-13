package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermutationTest;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.AcTraversalRes;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.BcTraversalRes;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.TraversalOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.hzf22.Hzf22TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.hzf22.Hzf22TraversalPtoDesc;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * oblivious 3p traversal test.
 *
 * @author Feng Han
 * @date 2024/03/05
 */
@RunWith(Parameterized.class)
public class TraversalTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutationTest.class);

    /**
     * operations to be tested
     */
    public static final TraversalOp[] opAll = new TraversalOp[]{
        TraversalOp.TRAVERSAL_A,
        TraversalOp.TRAVERSAL_B
    };

    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * bit dimension
     */
    private static final int BIT_DIM = 64;
    /**
     * long input dimension
     */
    private static final int LONG_DIM = 2;
    /**
     * small test size
     */
    private static final int SMALL_SIZE = 22;
    /**
     * middle test size
     */
    private static final int MIDDLE_SIZE = 2313;
    /**
     * large test size
     */
    private static final int LARGE_SIZE = 123123;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

//        configurations.add(new Object[]{
//            Hzf22TraversalPtoDesc.getInstance().getPtoName() + "(semi-honest)",
//            new Hzf22TraversalConfig.Builder(false).build(), false
//        });
        configurations.add(new Object[]{
            Hzf22TraversalPtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new Hzf22TraversalConfig.Builder(true).build(), false
        });
//        configurations.add(new Object[]{
//            Hzf22TraversalPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
//            new Hzf22TraversalConfig.Builder(true).build(), true
//        });

        return configurations;
    }

    /**
     * protocol configure
     */
    private final TraversalConfig config;
    /**
     * verify arithmetic shares with mac
     */
    private final boolean baseUseMac;

    public TraversalTest(String name, TraversalConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testAllSmallSize() {
        testOpi(false, opAll, SMALL_SIZE);
    }

    @Test
    public void testEachSmallSize() {
        for (TraversalOp op : opAll) {
            TraversalOp[] single = new TraversalOp[]{op};
            testOpi(false, single, SMALL_SIZE);
        }
    }

    @Test
    public void testAllMiddleSize() {
        testOpi(false, opAll, MIDDLE_SIZE);
    }

    @Test
    public void testEachMiddleSize() {
        for (TraversalOp op : opAll) {
            TraversalOp[] single = new TraversalOp[]{op};
            testOpi(false, single, MIDDLE_SIZE);
        }
    }

    @Test
    public void testAllLargeSize() {
        testOpi(true, opAll, LARGE_SIZE);
    }
    @Test
    public void testEachLargeSize() {
        for(TraversalOp op : opAll){
            TraversalOp[] single = new TraversalOp[]{op};
            testOpi(true, single, LARGE_SIZE);
        }
    }

    private TraversalParty[] getParties(boolean parallel) {
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

        TraversalParty[] traversalParties = Arrays.stream(parties).map(each ->
            TraversalFactory.createParty(each, config)).toArray(TraversalParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(traversalParties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return traversalParties;
    }

    private void testAcRes(TraversalOp op, AcTraversalRes x) {
        LOGGER.info("verifying " + op.name());
        LongVector[] input = x.input;
        LongVector[] output = x.output;
        long[] flag = x.flag.getElements();
        MathPreconditions.checkEqual("input.length", "output.length", input.length, output.length);
        for(int i = 0; i < input.length; i++) {
            long[] in = input[i].getElements();
            long[] out = output[i].getElements();
            MathPreconditions.checkEqual("in.length", "out.length", in.length, out.length);
            if(!x.isInv){
                Assert.assertEquals(in[0], out[0]);
                for(int j = 1; j < in.length; j++){
                    if(flag[j] == 1){
                        Assert.assertEquals(out[j - 1] + (x.theta ? 0 : in[j]), out[j]);
                    }else{
                        Assert.assertEquals(in[j], out[j]);
                    }
                }
            }else{
                Assert.assertEquals(in[in.length - 1], out[in.length - 1]);
                for(int j = in.length - 2; j >= 0; j--){
                    if(flag[j] == 1){
                        Assert.assertEquals(out[j + 1] + (x.theta ? 0 : in[j]), out[j]);
                    }else{
                        Assert.assertEquals(in[j], out[j]);
                    }
                }
            }
        }
    }

    private void testBcRes(TraversalOp op, BcTraversalRes x) {
        LOGGER.info("verifying " + op.name());
        BitVector[] input = x.input;
        BitVector[] output = x.output;
        boolean[] flag = BinaryUtils.byteArrayToBinary(x.flag.getBytes(), x.flag.bitNum());
        MathPreconditions.checkEqual("input.length", "output.length", input.length, output.length);
        for(int i = 0; i < input.length; i++) {
            boolean[] in = BinaryUtils.byteArrayToBinary(input[i].getBytes(), x.flag.bitNum());
            boolean[] out = BinaryUtils.byteArrayToBinary(output[i].getBytes(), x.flag.bitNum());
            MathPreconditions.checkEqual("in.length", "out.length", in.length, out.length);
            if(!x.isInv){
                Assert.assertEquals(in[0], out[0]);
                for(int j = 1; j < in.length; j++){
                    if(flag[j]){
                        Assert.assertEquals(out[j - 1] | in[j], out[j]);
                    }else{
                        Assert.assertEquals(in[j], out[j]);
                    }
                }
            }else{
                Assert.assertEquals(in[in.length - 1], out[in.length - 1]);
                for(int j = in.length - 2; j >= 0; j--){
                    if(flag[j]){
                        Assert.assertEquals(out[j + 1] | in[j], out[j]);
                    }else{
                        Assert.assertEquals(in[j], out[j]);
                    }
                }
            }
        }
    }

    private void testOpi(boolean parallel, TraversalOp[] ops, int dataNum) {
        TraversalParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, (dataNum = {}, bitDim = {}, longDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), dataNum, BIT_DIM, LONG_DIM);
            TraversalPartyThread[] threads = Arrays.stream(parties).map(p ->
                new TraversalPartyThread(p, ops, dataNum, BIT_DIM, LONG_DIM)).toArray(TraversalPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (TraversalPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            // verify
            for(TraversalOp op : ops){
                if(op.name().endsWith("_B")){
                    List<BcTraversalRes> tmp = threads[0].getBcRes();
                    for(BcTraversalRes each : tmp){
                        testBcRes(op, each);
                    }
                }else{
                    List<AcTraversalRes> tmp = threads[0].getAcRes();
                    for(AcTraversalRes each : tmp){
                        testAcRes(op, each);
                    }
                }
            }

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (dataNum = {}, bitDim = {}, longDim = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                dataNum, BIT_DIM, LONG_DIM, time);
            LOGGER.info("op:[{}] test pass", Arrays.toString(ops));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
