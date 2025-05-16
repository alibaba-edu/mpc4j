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
import edu.alibaba.mpc4j.work.db.dynamic.orderby.DynamicDbOrderByCircuitFactory.DynamicDbOrderByCircuitType;
import edu.alibaba.mpc4j.work.db.dynamic.orderby.OrderByMt;
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
 * test cases for dynamic db order by.
 *
 * @author Feng Han
 * @date 2025/3/11
 */
@RunWith(Parameterized.class)
public class DynamicDbOrderByTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDbOrderByTest.class);
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
            DynamicDbOrderByCircuitType.ZGC24.name(),
            new DynamicDbCircuitConfig.Builder().setOrderByCircuitType(DynamicDbOrderByCircuitType.ZGC24).build()
        });
        configurations.add(new Object[]{
            DynamicDbOrderByCircuitType.ZGC24_OPT.name(),
            new DynamicDbCircuitConfig.Builder().setOrderByCircuitType(DynamicDbOrderByCircuitType.ZGC24_OPT).build()
        });
        return configurations;
    }

    /**
     * the config
     */
    private final DynamicDbCircuitConfig config;

    public DynamicDbOrderByTest(String name, DynamicDbCircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void test1Num1pc() {
        testRandom1pc(1, 1, DEFAULT_DIM);
    }

    @Test
    public void test2Num1pc() {
        testRandom1pc(2, 1, DEFAULT_DIM);
    }

    @Test
    public void test8Num1pc() {
        testRandom1pc(8, 1, DEFAULT_DIM);
    }

    @Test
    public void testDefaultNum1pc() {
        testRandom1pc(DEFAULT_NUM, 1, LARGE_DIM);
    }

    @Test
    public void testLargeNum1pc() {
        for (int i = 0; i < 100; i++) {
            testRandom1pc(LARGE_NUM, 1, LARGE_DIM);
        }
    }

    @Test
    public void test1Num2pc() {
        testRandom2pc(1, 1, DEFAULT_DIM);
    }

    @Test
    public void test2Num2pc() {
        testRandom2pc(2, 1, DEFAULT_DIM);
    }

    @Test
    public void test8Num2pc() {
        testRandom2pc(8, 1, DEFAULT_DIM);
    }

    @Test
    public void testDefaultNum2pc() {
        testRandom2pc(DEFAULT_NUM, 1, LARGE_DIM);
    }

    @Test
    public void testLargeNum2pc() {
        testRandom2pc(LARGE_NUM, 1, LARGE_DIM);
    }

    @Test
    public void testLargeNum3pc() {
        testRandom3pc(LARGE_NUM, 1, LARGE_DIM);
    }

    private void testRandom1pc(int limit, int deleteThreshold, int payloadDim) {
        for (OperationEnum op : OPERATION_ENUMS) {
            z2cParties = new MpcZ2cParty[]{new PlainZ2cParty()};
            testPto(limit, deleteThreshold, payloadDim, op);
        }
    }

    private void testRandom2pc(int limit, int deleteThreshold, int payloadDim) {
        for (OperationEnum op : OPERATION_ENUMS) {
            DynamicDb2pcZ2Party dynamicDb2pcZ2Party = new DynamicDb2pcZ2Party("test");
            z2cParties = dynamicDb2pcZ2Party.genParties(true);
            testPto(limit, deleteThreshold, payloadDim, op);
        }
    }

    private void testRandom3pc(int limit, int deleteThreshold, int payloadDim) {
        for (OperationEnum op : OPERATION_ENUMS) {
            DynamicDb3pcZ2Party dynamicDb3pcZ2Party = new DynamicDb3pcZ2Party("test");
            z2cParties = dynamicDb3pcZ2Party.genParties(true);
            testPto(limit, deleteThreshold, payloadDim, op);
        }
    }

    private void testPto(int limit, int deleteThreshold, int payloadDim, OperationEnum operation) {
        LOGGER.info("test ({}), payloadDim = {}, num = {}, op = {}",
            MaterializedTableType.ORDER_BY_MT, payloadDim, limit + deleteThreshold, operation.name());
        try {
            OrderByMt[] orderByMts = createOrderByByMt(limit, deleteThreshold, payloadDim, false);
            UpdateMessage[] updateMessages = createUpdateMsg(limit, deleteThreshold, payloadDim, operation);
            DynamicDbCircuitPartyThread[] threads = IntStream.range(0, z2cParties.length)
                .mapToObj(p -> new DynamicDbCircuitPartyThread(config, z2cParties[p], updateMessages[p], orderByMts[p]))
                .toArray(DynamicDbCircuitPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (DynamicDbCircuitPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            assertOutput(limit, deleteThreshold, threads[0].getPlainUpdateResult(), orderByMts[0], operation);
            // destroy
            LOGGER.info("test ({}), payloadDim = {}, num = {}, op = {}, time = {}ms",
                MaterializedTableType.ORDER_BY_MT, payloadDim, limit + deleteThreshold, operation.name(), time);
        } catch (InterruptedException | MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }

    private OrderByMt[] createOrderByByMt(int limit, int deleteThreshold, int payloadDim, boolean isOutput) {
        int num = limit + deleteThreshold;
        int idLen = LongUtils.ceilLog2(num) + 1;
        int byteLen = CommonUtils.getByteLength(payloadDim);

        input = new BitVector[idLen + payloadDim + 1];
        input[0] = BitVectorFactory.createZeros(num);
        BitVector[] indexes = Z2VectorUtils.getBinaryIndex(num);
        System.arraycopy(indexes, 0, input, 1, indexes.length);

        BigInteger upperBound = BigInteger.ONE.shiftLeft(payloadDim);
        BigInteger[] randomPayload = IntStream.range(0, num)
            .mapToObj(i -> BigIntegerUtils.randomNonNegative(upperBound, secureRandom))
            .toArray(BigInteger[]::new);
        BigInteger[] sortRes = Arrays.stream(randomPayload).sorted((a, b) -> -a.compareTo(b)).toArray(BigInteger[]::new);
        byte[][] sortByte = Arrays.stream(sortRes)
            .map(ea -> BigIntegerUtils.nonNegBigIntegerToByteArray(ea, byteLen))
            .toArray(byte[][]::new);
        BitVector[] payloadData = ZlDatabase.create(payloadDim, sortByte).bitPartition(EnvType.STANDARD_JDK, true);
        System.arraycopy(payloadData, 0, input, idLen, payloadDim);

        // set some data to be dummy point
        int randValidNum = secureRandom.nextInt(1, num);
        input[input.length - 1] = BitVectorFactory.createOnes(randValidNum);
        input[input.length - 1] = input[input.length - 1].padShiftLeft(num - randValidNum);

        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(input))
            .map(s -> new OrderByMt(s, idLen + payloadDim, isOutput,
                IntStream.range(idLen, idLen + payloadDim).toArray(),
                IntStream.range(0, idLen).toArray(),
                limit, deleteThreshold))
            .toArray(OrderByMt[]::new);
    }

    private UpdateMessage[] createUpdateMsg(int limit, int deleteThreshold, int payloadDim, OperationEnum operation) {
        int num = limit + deleteThreshold;
        int idLen = LongUtils.ceilLog2(num) + 1;
        update = new BitVector[idLen + payloadDim + 1];
        if (operation.equals(OperationEnum.INSERT)) {
            // 插入一个新的
            update[0] = BitVectorFactory.createOnes(1);
            for (int i = 1; i < update.length; i++) {
                update[i] = BitVectorFactory.createRandom(1, secureRandom);
            }
        } else {
            // 删除一个旧的
            int randInd = secureRandom.nextInt(limit);
            for (int i = 0; i < update.length; i++) {
                update[i] = input[i].get(randInd) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1);
            }
        }
        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(update))
            .map(s -> new UpdateMessage(operation, s))
            .toArray(UpdateMessage[]::new);
    }

    private void assertOutput(int limit, int deleteThreshold, List<UpdateMessage> result, OrderByMt afterUpdateMt, OperationEnum operation) {
        int num = limit + deleteThreshold;
        int idLen = LongUtils.ceilLog2(num) + 1;

        if (operation.equals(OperationEnum.DELETE)) {
            Assert.assertEquals(1, afterUpdateMt.getCurrentDeleteNum());
        } else {
            Assert.assertEquals(0, afterUpdateMt.getCurrentDeleteNum());
        }

        // 原则是保证有效数据都没有变化，删除和插入成功，不验证其他dummy的数据
        boolean[] flag = BinaryUtils.byteArrayToBinary(input[input.length - 1].getBytes(), input[input.length - 1].bitNum());
        BigInteger[] beforeIdData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(input, idLen)).getBigIntegerData();
        BigInteger[] beforeValueData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOfRange(input, idLen, input.length - 1)).getBigIntegerData();

        BitVector[] afterUpData = Arrays.stream(afterUpdateMt.getData()).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        boolean[] afterFlag = BinaryUtils.byteArrayToBinary(afterUpData[afterUpData.length - 1].getBytes(),
            afterUpData[afterUpData.length - 1].bitNum());
        BigInteger[] afterUpIdData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(afterUpData, idLen)).getBigIntegerData();
        BigInteger[] afterUpValueData = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOfRange(afterUpData, idLen, afterUpData.length - 1)).getBigIntegerData();

        boolean updateValid = update[update.length - 1].get(0);
        BigInteger upId = columnBitToBigInteger(Arrays.copyOf(update, idLen));
        BigInteger upValue = columnBitToBigInteger(Arrays.copyOfRange(update, idLen, update.length - 1));

        if (operation.equals(OperationEnum.INSERT)) {
            // insert
            boolean insertValid = false;
            for (int i = 0, j = 0; j < beforeIdData.length; ) {
                if (!afterFlag[j]) {
                    // 如果在前面位置都没有插入，则后面无效位都不用比较了
                    Assert.assertFalse(flag[i]);
                    i++;
                    j++;
                    continue;
                }
                if ((!insertValid) && updateValid && (beforeValueData[i].compareTo(upValue) < 0 || (!flag[i]))) {
                    // 如果该插在这个位置
                    Assert.assertEquals(upId, afterUpIdData[j]);
                    Assert.assertEquals(upValue, afterUpValueData[j]);
                    Assert.assertTrue(afterFlag[j]);
                    j++;
                    insertValid = true;
                } else {
                    Assert.assertEquals(beforeValueData[i], afterUpValueData[j]);
                    Assert.assertEquals(beforeIdData[i], afterUpIdData[j]);
                    Assert.assertEquals(flag[i], afterFlag[j]);
                    i++;
                    j++;
                }
            }
        } else {
            // delete
            boolean deleteValid = false;
            for (int i = 0, j = 0; i < beforeIdData.length; ) {
                if (!flag[i]) {
                    Assert.assertFalse(afterFlag[j]);
                    i++;
                    j++;
                    continue;
                }
                if (updateValid && beforeIdData[i].compareTo(upId) == 0) {
                    i++;
                    Assert.assertFalse(deleteValid);
                    deleteValid = true;
                } else {
                    Assert.assertEquals(beforeValueData[i], afterUpValueData[j]);
                    Assert.assertEquals(beforeIdData[i], afterUpIdData[j]);
                    Assert.assertEquals(flag[i], afterFlag[j]);
                    i++;
                    j++;
                }
            }
            if (deleteValid) {
                Assert.assertFalse(afterFlag[afterFlag.length - 1]);
            }
        }

        if (!afterUpdateMt.isOutputTable()) {
            Map<BigInteger, BigInteger> beforeValidMap = new HashMap<>();
            for (int i = 0; i < limit; i++) {
                if (flag[i]) {
                    beforeValidMap.put(beforeIdData[i], beforeValueData[i]);
                }
            }
            Map<BigInteger, BigInteger> afterValidMap = new HashMap<>();
            for (int i = 0; i < limit; i++) {
                if (afterFlag[i]) {
                    afterValidMap.put(afterUpIdData[i], afterUpValueData[i]);
                }
            }

            Assert.assertEquals(2, result.size());
            Assert.assertEquals(OperationEnum.DELETE, result.get(0).getOperation());
            Assert.assertEquals(OperationEnum.INSERT, result.get(1).getOperation());

            for (UpdateMessage updateMessage : result) {
                BitVector[] resData = Arrays.stream(updateMessage.getRowData()).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
                boolean resValid = resData[resData.length - 1].get(0);
                BigInteger resId = columnBitToBigInteger(Arrays.copyOf(resData, idLen));
                BigInteger resValue = columnBitToBigInteger(Arrays.copyOfRange(resData, idLen, resData.length - 1));

                if (updateMessage.getOperation().equals(OperationEnum.DELETE)) {
                    if (resValid) {
                        // 如果真的删除了，删除的是原本的数据
                        Assert.assertTrue(beforeValidMap.containsKey(resId));
                        Assert.assertEquals(beforeValidMap.get(resId), resValue);
                        beforeValidMap.remove(resId);
                    }
                } else {
                    if (resValid) {
                        // 如果要插入，则插入的一定是原本没有的，因为delete的命令应该在insert前面出现
                        Assert.assertFalse(beforeValidMap.containsKey(resId));
                        beforeValidMap.put(resId, resValue);
                    }
                }
            }
            Assert.assertEquals(beforeValidMap, afterValidMap);
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
