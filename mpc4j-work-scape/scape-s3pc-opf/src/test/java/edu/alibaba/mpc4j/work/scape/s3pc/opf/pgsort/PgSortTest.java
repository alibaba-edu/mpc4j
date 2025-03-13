package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic.BitonicPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic.BitonicPgSortPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortPtoDesc;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * oblivious 3p sorting test.
 *
 * @author Feng Han
 * @date 2024/03/04
 */
@RunWith(Parameterized.class)
public class PgSortTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgSortTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * bit dimension to be sorted in our test
     */
    private static final int[] BIT_DIM = new int[]{2, 4, 64};
    /**
     * small input size
     */
    private static final int SMALL_SIZE = 1 << 2;
    /**
     * middle input size
     */
    private static final int MIDDLE_SIZE = 213;
    /**
     * large input size
     */
    private static final int LARGE_SIZE = 3375;

    @Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // bitonic sort
        configurations.add(new Object[]{
            BitonicPgSortPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new BitonicPgSortConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            BitonicPgSortPtoDesc.getInstance().getPtoName() + "(semi-honest, stable)",
            new BitonicPgSortConfig.Builder(false).setStableSign(true).build(), false
        });
        configurations.add(new Object[]{
            BitonicPgSortPtoDesc.getInstance().getPtoName() + "(malicious)",
            new BitonicPgSortConfig.Builder(true).build(), false
        });

        // quick sort
        configurations.add(new Object[]{
            QuickPgSortPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new QuickPgSortConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            QuickPgSortPtoDesc.getInstance().getPtoName() + "(malicious)",
            new QuickPgSortConfig.Builder(true).build(), false
        });

        // radix sort
        configurations.add(new Object[]{
            RadixPgSortPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new RadixPgSortConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            RadixPgSortPtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new RadixPgSortConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            RadixPgSortPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new RadixPgSortConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * sort configure
     */
    private final PgSortConfig config;
    /**
     * verify with mac or not
     */
    private final boolean baseUseMac;

    public PgSortTest(String name, PgSortConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void test1() {
        for (int bit : BIT_DIM) {
            testOpi(false, 1, bit);
        }
    }

    @Test
    public void testSmallSize() {
        for (int bit : BIT_DIM) {
            testOpi(false, SMALL_SIZE, bit);
        }
    }

    @Test
    public void testMiddleSize() {
        for (int bit : BIT_DIM) {
            testOpi(true, MIDDLE_SIZE, bit);
        }
    }

    @Test
    public void testLargeSize() {
        for (int bit : BIT_DIM) {
            testOpi(true, LARGE_SIZE, bit);
        }
    }

    private PgSortParty[] getParties(boolean parallel) {
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

        PgSortParty[] sortParties = Arrays.stream(parties).map(each ->
            PgSortFactory.createParty(each, config)).toArray(PgSortParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(sortParties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return sortParties;
    }

    private void testOpi(boolean parallel, int dataNum, int dataDim) {
        for (boolean flag : new boolean[]{true, false}) {
            PgSortParty[] parties = getParties(parallel);
            try {
                LOGGER.info("-----test {}, (dataNum = {}, dataDim = {}, saveSortRes:{}) start-----",
                    parties[0].getPtoDesc().getPtoName(), dataNum, dataDim, flag);
                PgSortPartyThread[] threads = Arrays.stream(parties).map(p ->
                    new PgSortPartyThread(p, dataNum, dataDim, flag)).toArray(PgSortPartyThread[]::new);
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                Arrays.stream(threads).forEach(Thread::start);
                for (PgSortPartyThread t : threads) {
                    t.join();
                }
                stopWatch.stop();
                long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
                // verify
                BigInteger[] input1 = threads[0].getInput();
                BigInteger[] output1 = threads[0].getOutput();
                int[] pai1 = threads[0].getPai();
                PgSortTestUtils.verify(input1, output1, pai1, config.isStable());
                // destroy
                Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
                LOGGER.info("-----test {}, (dataNum = {}, dataDim = {}, saveSortRes:{}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                    dataNum, dataDim, flag, time);
            } catch (InterruptedException | MpcAbortException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

