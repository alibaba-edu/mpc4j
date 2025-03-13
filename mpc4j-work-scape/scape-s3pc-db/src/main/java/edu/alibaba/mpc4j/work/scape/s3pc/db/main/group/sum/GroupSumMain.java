package edu.alibaba.mpc4j.work.scape.s3pc.db.main.group.sum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam.GroupOp;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumParty;
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
public class GroupSumMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupSumMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "GROUP_SUM";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "group_pto_name";

    /**
     * warmup set size
     */
    private static final int WARMUP_INPUT_SIZE = 1 << 10;
    /**
     * input array num
     */
    private final int inputDim;
    /**
     * input sizes
     */
    private final int[] inputSizes;
    /**
     * config
     */
    private final GroupSumConfig config;
    /**
     * arithmetic input data
     */
    private HashMap<Integer, TripletLongVector[][]> aInputDataMap;

    public GroupSumMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        inputDim = PropertiesUtils.readInt(properties, "input_dim");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        // read permutation config
        LOGGER.info("{} read group sum config", ownRpc.ownParty().getPartyName());
        config = GroupSumConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + inputDim
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
        for (int size : inputSizes) {
            runOneTest(parallel, ownRpc, taskId, size, printWriter);
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
        inputSizesList.sort(Integer::compareTo);
        for (int inputSize : inputSizesList) {
            TripletLongVector[][] share = new TripletLongVector[2][];
            share[0] = new TripletLongVector[inputDim];
            if (ownRpc.ownParty().getPartyId() == 0) {
                LongVector[][] aInputData = genLongInputData(inputSize);
                for (int i = 0; i < inputDim; i++) {
                    share[0][i] = (TripletLongVector) abb3PartyTmp.getLongParty().shareOwn(aInputData[0][i]);
                }
                share[1] = new TripletLongVector[]{(TripletLongVector) abb3PartyTmp.getLongParty().shareOwn(aInputData[1][0])};
                aInputDataMap.put(inputSize, share);
            } else {
                for (int i = 0; i < inputDim; i++) {
                    share[0][i] = (TripletLongVector) abb3PartyTmp.getLongParty().shareOther(inputSize, ownRpc.getParty(0));
                }
                share[1] = new TripletLongVector[]{(TripletLongVector) abb3PartyTmp.getLongParty()
                    .shareOther(inputSize, ownRpc.getParty(0))};
                aInputDataMap.put(inputSize, share);
            }
        }
        abb3PartyTmp.destroy();
    }

    private void warmup(Rpc ownRpc, int taskId) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        GroupSumParty groupSumParty = GroupSumFactory.createParty(abb3Party, config);
        groupSumParty.setTaskId(taskId);
        groupSumParty.setParallel(false);
        groupSumParty.getRpc().synchronize();
        groupSumParty.setUsage(new GroupFnParam(GroupOp.SUM, inputDim, WARMUP_INPUT_SIZE));
        // 初始化协议
        LOGGER.info("(warmup) {} init", groupSumParty.ownParty().getPartyName());
        groupSumParty.init();
        groupSumParty.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", groupSumParty.ownParty().getPartyName());
        runOp(groupSumParty, WARMUP_INPUT_SIZE);
        groupSumParty.getRpc().synchronize();
        groupSumParty.getRpc().reset();
        LOGGER.info("(warmup) {} finish", groupSumParty.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: inputSize = {}, inputDim = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), inputSize, inputDim, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        GroupSumParty groupSumParty = GroupSumFactory.createParty(abb3Party, config);
        groupSumParty.setTaskId(taskId);
        groupSumParty.setParallel(parallel);
        // 启动测试
        groupSumParty.setUsage(new GroupFnParam(GroupOp.SUM, inputDim, inputSize));
        groupSumParty.getRpc().synchronize();
        groupSumParty.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", groupSumParty.ownParty().getPartyName());
        stopWatch.start();
        groupSumParty.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = groupSumParty.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = groupSumParty.getRpc().getPayloadByteLength();
        long initSendByteLength = groupSumParty.getRpc().getSendByteLength();
        groupSumParty.getRpc().synchronize();
        groupSumParty.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", groupSumParty.ownParty().getPartyName());
        stopWatch.start();
        runOp(groupSumParty, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = groupSumParty.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = groupSumParty.getRpc().getPayloadByteLength();
        long ptoSendByteLength = groupSumParty.getRpc().getSendByteLength();
        String info = groupSumParty.ownParty().getPartyId()
            + "\t" + inputSize
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        groupSumParty.getRpc().synchronize();
        groupSumParty.getRpc().reset();
        LOGGER.info("{} finish", groupSumParty.ownParty().getPartyName());
    }

    private void runOp(GroupSumParty groupSumParty, int inputSize) throws MpcAbortException {
        TripletLongVector[][] dataShareA = aInputDataMap.get(inputSize);
        groupSumParty.groupSum(dataShareA[0], dataShareA[1][0]);
        groupSumParty.getAbb3Party().checkUnverified();
    }

    private LongVector[][] genLongInputData(int inputSize) {
        LongVector[] value = IntStream.range(0, inputDim)
            .mapToObj(i -> LongVector.createRandom(inputSize, secureRandom))
            .toArray(LongVector[]::new);
        LongVector flag = LongVector.create(IntStream.range(0, inputSize).mapToLong(i -> secureRandom.nextBoolean() ? 1 : 0).toArray());
        flag.setElements(LongVector.createZeros(1), 0, 0, 1);
        return new LongVector[][]{value, new LongVector[]{flag}};
    }
}
