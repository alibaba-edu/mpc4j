package edu.alibaba.mpc4j.s3pc.abb3.main.shuffle;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
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
 * shuffle main
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class ShuffleMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "SHUFFLE";
    /**
     * warmup set size
     */
    private static final int WARMUP_INPUT_SIZE = 1 << 10;
    /**
     * number of input arithmetic array
     */
    private final int longEleDim;
    /**
     * element bit length
     */
    private final int elementBitLength;
    /**
     * input sizes
     */
    private final int[] inputSizes;
    /**
     * input sizes
     */
    private final ShuffleOp[] ops;
    /**
     * binary input data
     */
    private HashMap<Integer, TripletZ2Vector[]> bInputDataMap;
    /**
     * arithmetic input data
     */
    private HashMap<Integer, TripletLongVector[]> aInputDataMap;

    public ShuffleMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        elementBitLength = PropertiesUtils.readInt(properties, "element_byte_length");
        longEleDim = PropertiesUtils.readInt(properties, "long_ele_dim");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        String[] opStrings = PropertiesUtils.readTrimStringArray(properties, "op");
        ops = Arrays.stream(opStrings)
            .map(ShuffleOp::valueOf)
            .toArray(ShuffleOp[]::new);
        for (ShuffleOp op : ops) {
            assert op.equals(ShuffleOp.A_SHUFFLE) || op.equals(ShuffleOp.B_SHUFFLE_COLUMN);
        }
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
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
        for (ShuffleOp op : ops) {
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
                BitVector[] bInputData = genBinaryInputData(inputSize);
                bInputDataMap.put(inputSize, abb3PartyTmp.getZ2cParty().shareOwn(bInputData));
                LongVector[] aInputData = genLongInputData(inputSize);
                TripletLongVector[] aShare = new TripletLongVector[longEleDim];
                for (int i = 0; i < longEleDim; i++) {
                    aShare[i] = (TripletLongVector) abb3PartyTmp.getLongParty().shareOwn(aInputData[i]);
                }
                aInputDataMap.put(inputSize, aShare);
            }
        } else {
            for (int inputSize : inputSizesList) {
                bInputDataMap.put(inputSize,
                    abb3PartyTmp.getZ2cParty().shareOther(
                        IntStream.range(0, elementBitLength).map(i -> inputSize).toArray(), ownRpc.getParty(0)));
                TripletLongVector[] aShare = new TripletLongVector[longEleDim];
                for (int i = 0; i < longEleDim; i++) {
                    aShare[i] = (TripletLongVector) abb3PartyTmp.getLongParty().shareOther(inputSize, ownRpc.getParty(0));
                }
                aInputDataMap.put(inputSize, aShare);
            }
        }
        abb3PartyTmp.destroy();
    }

    private void warmup(Rpc ownRpc, int taskId, ShuffleOp op) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3Party.setTaskId(taskId);
        abb3Party.setParallel(false);
        abb3Party.getRpc().synchronize();
        int inputDim = op.equals(ShuffleOp.B_SHUFFLE_COLUMN) ? elementBitLength : longEleDim;
        abb3Party.updateNum(abb3Party.getShuffleParty().getTupleNum(op, WARMUP_INPUT_SIZE, WARMUP_INPUT_SIZE, inputDim), 0);
        // 初始化协议
        LOGGER.info("(warmup) {} init", abb3Party.ownParty().getPartyName());
        abb3Party.init();
        abb3Party.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", abb3Party.ownParty().getPartyName());
        runOp(abb3Party, op, WARMUP_INPUT_SIZE);
        abb3Party.getRpc().synchronize();
        abb3Party.getRpc().reset();
        LOGGER.info("(warmup) {} finish", abb3Party.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize, ShuffleOp op,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: op:{}, inputSize = {}, bitLength = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), op.name(), inputSize, elementBitLength, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3Party.setTaskId(taskId);
        abb3Party.setParallel(parallel);
        // 启动测试
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
        runOp(abb3Party, op, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = abb3Party.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = abb3Party.getRpc().getPayloadByteLength();
        long ptoSendByteLength = abb3Party.getRpc().getSendByteLength();
        String info = op.name()
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

    private void runOp(Abb3Party abb3Party, ShuffleOp op, int inputSize) throws MpcAbortException {
        TripletZ2Vector[] dataShareB = bInputDataMap.get(inputSize);
        TripletLongVector[] dataShareA = aInputDataMap.get(inputSize);
        switch (op) {
            case A_SHUFFLE:
                abb3Party.getShuffleParty().shuffle(dataShareA);
                break;
            case B_SHUFFLE_COLUMN:
                abb3Party.getShuffleParty().shuffleColumn(dataShareB);
                break;
            default:
                throw new IllegalArgumentException("Unsupported op: " + op.name());
        }
        abb3Party.checkUnverified();
    }

    private BitVector[] genBinaryInputData(int inputSize) throws MpcAbortException {
        return IntStream.range(0, elementBitLength)
            .mapToObj(i -> BitVectorFactory.createRandom(inputSize, secureRandom))
            .toArray(BitVector[]::new);
    }

    private LongVector[] genLongInputData(int inputSize) {
        return IntStream.range(0, longEleDim).mapToObj(i -> LongVector.createRandom(inputSize, secureRandom))
            .toArray(LongVector[]::new);
    }
}
