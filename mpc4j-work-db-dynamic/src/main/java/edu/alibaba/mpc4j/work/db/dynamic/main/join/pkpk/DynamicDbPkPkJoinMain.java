package edu.alibaba.mpc4j.work.db.dynamic.main.join.pkpk;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake.FakeZ2TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake.FakeZ2TripleGenSender;
import edu.alibaba.mpc4j.work.db.dynamic.DynamicDbCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.DynamicDbCircuitConfig;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.JoinInputMt;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.PkPkJoinMt;
import edu.alibaba.mpc4j.work.db.dynamic.main.DynamicDbCircuitUtils;
import edu.alibaba.mpc4j.work.db.dynamic.structure.OperationEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author Feng Han
 * @date 2025/3/17
 */
public class DynamicDbPkPkJoinMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDbPkPkJoinMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "DYN_DB_PK_PK_JOIN";
    /**
     * z2 circuit config
     */
    private final DynamicDbCircuitConfig config;
    /**
     * warmup set size
     */
    private static final int WARMUP_INPUT_SIZE = 1 << 10;
    /**
     * warmup set size
     */
    private static final int WARMUP_UPDATE_NUM = 10;
    /**
     * payload dim
     */
    private final int joinKeyDim;
    /**
     * payload dim
     */
    private final int leftPayloadDim;
    /**
     * payload dim
     */
    private final int rightPayloadDim;
    /**
     * input sizes
     */
    private final int[] inputSizes;
    /**
     * input sizes
     */
    private final int[] updateNums;

    public DynamicDbPkPkJoinMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        joinKeyDim = PropertiesUtils.readInt(properties, "join_key_dim");
        leftPayloadDim = PropertiesUtils.readInt(properties, "left_payload_dim");
        rightPayloadDim = PropertiesUtils.readInt(properties, "right_payload_dim");
        inputSizes = PropertiesUtils.readLogIntArray(properties, "log_input_size");
        for (int inputSize : inputSizes) {
            MathPreconditions.checkGreaterOrEqual("joinKeyDim >= log_2(inputSizes[i])", joinKeyDim, inputSize);
        }
        IntStream.range(0, inputSizes.length).forEach(i -> inputSizes[i] = 1 << inputSizes[i]);
        updateNums = PropertiesUtils.readIntArray(properties, "update_num");
        config = DynamicDbCircuitUtils.createConfig(properties);
    }

    @Override
    public void runParty1(Rpc ownRpc, Party otherParty) throws IOException, MpcAbortException {
        runParty(ownRpc, otherParty);
    }

    @Override
    public void runParty2(Rpc ownRpc, Party otherParty) throws IOException, MpcAbortException {
        runParty(ownRpc, otherParty);
    }

    public void runParty(Rpc ownRpc, Party otherParty) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getCircuitConfig().getComparatorType().name()
            + "_" + appendString
            + "_" + ownRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tInput Size\tUpdate Num\tKey Dim\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)\tMt num";
        printWriter.println(tab);
        // 建立连接
        ownRpc.connect();
        LOGGER.info("{} ready to run", ownRpc.ownParty().getPartyName());
        // 启动测试
        int taskId = 0;
        // 预热
        warmup(ownRpc, otherParty, taskId);
        taskId++;
        // 正式测试
        for (int inputSize : inputSizes) {
            for (int updateNum : updateNums) {
                runOneTest(true, ownRpc, otherParty, taskId, inputSize, updateNum, printWriter);
                taskId++;
            }
        }
        // 断开连接
        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmup(Rpc ownRpc, Party otherParty, int taskId) throws MpcAbortException {
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
        Z2cParty party = ownRpc.ownParty().getPartyId() == 0
            ? Z2cFactory.createSender(ownRpc, otherParty, z2cConfig)
            : Z2cFactory.createReceiver(ownRpc, otherParty, z2cConfig);
        party.setTaskId(taskId);
        party.setParallel(false);
        DynamicDbCircuit circuit = new DynamicDbCircuit(config, party);
        party.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", party.ownParty().getPartyName());
        party.init();
        party.getRpc().synchronize();
        LOGGER.info("(generate data) {}", party.ownParty().getPartyName());
        PkPkJoinMt pkPkJoinMt = genInputData(party, WARMUP_INPUT_SIZE);
        UpdateMessage[] upMsg = getUpdateMsg(pkPkJoinMt, WARMUP_UPDATE_NUM);
        // 执行协议
        LOGGER.info("(warmup) {} execute", party.ownParty().getPartyName());
        runOp(circuit, pkPkJoinMt, upMsg);
        party.getRpc().synchronize();
        party.getRpc().reset();
        LOGGER.info("(warmup) {} finish", party.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, Party otherParty,
                            int taskId, int inputSize, int updateNum,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: inputSize = {}, updateNum = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), inputSize, updateNum, parallel
        );
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
        Z2cParty party = ownRpc.ownParty().getPartyId() == 0
            ? Z2cFactory.createSender(ownRpc, otherParty, z2cConfig)
            : Z2cFactory.createReceiver(ownRpc, otherParty, z2cConfig);
        party.setTaskId(taskId);
        party.setParallel(parallel);
        DynamicDbCircuit circuit = new DynamicDbCircuit(config, party);
        party.getRpc().synchronize();
        party.getRpc().reset();
        FakeZ2TripleGenSender.mtNum = 0;
        FakeZ2TripleGenReceiver.mtNum = 0;
        // 初始化协议
        LOGGER.info("{} init", party.ownParty().getPartyName());
        stopWatch.start();
        party.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        LOGGER.info("(generate data) {}", party.ownParty().getPartyName());
        PkPkJoinMt pkPkJoinMt = genInputData(party, inputSize);
        UpdateMessage[] upMsg = getUpdateMsg(pkPkJoinMt, updateNum);
        long initDataPacketNum = party.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = party.getRpc().getPayloadByteLength();
        long initSendByteLength = party.getRpc().getSendByteLength();
        party.getRpc().synchronize();
        party.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", party.ownParty().getPartyName());
        stopWatch.start();
        runOp(circuit, pkPkJoinMt, upMsg);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = party.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = party.getRpc().getPayloadByteLength();
        long ptoSendByteLength = party.getRpc().getSendByteLength();

        long mtNum = ownRpc.ownParty().getPartyId() == 0 ? FakeZ2TripleGenSender.mtNum : FakeZ2TripleGenReceiver.mtNum;
        FakeZ2TripleGenSender.mtNum = 0;
        FakeZ2TripleGenReceiver.mtNum = 0;

        String info = party.ownParty().getPartyId()
            + "\t" + inputSize + "\t" + updateNum + "\t" + joinKeyDim
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength
            + "\t" + mtNum;
        printWriter.println(info);
        // 同步
        party.getRpc().synchronize();
        party.getRpc().reset();
        LOGGER.info("{} finish", party.ownParty().getPartyName());
    }

    private void runOp(DynamicDbCircuit circuit, PkPkJoinMt joinMt, UpdateMessage[] updateMessages) throws MpcAbortException {
        for (UpdateMessage updateMessage : updateMessages) {
            circuit.twoTabUpdate(updateMessage, joinMt, true);
        }
    }

    private PkPkJoinMt genInputData(MpcZ2cParty z2cParty, int inputSize) {
        int bitReq = LongUtils.ceilLog2(inputSize);
        MathPreconditions.checkGreaterOrEqual("joinKeyDim >= bitReq", joinKeyDim, bitReq);
        BitVector[] index = Z2VectorUtils.getBinaryIndex(inputSize);

        BitVector[] leftData = new BitVector[joinKeyDim + leftPayloadDim + 1];
        BitVector[] rightData = new BitVector[joinKeyDim + rightPayloadDim + 1];
        System.arraycopy(index, 0, leftData, 0, index.length);
        for (int i = 0; i < index.length; i++) {
            rightData[i] = index[i].copy();
        }
        for (int i = index.length; i < leftData.length; i++) {
            leftData[i] = BitVectorFactory.createZeros(inputSize);
        }
        for (int i = index.length; i < rightData.length; i++) {
            rightData[i] = BitVectorFactory.createZeros(inputSize);
        }
        BitVector[] joinRes = new BitVector[joinKeyDim + leftPayloadDim + rightPayloadDim + 1];
        for (int i = 0; i < index.length; i++) {
            joinRes[i] = index[i].copy();
        }
        for (int i = index.length; i < joinRes.length; i++) {
            joinRes[i] = BitVectorFactory.createZeros(inputSize);
        }
        MpcZ2Vector[] leftShare = z2cParty.setPublicValues(leftData);
        MpcZ2Vector[] rightShare = z2cParty.setPublicValues(rightData);
        MpcZ2Vector[] joinResShare = z2cParty.setPublicValues(joinRes);

        return new PkPkJoinMt(joinResShare, joinResShare.length - 1, false,
            IntStream.range(0, joinKeyDim).toArray(),
            IntStream.range(joinKeyDim, joinKeyDim + leftPayloadDim).toArray(),
            IntStream.range(joinKeyDim + leftPayloadDim, joinKeyDim + leftPayloadDim + rightPayloadDim).toArray(),
            new JoinInputMt(leftShare, leftShare.length - 1, IntStream.range(0, joinKeyDim).toArray()),
            new JoinInputMt(rightShare, rightShare.length - 1, IntStream.range(0, joinKeyDim).toArray())
        );
    }

    private UpdateMessage[] getUpdateMsg(PkPkJoinMt joinMt, int updateNum) {
        UpdateMessage[] updateMsg = new UpdateMessage[2 * updateNum];
        MpcZ2Vector[] left = joinMt.getLeftMt().getData();
        for (int i = 0; i < updateNum; i++) {
            MpcZ2Vector[] updateData = new MpcZ2Vector[left.length];
            for (int j = 0; j < left.length; j++) {
                updateData[j] = left[j].reduceShiftRight(i);
                updateData[j].reduce(1);
            }
            updateMsg[i * 2] = new UpdateMessage(OperationEnum.DELETE, updateData);
            updateMsg[i * 2 + 1] = new UpdateMessage(OperationEnum.DELETE, updateData);
        }
        return updateMsg;
    }

}
