package edu.alibaba.mpc4j.work.scape.s3pc.opf.main.pgsort;

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
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
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
 * pgsort main
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class PgSortMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgSortMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "PG_SORT";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "pgsort_pto_name";
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
    private final PgSortOp[] ops;
    /**
     * config
     */
    private final PgSortConfig config;
    /**
     * binary input data
     */
    private HashMap<Integer, TripletZ2Vector[]> bInputDataMap;
    /**
     * arithmetic input data
     */
    private HashMap<Integer, TripletLongVector> aInputDataMap;

    public PgSortMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        elementBitLength = PropertiesUtils.readInt(properties, "element_byte_length");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        String[] opStrings = PropertiesUtils.readTrimStringArray(properties, "op");
        ops = Arrays.stream(opStrings)
            .map(PgSortOp::valueOf)
            .toArray(PgSortOp[]::new);
        // read permutation config
        LOGGER.info("{} read pgSort config", ownRpc.ownParty().getPartyName());
        config = PgSortConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getSortType().name()
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
        for (PgSortOp op : ops) {
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
                LongVector aInputData = genLongInputData(inputSize);
                bInputDataMap.put(inputSize, abb3PartyTmp.getZ2cParty().shareOwn(bInputData));
                aInputDataMap.put(inputSize, (TripletLongVector) abb3PartyTmp.getLongParty().shareOwn(aInputData));
            }
        } else {
            for (int inputSize : inputSizesList) {
                bInputDataMap.put(inputSize, abb3PartyTmp.getZ2cParty().shareOther(
                    IntStream.range(0, elementBitLength).map(i -> inputSize).toArray(), ownRpc.getParty(0)));
                aInputDataMap.put(inputSize, (TripletLongVector) abb3PartyTmp.getLongParty().shareOther(
                    inputSize, ownRpc.getParty(0)));
            }
        }
        abb3PartyTmp.destroy();
    }

    private void warmup(Rpc ownRpc, int taskId, PgSortOp op) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        PgSortParty pgSortParty = PgSortFactory.createParty(abb3Party, config);
        pgSortParty.setTaskId(taskId);
        pgSortParty.setParallel(false);
        pgSortParty.getRpc().synchronize();
        int[] inputDim = op.equals(PgSortOp.SORT_B) || op.equals(PgSortOp.SORT_PERMUTE_B) ? new int[]{elementBitLength} : new int[]{64};
        pgSortParty.setUsage(new PgSortFnParam(op, WARMUP_INPUT_SIZE, inputDim));
        // 初始化协议
        LOGGER.info("(warmup) {} init", pgSortParty.ownParty().getPartyName());
        pgSortParty.init();
        pgSortParty.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pgSortParty.ownParty().getPartyName());
        runOp(pgSortParty, op, WARMUP_INPUT_SIZE);
        pgSortParty.getRpc().synchronize();
        pgSortParty.getRpc().reset();
        LOGGER.info("(warmup) {} finish", pgSortParty.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize, PgSortOp op,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: op:{}, inputSize = {}, bitLength = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), op.name(), inputSize, elementBitLength, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        PgSortParty pgSortParty = PgSortFactory.createParty(abb3Party, config);
        pgSortParty.setTaskId(taskId);
        pgSortParty.setParallel(parallel);
        // 启动测试
        int[] inputDim = op.equals(PgSortOp.SORT_B) || op.equals(PgSortOp.SORT_PERMUTE_B) ? new int[]{elementBitLength} : new int[]{64};
        pgSortParty.setUsage(new PgSortFnParam(op, inputSize, inputDim));
        pgSortParty.getRpc().synchronize();
        pgSortParty.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pgSortParty.ownParty().getPartyName());
        stopWatch.start();
        pgSortParty.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = pgSortParty.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pgSortParty.getRpc().getPayloadByteLength();
        long initSendByteLength = pgSortParty.getRpc().getSendByteLength();
        pgSortParty.getRpc().synchronize();
        pgSortParty.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pgSortParty.ownParty().getPartyName());
        stopWatch.start();
        runOp(pgSortParty, op, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = pgSortParty.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pgSortParty.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pgSortParty.getRpc().getSendByteLength();
        String info = op.name()
            + "\t" + pgSortParty.ownParty().getPartyId()
            + "\t" + inputSize
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        pgSortParty.getRpc().synchronize();
        pgSortParty.getRpc().reset();
        LOGGER.info("{} finish", pgSortParty.ownParty().getPartyName());
    }

    private void runOp(PgSortParty pgSortParty, PgSortOp op, int inputSize) throws MpcAbortException {
        TripletZ2Vector[] dataShareB = bInputDataMap.get(inputSize);
        TripletLongVector dataShareA = aInputDataMap.get(inputSize);
        switch (op) {
            case SORT_A:
                pgSortParty.perGen4MultiDim(new TripletLongVector[]{dataShareA}, new int[]{64});
                break;
            case SORT_PERMUTE_A:
                TripletZ2Vector[] res = new TripletZ2Vector[64];
                pgSortParty.perGen4MultiDimWithOrigin(new TripletLongVector[]{dataShareA}, new int[]{64}, res);
                break;
            case SORT_B:
                pgSortParty.perGen(dataShareB);
                break;
            case SORT_PERMUTE_B:
                pgSortParty.perGenAndSortOrigin(dataShareB);
                break;
            default:
                throw new IllegalArgumentException("Unsupported op: " + op.name());
        }
        pgSortParty.getAbb3Party().checkUnverified();
    }

    private BitVector[] genBinaryInputData(int inputSize) {
        return IntStream.range(0, elementBitLength)
            .mapToObj(i -> BitVectorFactory.createRandom(inputSize, secureRandom))
            .toArray(BitVector[]::new);
    }

    private LongVector genLongInputData(int inputSize) {
        return LongVector.createRandom(inputSize, secureRandom);
    }
}
