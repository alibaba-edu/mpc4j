package edu.alibaba.mpc4j.work.db.sketch.GK;

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
import edu.alibaba.mpc4j.work.db.sketch.GK.z2.GKz2Config;
import edu.alibaba.mpc4j.work.db.sketch.GK.z2.GKz2PtoDesc;
import edu.alibaba.mpc4j.work.db.sketch.utils.gk.GKBatchImpl;
import edu.alibaba.mpc4j.work.db.sketch.utils.gk.Representative;
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
 * Test class for GK (Greenwald-Khanna) quantile sketch protocol in 3PC setting.
 * Tests both small and medium scale inputs with correctness verification.
 */
@RunWith(Parameterized.class)
public class GKTest extends AbstractThreePartyMemoryRpcPto {

    private static final Logger LOGGER = LoggerFactory.getLogger(GKTest.class);
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
    private static final int SMALL_PAYLOAD_BIT_LEN = 7;
    /**
     * Number of updates for small test
     */
    private static final int SMALL_UPDATE_NUM = 1<<6;
    /**
     * Error parameter for small test
     */
    private static final double SMALL_EPSILON = 0.2;
    /**
     * Number of queries for small test
     */
    private static final int SMALL_QUERY_NUM = 10;
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
    private static final int MIDDLE_PAYLOAD_BIT_LEN = 12;
    /**
     * Number of updates for medium test
     */
    private static final int MIDDLE_UPDATE_NUM = 1 << 12;
    /**
     * Error parameter for medium test
     */
    private static final double MIDDLE_EPSILON = 0.1;
    /**
     * Number of queries for medium test
     */
    private static final int MIDDLE_QUERY_NUM = 100;

    /**
     * Parameterized test configurations
     *
     * @return collection of test configurations
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
            GKz2PtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new GKz2Config.Builder(false).build(), false
        });
        return configurations;
    }

    /**
     * Configuration for GK protocol
     */
    private final GKConfig config;
    /**
     * Flag to enable MAC verification
     */
    private final boolean baseUseMac;

