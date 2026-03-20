package edu.alibaba.mpc4j.work.db.sketch.SS;

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
import edu.alibaba.mpc4j.work.db.sketch.SS.z2.SSz2Config;
import edu.alibaba.mpc4j.work.db.sketch.SS.z2.SSz2PtoDesc;
import edu.alibaba.mpc4j.work.db.sketch.utils.ss.SS;
import edu.alibaba.mpc4j.work.db.sketch.utils.ss.SSBatchImpl;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Test class for SS (Space-Saving) top-k frequent items protocol in 3PC setting.
 * Tests both small and medium scale inputs with correctness verification.
 */
@RunWith(Parameterized.class)
public class SSTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSTest.class);
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * Logarithm of small sketch size (2^4 = 16)
     */
    private static final int SMALL_LOG_SKETCH_SIZE = 4;
    /**
     * Bit length for small elements
     */
    private static final int SMALL_ELEMENT_BIT_LEN = 6;
    /**
     * Bit length for small payload
     */
    private static final int SMALL_PAYLOAD_BIT_LEN = 6;
    /**
     * Number of updates for small test
     */
    private static final int SMALL_UPDATE_NUM = 1<<6;
    /**
     * Number of top-k items for small test
     */
    private static final int SMALL_TOP_K = 10;
    /**
     * Logarithm of medium sketch size (2^10 = 1024)
     */
    private static final int MIDDLE_LOG_SKETCH_SIZE = 10;
    /**
     * Bit length for medium elements
     */
    private static final int MIDDLE_ELEMENT_BIT_LEN = 16;
    /**
     * Bit length for medium payload
     */
    private static final int MIDDLE_PAYLOAD_BIT_LEN = 10;
    /**
     * Number of updates for medium test
     */
    private static final int MIDDLE_UPDATE_NUM = 1 << 12;
    /**
     * Number of top-k items for medium test
     */
    private static final int MIDDLE_TOP_K = 100;

    /**
     * Parameterized test configurations
     * @return collection of test configurations
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
            SSz2PtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new SSz2Config.Builder(false).build(), false
        });
        return configurations;
    }

    /**
     * Configuration for SS protocol
     */
    private final SSConfig config;
    /**
     * Flag to enable MAC verification
     */
    private final boolean baseUseMac;

    /**
     * Constructs an SS test with the specified configuration
     * @param name test name
     * @param config SS configuration
     * @param baseUseMac whether to use MAC for verification
     */
    public SSTest(String name, SSConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    /**
     * Test with small input size using Z2 implementation
     */
    @Test
    public void testZ2SmallSize() {
        testOpi(false, SMALL_LOG_SKETCH_SIZE, SMALL_ELEMENT_BIT_LEN, SMALL_PAYLOAD_BIT_LEN, SMALL_UPDATE_NUM, SMALL_TOP_K);
    }

    /**
     * Test with medium input size using Z2 implementation
     */
    @Test
    public void testZ2MiddleSize() {
        testOpi(false, MIDDLE_LOG_SKETCH_SIZE, MIDDLE_ELEMENT_BIT_LEN, MIDDLE_PAYLOAD_BIT_LEN, MIDDLE_UPDATE_NUM, MIDDLE_TOP_K);
    }

    /**
     * Main test method for SS protocol
     * @param parallel whether to enable parallel execution
     * @param logSketchSize log of sketch table size
     * @param keyBitLen bit length of keys
     * @param payloadBitLen bit length of payload
     * @param updateNum number of updates
     * @param topK number of top-k items to retrieve
     */
    private void testOpi(boolean parallel, int logSketchSize, int keyBitLen, int payloadBitLen, int updateNum, int topK) {
        SSParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, (updateNum = {}) start-----", parties[0].getPtoDesc().getPtoName(), logSketchSize);
            // Generate Gaussian-distributed update data for more realistic testing
            BigInteger[] updateData=genGaussianUpdateData(keyBitLen, updateNum);
            // Create party threads
            SSPartyThread[] threads = Arrays.stream(parties)
                .map(p -> new SSPartyThread(p, logSketchSize, keyBitLen, payloadBitLen, updateData, topK))
                .toArray(SSPartyThread[]::new);

            // Execute the protocol and measure time
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (SSPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            // Get results and verify correctness
            Pair<BigInteger[], BigInteger[]> sketchRes = threads[0].getSketchRes();
            Pair<BigInteger[], BigInteger[]> queryRes = threads[0].getQueryRes();
            // Verify sketch and query results
            verify(updateData, sketchRes.getLeft(), sketchRes.getRight());
            LOGGER.info("query key out: {}", Arrays.toString(queryRes.getLeft()));
            LOGGER.info("query count out: {}", Arrays.toString(queryRes.getRight()));

            // Cleanup
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (updateNum = {}) end, communication:{}, time:{} ms-----",
                parties[0].getPtoDesc().getPtoName(), logSketchSize, parties[0].getRpc().getSendByteLength(), time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates and initializes SS parties for the test
     * @param parallel whether to enable parallel execution
     * @return array of three SS parties
     */
    private SSParty[] getParties(boolean parallel) {
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

        SSParty[] parties = Arrays.stream(abb3Parties).map(each ->
                SSFactory.createParty(each, config)).toArray(SSParty[]::new);

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
     * Generates Gaussian-distributed update data for more realistic testing
     * @param elementBitLen bit length of elements
     * @param updateRowNum number of elements to generate
     * @return array of Gaussian-distributed BigIntegers
     */
    private BigInteger[] genGaussianUpdateData(int elementBitLen, int updateRowNum) {
        Random random = new Random();
        BigInteger[] updateData = new BigInteger[updateRowNum];
        for (int i = 0; i < updateData.length; i++) {
            updateData[i]=BigInteger.valueOf((long)random.nextGaussian(Math.pow(2,elementBitLen-1),Math.pow(2,elementBitLen-2)));
            if(updateData[i].compareTo(BigInteger.valueOf(1L <<elementBitLen))>=0){
                updateData[i]=BigInteger.valueOf(1L <<elementBitLen-1);
            }
            if(updateData[i].compareTo(BigInteger.ZERO)<=0){
                updateData[i]=BigInteger.ONE;
            }
        }
        return updateData;
    }

    /**
     * Verifies the sketch results against plain implementation
     * @param updateElements array of update elements
     * @param sketchKeys sketch keys from MPC
     * @param sketchCounts sketch counts from MPC
     */
    private void verify(BigInteger[] updateElements, BigInteger[] sketchKeys, BigInteger[] sketchCounts) {
        // Build histogram from update elements
        Map<BigInteger, Integer> updateMap = new HashMap<>();
        Map<BigInteger, BigInteger> secretMap = new HashMap<>();
        for (BigInteger updateElement : updateElements) {
            updateMap.put(updateElement, updateMap.getOrDefault(updateElement, 0) + 1);
        }
        // Compare with plain implementation
        SS plainSS = new SSBatchImpl(sketchKeys.length);
        plainSS.input(updateElements);
        Map<BigInteger,BigInteger> plainRes= plainSS.query();
        BigInteger[] keyPlain=plainRes.keySet().toArray(new BigInteger[0]);
        BigInteger[] countsPlain=plainRes.values().toArray(new BigInteger[0]);
        // Extract non-zero entries from sketch
        for (int i = 0; i < sketchKeys.length; i++) {
            if(!sketchKeys[i].equals(BigInteger.ZERO)) {
                if (!secretMap.containsKey(sketchKeys[i]) && (!sketchCounts[i].equals(BigInteger.ZERO))) {
                    secretMap.put(sketchKeys[i], sketchCounts[i]);
                }
            }
        }
        BigInteger[] keysReal = updateMap.keySet().toArray(new BigInteger[0]);
        Integer[] countsReal = updateMap.values().toArray(new Integer[0]);
        LOGGER.info("real key out: {}", Arrays.toString(keysReal));
        LOGGER.info("real count out: {}", Arrays.toString(countsReal));
        LOGGER.info("plain key out: {}", Arrays.toString(keyPlain));
        LOGGER.info("plain count out: {}", Arrays.toString(countsPlain));
        LOGGER.info("sketch key out: {}", Arrays.toString(sketchKeys));
        LOGGER.info("sketch count out: {}", Arrays.toString(sketchCounts));
        // Verify that the number of non-zero entries matches
        assert (secretMap.size() == plainRes.size());
    }
}
