package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.db.sketch.HLL.v1.v1HLLConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.v1.v1HLLPtoDesc;
import edu.alibaba.mpc4j.work.db.sketch.utils.hll.HLLImpl;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RunWith(Parameterized.class)
public class HLLTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(HLLTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * small sketch size
     */
    private static final int SMALL_LOG_SKETCH_SIZE = 6;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LEN = 6;
    /**
     * small payload bit length
     */
    private static final int SMALL_HASH_BIT_LEN = 6;
    /**
     * small update number
     */
    private static final int SMALL_UPDATE_NUM = 1 << 6;
    /**
     * middle sketch size
     */
    private static final int MIDDLE_LOG_SKETCH_SIZE = 10;
    /**
     * middle element bit length
     */
    private static final int MIDDLE_ELEMENT_BIT_LEN = 16;
    /**
     * middle payload bit length
     */
    private static final int MIDDLE_HASH_BIT_LEN = 10;
    /**
     * middle update number
     */
    private static final int MIDDLE_UPDATE_NUM = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
                v1HLLPtoDesc.getInstance().getPtoName() + "(semi-honest)",
                new v1HLLConfig.Builder(false).build(), false
        });
        return configurations;
    }

    /**
     * party config
     */
    private final HLLConfig config;
    /**
     * verify with mac
     */
    private final boolean baseUseMac;

    public HLLTest(String name, HLLConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_LOG_SKETCH_SIZE, SMALL_ELEMENT_BIT_LEN, SMALL_HASH_BIT_LEN, SMALL_UPDATE_NUM);
    }

    @Test
    public void testMiddleSize() {
        testOpi(false, MIDDLE_LOG_SKETCH_SIZE, MIDDLE_ELEMENT_BIT_LEN, MIDDLE_HASH_BIT_LEN, MIDDLE_UPDATE_NUM);
    }

    private HLLParty[] getParties(boolean parallel){
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

        HLLParty[] parties = Arrays.stream(abb3Parties).map(each ->
                HLLFactory.createHLLParty(each,config)).toArray(HLLParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    /**
     * generate update data stored in the row form
     *
     * @param elementBitLen element bit length
     * @param updateRowNum  how many rows is required
     * @return update data in row form
     */
    private BigInteger[] genUpdateData(int elementBitLen, int updateRowNum) {
        MathPreconditions.checkPositiveInRangeClosed("0 < elementBitLen <= 64", elementBitLen, 64);
        return IntStream.range(0, updateRowNum).mapToObj(i ->
                BitVectorFactory.createRandom(elementBitLen, SECURE_RANDOM).getBigInteger()).toArray(BigInteger[]::new);
    }

    private void testOpi(boolean parallel, int logSketchSize, int elementBitLen, int hashBitLen, int updateNum){
        HLLParty[] parties = getParties(parallel);
        BigInteger[] updateKeys = genUpdateData(elementBitLen, updateNum);

        try{
            LOGGER.info("------------test {}",parties[0].getPtoDesc().getPtoName());

            HLLPartyThread[] threads = Arrays.stream(parties).map(p->
                    new HLLPartyThread(p, updateKeys, elementBitLen, logSketchSize, hashBitLen)).toArray(HLLPartyThread[]::new);

            StopWatch stopWatch=new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for(HLLPartyThread thread:threads){
                thread.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            long[] sketchRes=threads[0].getSketchRes();
            long result=threads[0].getQueryRes();

            HLLImpl plainHLL=new HLLImpl(1<<logSketchSize,threads[0].getHashKey(),hashBitLen);
            plainHLL.input(updateKeys);
            int[] plainRes= plainHLL.getTable();

//            LOGGER.info("sketch out: {}", Arrays.toString(sketchRes));
//            LOGGER.info("Plain out: {}",Arrays.toString(plainRes));
            for(int i=0;i<sketchRes.length;i++){
                assert (sketchRes[i]==plainRes[i]);
            }
//            LOGGER.info("sketch sum: {}", sum);
            LOGGER.info("------------test {}, estimated distinct count:{}",parties[0].getPtoDesc().getPtoName(),result);
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("------------test {}, end, time:{}-----", parties[0].getPtoDesc().getPtoName(),time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
