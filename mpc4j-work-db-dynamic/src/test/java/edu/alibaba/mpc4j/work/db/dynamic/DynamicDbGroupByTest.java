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
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.work.db.dynamic.group.DynamicDbGroupByCircuitFactory.DynamicDbGroupByCircuitType;
import edu.alibaba.mpc4j.work.db.dynamic.group.GroupByMt;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AggregateEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;
import edu.alibaba.mpc4j.work.db.dynamic.structure.OperationEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;
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
 * test cases for dynamic db group by.
 *
 * @author Feng Han
 * @date 2025/3/11
 */
@RunWith(Parameterized.class)
public class DynamicDbGroupByTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDbGroupByTest.class);
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
     * input data
     */
    private BitVector[] input;
    /**
     * update data
     */
    private BitVector[] update;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            DynamicDbGroupByCircuitType.ZGC24.name(),
            new DynamicDbCircuitConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * the config
     */
    private final DynamicDbCircuitConfig config;

    public DynamicDbGroupByTest(String name, DynamicDbCircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void test1Num1pc() {
        testRandom1pc(DEFAULT_DIM, 1);
    }

    @Test
    public void test2Num1pc() {
        testRandom1pc(DEFAULT_DIM, 2);
    }

    @Test
    public void test8Num1pc() {
        testRandom1pc(DEFAULT_DIM, 8);
    }

    @Test
    public void testDefaultNum1pc() {
        testRandom1pc(LARGE_DIM, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum1pc() {
        for(int i = 0; i < 100; i++){
            testRandom1pc(LARGE_DIM, LARGE_NUM);
        }
    }

    @Test
    public void test1Num2pc() {
        testRandom2pc(DEFAULT_DIM, 1);
    }

    @Test
    public void test2Num2pc() {
        testRandom2pc(DEFAULT_DIM, 2);
    }

    @Test
    public void test8Num2pc() {
        testRandom2pc(DEFAULT_DIM, 8);
    }

    @Test
    public void testDefaultNum2pc() {
        testRandom2pc(LARGE_DIM, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum2pc() {
        testRandom2pc(LARGE_DIM, LARGE_NUM);
    }

    @Test
    public void testLargeNum3pc() {
        testRandom3pc(LARGE_DIM, LARGE_NUM);
    }

    private void testRandom1pc(int payloadDim, int num) {
        z2cParties = new MpcZ2cParty[]{new PlainZ2cParty()};
        for (OperationEnum op : OPERATION_ENUMS) {
            for (AggregateEnum aggType : AggregateEnum.values()) {

                if (!(op.equals(OperationEnum.DELETE) && !aggType.equals(AggregateEnum.SUM))) {
                    testPto(payloadDim, num, op, aggType);
                }
            }
        }
    }

    private void testRandom2pc(int payloadDim, int num) {
        DynamicDb2pcZ2Party dynamicDb2pcZ2Party = new DynamicDb2pcZ2Party("test");
        for (OperationEnum op : OPERATION_ENUMS) {
            for (AggregateEnum aggType : AggregateEnum.values()) {
                z2cParties = dynamicDb2pcZ2Party.genParties(true);
                if (!(op.equals(OperationEnum.DELETE) && !aggType.equals(AggregateEnum.SUM))) {
                    testPto(payloadDim, num, op, aggType);
                }
            }
        }
    }

    private void testRandom3pc(int payloadDim, int num) {
        DynamicDb3pcZ2Party dynamicDb3pcZ2Party = new DynamicDb3pcZ2Party("test");
        for (OperationEnum op : OPERATION_ENUMS) {
            for (AggregateEnum aggType : AggregateEnum.values()) {
                z2cParties = dynamicDb3pcZ2Party.genParties(true);
                if (!(op.equals(OperationEnum.DELETE) && !aggType.equals(AggregateEnum.SUM))) {
                    testPto(payloadDim, num, op, aggType);
                }
            }
        }
    }

    private void testPto(int payloadDim, int num, OperationEnum operation, AggregateEnum aggType) {
        LOGGER.info("test ({}), payloadDim = {}, num = {}, op = {}", MaterializedTableType.SELECT_MT, payloadDim, num, operation.name());
        try {
            GroupByMt[] groupByMts = createGroupByMt(num, payloadDim, false, aggType);
            UpdateMessage[] updateMessages = createUpdateMsg(num, payloadDim, operation);
            DynamicDbCircuitPartyThread[] threads = IntStream.range(0, z2cParties.length)
                .mapToObj(p -> new DynamicDbCircuitPartyThread(config, z2cParties[p], updateMessages[p], groupByMts[p]))
                .toArray(DynamicDbCircuitPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (DynamicDbCircuitPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            assertOutput(num, threads[0].getPlainUpdateResult(), groupByMts[0], operation);
            // destroy
            LOGGER.info("test ({}), payloadDim = {}, num = {} end, time= {}ms", MaterializedTableType.SELECT_MT, payloadDim, num, time);
        } catch (InterruptedException | MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }

    private GroupByMt[] createGroupByMt(int num, int payloadDim, boolean isOutput, AggregateEnum aggType) {
        int idLen = LongUtils.ceilLog2(num) + 1;
        input = new BitVector[idLen + payloadDim + 1];
        input[0] = BitVectorFactory.createZeros(num);
        BitVector[] indexes = Z2VectorUtils.getBinaryIndex(num);
        System.arraycopy(indexes, 0, input, 1, indexes.length);
        for (int i = 0; i < payloadDim + 1; i++) {
            input[i + idLen] = BitVectorFactory.createRandom(num, secureRandom);
        }
        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(input))
            .map(s -> new GroupByMt(s, idLen + payloadDim, isOutput, IntStream.range(0, idLen).toArray(), aggType))
            .toArray(GroupByMt[]::new);
    }

    private UpdateMessage[] createUpdateMsg(int num, int payloadDim, OperationEnum operation) {
        int idLen = LongUtils.ceilLog2(num) + 1;
        update = new BitVector[idLen + payloadDim + 1];
        // 1. 插入一个已存的有效group_id对应的有效输入；
        // 2. 插入一个有效group_id对应的无效输入；
        // 3. 插入一个新的有效group_id的有效输入；
        // 4. 插入一个新的有效group_id对应的无效输入
        int randInd = secureRandom.nextInt(num);
        // test update a non-existing element
        update[0] = BitVectorFactory.createOnes(1);
        for (int i = 1; i < idLen; i++) {
            update[i] = input[i].get(randInd) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1);
        }
        for (int i = idLen; i < idLen + payloadDim + 1; i++) {
            update[i] = BitVectorFactory.createRandom(1, secureRandom);
        }
        update[update.length - 1] = BitVectorFactory.createZeros(1);
        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(update))
            .map(s -> new UpdateMessage(operation, s))
            .toArray(UpdateMessage[]::new);
    }

    private void assertOutput(int num, List<UpdateMessage> result, GroupByMt afterUpdateMt, OperationEnum operation) {
        int idLen = LongUtils.ceilLog2(num) + 1;
        // 原则是保证有效数据都没有变化，删除和插入成功，不验证其他dummy的数据
        boolean[] flag = BinaryUtils.byteArrayToBinary(input[input.length - 1].getBytes(), input[input.length - 1].bitNum());
        BigInteger[] beforeIdData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(input, idLen)).getBigIntegerData();
        BigInteger[] beforeValueData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOfRange(input, idLen, input.length - 1)).getBigIntegerData();
        Map<BigInteger, BigInteger> beforeValidMap = new HashMap<>();
        Map<BigInteger, BigInteger> beforeBufferMap = new HashMap<>();
        for (int i = 0; i < beforeIdData.length; i++) {
            if (flag[i]) {
                beforeValidMap.put(beforeIdData[i], beforeValueData[i]);
                beforeBufferMap.put(beforeIdData[i], beforeValueData[i]);
            }
        }

        BitVector[] afterUpData = Arrays.stream(afterUpdateMt.getData()).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        boolean[] AfterFlag = BinaryUtils.byteArrayToBinary(afterUpData[afterUpData.length - 1].getBytes(),
            afterUpData[afterUpData.length - 1].bitNum());
        BigInteger[] afterUpIdData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(afterUpData, idLen)).getBigIntegerData();
        BigInteger[] afterUpValueData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOfRange(afterUpData, idLen, afterUpData.length - 1)).getBigIntegerData();
        Map<BigInteger, BigInteger> afterValidMap = new HashMap<>();
        for (int i = 0; i < afterUpIdData.length; i++) {
            if (AfterFlag[i]) {
                afterValidMap.put(afterUpIdData[i], afterUpValueData[i]);
            }
        }

        Assert.assertTrue(Math.abs(afterValidMap.size() - beforeValidMap.size()) <= 1);

        boolean updateValid = update[update.length - 1].get(0);
        BigInteger upId = columnBitToBigInteger(Arrays.copyOf(update, idLen));
        BigInteger upValue = columnBitToBigInteger(Arrays.copyOfRange(update, idLen, update.length - 1));
        BigInteger deleteId = null, deleteValue = null;

        if (updateValid) {
            if (beforeValidMap.containsKey(upId)) {
                BigInteger original = beforeValidMap.get(upId);
                deleteId = upId;
                deleteValue = original;
                if (operation.equals(OperationEnum.INSERT)) {
                    if (afterUpdateMt.getAggType().equals(AggregateEnum.MAX)) {
                        original = original.max(upValue);
                    }
                    if (afterUpdateMt.getAggType().equals(AggregateEnum.MIN)) {
                        original = original.min(upValue);
                    }
                    if (afterUpdateMt.getAggType().equals(AggregateEnum.SUM)) {
                        original = original.add(upValue).mod(BigInteger.ONE.shiftLeft(input.length - 1 - idLen));
                    }
                } else {
                    original = (original.compareTo(upValue) < 0 ? original.add(BigInteger.ONE.shiftLeft(input.length - 1 - idLen)) : original).subtract(upValue);
                }
                beforeValidMap.put(upId, original);
            } else {
                if(operation.equals(OperationEnum.INSERT)){
                    beforeValidMap.put(upId, upValue);
                }
            }
        }
        Assert.assertEquals(beforeValidMap, afterValidMap);

        if (!afterUpdateMt.isOutputTable()) {
            Assert.assertEquals(operation.equals(OperationEnum.DELETE) ? 2 : 3, result.size());
            for (UpdateMessage updateMessage : result) {
                BitVector[] resData = Arrays.stream(updateMessage.getRowData()).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
                boolean resValid = resData[resData.length - 1].get(0);
                BigInteger resId = columnBitToBigInteger(Arrays.copyOf(resData, idLen));
                BigInteger resValue = columnBitToBigInteger(Arrays.copyOfRange(resData, idLen, resData.length - 1));

                if(updateMessage.getOperation().equals(OperationEnum.DELETE)){
                    if(resValid){
                        // 如果真的删除了，删除的是原本的数据
                        Assert.assertEquals(deleteId, resId);
                        Assert.assertEquals(deleteValue, resValue);
                        Assert.assertTrue(beforeBufferMap.containsKey(resId));
                        beforeBufferMap.remove(resId);
                    }
                }else{
                    if(resValid){
                        // 如果要插入，则插入的一定是原本没有的，因为delete的命令应该在insert前面出现
                        Assert.assertFalse(beforeBufferMap.containsKey(resId));
                        beforeBufferMap.put(resId, resValue);
                    }
                }
            }
            Assert.assertEquals(beforeBufferMap, afterValidMap);
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
