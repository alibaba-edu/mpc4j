package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvRes;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvPtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongCpFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
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
 * type conversion circuit test.
 *
 * @author Feng Han
 * @date 2024/02/06
 */
@RunWith(Parameterized.class)
public class Aby3ConvTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aby3ConvTest.class);
    public static final ConvOp[] opAll = new ConvOp[]{
        ConvOp.B2A,
        ConvOp.BIT2A,
        ConvOp.A2B,
        ConvOp.BIT_EXTRACTION,
        ConvOp.A_MUL_B
    };

    private static final boolean USE_MT_TEST_MODE = false;
    private static final String TUPLE_DIR = "./";

    private static final int BATCH_NUM = 2;

    private static final int SMALL_SIZE = 1 << 2;

    private static final int MIDDLE_SIZE = 1 << 10;

    private static final int LARGE_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Aby3ConvPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Aby3ConvConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Aby3ConvPtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new Aby3ConvConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Aby3ConvPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Aby3ConvConfig.Builder(true).build(), true
        });

        return configurations;
    }

    private final Aby3ConvConfig config;
    private final boolean baseUseMac;

    public Aby3ConvTest(String name, Aby3ConvConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testAllSmallSize() {
        testOpi(false, opAll, SMALL_SIZE, BATCH_NUM);
    }

    @Test
    public void testEachSmallSize() {
        for (ConvOp op : opAll) {
            ConvOp[] single = new ConvOp[]{op};
            testOpi(false, single, SMALL_SIZE, BATCH_NUM);
        }
    }

    @Test
    public void testAllMiddleSize() {
        testOpi(true, opAll, MIDDLE_SIZE, BATCH_NUM);
    }

    @Test
    public void testEachMiddleSize() {
        for (ConvOp op : opAll) {
            ConvOp[] single = new ConvOp[]{op};
            testOpi(true, single, MIDDLE_SIZE, BATCH_NUM);
        }
    }

    @Test
    public void testAllLargeSize() {
        testOpi(true, opAll, LARGE_SIZE, BATCH_NUM);
    }
    @Test
    public void testEachLargeSize() {
        for(ConvOp op : opAll){
            ConvOp[] single = new ConvOp[]{op};
            testOpi(true, single, LARGE_SIZE, BATCH_NUM);
        }
    }


    private Aby3ConvParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);
        TripletProviderConfig providerConfig;
        if (isMalicious && USE_MT_TEST_MODE) {
            providerConfig = new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode(TUPLE_DIR))
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode(TUPLE_DIR))
                .build();
        } else {
            providerConfig = new TripletProviderConfig.Builder(isMalicious).build();
        }

        TripletProvider[] tripletProviders = IntStream.range(0, 3).mapToObj(i ->
            new TripletProvider(rpcAll[i], providerConfig)).toArray(TripletProvider[]::new);

        TripletZ2cParty[] bcParties = IntStream.range(0, 3).mapToObj(i ->
                Aby3Z2cFactory.createParty(rpcAll[i], new Aby3Z2cConfig.Builder(isMalicious).build(), tripletProviders[i]))
            .toArray(TripletZ2cParty[]::new);

        TripletRpLongConfig tripletRpZl64cConfig = baseUseMac
            ? new Cgh18RpLongConfig.Builder().build()
            : new Aby3LongConfig.Builder(isMalicious).build();
        TripletLongParty[] acParties = IntStream.range(0, 3).mapToObj(i ->
            TripletRpLongCpFactory.createParty(rpcAll[i], tripletRpZl64cConfig, tripletProviders[i])).toArray(TripletLongParty[]::new);

        Aby3ConvParty[] convParties = IntStream.range(0, 3).mapToObj(i ->
            Aby3ConvFactory.createParty(config, bcParties[i], acParties[i])).toArray(Aby3ConvParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(convParties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return convParties;
    }

    private void verifyRes(ConvOp op, ConvRes[] data) {
        LOGGER.info("verifying " + op.name());
        switch (op){
            case BIT_EXTRACTION:{
                for(ConvRes oneRes : data){
                    LOGGER.info("verifying bitIndex:{}", oneRes.bitLen);
                    for(int i = 0; i < oneRes.aValues.length; i++){
                        Assert.assertEquals(oneRes.bValues[i][0].bitNum(), oneRes.aValues[i].getNum());
                        boolean[] compRes = BinaryUtils.byteArrayToBinary(oneRes.bValues[i][0].getBytes(), oneRes.bValues[i][0].bitNum());
                        long[] originData = oneRes.aValues[i].getElements();
                        if(oneRes.bitLen == 0){
                            for(int j = 0; j < originData.length; j++){
                                Assert.assertEquals(compRes[j], originData[j] < 0);
                            }
                        }else{
                            long andNum = 1L<<(63 - oneRes.bitLen);
                            boolean[] tureRes = new boolean[originData.length];
                            for(int j = 0; j < originData.length; j++){
                                tureRes[j] = (originData[j] & andNum) != 0;
                            }
                            Assert.assertArrayEquals(tureRes, compRes);
                        }
                    }
                }
                break;
            }
            case A2B:{
                for(ConvRes oneRes : data){
                    for(int i = 0; i < oneRes.aValues.length; i++){
                        long[] originData = oneRes.aValues[i].getElements();
                        BigInteger[] res = ZlDatabase.create(EnvType.STANDARD, true, oneRes.bValues[i]).getBigIntegerData();
                        long[] resLong = Arrays.stream(res).mapToLong(BigInteger::longValue).toArray();
                        if(oneRes.bitLen == 64){
                            Assert.assertArrayEquals(originData, resLong);
                        }else{
                            long andMask = (1L<<oneRes.bitLen) - 1;
                            for(int j = 0; j < originData.length; j++){
                                Assert.assertEquals(resLong[j], originData[j] & andMask);
                            }
                        }
                    }
                }
                break;
            }
            case BIT2A:{
                for(int i = 0; i < data[0].aValues.length; i++){
                    Assert.assertEquals(data[0].aValues[i].getNum(), data[0].bValues[i][0].bitNum());
                    boolean[] flag = BinaryUtils.byteArrayToBinary(data[0].bValues[i][0].getBytes(), data[0].bValues[i][0].bitNum());
                    long[] compRes = data[0].aValues[i].getElements();
                    for(int j = 0; j < data[0].aValues[i].getNum(); j++){
                        Assert.assertEquals(flag[j] ? 1L : 0L, compRes[j]);
                    }
                }
                break;
            }
            case A_MUL_B:{
                for(int i = 0; i < data[0].aValues.length; i++){
                    Assert.assertEquals(data[0].aValues[i].getNum(), data[0].bValues[i][0].bitNum());
                    Assert.assertEquals(data[0].aValues[i].getNum(), data[0].mulRes[i].getNum());
                    boolean[] flag = BinaryUtils.byteArrayToBinary(data[0].bValues[i][0].getBytes(), data[0].bValues[i][0].bitNum());
                    long[] originalValue = data[0].aValues[i].getElements();
                    long[] res = data[0].mulRes[i].getElements();
                    for(int j = 0; j < data[0].aValues[i].getNum(); j++){
                        Assert.assertEquals(res[j], flag[j] ? originalValue[j] : 0L);
                    }
                }
                break;
            }
            case B2A:{
                for(ConvRes oneRes : data){
                    for(int i = 0; i < oneRes.aValues.length; i++){
                        Assert.assertEquals(oneRes.aValues[i].getNum(), oneRes.bValues[i][0].bitNum());
                        BigInteger[] originBig = ZlDatabase.create(EnvType.STANDARD, true, oneRes.bValues[i]).getBigIntegerData();
                        long[] originLong = Arrays.stream(originBig).mapToLong(BigInteger::longValue).toArray();
                        long[] compRes = oneRes.aValues[i].getElements();
                        Assert.assertArrayEquals(originLong, compRes);
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("error ConvOp: " + op.name());
        }
    }

    private void testOpi(boolean parallel, ConvOp[] ops, int dataNum, int dataDim) {
        Aby3ConvParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, (dataNum = {}, dataDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), dataNum, dataDim);
            Aby3ConvPartyThread[] threads = Arrays.stream(parties).map(p ->
                new Aby3ConvPartyThread(p, dataNum, dataDim, ops)).toArray(Aby3ConvPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (Aby3ConvPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            for (ConvOp op : ops) {
                ConvRes[] res = threads[0].getConvRes(op);
                verifyRes(op, res);
            }
            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (dataNum = {}, dataDim = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                dataNum, dataDim, time);
            LOGGER.info("op:[{}] test pass", Arrays.toString(ops));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
