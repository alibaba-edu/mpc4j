package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcParamUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcSoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcSoprpPtoDesc;
import org.apache.commons.lang3.time.StopWatch;
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
 * oblivious 3p soprp test.
 *
 * @author Feng Han
 * @date 2024/03/04
 */
@RunWith(Parameterized.class)
public class SoprpTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoprpTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
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
    private static final int LARGE_SIZE = 23131;
    /**
     * test parameters
     */
    private static final LowMcParam[] LOW_MC_PARAMS =
        Arrays.stream(new int[]{64, 80})
        .mapToObj(x -> LowMcParamUtils.getParam(x, LARGE_SIZE, 40))
        .toArray(LowMcParam[]::new);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // lowmc
        configurations.add(new Object[]{
            LowMcSoprpPtoDesc.getInstance().getPtoName() + "(semi-honest, 64)",
            new LowMcSoprpConfig.Builder(false, LOW_MC_PARAMS[0]).build()
        });
        configurations.add(new Object[]{
            LowMcSoprpPtoDesc.getInstance().getPtoName() + "(semi-honest, 80)",
            new LowMcSoprpConfig.Builder(false, LOW_MC_PARAMS[1]).build()
        });
        configurations.add(new Object[]{
            LowMcSoprpPtoDesc.getInstance().getPtoName() + "(malicious, 64)",
            new LowMcSoprpConfig.Builder(true, LOW_MC_PARAMS[0]).build()
        });
        configurations.add(new Object[]{
            LowMcSoprpPtoDesc.getInstance().getPtoName() + "(malicious, 80)",
            new LowMcSoprpConfig.Builder(true, LOW_MC_PARAMS[1]).build()
        });

        return configurations;
    }
    /**
     * configure
     */
    private final SoprpConfig config;

    public SoprpTest(String name, SoprpConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_SIZE);
    }

    @Test
    public void testMiddleSize() {
        testOpi(true, MIDDLE_SIZE);
    }

    @Test
    public void testLargeSize() {
        testOpi(true, LARGE_SIZE);
    }

    private SoprpParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);

        Abb3RpConfig abb3RpConfig = (isMalicious && USE_MT_TEST_MODE)
            ? new Abb3RpConfig.Builder(isMalicious, false)
            .setTripletProviderConfig(new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode())
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode())
                .build()).build()
            : new Abb3RpConfig.Builder(isMalicious, false).build();
        Abb3Party[] parties = IntStream.range(0, 3).mapToObj(i ->
                new Abb3RpParty(rpcAll[i], abb3RpConfig))
            .toArray(Abb3RpParty[]::new);

        SoprpParty[] sortParties = Arrays.stream(parties).map(each ->
            SoprpFactory.createParty(each, config)).toArray(SoprpParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(sortParties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return sortParties;
    }

    private void testOpi(boolean parallel, int dataNum) {
        SoprpParty[] parties = getParties(parallel);
        int dataDim = parties[0].getInputDim();
        try {
            LOGGER.info("-----test {}, (dataNum = {}, dataDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), dataNum, dataDim);
            SoprpPartyThread[] threads = Arrays.stream(parties).map(p ->
                new SoprpPartyThread(p, dataNum, dataDim)).toArray(SoprpPartyThread[]::new);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (SoprpPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (dataNum = {}, dataDim = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                dataNum, dataDim, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
