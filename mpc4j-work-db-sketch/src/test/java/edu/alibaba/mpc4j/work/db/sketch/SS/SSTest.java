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
import edu.alibaba.mpc4j.work.db.sketch.SS.v1.v1SSConfig;
import edu.alibaba.mpc4j.work.db.sketch.SS.v1.v1SSPtoDesc;
import edu.alibaba.mpc4j.work.db.sketch.utils.mg.MG;
import edu.alibaba.mpc4j.work.db.sketch.utils.mg.MGBatchImpl;
import edu.alibaba.mpc4j.work.db.sketch.utils.mg.MGNaiveImpl;
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
 * MG test.
 */
@RunWith(Parameterized.class)
public class SSTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSTest.class);
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * small sketch size
     */
    private static final int SMALL_LOG_SKETCH_SIZE = 4;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LEN = 6;
    /**
     * small payload bit length
     */
    private static final int SMALL_PAYLOAD_BIT_LEN = 6;
    /**
     * small payload bit length
     */
    private static final int SMALL_UPDATE_NUM = 1<<6;
    /**
     * top k
     */
    private static final int SMALL_TOP_K = 10;
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
    private static final int MIDDLE_PAYLOAD_BIT_LEN = 10;
    /**
     * middle update number
     */
    private static final int MIDDLE_UPDATE_NUM = 1 << 12;
    /**
     * top k
     */
    private static final int MIDDLE_TOP_K = 100;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
            v1SSPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new v1SSConfig.Builder(false).build(), false
        });
        return configurations;
    }

    /**
     * party config
     */
    private final SSConfig config;
    /**
     * verify with mac
     */
    private final boolean baseUseMac;

    public SSTest(String name, SSConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testZ2SmallSize() {
        testOpi(false, SMALL_LOG_SKETCH_SIZE, SMALL_ELEMENT_BIT_LEN, SMALL_PAYLOAD_BIT_LEN, SMALL_UPDATE_NUM, SMALL_TOP_K);
    }

    @Test
    public void testZ2MiddleSize() {
        testOpi(false, MIDDLE_LOG_SKETCH_SIZE, MIDDLE_ELEMENT_BIT_LEN, MIDDLE_PAYLOAD_BIT_LEN, MIDDLE_UPDATE_NUM, MIDDLE_TOP_K);
    }

    private void testOpi(boolean parallel, int logSketchSize, int keyBitLen, int payloadBitLen, int updateNum, int topK) {
        SSParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, (updateNum = {}) start-----", parties[0].getPtoDesc().getPtoName(), logSketchSize);
//            BigInteger[] updateData = genUpdateData(keyBitLen, updateNum);
            BigInteger[] updateData=genGaussianUpdateData(keyBitLen, updateNum);
            SSPartyThread[] threads = Arrays.stream(parties)
                .map(p -> new SSPartyThread(p, logSketchSize, keyBitLen, payloadBitLen, updateData, topK))
                .toArray(SSPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (SSPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            Pair<BigInteger[], BigInteger[]> sketchRes = threads[0].getSketchRes();
            Pair<BigInteger[], BigInteger[]> queryRes = threads[0].getQueryRes();
            // verify
            verify(updateData, sketchRes.getLeft(), sketchRes.getRight());
            LOGGER.info("query key out: {}", Arrays.toString(queryRes.getLeft()));
            LOGGER.info("query count out: {}", Arrays.toString(queryRes.getRight()));

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (updateNum = {}) end, communication:{}, time:{} ms-----",
                parties[0].getPtoDesc().getPtoName(), logSketchSize, parties[0].getRpc().getSendByteLength(), time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

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

    private void verify(BigInteger[] updateElements, BigInteger[] sketchKeys, BigInteger[] sketchCounts) {
        Map<BigInteger, Integer> updateMap = new HashMap<>();
        Map<BigInteger, BigInteger> secretMap = new HashMap<>();
        for (BigInteger updateElement : updateElements) {
            updateMap.put(updateElement, updateMap.getOrDefault(updateElement, 0) + 1);
        }
        // todo currently we only verify the result is not smaller than true result
        MG plainMG;
        switch (config.getPtoType()){
            case V1: plainMG=new MGBatchImpl(sketchKeys.length);break;
            case BK21:plainMG=new MGNaiveImpl(sketchKeys.length);break;
            default:plainMG=new MGNaiveImpl(sketchKeys.length);break;
        }
        plainMG.input(updateElements);
        Map<BigInteger,BigInteger> plainRes=plainMG.query();
        BigInteger[] keyPlain=plainRes.keySet().toArray(new BigInteger[0]);
        BigInteger[] countsPlain=plainRes.values().toArray(new BigInteger[0]);
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
        assert (secretMap.size() == plainRes.size());
        BigInteger[] plainKey = plainRes.keySet().toArray(new BigInteger[0]);
        for (int i =0; i < secretMap.size(); i++) {
            BigInteger plainCount = plainRes.get(plainKey[i]);
            BigInteger secretCount = secretMap.get(plainKey[i]);
            assert (plainCount.compareTo(secretCount) == 0):Integer.toString(i)+plainKey[i].toString();
        }
    }
}
