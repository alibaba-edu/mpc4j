package edu.alibaba.mpc4j.work.scape.s3pc.db.main.orderby;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByParty;
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
import java.util.stream.LongStream;

/**
 * order-by main
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class OrderByMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderByMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "ORDER_BY";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "pto_name";
    /**
     * warmup set size
     */
    private static final int WARMUP_INPUT_SIZE = 1 << 10;
    /**
     * key dim
     */
    private final int aKeyDim;
    /**
     * payload dim
     */
    private final int payloadDim;
    /**
     * input sizes
     */
    private final int[] inputSizes;
    /**
     * config
     */
    private final OrderByConfig config;
    /**
     * arithmetic input data
     */
    private HashMap<Integer, TripletLongVector[]> aInputDataMap;

    public OrderByMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        aKeyDim = PropertiesUtils.readInt(properties, "key_dim");
        payloadDim = PropertiesUtils.readInt(properties, "payload_dim");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        // read permutation config
        LOGGER.info("{} read pgSort config", ownRpc.ownParty().getPartyName());
        config = OrderByConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getOrderByPtoType().name()
            + "_" + appendString
            + "_" + aKeyDim + "_" + payloadDim
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
        if(!inputSizesList.contains(WARMUP_INPUT_SIZE)){
            inputSizesList.add(WARMUP_INPUT_SIZE);
        }
        inputSizesList.sort(Integer::compareTo);
        if (ownRpc.ownParty().getPartyId() == 0) {
            for (int inputSize : inputSizesList) {
                LongVector[] aInputData = genLongInputData(inputSize);
                aInputDataMap.put(inputSize, (TripletLongVector[]) abb3PartyTmp.getLongParty().shareOwn(aInputData));
            }
        } else {
            for (int inputSize : inputSizesList) {
                aInputDataMap.put(inputSize, (TripletLongVector[]) abb3PartyTmp.getLongParty().shareOther(
                    IntStream.range(0, payloadDim + aKeyDim + 1).map(i -> inputSize).toArray(), ownRpc.getParty(0)));
            }
        }

        abb3PartyTmp.destroy();
    }

    private void warmup(Rpc ownRpc, int taskId) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        OrderByParty orderByParty = OrderByFactory.createParty(abb3Party, config);
        orderByParty.setTaskId(taskId);
        orderByParty.setParallel(false);
        orderByParty.getRpc().synchronize();
        orderByParty.setUsage(new OrderByFnParam(false, WARMUP_INPUT_SIZE, aKeyDim, aKeyDim + payloadDim + 1));
        // 初始化协议
        LOGGER.info("(warmup) {} init", orderByParty.ownParty().getPartyName());
        orderByParty.init();
        orderByParty.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", orderByParty.ownParty().getPartyName());
        runOp(orderByParty, WARMUP_INPUT_SIZE);
        orderByParty.getRpc().synchronize();
        orderByParty.getRpc().reset();
        LOGGER.info("(warmup) {} finish", orderByParty.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int inputSize,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: keyDim:{}, payloadDim = {}, inputSize = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), aKeyDim, payloadDim, inputSize, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        OrderByParty orderByParty = OrderByFactory.createParty(abb3Party, config);
        orderByParty.setTaskId(taskId);
        orderByParty.setParallel(parallel);
        // 启动测试
        orderByParty.setUsage(new OrderByFnParam(false, inputSize, aKeyDim, aKeyDim + payloadDim + 1));
        orderByParty.getRpc().synchronize();
        orderByParty.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", orderByParty.ownParty().getPartyName());
        stopWatch.start();
        orderByParty.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = orderByParty.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = orderByParty.getRpc().getPayloadByteLength();
        long initSendByteLength = orderByParty.getRpc().getSendByteLength();
        orderByParty.getRpc().synchronize();
        orderByParty.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", orderByParty.ownParty().getPartyName());
        stopWatch.start();
        runOp(orderByParty, inputSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = orderByParty.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = orderByParty.getRpc().getPayloadByteLength();
        long ptoSendByteLength = orderByParty.getRpc().getSendByteLength();
        String info = orderByParty.ownParty().getPartyId()
            + "\t" + inputSize
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        orderByParty.getRpc().synchronize();
        orderByParty.getRpc().reset();
        LOGGER.info("{} finish", orderByParty.ownParty().getPartyName());
    }

    private void runOp(OrderByParty orderByParty, int inputSize) throws MpcAbortException {
        TripletLongVector[] dataShareA = Arrays.stream(aInputDataMap.get(inputSize))
            .map(ea -> (TripletLongVector) ea.copy()).toArray(TripletLongVector[]::new);
        orderByParty.orderBy(dataShareA, IntStream.range(0, aKeyDim).toArray());
        orderByParty.getAbb3Party().checkUnverified();
        orderByParty.getAbb3Party().getLongParty().open(dataShareA);
    }

    private LongVector[] genLongInputData(int inputSize) {
        LongVector[] input = new LongVector[payloadDim + aKeyDim + 1];
        for (int i = 0; i < payloadDim + aKeyDim; i++) {
            input[i] = LongVector.createRandom(inputSize, secureRandom);
        }
        input[payloadDim + aKeyDim] = LongVector.create(LongStream.range(0, inputSize)
            .map(i -> secureRandom.nextBoolean() ? 1 : 0).toArray());
        return input;
    }
}
