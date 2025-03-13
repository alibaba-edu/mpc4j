package edu.alibaba.mpc4j.work.scape.s3pc.opf.main.permutation;

import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
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
 * permutation main
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class PermutationMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutationMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "PERMUTATION";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "permute_pto_name";
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
    private final PermuteOp[] ops;
    /**
     * config
     */
    private final PermuteConfig config;
    /**
     * binary input data
     */
    private HashMap<Integer, TripletZ2Vector[][]> bInputDataMap;
    /**
     * arithmetic input data
     */
    private HashMap<Integer, TripletLongVector[]> aInputDataMap;

    public PermutationMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        elementBitLength = PropertiesUtils.readInt(properties, "element_byte_length");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        String[] opStrings = PropertiesUtils.readTrimStringArray(properties, "op");
        ops = Arrays.stream(opStrings)
            .map(PermuteOp::valueOf)
            .toArray(PermuteOp[]::new);
        // read permutation config
        LOGGER.info("{} read permutation config", ownRpc.ownParty().getPartyName());
        config = PermutationConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPermuteType().name()
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
        for (PermuteOp op : ops) {
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
            for(int inputSize : inputSizesList){
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
        }else{
            for(int inputSize : inputSizesList){
                int parDim = LongUtils.ceilLog2(inputSize);
                bInputDataMap.put(inputSize, new TripletZ2Vector[][]{
                    abb3PartyTmp.getZ2cParty().shareOther(
                        IntStream.range(0, parDim).map(i -> inputSize).toArray(), ownRpc.getParty(0)),
                    abb3PartyTmp.getZ2cParty().shareOther(
                        IntStream.range(0, elementBitLength).map(i -> inputSize).toArray(), ownRpc.getParty(0)),
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

    private void warmup(Rpc ownRpc, int taskId, PermuteOp op) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        PermuteParty permuteParty = PermuteFactory.createParty(abb3Party, config);
        permuteParty.setTaskId(taskId);
        permuteParty.setParallel(false);
        permuteParty.getRpc().synchronize();
        int inputDim = op.equals(PermuteOp.COMPOSE_B_B) || op.equals(PermuteOp.APPLY_INV_B_B) ? elementBitLength : 1;
        permuteParty.setUsage(new PermuteFnParam(op, WARMUP_INPUT_SIZE, inputDim, LongUtils.ceilLog2(WARMUP_INPUT_SIZE)));
        // 初始化协议
        LOGGER.info("(warmup) {} init", permuteParty.ownParty().getPartyName());
        permuteParty.init();
        permuteParty.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", permuteParty.ownParty().getPartyName());
        runOp(permuteParty, op, WARMUP_INPUT_SIZE);
        permuteParty.getRpc().synchronize();
        permuteParty.getRpc().reset();
        LOGGER.info("(warmup) {} finish", permuteParty.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize, PermuteOp op,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: op:{}, inputSize = {}, bitLength = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), op.name(), inputSize, elementBitLength, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        PermuteParty permuteParty = PermuteFactory.createParty(abb3Party, config);
        permuteParty.setTaskId(taskId);
        permuteParty.setParallel(parallel);
        // 启动测试
        int inputDim = op.equals(PermuteOp.COMPOSE_B_B) || op.equals(PermuteOp.APPLY_INV_B_B) ? elementBitLength : 1;
        permuteParty.setUsage(new PermuteFnParam(op, inputSize, inputDim, LongUtils.ceilLog2(inputSize)));
        permuteParty.getRpc().synchronize();
        permuteParty.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", permuteParty.ownParty().getPartyName());
        stopWatch.start();
        permuteParty.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = permuteParty.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = permuteParty.getRpc().getPayloadByteLength();
        long initSendByteLength = permuteParty.getRpc().getSendByteLength();
        permuteParty.getRpc().synchronize();
        permuteParty.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", permuteParty.ownParty().getPartyName());
        stopWatch.start();
        runOp(permuteParty, op, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = permuteParty.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = permuteParty.getRpc().getPayloadByteLength();
        long ptoSendByteLength = permuteParty.getRpc().getSendByteLength();
        String info = op.name()
            + "\t" + permuteParty.ownParty().getPartyId()
            + "\t" + inputSize
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        permuteParty.getRpc().synchronize();
        permuteParty.getRpc().reset();
        LOGGER.info("{} finish", permuteParty.ownParty().getPartyName());
    }

    private void runOp(PermuteParty permuteParty, PermuteOp op, int inputSize) throws MpcAbortException {
        TripletZ2Vector[] paiShareB = bInputDataMap.get(inputSize)[0];
        TripletZ2Vector[] dataShareB= bInputDataMap.get(inputSize)[1];
        TripletLongVector paiShareA = aInputDataMap.get(inputSize)[0];
        TripletLongVector dataShareA= aInputDataMap.get(inputSize)[1];
        switch (op) {
            case COMPOSE_B_B:
                permuteParty.composePermutation(paiShareB, dataShareB);
                break;
            case APPLY_INV_B_B:
                permuteParty.applyInvPermutation(paiShareB, dataShareB);
                break;
            case COMPOSE_A_A:
                permuteParty.composePermutation(paiShareA, dataShareA);
                break;
            case APPLY_INV_A_A:
                permuteParty.applyInvPermutation(paiShareA, dataShareA);
                break;
            case APPLY_INV_A_B:
                permuteParty.applyInvPermutation(paiShareA, dataShareB);
                break;
            default:
                throw new IllegalArgumentException("Unsupported op: " + op.name());
        }
        permuteParty.getAbb3Party().checkUnverified();
    }

    private BitVector[][] genBinaryInputData(int inputSize) throws MpcAbortException {
        BitVector[] perm = Z2VectorUtils.getBinaryIndex(inputSize);
        int[] rands = secureRandom.ints(inputSize, 0, Integer.MAX_VALUE).toArray();
        int[] pai = ShuffleUtils.permutationGeneration(rands);
        perm = ShuffleUtils.applyPermutationToRows(perm, pai);
        BitVector[] data = IntStream.range(0, elementBitLength)
            .mapToObj(i -> BitVectorFactory.createRandom(inputSize, secureRandom))
            .toArray(BitVector[]::new);
        return new BitVector[][]{perm, data};
    }

    private LongVector[] genLongInputData(int inputSize) {
        int[] rands = secureRandom.ints(inputSize, 0, Integer.MAX_VALUE).toArray();
        int[] pai = ShuffleUtils.permutationGeneration(rands);
        LongVector perm = LongVector.create(Arrays.stream(pai).mapToLong(i -> i).toArray());
        LongVector data = LongVector.createRandom(inputSize, secureRandom);
        return new LongVector[]{perm, data};
    }

}
