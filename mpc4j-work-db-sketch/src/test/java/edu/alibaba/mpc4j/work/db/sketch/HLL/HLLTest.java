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
import edu.alibaba.mpc4j.work.db.sketch.HLL.z2.HLLz2Config;
import edu.alibaba.mpc4j.work.db.sketch.HLL.z2.HLLz2PtoDesc;
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

/**
 * Test class for HLL (HyperLogLog) cardinality estimation protocol in 3PC setting.
 * Tests both small and medium scale inputs with correctness verification.
 */
@RunWith(Parameterized.class)
public class HLLTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(HLLTest.class);
    /**
     * Flag to use simulated MTP (Multiplication Triples) for testing
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * Logarithm of small sketch size (2^6 = 64)
     */
    private static final int SMALL_LOG_SKETCH_SIZE = 6;
    /**
     * Bit length for small elements
     */
    private static final int SMALL_ELEMENT_BIT_LEN = 6;
    /**
     * Bit length for small hash output
     */
    private static final int SMALL_HASH_BIT_LEN = 6;
    /**
     * Number of updates for small test
     */
    private static final int SMALL_UPDATE_NUM = 1 << 6;
    /**
     * Logarithm of medium sketch size (2^10 = 1024)
     */
    private static final int MIDDLE_LOG_SKETCH_SIZE = 10;
    /**
     * Bit length for medium elements
     */
    private static final int MIDDLE_ELEMENT_BIT_LEN = 16;
    /**
     * Bit length for medium hash output
     */
    private static final int MIDDLE_HASH_BIT_LEN = 10;
    /**
     * Number of updates for medium test
     */
    private static final int MIDDLE_UPDATE_NUM = 1 << 12;

    /**
     * Parameterized test configurations
     * @return collection of test configurations
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
                HLLz2PtoDesc.getInstance().getPtoName() + "(semi-honest)",
                new HLLz2Config.Builder(false).build(), false
        });
        return configurations;
    }

    /**
     * Configuration for HLL protocol
     */
    private final HLLConfig config;
    /**
     * Flag to enable MAC verification
     */
    private final boolean baseUseMac;

    /**
     * Constructs an HLL test with the specified configuration
     * @param name test name
     * @param config HLL configuration
     * @param baseUseMac whether to use MAC for verification
     */
    public HLLTest(String name, HLLConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    /**
     * Test with small input size
     */
    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_LOG_SKETCH_SIZE, SMALL_ELEMENT_BIT_LEN, SMALL_HASH_BIT_LEN, SMALL_UPDATE_NUM);
    }

    /**
     * Test with medium input size
     */
    @Test
    public void testMiddleSize() {
        testOpi(false, MIDDLE_LOG_SKETCH_SIZE, MIDDLE_ELEMENT_BIT_LEN, MIDDLE_HASH_BIT_LEN, MIDDLE_UPDATE_NUM);
    }

    /**
     * Creates and initializes HLL parties for the test
     * @param parallel whether to enable parallel execution
     * @return array of three HLL parties
     */
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
     * Generates random update data
     * @param elementBitLen bit length of elements
     * @param updateRowNum number of elements to generate
     * @return array of random BigIntegers
     */
    private BigInteger[] genUpdateData(int elementBitLen, int updateRowNum) {
        MathPreconditions.checkPositiveInRangeClosed("0 < elementBitLen <= 64", elementBitLen, 64);
        return IntStream.range(0, updateRowNum).mapToObj(i ->
                BitVectorFactory.createRandom(elementBitLen, SECURE_RANDOM).getBigInteger()).toArray(BigInteger[]::new);
    }

    /**
     * Main test method for HLL protocol
     * @param parallel whether to enable parallel execution
     * @param logSketchSize log of sketch table size
     * @param elementBitLen bit length of elements
     * @param hashBitLen bit length of hash output
     * @param updateNum number of updates
     */
    private void testOpi(boolean parallel, int logSketchSize, int elementBitLen, int hashBitLen, int updateNum) {
        HLLParty[] parties = getParties(parallel);
        BigInteger[] updateKeys = genUpdateData(elementBitLen, updateNum);

        try{
            LOGGER.info("------------test {}",parties[0].getPtoDesc().getPtoName());

            // Create party threads
            HLLPartyThread[] threads = Arrays.stream(parties).map(p->
                    new HLLPartyThread(p, updateKeys, elementBitLen, logSketchSize, hashBitLen)).toArray(HLLPartyThread[]::new);

            // Execute the protocol and measure time
            StopWatch stopWatch=new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for(HLLPartyThread thread:threads){
                thread.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            
            // Get results and verify correctness
            long[] sketchRes=threads[0].getSketchRes();
            long result=threads[0].getQueryRes();

            // Verify sketch correctness against plain implementation
            HLLImpl plainHLL = new HLLImpl(1 << logSketchSize, threads[0].getHashKey(), hashBitLen);
            plainHLL.input(updateKeys);
            int[] plainRes = plainHLL.getTable();

            for (int i = 0; i < sketchRes.length; i++) {
                assert (sketchRes[i] == plainRes[i]);
            }
            
            LOGGER.info("------------test {}, estimated distinct count:{}",parties[0].getPtoDesc().getPtoName(),result);
            
            // Cleanup
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("------------test {}, end, time:{}-----", parties[0].getPtoDesc().getPtoName(),time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
