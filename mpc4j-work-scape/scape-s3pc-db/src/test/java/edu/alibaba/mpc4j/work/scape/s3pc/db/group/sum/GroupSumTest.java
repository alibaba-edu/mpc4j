package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22.Hzf22GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22.Hzf22GroupSumPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext.Hzf22ExtGroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext.Hzf22ExtGroupSumPtoDesc;
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
 * test case for group sum
 *
 * @author Feng Han
 * @date 2025/2/24
 */
@RunWith(Parameterized.class)
public class GroupSumTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupSumTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * small input size
     */
    private static final int SMALL_SIZE = 7;
    /**
     * middle input size
     */
    private static final int MIDDLE_SIZE = (1 << 7) + 3;
    /**
     * large input size
     */
    private static final int LARGE_SIZE = 1 << 12;
    /**
     * input key dimensions, where the key is stored in long form
     */
    private static final int[] KEY_DIM = new int[]{1, 3};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Hzf22GroupSumPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22GroupSumConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22GroupSumPtoDesc.getInstance().getPtoName() + "(malicious, tuple)",
            new Hzf22GroupSumConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Hzf22GroupSumPtoDesc.getInstance().getPtoName() + "(malicious, mac)",
            new Hzf22GroupSumConfig.Builder(true).build(), true
        });
        configurations.add(new Object[]{
            Hzf22ExtGroupSumPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22ExtGroupSumConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22ExtGroupSumPtoDesc.getInstance().getPtoName() + "(malicious, tuple)",
            new Hzf22ExtGroupSumConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Hzf22ExtGroupSumPtoDesc.getInstance().getPtoName() + "(malicious, mac)",
            new Hzf22ExtGroupSumConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * config
     */
    private final GroupSumConfig config;
    /**
     * verify with mac
     */
    private final boolean baseUseMac;

    public GroupSumTest(String name, GroupSumConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_SIZE, KEY_DIM[0]);
    }

    @Test
    public void testMiddleSize() {
        testOpi(false, MIDDLE_SIZE, KEY_DIM[1]);
    }

    @Test
    public void testLargeSize() {
        testOpi(true, LARGE_SIZE, KEY_DIM[1]);
    }

    private GroupSumParty[] getParties(boolean parallel) {
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

        GroupSumParty[] parties = Arrays.stream(abb3Parties).map(each ->
            GroupSumFactory.createParty(each, config)).toArray(GroupSumParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void testOpi(boolean parallel, int inputSize, int inputDim) {
        GroupSumParty[] parties = getParties(parallel);
        LongVector[] plainTab = IntStream.range(0, inputDim)
            .mapToObj(i -> LongVector.createRandom(inputSize, SECURE_RANDOM))
            .toArray(LongVector[]::new);
        LongVector plainFlag = LongVector.create(IntStream.range(0, inputSize).mapToLong(i -> SECURE_RANDOM.nextBoolean() ? 1 : 0).toArray());
        plainFlag.setElements(LongVector.createZeros(1), 0, 0, 1);

        try {
            LOGGER.info("-----test {}, (inputSize = {}, inputDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), inputSize, inputDim);
            GroupSumPartyThread[] threads = Arrays.stream(parties).map(p ->
                new GroupSumPartyThread(p, plainTab, plainFlag)).toArray(GroupSumPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (GroupSumPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            LongVector[] res = threads[0].getResult();
            verify(plainTab, plainFlag, res);

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (inputSize = {}, inputDim = {}) end, time:{}-----",
                parties[0].getPtoDesc().getPtoName(), inputSize, inputDim, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void verify(LongVector[] plainTab, LongVector plainFlag, LongVector[] res) {
        MathPreconditions.checkEqual("res.length", "plainTab.length + 1", res.length, plainTab.length + 1);
        long[] resFlag = res[res.length - 1].getElements();
        long[] lastSum = new long[plainTab.length];
        for (int i = 0; i < plainTab[0].getNum(); i++) {
            if (plainFlag.getElement(i) == 1) {
                for (int j = 0; j < plainTab.length; j++) {
                    lastSum[j] += plainTab[j].getElement(i);
                }
            } else {
                for (int j = 0; j < plainTab.length; j++) {
                    lastSum[j] = plainTab[j].getElement(i);
                }
            }
            boolean isLast = (i == plainTab[0].getNum() - 1 || (plainFlag.getElement(i + 1) == 0));
            if (isLast) {
                Assert.assertEquals(1, resFlag[i]);
                for (int j = 0; j < plainTab.length; j++) {
                    if(lastSum[j] != res[j].getElement(i)) {
                        LOGGER.info("lastSum: {}, res: {}", lastSum[j], res[j].getElement(i));
                    }
                    Assert.assertEquals(lastSum[j], res[j].getElement(i));
                }
            } else {
                Assert.assertEquals(0, resFlag[i]);
            }
        }
    }
}
