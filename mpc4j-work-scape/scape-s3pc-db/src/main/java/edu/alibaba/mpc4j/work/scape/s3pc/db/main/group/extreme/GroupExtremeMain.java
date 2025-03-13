package edu.alibaba.mpc4j.work.scape.s3pc.db.main.group.extreme;

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
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam.GroupOp;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory.ExtremeType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeParty;
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
 * group extreme main
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class GroupExtremeMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupExtremeMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "GROUP_EXTREME";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "group_pto_name";

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
     * config
     */
    private final GroupExtremeConfig config;
    /**
     * binary input data
     */
    private HashMap<Integer, TripletZ2Vector[][]> bInputDataMap;

    public GroupExtremeMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        elementBitLength = PropertiesUtils.readInt(properties, "element_byte_length");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        // read permutation config
        LOGGER.info("{} read pgSort config", ownRpc.ownParty().getPartyName());
        config = GroupExtremeConfigUtils.createConfig(properties);
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
                        IntStream.range(0, 1).map(i -> inputSize).toArray(), ownRpc.getParty(0)),
                });
            }
        }
        abb3PartyTmp.destroy();
    }

    private void warmup(Rpc ownRpc, int taskId) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        GroupExtremeParty groupExtremeParty = GroupExtremeFactory.createParty(abb3Party, config);
        groupExtremeParty.setTaskId(taskId);
        groupExtremeParty.setParallel(false);
        groupExtremeParty.getRpc().synchronize();
        groupExtremeParty.setUsage(new GroupFnParam(GroupOp.EXTREME, elementBitLength, WARMUP_INPUT_SIZE));
        // 初始化协议
        LOGGER.info("(warmup) {} init", groupExtremeParty.ownParty().getPartyName());
        groupExtremeParty.init();
        groupExtremeParty.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", groupExtremeParty.ownParty().getPartyName());
        runOp(groupExtremeParty, WARMUP_INPUT_SIZE);
        groupExtremeParty.getRpc().synchronize();
        groupExtremeParty.getRpc().reset();
        LOGGER.info("(warmup) {} finish", groupExtremeParty.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: inputSize = {}, bitLength = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), inputSize, elementBitLength, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        GroupExtremeParty groupExtremeParty = GroupExtremeFactory.createParty(abb3Party, config);
        groupExtremeParty.setTaskId(taskId);
        groupExtremeParty.setParallel(parallel);
        // 启动测试
        groupExtremeParty.setUsage(new GroupFnParam(GroupOp.EXTREME, elementBitLength, inputSize));
        groupExtremeParty.getRpc().synchronize();
        groupExtremeParty.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", groupExtremeParty.ownParty().getPartyName());
        stopWatch.start();
        groupExtremeParty.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = groupExtremeParty.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = groupExtremeParty.getRpc().getPayloadByteLength();
        long initSendByteLength = groupExtremeParty.getRpc().getSendByteLength();
        groupExtremeParty.getRpc().synchronize();
        groupExtremeParty.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", groupExtremeParty.ownParty().getPartyName());
        stopWatch.start();
        runOp(groupExtremeParty, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = groupExtremeParty.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = groupExtremeParty.getRpc().getPayloadByteLength();
        long ptoSendByteLength = groupExtremeParty.getRpc().getSendByteLength();
        String info = groupExtremeParty.ownParty().getPartyId()
            + "\t" + inputSize
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        groupExtremeParty.getRpc().synchronize();
        groupExtremeParty.getRpc().reset();
        LOGGER.info("{} finish", groupExtremeParty.ownParty().getPartyName());
    }

    private void runOp(GroupExtremeParty groupExtremeParty, int inputSize) throws MpcAbortException {
        TripletZ2Vector[][] dataShareB = bInputDataMap.get(inputSize);
        groupExtremeParty.groupExtreme(dataShareB[0], dataShareB[1][0], ExtremeType.MAX);
        groupExtremeParty.getAbb3Party().checkUnverified();
    }

    private BitVector[][] genBinaryInputData(int inputSize) {
        BitVector[] data = IntStream.range(0, elementBitLength)
            .mapToObj(i -> BitVectorFactory.createRandom(inputSize, secureRandom))
            .toArray(BitVector[]::new);
        BitVector groupFlag = BitVectorFactory.createRandom(inputSize, secureRandom);
        groupFlag.set(0, false);
        return new BitVector[][]{data, new BitVector[]{groupFlag}};
    }
}
