package edu.alibaba.mpc4j.work.db.dynamic.main3;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake.FakeZ2TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake.FakeZ2TripleGenSender;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.work.db.dynamic.DynamicDbCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.DynamicDbCircuitConfig;
import edu.alibaba.mpc4j.work.db.dynamic.group.GroupByMt;
import edu.alibaba.mpc4j.work.db.dynamic.main.DynamicDbCircuitUtils;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AggregateEnum;
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
 * shortcut group operation start from no data
 */
public class DynamicDbGroupMain3p extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDbGroupMain3p.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "DYN_DB_GROUP";
    /**
     * z2 circuit config
     */
    private final DynamicDbCircuitConfig config;
    /**
     * warmup init set size
     */
    private static final int WARMUP_INIT_NUM = 1024;
    /**
     * warmup set size
     */
    private static final int WARMUP_UPDATE_NUM = 16;
    /**
     * aggregate type
     */
    private final AggregateEnum aggType;
    /**
     * start update from empty view or not
     */
    private final boolean startFromEmpty;
    /**
     * is output table or not
     */
    private final boolean isOutputTable;
    /**
     * key dim
     */
    private final int keyDim;
    /**
     * payload dim
     */
    private final int payloadDim;
    /**
     * initialized view sizes
     */
    private final int[] initNums;
    /**
     * input sizes
     */
    private final int[] updateNums;

    public DynamicDbGroupMain3p(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        aggType = MainPtoConfigUtils.readEnum(AggregateEnum.class, properties, "agg_type");
        isOutputTable = PropertiesUtils.readBoolean(properties, "is_output_table");
        startFromEmpty = PropertiesUtils.readBoolean(properties, "start_from_empty");
        keyDim = PropertiesUtils.readInt(properties, "key_dim");
        payloadDim = PropertiesUtils.readInt(properties, "payload_dim");
        updateNums = PropertiesUtils.readLogIntArray(properties, "log_update_size");
        for (int i = 0; i < updateNums.length; i++) {
            updateNums[i] = 1 << updateNums[i];
        }
        if(startFromEmpty){
            initNums = IntStream.range(0, updateNums.length).map(i -> 0).toArray();
        }else{
            initNums = PropertiesUtils.readLogIntArray(properties, "log_init_size");
            for (int i = 0; i < initNums.length; i++) {
                MathPreconditions.checkGreater("keyDim > log_init_size", keyDim, initNums[i]);
                initNums[i] = 1 << initNums[i];
            }
        }
        MathPreconditions.checkEqual("initNums.length", "updateNums.length", initNums.length, updateNums.length);
        config = DynamicDbCircuitUtils.createConfig(properties);
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());

        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getCircuitConfig().getComparatorType().name()
            + "_" + appendString
            + "_" + ownRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + "_3p.output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        String tab = "Party ID\tInit Num\tUpdate Num\tKey Dim\tPayload Dim\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)\tMt num";
        printWriter.println(tab);

        ownRpc.connect();
        LOGGER.info("{} ready to run", ownRpc.ownParty().getPartyName());

        int taskId = 0;

        warmup(ownRpc, taskId);
        taskId++;

        for (int i = 0; i < updateNums.length; i++) {
            int updateNum = updateNums[i];
            int initNum = initNums[i];
            runOneTest(true, ownRpc, taskId, initNum, updateNum, printWriter);
            taskId++;
        }


        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmup(Rpc ownRpc, int taskId) throws MpcAbortException {
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        TripletZ2cParty party = abb3Party.getZ2cParty();
        abb3Party.setTaskId(taskId);
        abb3Party.setParallel(false);
        DynamicDbCircuit circuit = new DynamicDbCircuit(config, party);
        abb3Party.getRpc().synchronize();

        LOGGER.info("(warmup) {} init", abb3Party.ownParty().getPartyName());
        abb3Party.init();
        abb3Party.getRpc().synchronize();
        LOGGER.info("(generate data) {}", abb3Party.ownParty().getPartyName());
        GroupByMt groupByMt = genInputData(party, WARMUP_INIT_NUM);
        UpdateMessage[] upMsg = getUpdateMsg(party, WARMUP_UPDATE_NUM);

        LOGGER.info("(warmup) {} execute", abb3Party.ownParty().getPartyName());
        runOp(circuit, groupByMt, upMsg);
        abb3Party.getRpc().synchronize();
        abb3Party.getRpc().reset();
        LOGGER.info("(warmup) {} finish", abb3Party.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int initNum, int updateNum,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: updateNum = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), updateNum, parallel
        );
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        TripletZ2cParty party = abb3Party.getZ2cParty();
        abb3Party.setTaskId(taskId);
        abb3Party.setParallel(parallel);
        DynamicDbCircuit circuit = new DynamicDbCircuit(config, party);
        abb3Party.getRpc().synchronize();
        abb3Party.getRpc().reset();
        FakeZ2TripleGenSender.mtNum = 0;
        FakeZ2TripleGenReceiver.mtNum = 0;

        LOGGER.info("{} init", abb3Party.ownParty().getPartyName());
        stopWatch.start();
        abb3Party.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        LOGGER.info("(generate data) {}", abb3Party.ownParty().getPartyName());
        GroupByMt groupByMt = genInputData(party, initNum);
        UpdateMessage[] upMsg = getUpdateMsg(party, updateNum);
        long initDataPacketNum = party.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = party.getRpc().getPayloadByteLength();
        long initSendByteLength = party.getRpc().getSendByteLength();
        abb3Party.getRpc().synchronize();
        abb3Party.getRpc().reset();

        LOGGER.info("{} execute", abb3Party.ownParty().getPartyName());
        stopWatch.start();
        runOp(circuit, groupByMt, upMsg);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = abb3Party.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = abb3Party.getRpc().getPayloadByteLength();
        long ptoSendByteLength = abb3Party.getRpc().getSendByteLength();

        long mtNum = ownRpc.ownParty().getPartyId() == 0 ? FakeZ2TripleGenSender.mtNum : FakeZ2TripleGenReceiver.mtNum;
        FakeZ2TripleGenSender.mtNum = 0;
        FakeZ2TripleGenReceiver.mtNum = 0;

        String info = abb3Party.ownParty().getPartyId()
            + "\t" + initNum + "\t" + updateNum + "\t" + keyDim + "\t" + payloadDim
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength
            + "\t" + mtNum;
        printWriter.println(info);

        party.getRpc().synchronize();
        party.getRpc().reset();
        LOGGER.info("{} finish", party.ownParty().getPartyName());
    }

    private void runOp(DynamicDbCircuit circuit, GroupByMt groupMt, UpdateMessage[] updateMessages) throws MpcAbortException {
        for (UpdateMessage updateMessage : updateMessages) {
            circuit.oneTabUpdate(updateMessage, groupMt);
        }
    }

    private GroupByMt genInputData(MpcZ2cParty z2cParty, int initNum) {
        MpcZ2Vector[] share;
        if (startFromEmpty) {
            share = IntStream.range(0, keyDim + payloadDim + 1)
                .mapToObj(i -> z2cParty.createEmpty(false))
                .toArray(MpcZ2Vector[]::new);
        }else{
            share = new MpcZ2Vector[keyDim + payloadDim + 1];
            BitVector[] orderKeys = Z2VectorUtils.getBinaryIndex(initNum);
            MpcZ2Vector[] shareOrderKeys = z2cParty.setPublicValues(orderKeys);
            System.arraycopy(shareOrderKeys, 0, share, 0, shareOrderKeys.length);
            for(int i = shareOrderKeys.length; i < share.length - 1; i++) {
                share[i] = z2cParty.createShareRandom(initNum);
            }
            share[share.length - 1] = z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createOnes(initNum)})[0];
        }
        return new GroupByMt(share, keyDim + payloadDim, isOutputTable, IntStream.range(0, keyDim).toArray(), aggType);
    }

    private UpdateMessage[] getUpdateMsg(MpcZ2cParty party, int updateNum) {
        UpdateMessage[] updateMsg = new UpdateMessage[updateNum];
        int dim = keyDim + payloadDim + 1;
        for (int i = 0; i < updateNum; i++) {
            MpcZ2Vector[] updateData = new MpcZ2Vector[dim];
            for (int j = 0; j < dim; j++) {
                updateData[j] = party.createShareRandom(1);
            }
            updateMsg[i] = new UpdateMessage(OperationEnum.INSERT, updateData);
        }
        return updateMsg;
    }
}
