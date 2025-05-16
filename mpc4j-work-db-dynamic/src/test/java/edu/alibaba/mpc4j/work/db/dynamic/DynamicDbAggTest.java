package edu.alibaba.mpc4j.work.db.dynamic;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2cParty;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.work.db.dynamic.agg.AggMt;
import edu.alibaba.mpc4j.work.db.dynamic.agg.DynamicDbAggCircuitFactory.DynamicDbAggCircuitType;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * test case for dynamic db agg.
 *
 * @author Feng Han
 * @date 2025/3/11
 */
@RunWith(Parameterized.class)
public class DynamicDbAggTest {
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
     * update Operation input data
     */
    private BitVector[] update;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            DynamicDbAggCircuitType.ZGC24.name(),
            new DynamicDbCircuitConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * the config
     */
    private final DynamicDbCircuitConfig config;

    public DynamicDbAggTest(String name, DynamicDbCircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void test1pcDefaultDim() {
        for (int i = 0; i < 100; i++) {
            testRandom1pc(DEFAULT_DIM);
        }
    }

    @Test
    public void test1pcLargeDim() {
        for (int i = 0; i < 100; i++) {
            testRandom1pc(LARGE_DIM);
        }
    }

    @Test
    public void test2pcDefaultDim() {
        for (int i = 0; i < 5; i++) {
            testRandom2pc(DEFAULT_DIM);
        }
    }

    @Test
    public void test2pcLargeDim() {
        for (int i = 0; i < 5; i++) {
            testRandom2pc(LARGE_DIM);
        }
    }

    @Test
    public void test3pcLargeDim() {
        for (int i = 0; i < 5; i++) {
            testRandom3pc(LARGE_DIM);
        }
    }

    private void testRandom1pc(int dim) {
        for (OperationEnum op : OPERATION_ENUMS) {
            for (AggregateEnum aggType : AggregateEnum.values()) {
                z2cParties = new MpcZ2cParty[]{new PlainZ2cParty()};
                if (!(op.equals(OperationEnum.DELETE) && !aggType.equals(AggregateEnum.SUM))) {
                    testPto(dim, op, aggType);
                }
            }
        }
    }

    private void testRandom2pc(int dim) {
        for (OperationEnum op : OPERATION_ENUMS) {
            for (AggregateEnum aggType : AggregateEnum.values()) {
                DynamicDb2pcZ2Party dynamicDb2pcZ2Party = new DynamicDb2pcZ2Party("test");
                z2cParties = dynamicDb2pcZ2Party.genParties(true);
                if (!(op.equals(OperationEnum.DELETE) && !aggType.equals(AggregateEnum.SUM))) {
                    testPto(dim, op, aggType);
                }
            }
        }
    }

    private void testRandom3pc(int dim) {
        for (OperationEnum op : OPERATION_ENUMS) {
            for (AggregateEnum aggType : AggregateEnum.values()) {
                DynamicDb3pcZ2Party dynamicDb3pcZ2Party = new DynamicDb3pcZ2Party("test");
                z2cParties = dynamicDb3pcZ2Party.genParties(true);
                if (!(op.equals(OperationEnum.DELETE) && !aggType.equals(AggregateEnum.SUM))) {
                    testPto(dim, op, aggType);
                }
            }
        }
    }

    private void testPto(int dim, OperationEnum operation, AggregateEnum aggType) {
        LOGGER.info("test ({}), dim = {}, op = {}, agg = {}",
            MaterializedTableType.GLOBAL_AGG_MT, dim, operation.name(), aggType.name());
        try {
            AggMt[] aggMts = createAggMt(dim, aggType);
            UpdateMessage[] updateMessages = createUpdateMsg(dim, operation);
            DynamicDbCircuitPartyThread[] threads = IntStream.range(0, z2cParties.length)
                .mapToObj(p -> new DynamicDbCircuitPartyThread(config, z2cParties[p], updateMessages[p], aggMts[p]))
                .toArray(DynamicDbCircuitPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (DynamicDbCircuitPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            assertOutput(aggMts[0], operation);
            // destroy
            LOGGER.info("test ({}), dim = {}, op = {}, agg = {} end, time= {}ms",
                MaterializedTableType.GLOBAL_AGG_MT, dim, operation.name(), aggType.name(), time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private AggMt[] createAggMt(int dim, AggregateEnum aggType) {
        input = IntStream.range(0, dim)
            .mapToObj(i -> BitVectorFactory.createRandom(1, secureRandom))
            .toArray(BitVector[]::new);
        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(input))
            .map(s -> new AggMt(s, dim - 1, aggType))
            .toArray(AggMt[]::new);
    }

    private UpdateMessage[] createUpdateMsg(int dim, OperationEnum operation) {
        update = IntStream.range(0, dim)
            .mapToObj(i -> BitVectorFactory.createRandom(1, secureRandom))
            .toArray(BitVector[]::new);
        return Arrays.stream(z2cParties)
            .map(party -> party.setPublicValues(update))
            .map(s -> new UpdateMessage(operation, s))
            .toArray(UpdateMessage[]::new);
    }

    private void assertOutput(AggMt afterUpdateMt, OperationEnum operation) {
        // 原则是保证有效数据都没有变化，删除和插入成功，不验证其他dummy的数据
        boolean originValid = input[input.length - 1].get(0);
        boolean updateValid = update[update.length - 1].get(0);
        boolean afterUpValid = afterUpdateMt.getData()[afterUpdateMt.getData().length - 1].getBitVector().get(0);

        BigInteger originBig = columnBitToBigInteger(Arrays.copyOf(input, input.length - 1));
        BigInteger updateBig = columnBitToBigInteger(Arrays.copyOf(update, update.length - 1));
        BigInteger afterUpBig = columnBitToBigInteger(
            Arrays.stream(afterUpdateMt.getData(), 0, afterUpdateMt.getData().length - 1)
                .map(MpcZ2Vector::getBitVector)
                .toArray(BitVector[]::new)
        );
        if (operation.equals(OperationEnum.INSERT)) {
            Assert.assertEquals(originValid | updateValid, afterUpValid);
            if (afterUpValid) {
                switch (afterUpdateMt.getAggType()) {
                    case MAX:
                        BigInteger max = (originValid && updateValid) ? originBig.max(updateBig) : (originValid ? originBig : updateBig);
                        Assert.assertEquals(max, afterUpBig);
                        break;
                    case MIN:
                        BigInteger min = (originValid && updateValid) ? originBig.min(updateBig) : (originValid ? originBig : updateBig);
                        Assert.assertEquals(min, afterUpBig);
                        break;
                    case SUM:
                        BigInteger sum = (originValid && updateValid) ? originBig.add(updateBig) : (originValid ? originBig : updateBig);
                        sum = sum.mod(BigInteger.ONE.shiftLeft(input.length - 1));
                        Assert.assertEquals(sum, afterUpBig);
                        break;
                }
            }
        } else {
            if (originValid && updateValid) {
                BigInteger sub = (originBig.compareTo(updateBig) < 0 ? originBig.add(BigInteger.ONE.shiftLeft(input.length - 1)) : originBig).subtract(updateBig);
                Assert.assertEquals(sub, afterUpBig);
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
