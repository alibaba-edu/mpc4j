package edu.alibaba.mpc4j.work.db.dynamic;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.DynamicDbPkPkJoinCircuitFactory.DynamicDbPkPkJoinCircuitType;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.JoinInputMt;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.PkPkJoinMt;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;
import edu.alibaba.mpc4j.work.db.dynamic.structure.OperationEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * test cases for dynamic db pk-pk join.
 *
 * @author Feng Han
 * @date 2025/3/11
 */
@RunWith(Parameterized.class)
public class DynamicDbPkPkJoinTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDbPkPkJoinTest.class);
    /**
     * operations
     */
    private static final OperationEnum[] OPERATION_ENUMS = new OperationEnum[]{
        OperationEnum.DELETE,
        OperationEnum.INSERT
    };
    /**
     * secure random
     */
    private final SecureRandom secureRandom = new SecureRandom();
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 100;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 12;
    /**
     * default total dimension
     */
    private static final int DEFAULT_DIM = 20;
    /**
     * large total dimension
     */
    private static final int LARGE_DIM = 64;
    /**
     * computational party
     */
    private MpcZ2cParty[] z2cParties;
    /**
     * left input table
     */
    private BitVector[] leftTabInput;
    /**
     * join_id -> payload map for left input table
     */
    private TIntObjectMap<BigInteger> leftMap;
    /**
     * right input table
     */
    private BitVector[] rightTabInput;
    /**
     * join_id -> payload map for right input table
     */
    private TIntObjectMap<BigInteger> rightMap;
    /**
     * left input table
     */
    private BitVector[] joinTabInput;
    /**
     * update data
     */
    private BitVector[] update;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            DynamicDbPkPkJoinCircuitType.ZGC24.name(),
            new DynamicDbCircuitConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * the config
     */
    private final DynamicDbCircuitConfig config;

    public DynamicDbPkPkJoinTest(String name, DynamicDbCircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void test1Num1pc() {
        testRandom1pc(DEFAULT_DIM, DEFAULT_DIM, 1);
    }

    @Test
    public void test2Num1pc() {
        testRandom1pc(DEFAULT_DIM, DEFAULT_DIM, 2);
    }

    @Test
    public void test8Num1pc() {
        testRandom1pc(DEFAULT_DIM, DEFAULT_DIM, 8);
    }

    @Test
    public void testDefaultNum1pcDL() {
        testRandom1pc(DEFAULT_DIM, LARGE_DIM, DEFAULT_NUM);
    }

    @Test
    public void testDefaultNum1pcLD() {
        testRandom1pc(LARGE_DIM, DEFAULT_DIM, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum1pcDL() {
        for (int i = 0; i < 100; i++) {
            testRandom1pc(DEFAULT_DIM, LARGE_DIM, LARGE_NUM);
        }
    }

    @Test
    public void testLargeNum1pcLD() {
        for (int i = 0; i < 100; i++) {
            testRandom1pc(LARGE_DIM, DEFAULT_DIM, LARGE_NUM);
        }
    }

    @Test
    public void test1Num2pc() {
        testRandom2pc(DEFAULT_DIM, DEFAULT_DIM, 1);
    }

    @Test
    public void test2Num2pc() {
        testRandom2pc(DEFAULT_DIM, DEFAULT_DIM, 2);
    }

    @Test
    public void test8Num2pc() {
        testRandom2pc(DEFAULT_DIM, DEFAULT_DIM, 8);
    }

    @Test
    public void testDefaultNum2pcDL() {
        testRandom2pc(DEFAULT_DIM, LARGE_DIM, DEFAULT_NUM);
    }

    @Test
    public void testDefaultNum2pcLD() {
        testRandom2pc(LARGE_DIM, DEFAULT_DIM, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum2pcDL() {
        testRandom2pc(DEFAULT_DIM, LARGE_DIM, LARGE_NUM);
    }

    @Test
    public void testLargeNum2pcLD() {
        testRandom2pc(LARGE_DIM, DEFAULT_DIM, LARGE_NUM);
    }

    @Test
    public void testLargeNum3pcLD() {
        testRandom3pc(LARGE_DIM, DEFAULT_DIM, LARGE_NUM);
    }

    private void testRandom1pc(int leftPayloadDim, int rightPayloadDim, int num) {
        for (OperationEnum op : OPERATION_ENUMS) {
            for (boolean updateLeft : new boolean[]{true, false}) {
                z2cParties = new MpcZ2cParty[]{new PlainZ2cParty()};
                testPto(leftPayloadDim, rightPayloadDim, num, op, updateLeft);
            }
        }
    }

    private void testRandom2pc(int leftPayloadDim, int rightPayloadDim, int num) {
        for (OperationEnum op : OPERATION_ENUMS) {
            for (boolean updateLeft : new boolean[]{true, false}) {
                DynamicDb2pcZ2Party dynamicDb2pcZ2Party = new DynamicDb2pcZ2Party("test");
                z2cParties = dynamicDb2pcZ2Party.genParties(true);
                testPto(leftPayloadDim, rightPayloadDim, num, op, updateLeft);
            }
        }
    }

    private void testRandom3pc(int leftPayloadDim, int rightPayloadDim, int num) {
        for (OperationEnum op : OPERATION_ENUMS) {
            for (boolean updateLeft : new boolean[]{true, false}) {
                DynamicDb3pcZ2Party dynamicDb3pcZ2Party = new DynamicDb3pcZ2Party("test");
                z2cParties = dynamicDb3pcZ2Party.genParties(true);
                testPto(leftPayloadDim, rightPayloadDim, num, op, updateLeft);
            }
        }
    }

    private void testPto(int leftPayloadDim, int rightPayloadDim, int num, OperationEnum operation, boolean updateLeft) {
        LOGGER.info("test ({}), leftPayloadDim = {}, rightPayloadDim = {} num = {}, op = {}, updateLeft = {}",
            MaterializedTableType.PK_PK_JOIN_MT, leftPayloadDim, rightPayloadDim, num, operation.name(), updateLeft);
        try {
            PkPkJoinMt[] pkPkJoinMts = createPkPkJoinMt(num, leftPayloadDim, rightPayloadDim, false);
            UpdateMessage[] updateMessages = createUpdateMsg(num, updateLeft ? leftPayloadDim : rightPayloadDim, operation, updateLeft);
            DynamicDbCircuitPartyThread[] threads = IntStream.range(0, z2cParties.length)
                .mapToObj(p -> new DynamicDbCircuitPartyThread(config, z2cParties[p], updateMessages[p], pkPkJoinMts[p]))
                .toArray(DynamicDbCircuitPartyThread[]::new);
            for (DynamicDbCircuitPartyThread t : threads) {
                t.setUpdateFromLeft(updateLeft);
            }

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (DynamicDbCircuitPartyThread t : threads) {
                t.setUpdateFromLeft(updateLeft);
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            assertOutput(num, leftPayloadDim, rightPayloadDim, threads[0].getPlainUpdateResult(), pkPkJoinMts[0], operation, updateLeft);
            // destroy
            LOGGER.info("test ({}), leftPayloadDim = {}, rightPayloadDim = {} num = {}, op = {}, updateLeft = {} end, time = {}ms",
                MaterializedTableType.PK_PK_JOIN_MT, leftPayloadDim, rightPayloadDim, num, operation.name(), updateLeft, time);
        } catch (InterruptedException | MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }

    private PkPkJoinMt[] createPkPkJoinMt(int num, int leftPayloadDim, int rightPayloadDim, boolean isOutput) {
        int idLen = LongUtils.ceilLog2(num) + 1;
        int byteLen = CommonUtils.getByteLength(idLen + leftPayloadDim + rightPayloadDim);
        leftMap = new TIntObjectHashMap<>();
        rightMap = new TIntObjectHashMap<>();
        leftTabInput = genTabData(num, leftPayloadDim, leftMap);
        rightTabInput = genTabData(num, rightPayloadDim, rightMap);
        List<BigInteger> joinRes = new LinkedList<>();
        for (int leftKey : leftMap.keys()) {
            if (rightMap.containsKey(leftKey)) {
                BigInteger oneRow = BigInteger.valueOf(leftKey);
                oneRow = oneRow.shiftLeft(leftPayloadDim).add(leftMap.get(leftKey));
                oneRow = oneRow.shiftLeft(rightPayloadDim).add(rightMap.get(leftKey));
                joinRes.add(oneRow);
            }
        }
        joinTabInput = new BitVector[idLen + leftPayloadDim + rightPayloadDim + 1];
        if (joinRes.isEmpty()) {
            for (int i = 0; i < idLen + leftPayloadDim + rightPayloadDim + 1; i++) {
                joinTabInput[i] = BitVectorFactory.createRandom(num, secureRandom);
            }
            joinTabInput[joinTabInput.length - 1] = BitVectorFactory.createZeros(num);
        } else {
            byte[][] idAndValues = joinRes.stream().map(ea -> BigIntegerUtils.nonNegBigIntegerToByteArray(ea, byteLen)).toArray(byte[][]::new);
            ZlDatabase zlDatabase = ZlDatabase.create(idLen + leftPayloadDim + rightPayloadDim, idAndValues);
            System.arraycopy(zlDatabase.bitPartition(EnvType.STANDARD, true), 0, joinTabInput, 0, idLen + leftPayloadDim + rightPayloadDim);
            joinTabInput[joinTabInput.length - 1] = BitVectorFactory.createOnes(joinRes.size());
        }

        return Arrays.stream(z2cParties).map(z2cParty -> {
                MpcZ2Vector[] leftShare = z2cParty.setPublicValues(leftTabInput);
                MpcZ2Vector[] rightShare = z2cParty.setPublicValues(rightTabInput);
                MpcZ2Vector[] joinShare = z2cParty.setPublicValues(joinTabInput);
                return new PkPkJoinMt(joinShare, joinShare.length - 1, isOutput,
                    IntStream.range(0, idLen).toArray(),
                    IntStream.range(idLen, idLen + leftPayloadDim).toArray(),
                    IntStream.range(idLen + leftPayloadDim, idLen + leftPayloadDim + rightPayloadDim).toArray(),
                    new JoinInputMt(leftShare, leftTabInput.length - 1, IntStream.range(0, idLen).toArray()),
                    new JoinInputMt(rightShare, rightTabInput.length - 1, IntStream.range(0, idLen).toArray())
                );
            })
            .toArray(PkPkJoinMt[]::new);
    }

    private BitVector[] genTabData(int num, int payloadDim, TIntObjectMap<BigInteger> idToValueMap) {
        int idLen = LongUtils.ceilLog2(num) + 1;
        BitVector[] input = new BitVector[idLen + payloadDim + 1];
        input[0] = BitVectorFactory.createZeros(num);
        BitVector[] indexes = Z2VectorUtils.getBinaryIndex(num);
        System.arraycopy(indexes, 0, input, 1, indexes.length);
        for (int i = 0; i < payloadDim + 1; i++) {
            input[i + idLen] = BitVectorFactory.createRandom(num, secureRandom);
        }
        BigInteger[] values = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOfRange(input, idLen, input.length - 1)).getBigIntegerData();
        for (int i = 0; i < num; i++) {
            if (input[payloadDim + idLen].get(i)) {
                idToValueMap.put(i, values[i]);
            }
        }

        return input;
    }

    private UpdateMessage[] createUpdateMsg(int num, int payloadDim, OperationEnum operation, boolean fromLeft) {
        int idLen = LongUtils.ceilLog2(num) + 1;
        update = new BitVector[idLen + payloadDim + 1];

        BitVector[] originalData = fromLeft ? leftTabInput : rightTabInput;
        int randInd = secureRandom.nextInt(num);
        if (operation.equals(OperationEnum.DELETE)) {
            for (int i = 0; i < update.length; i++) {
                update[i] = originalData[i].get(randInd) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1);
            }
        } else {
            // 插入一个
            TIntObjectMap<BigInteger> map = fromLeft ? leftMap : rightMap;
            if (map.size() < num) {
                // 如果存在非有效的input，可以直接插入这个数据，达到插入有效行的效果
                while (map.size() < num && map.containsKey(randInd)) {
                    randInd = secureRandom.nextInt(num);
                }
                for (int i = 0; i < update.length - 1; i++) {
                    update[i] = originalData[i].get(randInd) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1);
                }
                update[update.length - 1] = BitVectorFactory.createOnes(1);
            } else {
                // 如果原本的input都是有效的，则插入一个不可能影响join结果的数据
                update[0] = BitVectorFactory.createOnes(1);
                for (int i = 1; i < update.length; i++) {
                    update[i] = BitVectorFactory.createRandom(1, secureRandom);
                }
            }
        }

        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(update))
            .map(s -> new UpdateMessage(operation, s))
            .toArray(UpdateMessage[]::new);
    }

    private void assertOutput(int num, int leftPayloadDim, int rightPayloadDim, List<UpdateMessage> result, PkPkJoinMt afterUpdateMt, OperationEnum operation, boolean updateLeft) {
        int idLen = LongUtils.ceilLog2(num) + 1;
        // 原则是保证有效数据都没有变化，删除和插入成功，不验证其他dummy的数据
        // 先不验证对Join输入两个表的影响

        TIntObjectMap<BigInteger[]> originalJoinRes = new TIntObjectHashMap<>();
        for (int leftKey : leftMap.keys()) {
            if (rightMap.containsKey(leftKey)) {
                originalJoinRes.put(leftKey, new BigInteger[]{leftMap.get(leftKey), rightMap.get(leftKey)});
            }
        }

        BitVector[] afterUpData = Arrays.stream(afterUpdateMt.getData()).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        boolean[] AfterFlag = BinaryUtils.byteArrayToBinary(afterUpData[afterUpData.length - 1].getBytes(),
            afterUpData[afterUpData.length - 1].bitNum());
        BigInteger[] afterUpIdData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(afterUpData, idLen)).getBigIntegerData();
        BigInteger[] afterUpLeftValueData = ZlDatabase.create(EnvType.STANDARD, true,
            Arrays.copyOfRange(afterUpData, idLen, idLen + leftPayloadDim)).getBigIntegerData();
        BigInteger[] afterUpRightValueData = ZlDatabase.create(EnvType.STANDARD, true,
            Arrays.copyOfRange(afterUpData, idLen + leftPayloadDim, afterUpData.length - 1)).getBigIntegerData();
        TIntObjectMap<BigInteger[]> afterJoinRes = new TIntObjectHashMap<>();
        for (int i = 0; i < afterUpIdData.length; i++) {
            if (AfterFlag[i]) {
                afterJoinRes.put(afterUpIdData[i].intValue(), new BigInteger[]{afterUpLeftValueData[i], afterUpRightValueData[i]});
            }
        }

        Assert.assertTrue(Math.abs(originalJoinRes.size() - afterJoinRes.size()) <= 1);

        boolean updateValid = update[update.length - 1].get(0);
        int upId = columnBitToBigInteger(Arrays.copyOf(update, idLen)).intValue();
        BigInteger upValue = columnBitToBigInteger(Arrays.copyOfRange(update, idLen, update.length - 1));
        // 明文更新表格
        TIntObjectMap<BigInteger> otherMap = updateLeft ? rightMap : leftMap;
        if (updateValid && otherMap.containsKey(upId)) {
            if (operation.equals(OperationEnum.INSERT)) {
                Assert.assertFalse(originalJoinRes.containsKey(upId));
                originalJoinRes.put(upId, new BigInteger[]{
                    updateLeft ? upValue : leftMap.get(upId),
                    updateLeft ? rightMap.get(upId) : upValue});

            } else {
                originalJoinRes.remove(upId);
            }
        }
        for (int joinKey : originalJoinRes.keys()) {
            Assert.assertArrayEquals(originalJoinRes.get(joinKey), afterJoinRes.get(joinKey));
        }

        if (!afterUpdateMt.isOutputTable()) {
            Assert.assertEquals(1, result.size());
            BitVector[] resData = Arrays.stream(result.get(0).getRowData()).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
            boolean resValid = resData[resData.length - 1].get(0);
            int resId = columnBitToBigInteger(Arrays.copyOf(resData, idLen)).intValue();
            BigInteger resLeftValue = columnBitToBigInteger(Arrays.copyOfRange(resData, idLen, idLen + leftPayloadDim));
            BigInteger resRightValue = columnBitToBigInteger(Arrays.copyOfRange(resData, idLen + leftPayloadDim, resData.length - 1));

            if (result.get(0).getOperation().equals(OperationEnum.DELETE)) {
                Assert.assertEquals(leftMap.containsKey(resId) && rightMap.containsKey(resId), resValid);
                if (resValid) {
                    // 如果真的删除了，删除的是原本的数据
                    Assert.assertEquals(leftMap.get(resId), resLeftValue);
                    Assert.assertEquals(rightMap.get(resId), resRightValue);
                }
            } else {
                // 如果是insert,只有当另一个表也存在这个key的时候才会带来后续更新
                Assert.assertEquals(otherMap.containsKey(resId), resValid);
                if (resValid) {
                    // 如果更新了，待更新的数据应该和更新后表格中的数据一致
                    Assert.assertEquals(afterJoinRes.get(resId)[0], resLeftValue);
                    Assert.assertEquals(afterJoinRes.get(resId)[1], resRightValue);
                }
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
