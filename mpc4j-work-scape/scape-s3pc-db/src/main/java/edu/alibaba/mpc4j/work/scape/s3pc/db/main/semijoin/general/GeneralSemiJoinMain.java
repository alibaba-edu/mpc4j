package edu.alibaba.mpc4j.work.scape.s3pc.db.main.semijoin.general;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.SemiJoinFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.JoinInputUtils;
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
 * general join main
 *
 * @author Feng Han
 * @date 2025/3/1
 */
public class GeneralSemiJoinMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralSemiJoinMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "GENERAL_SEMI_JOIN";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "join_pto_name";

    /**
     * warmup set size
     */
    private static final int WARMUP_INPUT_SIZE = 1 << 10;
    /**
     * whether the input is sorted in the order of the key attribute
     */
    private final boolean isSorted;
    /**
     * input sizes
     */
    private final int[] inputSizes;
    /**
     * config
     */
    private final GeneralSemiJoinConfig config;
    /**
     * arithmetic input data
     */
    private HashMap<Integer, TripletLongVector[][]> aInputDataMap;

    public GeneralSemiJoinMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        isSorted = PropertiesUtils.readBoolean(properties, "is_sorted");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        // read permutation config
        LOGGER.info("{} read pgSort config", ownRpc.ownParty().getPartyName());
        config = GeneralSemiJoinConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getGeneralSemiJoinPtoType().name()
            + "_" + appendString
            + "_" + isSorted
            + "_" + ownRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tInput Size\tIs Parallel\tThread Num"
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
        // 预热
        warmup(ownRpc, taskId);
        taskId++;
        // 正式测试
        for (int inputSize : inputSizes) {
            runOneTest(parallel, ownRpc, taskId, inputSize, printWriter);
            taskId++;
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
        aInputDataMap = new HashMap<>();
        List<Integer> inputSizesList = new java.util.ArrayList<>(Arrays.stream(inputSizes).boxed().toList());
        inputSizesList.add(WARMUP_INPUT_SIZE);
        for (int inputSize : inputSizesList) {
            TripletLongVector[][] share = new TripletLongVector[2][2];
            if (ownRpc.ownParty().getPartyId() == 0) {
                LongVector[][] aInputData = genLongInputData(inputSize);
                for (int tabId = 0; tabId < 2; tabId++) {
                    for (int i = 0; i < 2; i++) {
                        share[tabId][i] = (TripletLongVector) abb3PartyTmp.getLongParty().shareOwn(aInputData[0][i]);
                    }
                }
                aInputDataMap.put(inputSize, share);
            } else {
                for (int tabId = 0; tabId < 2; tabId++) {
                    for (int i = 0; i < 2; i++) {
                        share[tabId][i] = (TripletLongVector) abb3PartyTmp.getLongParty().shareOther(inputSize, ownRpc.getParty(0));
                    }
                }
                aInputDataMap.put(inputSize, share);
            }
        }
        abb3PartyTmp.destroy();
    }

    private void warmup(Rpc ownRpc, int taskId) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        GeneralSemiJoinParty joinParty = GeneralSemiJoinFactory.createParty(abb3Party, config);
        joinParty.setTaskId(taskId);
        joinParty.setParallel(false);
        joinParty.getRpc().synchronize();
        joinParty.setUsage(new SemiJoinFnParam(WARMUP_INPUT_SIZE, WARMUP_INPUT_SIZE, 1, isSorted));
        // 初始化协议
        LOGGER.info("(warmup) {} init", joinParty.ownParty().getPartyName());
        joinParty.init();
        joinParty.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", joinParty.ownParty().getPartyName());
        runOp(joinParty, WARMUP_INPUT_SIZE);
        joinParty.getRpc().synchronize();
        joinParty.getRpc().reset();
        LOGGER.info("(warmup) {} finish", joinParty.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: inputSize = {}, isSorted = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), inputSize, isSorted, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        GeneralSemiJoinParty joinParty = GeneralSemiJoinFactory.createParty(abb3Party, config);
        joinParty.setTaskId(taskId);
        joinParty.setParallel(parallel);
        // 启动测试
        joinParty.setUsage(new SemiJoinFnParam(inputSize, inputSize, 1, isSorted));
        joinParty.getRpc().synchronize();
        joinParty.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", joinParty.ownParty().getPartyName());
        stopWatch.start();
        joinParty.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = joinParty.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = joinParty.getRpc().getPayloadByteLength();
        long initSendByteLength = joinParty.getRpc().getSendByteLength();
        joinParty.getRpc().synchronize();
        joinParty.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", joinParty.ownParty().getPartyName());
        stopWatch.start();
        runOp(joinParty, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = joinParty.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = joinParty.getRpc().getPayloadByteLength();
        long ptoSendByteLength = joinParty.getRpc().getSendByteLength();
        String info = joinParty.ownParty().getPartyId()
            + "\t" + inputSize
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        joinParty.getRpc().synchronize();
        joinParty.getRpc().reset();
        LOGGER.info("{} finish", joinParty.ownParty().getPartyName());
    }

    private void runOp(GeneralSemiJoinParty joinParty, int inputSize) throws MpcAbortException {
        TripletLongVector[][] dataShareB = aInputDataMap.get(inputSize);
        int[] keyIndex = new int[]{0};
        joinParty.semiJoin(dataShareB[0], dataShareB[1], keyIndex, keyIndex, isSorted);
        joinParty.getAbb3Party().checkUnverified();
    }

    private LongVector[][] genLongInputData(int inputSize) {
        // this freq bound has no effect on performance
        int freqBound = 4;
        HashMap<Long, List<Integer>> leftHash = new HashMap<>(inputSize), rightHash = new HashMap<>(inputSize);
        long[][] leftPlain = JoinInputUtils.getInput4GeneralJoin(inputSize, 0, freqBound, leftHash);
        long[][] rightPlain = JoinInputUtils.getInput4GeneralJoin(inputSize, 0, freqBound, rightHash);
        LongVector[] leftPlainVec = Arrays.stream(leftPlain).map(LongVector::create).toArray(LongVector[]::new);
        LongVector[] rightPlainVec = Arrays.stream(rightPlain).map(LongVector::create).toArray(LongVector[]::new);
        return new LongVector[][]{leftPlainVec, rightPlainVec};
    }
}
