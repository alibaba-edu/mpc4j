package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper;


import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations.FillPerOp;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.hzf22.Hzf22FillPermutationConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.hzf22.Hzf22FillPermutationPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.kks20.Kks20FillPermutationConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.kks20.Kks20FillPermutationPtoDesc;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * test cases of filling permutation protocol
 *
 * @author Feng Han
 * @date 2025/2/18
 */
@RunWith(Parameterized.class)
public class FillPermutationTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(FillPermutationTest.class);
    /**
     * operations
     */
    public static final FillPerOp[] opAll = new FillPerOp[]{
        FillPerOp.FILL_ONE_PER_A,
        FillPerOp.FILL_TWO_PER_A,
    };
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * small input size
     */
    private static final int SMALL_SIZE = 11;
    /**
     * middle input size
     */
    private static final int MIDDLE_SIZE = 1 << 9;
    /**
     * large input size
     */
    private static final int LARGE_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Kks20FillPermutationPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Kks20FillPermutationConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Kks20FillPermutationPtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new Kks20FillPermutationConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Kks20FillPermutationPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Kks20FillPermutationConfig.Builder(true).build(), true
        });

        configurations.add(new Object[]{
            Hzf22FillPermutationPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22FillPermutationConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22FillPermutationPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Hzf22FillPermutationConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * configure
     */
    private final FillPermutationConfig config;
    /**
     * verify with mac
     */
    private final boolean baseUseMac;

    public FillPermutationTest(String name, FillPermutationConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testEachSmallSize() {
        for (FillPerOp op : opAll) {
            FillPerOp[] single = new FillPerOp[]{op};
            testOpi(false, single, SMALL_SIZE);
        }
    }

    @Test
    public void testEachMiddleSize() {
        for (FillPerOp op : opAll) {
            FillPerOp[] single = new FillPerOp[]{op};
            testOpi(false, single, MIDDLE_SIZE);
        }
    }

    @Test
    public void testEachLargeSize() {
        for(FillPerOp op : opAll){
            FillPerOp[] single = new FillPerOp[]{op};
            testOpi(true, single, LARGE_SIZE);
        }
    }

    private FillPermutationParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);
        Abb3RpConfig abb3RpConfig = (isMalicious && USE_MT_TEST_MODE)
            ? new Abb3RpConfig.Builder(isMalicious, baseUseMac)
            .setTripletProviderConfig(new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode())
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode())
                .build()).build()
            : new Abb3RpConfig.Builder(isMalicious, baseUseMac).build();
        Abb3Party[] abb3Parties = IntStream.range(0, 3).mapToObj(i ->
            new Abb3RpParty(rpcAll[i], abb3RpConfig)).toArray(Abb3RpParty[]::new);

        FillPermutationParty[] parties = Arrays.stream(abb3Parties).map(each ->
            FillPermutationFactory.createParty(each, config)).toArray(FillPermutationParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    public int[][] getInput(int dataNum){
        SecureRandom secureRandom = new SecureRandom();
        int[][] res = new int[6][];
        for(int i = 0; i < 3; i++){
            res[2 * i] = new int[3];
            res[2 * i][0] = Math.min(dataNum, secureRandom.nextInt(Math.max(2, dataNum / 4)) + 1);
            res[2 * i][1] = Math.min(dataNum, secureRandom.nextInt(Math.max(2, dataNum / 2)) + 1);
            res[2 * i][2] = Math.min(dataNum, secureRandom.nextInt(Math.max(2, dataNum / 2)) + Math.max(1, dataNum / 4));
            res[2 * i + 1] = Arrays.stream(res[2 * i]).map(secureRandom::nextInt).toArray();
        }
        return res;
    }

    private void testOpi(boolean parallel, FillPerOp[] ops, int dataNum) {
        FillPermutationParty[] parties = getParties(parallel);
        int[][] param = getInput(dataNum);
        try {
            LOGGER.info("-----test {}, (dataNum = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), dataNum);
            FillPermutationPartyThread[] threads = Arrays.stream(parties).map(p ->
                new FillPermutationPartyThread(p, dataNum, ops)).toArray(FillPermutationPartyThread[]::new);

            for (FillPermutationPartyThread t : threads) {
                t.setParam(param[0], param[1], param[2], param[3], param[4], param[5]);
            }

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (FillPermutationPartyThread t : threads) {
                t.setParam(param[0], param[1], param[2], param[3], param[4], param[5]);
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            for (FillPerOp op : ops) {
                if (op.equals(FillPerOp.FILL_ONE_PER_A)) {
                    verifyFillOne(threads[0].getFillOneInput(), threads[0].getFillOneOutput(), dataNum);
                } else {
                    verifyFillTwo(threads[0].getFillTwoInput(), threads[0].getFillTwoOutput(), dataNum);
                }
            }

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (dataNum = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                dataNum, time);
            LOGGER.info("op:[{}] test pass", Arrays.toString(ops));
        } catch (InterruptedException | MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }

    private static void verifyFillOne(LongVector[] input, LongVector[] output, int outputSize) throws MpcAbortException {
        for (int i = 0; i < input.length; i++) {
            verify(input[i], output[i], outputSize);
        }
    }

    private static void verifyFillTwo(LongVector[][] input, LongVector[][] output, int outputSize) throws MpcAbortException {
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < 2; j++) {
                verify(input[i][j], output[i][j], outputSize);
            }
        }
    }

    private static void verify(LongVector input, LongVector output, int outputSize) throws MpcAbortException {
        Assert.assertArrayEquals(input.getElements(), Arrays.copyOf(output.getElements(), input.getNum()));
        ShuffleUtils.checkCorrectIngFun(Arrays.stream(output.getElements()).mapToInt(x -> (int) x).toArray(), outputSize);
    }
}

