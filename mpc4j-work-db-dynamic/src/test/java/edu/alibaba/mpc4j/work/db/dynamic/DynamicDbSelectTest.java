package edu.alibaba.mpc4j.work.db.dynamic;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.work.db.dynamic.select.DynamicDbSelectCircuitFactory.DynamicDbSelectCircuitType;
import edu.alibaba.mpc4j.work.db.dynamic.select.SelectMt;
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
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * test cases for dynamic db select.
 *
 * @author Feng Han
 * @date 2025/3/10
 */
@RunWith(Parameterized.class)
public class DynamicDbSelectTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDbSelectTest.class);
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

    private BitVector[] update;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            DynamicDbSelectCircuitType.ZGC24.name(),
            new DynamicDbCircuitConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * the config
     */
    private final DynamicDbCircuitConfig config;

    public DynamicDbSelectTest(String name, DynamicDbCircuitConfig config) {
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
        testRandom1pc(LARGE_DIM, LARGE_NUM);
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

    private void testRandom1pc(int dim, int num) {
        for (OperationEnum op : OPERATION_ENUMS) {
            z2cParties = new MpcZ2cParty[]{new PlainZ2cParty()};
            testPto(dim, num, op);
        }
    }

    private void testRandom2pc(int dim, int num) {
        for (OperationEnum op : OPERATION_ENUMS) {
            DynamicDb2pcZ2Party dynamicDb2pcZ2Party = new DynamicDb2pcZ2Party("test");
            z2cParties = dynamicDb2pcZ2Party.genParties(true);
            testPto(dim, num, op);
        }
    }

    private void testRandom3pc(int dim, int num) {
        for (OperationEnum op : OPERATION_ENUMS) {
            DynamicDb3pcZ2Party dynamicDb3pcZ2Party = new DynamicDb3pcZ2Party("test");
            z2cParties = dynamicDb3pcZ2Party.genParties(true);
            testPto(dim, num, op);
        }
    }

    private void testPto(int dim, int num, OperationEnum operation) {
        LOGGER.info("test ({}), dim = {}, num = {}, op = {}", MaterializedTableType.SELECT_MT, dim, num, operation.name());
        try {
            SelectMt[] selectMts = createSelectMt(num, dim, false);
            UpdateMessage[] updateMessages = createUpdateMsg(num, dim, operation);
            DynamicDbCircuitPartyThread[] threads = IntStream.range(0, z2cParties.length)
                .mapToObj(p -> new DynamicDbCircuitPartyThread(config, z2cParties[p], updateMessages[p], selectMts[p]))
                .toArray(DynamicDbCircuitPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (DynamicDbCircuitPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            assertOutput(threads[0].getPlainUpdateResult(), selectMts[0], operation);
            // destroy
            LOGGER.info("test ({}), dim = {}, num = {} end, time= {}ms", MaterializedTableType.SELECT_MT, dim, num, time);
        } catch (InterruptedException | MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }


    private SelectMt[] createSelectMt(int num, int dim, boolean isOutput) {
        BiFunction<Z2IntegerCircuit, MpcZ2Vector[], MpcZ2Vector> function = (circuit, columnData) -> {
            try {
                return circuit.getParty().and(columnData[0], columnData[columnData.length - 1]);
            } catch (MpcAbortException e) {
                throw new RuntimeException(e);
            }
        };

        int idLen = LongUtils.ceilLog2(num) + 1;
        int valueDim = dim - idLen - 1;
        Assert.assertTrue(valueDim > 0);
        input = new BitVector[dim];
        input[0] = BitVectorFactory.createZeros(num);
        BitVector[] indexes = Z2VectorUtils.getBinaryIndex(num);
        System.arraycopy(indexes, 0, input, 1, indexes.length);
        for (int i = 0; i < valueDim; i++) {
            input[i + idLen] = BitVectorFactory.createRandom(num, secureRandom);
        }
        input[dim - 1] = input[idLen].copy();
        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(input))
            .map(s -> new SelectMt(s, dim - 1, isOutput, IntStream.range(0, idLen).toArray(), function))
            .toArray(SelectMt[]::new);
    }

    private UpdateMessage[] createUpdateMsg(int mtNum, int dim, OperationEnum operation) {
        update = new BitVector[dim];
        if (operation.equals(OperationEnum.INSERT)) {
            update[0] = BitVectorFactory.createOnes(1);
            for (int i = 1; i < dim; i++) {
                update[i] = BitVectorFactory.createRandom(1, secureRandom);
            }
        } else {
            int randInd = secureRandom.nextInt(mtNum);
            for (int i = 0; i < dim; i++) {
                update[i] = input[i].get(randInd) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1);
            }
        }
        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(update))
            .map(s -> new UpdateMessage(operation, s))
            .toArray(UpdateMessage[]::new);
    }

    private void assertOutput(List<UpdateMessage> result, SelectMt afterUpdateMt, OperationEnum operation) {
        // 原则是保证有效数据都没有变化，删除和插入成功，不验证其他dummy的数据
        boolean[] flag = BinaryUtils.byteArrayToBinary(input[input.length - 1].getBytes(), input[input.length - 1].bitNum());
        BigInteger[] beforeData = ZlDatabase.create(EnvType.STANDARD, true, input).getBigIntegerData();
        Set<BigInteger> beforeValidSet = new HashSet<>();
        for (int i = 0; i < beforeData.length; i++) {
            if (flag[i]) {
                beforeValidSet.add(beforeData[i]);
            }
        }

        BitVector[] afterUpData = Arrays.stream(afterUpdateMt.getData()).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        boolean[] AfterFlag = BinaryUtils.byteArrayToBinary(afterUpData[afterUpData.length - 1].getBytes(),
            afterUpData[afterUpData.length - 1].bitNum());
        BigInteger[] afterUpBigData = ZlDatabase.create(EnvType.STANDARD, true, afterUpData).getBigIntegerData();
        Set<BigInteger> afterValidSet = new HashSet<>();
        for (int i = 0; i < afterUpBigData.length; i++) {
            if (AfterFlag[i]) {
                afterValidSet.add(afterUpBigData[i]);
            }
        }

        BigInteger a = columnBitToBigInteger(update);
        if (operation.equals(OperationEnum.INSERT)) {
            int idLen = LongUtils.ceilLog2(beforeData.length) + 1;
            if (update[idLen].get(0) && update[update.length - 1].get(0)) {
                beforeValidSet.add(a);
            }else{
                // change the last bit
                a = a.xor(a.and(BigInteger.ONE));
            }
        } else {
            beforeValidSet.remove(a);
        }
        Assert.assertEquals(beforeValidSet, afterValidSet);

        if (!afterUpdateMt.isOutputTable()) {
            Assert.assertEquals(1, result.size());
            Assert.assertEquals(result.get(0).getOperation(), operation);
            BigInteger actual = columnBitToBigInteger(Arrays.stream(result.get(0).getRowData())
                .map(MpcZ2Vector::getBitVector)
                .toArray(BitVector[]::new));
            Assert.assertEquals(a, actual);
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
