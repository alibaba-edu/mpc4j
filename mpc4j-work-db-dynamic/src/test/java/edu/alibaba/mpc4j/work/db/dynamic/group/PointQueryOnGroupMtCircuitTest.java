package edu.alibaba.mpc4j.work.db.dynamic.group;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.work.db.dynamic.DynamicDb2pcZ2Party;
import edu.alibaba.mpc4j.work.db.dynamic.DynamicDb3pcZ2Party;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AggregateEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * point query test case
 */
@RunWith(Parameterized.class)
public class PointQueryOnGroupMtCircuitTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PointQueryOnGroupMtCircuitTest.class);
    /**
     * secure random
     */
    private final SecureRandom secureRandom = new SecureRandom();
    /**
     * default mt num
     */
    private static final int DEFAULT_MT_NUM = 1000;
    /**
     * default query num
     */
    private static final int DEFAULT_NUM = 20;
    /**
     * large num
     */
    private static final int LARGE_NUM = 50;
    /**
     * default key dim
     */
    private static final int DEFAULT_KEY_NUM = 20;
    /**
     * large key dim
     */
    private static final int LARGE_KEY_NUM = 64;
    /**
     * default key dim
     */
    private static final int DEFAULT_PAYLOAD_NUM = 64;
    /**
     * computational party
     */
    private MpcZ2cParty[] z2cParties;
    /**
     * input data
     */
    private BitVector[] input;
    /**
     * update data
     */
    private BitVector[][] keys;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            "default",
            new Z2CircuitConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * the config
     */
    private final Z2CircuitConfig config;

    public PointQueryOnGroupMtCircuitTest(String name, Z2CircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void test1Num1pc() {
        testRandom1pc(DEFAULT_KEY_NUM, 1);
    }

    @Test
    public void test2Num1pc() {
        testRandom1pc(DEFAULT_KEY_NUM, 2);
    }

    @Test
    public void test8Num1pc() {
        testRandom1pc(DEFAULT_KEY_NUM, 8);
    }

    @Test
    public void testDefaultNum1pc() {
        testRandom1pc(LARGE_KEY_NUM, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum1pc() {
        testRandom1pc(LARGE_KEY_NUM, LARGE_NUM);
    }

    @Test
    public void test1Num2pc() {
        testRandom2pc(DEFAULT_KEY_NUM, 1);
    }

    @Test
    public void test2Num2pc() {
        testRandom2pc(DEFAULT_KEY_NUM, 2);
    }

    @Test
    public void test8Num2pc() {
        testRandom2pc(DEFAULT_KEY_NUM, 8);
    }

    @Test
    public void testDefaultNum2pc() {
        testRandom2pc(LARGE_KEY_NUM, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum2pc() {
        testRandom2pc(LARGE_KEY_NUM, LARGE_NUM);
    }

    @Test
    public void test1Num3pc() {
        testRandom3pc(DEFAULT_KEY_NUM, 1);
    }

    @Test
    public void test2Num3pc() {
        testRandom3pc(DEFAULT_KEY_NUM, 2);
    }

    @Test
    public void test8Num3pc() {
        testRandom3pc(DEFAULT_KEY_NUM, 8);
    }

    @Test
    public void testDefaultNum3pc() {
        testRandom3pc(LARGE_KEY_NUM, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum3pc() {
        testRandom3pc(LARGE_KEY_NUM, LARGE_NUM);
    }

    private void testRandom1pc(int keyDim, int num) {
        z2cParties = new MpcZ2cParty[]{new PlainZ2cParty()};
        testPto(keyDim, DEFAULT_PAYLOAD_NUM, DEFAULT_MT_NUM, num);
    }

    private void testRandom2pc(int keyDim, int num) {
        DynamicDb2pcZ2Party dynamicDb2pcZ2Party = new DynamicDb2pcZ2Party("test");
        z2cParties = dynamicDb2pcZ2Party.genParties(true);
        testPto(keyDim, DEFAULT_PAYLOAD_NUM, DEFAULT_MT_NUM, num);
    }

    private void testRandom3pc(int keyDim, int num) {
        DynamicDb3pcZ2Party dynamicDb3pcZ2Party = new DynamicDb3pcZ2Party("test");
        z2cParties = dynamicDb3pcZ2Party.genParties(true);
        testPto(keyDim, DEFAULT_PAYLOAD_NUM, DEFAULT_MT_NUM, num);
    }

    private void testPto(int keyDim, int payloadDim, int mtSize, int queryNum) {
        LOGGER.info("test (group point query), keyDim = {}, payloadDim = {}, mtSize = {}, queryNum = {}", keyDim, payloadDim, mtSize, queryNum);
        try {
            GroupByMt[] groupByMts = createGroupByMt(keyDim, payloadDim, mtSize);
            MpcZ2Vector[][][] queryKeys = createQueryKey(queryNum, keyDim, mtSize);
            PointQueryOnGroupMtCircuitPartyThread[] threads = IntStream.range(0, z2cParties.length)
                .mapToObj(p -> new PointQueryOnGroupMtCircuitPartyThread(config, z2cParties[p], groupByMts[p], queryKeys[p]))
                .toArray(PointQueryOnGroupMtCircuitPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (PointQueryOnGroupMtCircuitPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            assertOutput(threads[0].getPlainResult(), keyDim, payloadDim, mtSize);
            // destroy
            LOGGER.info("test (group point query), keyDim = {}, payloadDim = {}, mtSize = {} queryNum = {} end, time= {}ms",
                keyDim, payloadDim, mtSize, queryNum, time);
        } catch (InterruptedException | MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }

    private GroupByMt[] createGroupByMt(int keyDim, int payloadDim, int mtSize) {
        input = new BitVector[keyDim + payloadDim + 1];
        int logLen = LongUtils.ceilLog2(mtSize);
        MathPreconditions.checkGreaterOrEqual("keyDim >= logLen", keyDim, logLen);
        for (int i = 0; i < keyDim - logLen; i++) {
            input[i] = BitVectorFactory.createZeros(mtSize);
        }
        BitVector[] indexes = Z2VectorUtils.getBinaryIndex(mtSize);
        System.arraycopy(indexes, 0, input, keyDim - logLen, indexes.length);
        for (int i = 0; i < payloadDim + 1; i++) {
            input[i + keyDim] = BitVectorFactory.createRandom(mtSize, secureRandom);
        }
        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(input))
            .map(s -> new GroupByMt(s, keyDim + payloadDim, false, IntStream.range(0, keyDim).toArray(), AggregateEnum.SUM))
            .toArray(GroupByMt[]::new);
    }

    private MpcZ2Vector[][][] createQueryKey(int num, int keyDim, int mtSize) {
        keys = new BitVector[num][];
        int logLen = LongUtils.ceilLog2(mtSize);
        MpcZ2Vector[][][] queryKeys = new MpcZ2Vector[z2cParties.length][num][];
        for (int i = 0; i < num; i++) {
            keys[i] = new BitVector[keyDim];
            for (int j = 0; j < keyDim - logLen; j++) {
                keys[i][j] = BitVectorFactory.createZeros(1);
            }
            for (int j = keyDim - logLen; j < keyDim; j++) {
                keys[i][j] = BitVectorFactory.createRandom(1, secureRandom);
            }
            int finalI = i;
            MpcZ2Vector[][] tmp = Arrays.stream(z2cParties)
                .map(party -> party.setPublicValues(keys[finalI]))
                .toArray(MpcZ2Vector[][]::new);
            for (int j = 0; j < z2cParties.length; j++) {
                queryKeys[j][i] = tmp[j];
            }
        }
        return queryKeys;
    }

    private void assertOutput(BitVector[][] result, int keyDim, int payloadDim, int mtSize) {
        MathPreconditions.checkEqual("result.length", "keys.length", result.length, keys.length);
        BitVector[] payload = Arrays.stream(input, keyDim, keyDim + payloadDim).toArray(BitVector[]::new);
        BigInteger[] BigPayload = ZlDatabase.create(EnvType.STANDARD, true, payload).getBigIntegerData();
        for (int i = 0; i < result.length; i++) {
            MathPreconditions.checkEqual("result[i].length", "payloadDim + 1", result[i].length, payloadDim + 1);
            int index = columnBitToBigInteger(keys[i]).intValue();
            BigInteger actualPayload = columnBitToBigInteger(result[i]);
            if (index < mtSize && input[input.length - 1].get(index)) {
                Assert.assertEquals(BigPayload[index].shiftLeft(1).add(BigInteger.ONE), actualPayload);
            } else {
                Assert.assertEquals(BigInteger.ZERO, actualPayload);
            }
        }
    }

    private BigInteger columnBitToBigInteger(BitVector[] data) {
        BigInteger a = BigInteger.ZERO;
        for (BitVector bitVector : data) {
            a = a.shiftLeft(1).add(bitVector.getBigInteger());
        }
        return a;
    }
}
