package edu.alibaba.mpc4j.s3pc.abb3.main.predicate;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * PredicateMain, test eq and leq
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class PredicateMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PredicateMain.class);
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "PREDICATE";

    /**
     * warmup set size
     */
    private static final int WARMUP_INPUT_SIZE = 1 << 10;
    /**
     * element bit length
     */
    private final int elementBitLength;
    /**
     * input sizes
     */
    private final int[] inputSizes;
    /**
     * ops
     */
    private final String[] ops;
    /**
     * ops
     */
    private final Z2CircuitConfig config;
    /**
     * binary input data
     */
    private HashMap<Integer, TripletZ2Vector[][]> bInputDataMap;

    public PredicateMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        elementBitLength = PropertiesUtils.readInt(properties, "element_byte_length");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        ops = PropertiesUtils.readTrimStringArray(properties, "op");
        Arrays.stream(ops).forEach(s -> Preconditions.checkArgument("EQ".equals(s) || "LEQ".equals(s)));
        // read permutation config
        LOGGER.info("{} read pgSort config", ownRpc.ownParty().getPartyName());
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        config = new Z2CircuitConfig.Builder().setComparatorType(comparatorType).build();
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getComparatorType().name()
            + "_" + appendString
            + "_" + elementBitLength
            + "_" + ownRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Function\tParty ID\tInput Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        // 建立连接
        ownRpc.connect();
        LOGGER.info("{} is connected and going to generate input data", ownRpc.ownParty().getPartyName());
        inputGen(ownRpc);
        LOGGER.info("{} ready to run", ownRpc.ownParty().getPartyName());
        // 启动测试
        int taskId = 0;
        for (String op : ops) {
            // 预热
            warmup(ownRpc, taskId, op);
            taskId++;
            // 正式测试
            for (int size : inputSizes) {
                runOneTest(parallel, ownRpc, taskId, size, op, printWriter);
                taskId++;
            }
        }
        // 断开连接
        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void inputGen(Rpc ownRpc) throws MpcAbortException {
        Abb3Party abb3PartyTmp = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3PartyTmp.init();
        // generate test data
        bInputDataMap = new HashMap<>();
        List<Integer> inputSizesList = new java.util.ArrayList<>(Arrays.stream(inputSizes).boxed().toList());
        inputSizesList.add(WARMUP_INPUT_SIZE);
        inputSizesList.sort(Integer::compareTo);
        if (ownRpc.ownParty().getPartyId() == 0) {
            for (int inputSize : inputSizesList) {
                BitVector[][] bInputData = genBinaryInputData(inputSize);
                bInputDataMap.put(inputSize, new TripletZ2Vector[][]{
                    abb3PartyTmp.getZ2cParty().shareOwn(bInputData[0]),
                    abb3PartyTmp.getZ2cParty().shareOwn(bInputData[1])
                });
            }
        } else {
            for (int inputSize : inputSizesList) {
                bInputDataMap.put(inputSize, new TripletZ2Vector[][]{
                    abb3PartyTmp.getZ2cParty().shareOther(
                        IntStream.range(0, elementBitLength).map(i -> inputSize).toArray(), ownRpc.getParty(0)),
                    abb3PartyTmp.getZ2cParty().shareOther(
                        IntStream.range(0, elementBitLength).map(i -> inputSize).toArray(), ownRpc.getParty(0)),
                });
            }
        }
        abb3PartyTmp.destroy();
    }

    private void warmup(Rpc ownRpc, int taskId, String op) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        Z2IntegerCircuit circuit = new Z2IntegerCircuit(abb3Party.getZ2cParty(), config);
        abb3Party.setTaskId(taskId);
        abb3Party.setParallel(false);
        abb3Party.updateNum((long) WARMUP_INPUT_SIZE *
                ("LEQ".equals(op) ? ComparatorFactory.getAndGateNum(config.getComparatorType(), elementBitLength) : elementBitLength),
            0);
        abb3Party.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", abb3Party.ownParty().getPartyName());
        abb3Party.init();
        abb3Party.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", abb3Party.ownParty().getPartyName());
        runOp(circuit, abb3Party, op, WARMUP_INPUT_SIZE);
        abb3Party.getRpc().synchronize();
        abb3Party.getRpc().reset();
        LOGGER.info("(warmup) {} finish", abb3Party.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize, String op,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: op:{}, inputSize = {}, bitLength = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), op, inputSize, elementBitLength, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        Z2IntegerCircuit circuit = new Z2IntegerCircuit(abb3Party.getZ2cParty(), config);
        abb3Party.setTaskId(taskId);
        abb3Party.setParallel(parallel);
        // 启动测试
        abb3Party.updateNum((long) inputSize *
                ("LEQ".equals(op) ? ComparatorFactory.getAndGateNum(config.getComparatorType(), elementBitLength) : elementBitLength),
            0);
        abb3Party.getRpc().synchronize();
        abb3Party.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", abb3Party.ownParty().getPartyName());
        stopWatch.start();
        abb3Party.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = abb3Party.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = abb3Party.getRpc().getPayloadByteLength();
        long initSendByteLength = abb3Party.getRpc().getSendByteLength();
        abb3Party.getRpc().synchronize();
        abb3Party.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", abb3Party.ownParty().getPartyName());
        stopWatch.start();
        runOp(circuit, abb3Party, op, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = abb3Party.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = abb3Party.getRpc().getPayloadByteLength();
        long ptoSendByteLength = abb3Party.getRpc().getSendByteLength();
        String info = op
            + "\t" + abb3Party.ownParty().getPartyId()
            + "\t" + inputSize
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        abb3Party.getRpc().synchronize();
        abb3Party.getRpc().reset();
        LOGGER.info("{} finish", abb3Party.ownParty().getPartyName());
    }

    private void runOp(Z2IntegerCircuit circuit, Abb3Party abb3Party, String op, int inputSize) throws MpcAbortException {
        TripletZ2Vector[][] dataShareB = bInputDataMap.get(inputSize);
        if ("LEQ".equals(op)) {
            circuit.leq(dataShareB[0], dataShareB[1]);
        } else if ("EQ".equals(op)) {
            circuit.eq(dataShareB[0], dataShareB[1]);
        } else {
            throw new IllegalArgumentException("Unsupported op: " + op);
        }
        abb3Party.checkUnverified();
    }

    private BitVector[][] genBinaryInputData(int inputSize) throws MpcAbortException {
        return IntStream.range(0, 2)
            .mapToObj(k ->
                IntStream.range(0, elementBitLength)
                    .mapToObj(i -> BitVectorFactory.createRandom(inputSize, secureRandom))
                    .toArray(BitVector[]::new))
            .toArray(BitVector[][]::new);
    }
}
