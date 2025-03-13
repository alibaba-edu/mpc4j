package edu.alibaba.mpc4j.work.scape.s3pc.opf.main.agg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFnParam.AggOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggParty;
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
 * agg main
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class AggMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "AGG";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "agg_pto_name";

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
    private final AggOp[] ops;
    /**
     * config
     */
    private final AggConfig config;
    /**
     * binary input data
     */
    private HashMap<Integer, TripletZ2Vector[][]> bInputDataMap;
    /**
     * arithmetic input data
     */
    private HashMap<Integer, TripletLongVector[]> aInputDataMap;

    public AggMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        elementBitLength = PropertiesUtils.readInt(properties, "element_byte_length");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        String[] opStrings = PropertiesUtils.readTrimStringArray(properties, "op");
        ops = Arrays.stream(opStrings)
            .map(AggOp::valueOf)
            .toArray(AggOp[]::new);
        // read permutation config
        LOGGER.info("{} read agg config", ownRpc.ownParty().getPartyName());
        config = AggConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
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
        for (AggOp op : ops) {
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
        aInputDataMap = new HashMap<>();
        List<Integer> inputSizesList = new java.util.ArrayList<>(Arrays.stream(inputSizes).boxed().toList());
        inputSizesList.add(WARMUP_INPUT_SIZE);
        inputSizesList.sort(Integer::compareTo);
        if (ownRpc.ownParty().getPartyId() == 0) {
            for (int inputSize : inputSizesList) {
                BitVector[][] bInputData = genBinaryInputData(inputSize);
                LongVector[] aInputData = genLongInputData(inputSize);
                bInputDataMap.put(inputSize, new TripletZ2Vector[][]{
                    abb3PartyTmp.getZ2cParty().shareOwn(bInputData[0]),
                    abb3PartyTmp.getZ2cParty().shareOwn(bInputData[1])
                });
                aInputDataMap.put(inputSize, new TripletLongVector[]{
                    (TripletLongVector) abb3PartyTmp.getLongParty().shareOwn(aInputData[0]),
                    (TripletLongVector) abb3PartyTmp.getLongParty().shareOwn(aInputData[1])
                });
            }
        } else {
            for (int inputSize : inputSizesList) {
                bInputDataMap.put(inputSize, new TripletZ2Vector[][]{
                    abb3PartyTmp.getZ2cParty().shareOther(
                        IntStream.range(0, elementBitLength).map(i -> inputSize).toArray(), ownRpc.getParty(0)),
                    abb3PartyTmp.getZ2cParty().shareOther(
                        IntStream.range(0, 1).map(i -> inputSize).toArray(), ownRpc.getParty(0)),
                });
                aInputDataMap.put(inputSize, new TripletLongVector[]{
                    (TripletLongVector) abb3PartyTmp.getLongParty().shareOther(
                        inputSize, ownRpc.getParty(0)),
                    (TripletLongVector) abb3PartyTmp.getLongParty().shareOther(
                        inputSize, ownRpc.getParty(0))
                });
            }
        }
        abb3PartyTmp.destroy();
    }

    private void warmup(Rpc ownRpc, int taskId, AggOp op) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        AggParty aggParty = AggFactory.createParty(abb3Party, config);
        aggParty.setTaskId(taskId);
        aggParty.setParallel(false);
        aggParty.getRpc().synchronize();
        int inputDim = op.equals(AggOp.SUM) ? 1 : elementBitLength;
        aggParty.setUsage(new AggFnParam(!op.equals(AggOp.SUM), op, WARMUP_INPUT_SIZE, inputDim));
        // 初始化协议
        LOGGER.info("(warmup) {} init", aggParty.ownParty().getPartyName());
        aggParty.init();
        aggParty.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", aggParty.ownParty().getPartyName());
        runOp(aggParty, op, WARMUP_INPUT_SIZE);
        aggParty.getRpc().synchronize();
        aggParty.getRpc().reset();
        LOGGER.info("(warmup) {} finish", aggParty.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize, AggOp op,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: op:{}, inputSize = {}, bitLength = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), op.name(), inputSize, elementBitLength, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        AggParty aggParty = AggFactory.createParty(abb3Party, config);
        aggParty.setTaskId(taskId);
        aggParty.setParallel(parallel);
        // 启动测试
        int inputDim = op.equals(AggOp.SUM) ? 1 : elementBitLength;
        aggParty.setUsage(new AggFnParam(!op.equals(AggOp.SUM), op, inputSize, inputDim));
        aggParty.getRpc().synchronize();
        aggParty.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", aggParty.ownParty().getPartyName());
        stopWatch.start();
        aggParty.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = aggParty.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = aggParty.getRpc().getPayloadByteLength();
        long initSendByteLength = aggParty.getRpc().getSendByteLength();
        aggParty.getRpc().synchronize();
        aggParty.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", aggParty.ownParty().getPartyName());
        stopWatch.start();
        runOp(aggParty, op, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = aggParty.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = aggParty.getRpc().getPayloadByteLength();
        long ptoSendByteLength = aggParty.getRpc().getSendByteLength();
        String info = op.name()
            + "\t" + aggParty.ownParty().getPartyId()
            + "\t" + inputSize
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        aggParty.getRpc().synchronize();
        aggParty.getRpc().reset();
        LOGGER.info("{} finish", aggParty.ownParty().getPartyName());
    }

    private void runOp(AggParty aggParty, AggOp op, int inputSize) throws MpcAbortException {
        TripletZ2Vector[][] dataShareB = bInputDataMap.get(inputSize);
        TripletLongVector[] dataShareA = aInputDataMap.get(inputSize);
        switch (op) {
            case SUM:
                aggParty.agg(dataShareA[0], dataShareA[1], op);
                break;
            case MAX, MIN:
                aggParty.agg(dataShareB[0], dataShareB[1][0], op);
                break;
            default:
                throw new IllegalArgumentException("Unsupported op: " + op.name());
        }
        aggParty.getAbb3Party().checkUnverified();
    }

    private BitVector[][] genBinaryInputData(int inputSize) {
        return new BitVector[][]{
            IntStream.range(0, elementBitLength)
                .mapToObj(i -> BitVectorFactory.createRandom(inputSize, secureRandom))
                .toArray(BitVector[]::new),
            new BitVector[]{BitVectorFactory.createRandom(inputSize, secureRandom)}
        };
    }

    private LongVector[] genLongInputData(int inputSize) {
        return new LongVector[]{
            LongVector.createRandom(inputSize, secureRandom),
            LongVector.create(IntStream.range(0, inputSize).mapToLong(i -> secureRandom.nextBoolean() ? 1 : 0).toArray())
        };
    }
}