    /**
     * Constructs a GK test with the specified configuration
     * @param name test name
     * @param config GK configuration
     * @param baseUseMac whether to use MAC for verification
     */
    public GKTest(String name, GKConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    /**
     * Test with small input size using Z2 implementation
     */
    @Test
    public void testZ2SmallSize() {
        testOpi(false, SMALL_LOG_SKETCH_SIZE, SMALL_ELEMENT_BIT_LEN, SMALL_PAYLOAD_BIT_LEN, SMALL_EPSILON, SMALL_UPDATE_NUM, SMALL_QUERY_NUM);
    }

    /**
     * Test with medium input size using Z2 implementation
     */
    @Test
    public void testZ2MiddleSize() {
        testOpi(false, MIDDLE_LOG_SKETCH_SIZE, MIDDLE_ELEMENT_BIT_LEN, MIDDLE_PAYLOAD_BIT_LEN, MIDDLE_EPSILON, MIDDLE_UPDATE_NUM, MIDDLE_QUERY_NUM);
    }

    /**
     * Main test method for GK protocol
     * @param parallel whether to enable parallel execution
     * @param logSketchSize log of sketch table size
     * @param keyBitLen bit length of keys
     * @param payloadBitLen bit length of payload
     * @param epsilon error parameter
     * @param updateNum number of updates
     * @param queryNum number of queries
     */
    private void testOpi(boolean parallel, int logSketchSize, int keyBitLen, int payloadBitLen, double epsilon, int updateNum, int queryNum) {
        GKParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, (updateNum = {}) start-----", parties[0].getPtoDesc().getPtoName(), logSketchSize);
            // Generate random update and query data
            BigInteger[] updateData= genUpdateData(keyBitLen, updateNum);
            BigInteger[] queryData = genUpdateData(keyBitLen, queryNum);
            // Create party threads
            GKPartyThread[] threads = Arrays.stream(parties)
                    .map(p -> new GKPartyThread(p, logSketchSize, keyBitLen, payloadBitLen, epsilon, updateData, queryData))
                    .toArray(GKPartyThread[]::new);

            // Execute the protocol and measure time
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (GKPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            // Get results and verify correctness
            BigInteger[][] sketchRes = threads[0].getSketchRes();
            int[] queryRes = threads[0].getQueryRes();
            // Verify sketch and query results
            verify(updateData, sketchRes,logSketchSize,epsilon);
            LOGGER.info("query key: {}", Arrays.toString(queryData));
            LOGGER.info("query count out: {}", Arrays.toString(queryRes));

            // Cleanup
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (updateNum = {}) end, communication:{}, time:{} ms-----",
                    parties[0].getPtoDesc().getPtoName(), logSketchSize, parties[0].getRpc().getSendByteLength(), time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates and initializes GK parties for the test
     * @param parallel whether to enable parallel execution
     * @return array of three GK parties
     */
    private GKParty[] getParties(boolean parallel) {
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

        GKParty[] parties = Arrays.stream(abb3Parties).map(each ->
                GKFactory.createParty(each, config)).toArray(GKParty[]::new);

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
     * Verifies the sketch results against plain implementation
     * @param updateElements array of update elements
     * @param sketchRes sketch results from MPC
     * @param logSketchSize log of sketch table size
     * @param epsilon error parameter
     */
    private void verify(BigInteger[] updateElements, BigInteger[][] sketchRes, int logSketchSize, double epsilon) {
        // Build histogram from update elements
        Map<BigInteger, Integer> updateMap = new HashMap<>();
        for (BigInteger updateElement : updateElements) {
            updateMap.put(updateElement, updateMap.getOrDefault(updateElement, 0) + 1);
        }

        // Sort elements by key
        Pair<BigInteger, Integer>[] test = new Pair[updateMap.size()];
        BigInteger[] keys = updateMap.keySet().toArray(new BigInteger[0]);
        Integer[] counts = updateMap.values().toArray(new Integer[0]);
        IntStream.range(0, updateMap.size()).forEach(i -> {
            test[i] = Pair.of(keys[i], counts[i]);
        });
        Pair<BigInteger, Integer>[] sort = Arrays.stream(test).sorted(Comparator.comparing(Pair::getLeft)).toArray(Pair[]::new);
        BigInteger[] keysReal = Arrays.stream(sort).map(ea -> ea.getLeft()).toArray(BigInteger[]::new);
        Integer[] countsReal = Arrays.stream(sort).map(ea -> ea.getRight()).toArray(Integer[]::new);
        LOGGER.info("real key out: {}", Arrays.toString(keysReal));
        LOGGER.info("real count out: {}", Arrays.toString(countsReal));
        LOGGER.info("sketch key out: {}", Arrays.toString(sketchRes[0]));
        LOGGER.info("sketch g1 out: {}", Arrays.toString(sketchRes[1]));
        LOGGER.info("sketch g2 out: {}", Arrays.toString(sketchRes[2]));
        LOGGER.info("sketch delta1 out: {}", Arrays.toString(sketchRes[3]));
        LOGGER.info("sketch delta2 out: {}", Arrays.toString(sketchRes[4]));
        LOGGER.info("sketch t out: {}", Arrays.toString(sketchRes[5]));
        LOGGER.info("sketch flag out: {}", Arrays.toString(sketchRes[6]));

        // Verification: compare plain implementation output with MPC version
        GKBatchImpl plainGK = new GKBatchImpl((float) epsilon, 1 << logSketchSize);
        plainGK.input(updateElements);
        ArrayList<Representative> plainRes = plainGK.getTable();
        for (int i = 0; i < plainRes.size(); i++) {
            LOGGER.info(plainRes.get(i).toString());
            assert (plainRes.get(i).getKey().equals(sketchRes[0][i]));
            assert (plainRes.get(i).getT() == (sketchRes[5][i].longValue()));
            assert (plainRes.get(i).getG1().equals(sketchRes[1][i]));
            assert (plainRes.get(i).getG2().equals(sketchRes[2][i]));
            assert (plainRes.get(i).getDelta2().equals(sketchRes[4][i]));
        }

    }
}
