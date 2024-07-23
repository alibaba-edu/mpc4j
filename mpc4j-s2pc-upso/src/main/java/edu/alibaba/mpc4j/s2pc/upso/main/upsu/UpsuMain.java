package edu.alibaba.mpc4j.s2pc.upso.main.upsu;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuReceiver;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuSender;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * UPSU main.
 *
 * @author Liqiang Peng
 * @date 2024/3/29
 */
public class UpsuMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpsuMain.class);
    /**
     * task name
     */
    public static final String PTO_TYPE_NAME = "UPSU";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "upsu_pto_name";
    /**
     * warmup element byte length
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
    /**
     * warmup server set size
     */
    private static final int WARMUP_SERVER_SET_SIZE = 1 << 5;
    /**
     * warmup client set size
     */
    private static final int WARMUP_CLIENT_SET_SIZE = 1 << 10;
    /**
     * element byte length
     */
    private final int elementByteLength;
    /**
     * set size num
     */
    private final int setSizeNum;
    /**
     * server set sizes
     */
    private final int[] serverSetSizes;
    /**
     * client set sizes
     */
    private final int[] clientSetSizes;
    /**
     * UPSU main type
     */
    private final UpsuMainType upsuMainType;
    /**
     * config
     */
    private final UpsuConfig config;

    public UpsuMain(Properties properties, String ownName) {
        super(properties, ownName);
        LOGGER.info("{} read common settings", ownRpc.ownParty().getPartyName());
        elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int[] clientLogSetSizes = PropertiesUtils.readLogIntArray(properties, "client_log_set_size");
        Preconditions.checkArgument(
            serverLogSetSizes.length == clientLogSetSizes.length,
            "# of server log_set_size = %s, $ of client log_set_size = %s, they must be equal",
            serverLogSetSizes.length, clientLogSetSizes.length
        );
        setSizeNum = serverLogSetSizes.length;
        serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        clientSetSizes = Arrays.stream(clientLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", ownRpc.ownParty().getPartyName());
        upsuMainType = MainPtoConfigUtils.readEnum(UpsuMainType.class, properties, PTO_NAME_KEY);
        config = UpsuConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty1(Rpc serverRpc, Party clientParty) throws IOException, MpcAbortException {
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(
                serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength
            );
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + upsuMainType
            + "_" + appendString
            + "_" + elementByteLength * Byte.SIZE
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        serverRpc.connect();
        int taskId = 0;
        warmupServer(serverRpc, clientParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            Set<ByteBuffer> serverElementSet = readServerElementSet(serverSetSize, elementByteLength);
            runServer(serverRpc, clientParty, config, taskId, serverElementSet, clientSetSize, printWriter, elementByteLength);
            taskId++;
        }
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readServerElementSet(int setSize, int elementByteLength) throws IOException {
        LOGGER.info("Server read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, elementByteLength)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> serverElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        return serverElementSet;
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, UpsuConfig config, int taskId) throws IOException, MpcAbortException {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        UpsuSender upsuSender = UpsuFactory.createSender(serverRpc, clientParty, config);
        upsuSender.setTaskId(taskId);
        upsuSender.setParallel(false);
        upsuSender.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", upsuSender.ownParty().getPartyName());
        upsuSender.init(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE);
        upsuSender.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", upsuSender.ownParty().getPartyName());
        upsuSender.psu(serverElementSet, WARMUP_ELEMENT_BYTE_LENGTH);
        upsuSender.getRpc().synchronize();
        upsuSender.getRpc().reset();
        upsuSender.destroy();
        LOGGER.info("(warmup) {} finish", upsuSender.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, UpsuConfig config, int taskId,
                           Set<ByteBuffer> serverElementSet, int clientSetSize, PrintWriter printWriter, int elementByteLength)
        throws MpcAbortException {
        int serverSetSize = serverElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        UpsuSender upsuSender = UpsuFactory.createSender(serverRpc, clientParty, config);
        upsuSender.setTaskId(taskId);
        upsuSender.setParallel(true);
        upsuSender.getRpc().synchronize();
        upsuSender.getRpc().reset();
        LOGGER.info("{} init", upsuSender.ownParty().getPartyName());
        stopWatch.start();
        upsuSender.init(serverSetSize, clientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = upsuSender.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = upsuSender.getRpc().getPayloadByteLength();
        long initSendByteLength = upsuSender.getRpc().getSendByteLength();
        upsuSender.getRpc().synchronize();
        upsuSender.getRpc().reset();
        LOGGER.info("{} execute", upsuSender.ownParty().getPartyName());
        stopWatch.start();
        upsuSender.psu(serverElementSet, elementByteLength);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = upsuSender.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = upsuSender.getRpc().getPayloadByteLength();
        long ptoSendByteLength = upsuSender.getRpc().getSendByteLength();
        String info = upsuSender.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + upsuSender.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        upsuSender.getRpc().synchronize();
        upsuSender.getRpc().reset();
        upsuSender.destroy();
        LOGGER.info("{} finish", upsuSender.ownParty().getPartyName());
    }

    @Override
    public void runParty2(Rpc clientRpc, Party serverParty) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + upsuMainType
            + "_" + appendString
            + "_" + elementByteLength * Byte.SIZE
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        clientRpc.connect();
        int taskId = 0;
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            // 读取输入文件
            Set<ByteBuffer> clientElementSet = readClientElementSet(clientSetSize, elementByteLength);
            runClient(clientRpc, serverParty, config, taskId, clientElementSet, serverSetSize, printWriter, elementByteLength);
            taskId++;
        }
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readClientElementSet(int setSize, int elementByteLength) throws IOException {
        LOGGER.info("Client read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, elementByteLength)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> clientElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        return clientElementSet;
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, UpsuConfig config, int taskId) throws IOException, MpcAbortException {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        UpsuReceiver upsuReceiver = UpsuFactory.createReceiver(clientRpc, serverParty, config);
        upsuReceiver.setTaskId(taskId);
        upsuReceiver.setParallel(false);
        upsuReceiver.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", upsuReceiver.ownParty().getPartyName());
        upsuReceiver.init(clientElementSet, WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        upsuReceiver.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", upsuReceiver.ownParty().getPartyName());
        upsuReceiver.psu(WARMUP_SERVER_SET_SIZE);
        upsuReceiver.getRpc().synchronize();
        upsuReceiver.getRpc().reset();
        upsuReceiver.destroy();
        LOGGER.info("(warmup) {} finish", upsuReceiver.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, UpsuConfig config, int taskId,
                           Set<ByteBuffer> clientElementSet, int serverSetSize, PrintWriter printWriter, int elementByteLength)
        throws MpcAbortException {
        int clientSetSize = clientElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        UpsuReceiver upsuReceiver = UpsuFactory.createReceiver(clientRpc, serverParty, config);
        upsuReceiver.setTaskId(taskId);
        upsuReceiver.setParallel(true);
        upsuReceiver.getRpc().synchronize();
        upsuReceiver.getRpc().reset();
        LOGGER.info("{} init", upsuReceiver.ownParty().getPartyName());
        stopWatch.start();
        upsuReceiver.init(clientElementSet, serverSetSize, elementByteLength);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = upsuReceiver.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = upsuReceiver.getRpc().getPayloadByteLength();
        long initSendByteLength = upsuReceiver.getRpc().getSendByteLength();
        upsuReceiver.getRpc().synchronize();
        upsuReceiver.getRpc().reset();
        LOGGER.info("{} execute", upsuReceiver.ownParty().getPartyName());
        stopWatch.start();
        upsuReceiver.psu(serverSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = upsuReceiver.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = upsuReceiver.getRpc().getPayloadByteLength();
        long ptoSendByteLength = upsuReceiver.getRpc().getSendByteLength();
        String info = upsuReceiver.ownParty().getPartyId()
            + "\t" + clientSetSize
            + "\t" + serverSetSize
            + "\t" + upsuReceiver.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        upsuReceiver.getRpc().synchronize();
        upsuReceiver.getRpc().reset();
        upsuReceiver.destroy();
        LOGGER.info("{} finish", upsuReceiver.ownParty().getPartyName());
    }
}
