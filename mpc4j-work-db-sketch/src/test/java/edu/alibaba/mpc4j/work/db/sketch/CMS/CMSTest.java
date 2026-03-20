package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
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
import edu.alibaba.mpc4j.work.db.sketch.CMS.z2.CMSz2Config;
import edu.alibaba.mpc4j.work.db.sketch.CMS.z2.CMSz2PtoDesc;
import edu.alibaba.mpc4j.work.db.sketch.utils.cms.CMS;
import edu.alibaba.mpc4j.work.db.sketch.utils.cms.CMSv1BatchImpl;
import edu.alibaba.mpc4j.work.db.sketch.utils.cms.CMSv2BatchImpl;
import org.apache.commons.lang3.time.StopWatch;
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
 * Test class for CMS (Count-Min Sketch) protocol in 3PC setting.
 * Tests both small and medium scale inputs with correctness verification.
 */
@RunWith(Parameterized.class)
public class CMSTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(CMSTest.class);
    /**
     * Flag to use simulated MTP (Multiplication Triples) for testing
     */
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
     * Bit length for small payload (count values)
     */
    private static final int SMALL_PAYLOAD_BIT_LEN = 6;
    /**
     * Number of updates for small test
     */
    private static final int SMALL_UPDATE_NUM = 1 << 4;
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
    private static final int MIDDLE_PAYLOAD_BIT_LEN = 10;
    /**
     * Number of updates for medium test
     */
    private static final int MIDDLE_UPDATE_NUM = 1 << 12;
    /**
     * Number of queries for medium test
     */
    private static final int MIDDLE_QUERY_NUM = 100;

    /**
     * Parameterized test configurations
     * @return collection of test configurations
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
            CMSz2PtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new CMSz2Config.Builder(false).build(), false
        });
        return configurations;
    }
    /**
     * Configuration for CMS protocol
     */
    private final CMSConfig config;
    /**
     * Flag to enable MAC verification
     */
    private final boolean baseUseMac;

    /**
     * Constructs a CMS test with the specified configuration
     * @param name test name
     * @param config CMS configuration
     * @param baseUseMac whether to use MAC for verification
     */
    public CMSTest(String name, CMSConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    /**
     * Test with small input size
     */
    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_LOG_SKETCH_SIZE, SMALL_ELEMENT_BIT_LEN, SMALL_PAYLOAD_BIT_LEN, SMALL_UPDATE_NUM, SMALL_QUERY_NUM);
    }

    /**
     * Test with medium input size
     */
    @Test
    public void testMiddleSize() {
        testOpi(false, MIDDLE_LOG_SKETCH_SIZE, MIDDLE_ELEMENT_BIT_LEN, MIDDLE_PAYLOAD_BIT_LEN, MIDDLE_UPDATE_NUM, MIDDLE_QUERY_NUM);
    }

    /**
     * Creates and initializes CMS parties for the test
     * @param parallel whether to enable parallel execution
     * @return array of three CMS parties
     */
    private CMSParty[] getParties(boolean parallel) {
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

        CMSParty[] parties = Arrays.stream(abb3Parties).map(each ->
                CMSFactory.createParty(each, config)).toArray(CMSParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    /**
     * Main test method for CMS protocol
     * @param parallel whether to enable parallel execution
     * @param logSketchSize log of sketch table size
     * @param elementBitLen bit length of elements
     * @param payloadBitLen bit length of payload
     * @param smallUpdateNum number of updates
     * @param smallQueryNum number of queries
     */
    private void testOpi(boolean parallel, int logSketchSize, int elementBitLen, int payloadBitLen, int smallUpdateNum, int smallQueryNum) {
        CMSParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, logSketchSize = {}, elementBitLen = {}, payloadBitLen = {} start-----",
                parties[0].getPtoDesc().getPtoName(), logSketchSize, elementBitLen, payloadBitLen);
            // Generate random update and query keys
            BigInteger[] updateKeys = genUpdateData(elementBitLen, smallUpdateNum);
            BigInteger[] queryKeys = genUpdateData(elementBitLen, smallQueryNum);
            // Create party threads
            CMSPartyThread[] threads = Arrays.stream(parties)
                .map(p -> new CMSPartyThread(p, updateKeys, queryKeys, elementBitLen, logSketchSize, payloadBitLen))
                .toArray(CMSPartyThread[]::new);

            // Execute the protocol and measure time
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (CMSPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            
            // Get sketch result and verify
            long[] sketchRes = threads[0].getSketchRes();
            long sum = Arrays.stream(sketchRes).sum();
            LOGGER.info("sketch out: {}", Arrays.toString(sketchRes));
            LOGGER.info("sketch sum: {}", sum);
            
            // Create plain CMS implementation for verification
            CMS plainCms;

            switch (config.getPtoType()) {
                case CMS_Z2 -> {
                    PlainZ2Vector encKey = threads[0].getHashKey();
                    plainCms = new CMSv2BatchImpl(1, sketchRes.length, new PlainZ2Vector[]{encKey}, elementBitLen);
                }
                default -> {
                    HashParameters para = threads[0].getHashParameters();
                    BigInteger[][] hashParameters = new BigInteger[2][1];
                    hashParameters[0][0] = BigInteger.valueOf(para.getA());
                    hashParameters[1][0] = BigInteger.valueOf(para.getB());
                    plainCms = new CMSv1BatchImpl(1, sketchRes.length, hashParameters, elementBitLen);
                }
            }

            // Verify sketch correctness
            plainCms.input(updateKeys);
            int[][] plainRes = plainCms.getTable();
            LOGGER.info("plain out: {}", Arrays.toString(plainRes[0]));

            for (int i = 0; i < sketchRes.length; i++) {
                assert (plainRes[0][i] == sketchRes[i]);
            }

            // Verify sum correctness
            assert updateKeys.length - (updateKeys.length % (1L << logSketchSize)) == sum;
            
            // Verify query results
            long[] queryRes = threads[0].getQueryRes();
            verifyQuery(updateKeys, queryKeys, queryRes);

            // Cleanup
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {} end, communication: {}B, time: {}ms-----",
                parties[0].getPtoDesc().getPtoName(), parties[0].getRpc().getSendByteLength(), time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
     * Verifies query results against expected values
     * @param updateKeys array of update keys
     * @param queryKeys array of query keys
     * @param queryRes query results from the sketch
     */
    private void verifyQuery(BigInteger[] updateKeys, BigInteger[] queryKeys, long[] queryRes) {
        Map<BigInteger, Long> updateMap = new HashMap<>();
        for (BigInteger updateKey : updateKeys) {
            updateMap.put(updateKey, updateMap.getOrDefault(updateKey, 0L) + 1);
        }
        long[] real = Arrays.stream(queryKeys).mapToLong(ea -> updateMap.getOrDefault(ea, 0L)).toArray();
        // CMS may overestimate, so we verify result is not smaller than true result
        LOGGER.info("real out: {}", Arrays.toString(real));
        LOGGER.info("actual out: {}", Arrays.toString(queryRes));
        for (int i = 0; i < real.length; i++) {
            assert queryRes[i] >= real[i];
        }
    }

}
