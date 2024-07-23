package edu.alibaba.mpc4j.work.payable.main.psi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiClient;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiConfig;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiFactory;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiServer;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Payable PSI main.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class PayablePsiMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayablePsiMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "PAYABLE_PSI_TASK";
    /**
     * protocol type name
     */
    public static final String PTO_NAME_KEY = "payable_psi_pto_name";
    /**
     * warmup element byte length
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
    /**
     * warmup set size
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
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
     * payable PSI main type
     */
    private final PayablePsiMainType payablePsiMainType;
    /**
     * config
     */
    private final PayablePsiConfig config;
    /**
     * parallel
     */
    private final boolean parallel;

    public PayablePsiMain(Properties properties, String ownName) {
        super(properties, ownName);
        LOGGER.info("{} read common settings", ownRpc.ownParty().getPartyName());
        elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        parallel = PropertiesUtils.readBoolean(properties, "parallel");
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
        payablePsiMainType = MainPtoConfigUtils.readEnum(PayablePsiMainType.class, properties, PTO_NAME_KEY);
        config = PayablePsiConfigUtils.createPayablePsiConfig(properties);
    }

    @Override
    public void runParty1(Rpc serverRpc, Party clientParty) throws IOException, MpcAbortException {
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(
                serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength
            );
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + TASK_NAME
            + "_" + payablePsiMainType
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
            runServer(serverRpc, clientParty, config, taskId, parallel, serverElementSet, clientSetSize, printWriter);
            taskId++;
        }
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readServerElementSet(int setSize, int elementByteLength) throws IOException {
        LOGGER.info("Server read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            Files.newInputStream(Paths.get(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, elementByteLength))),
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

    private void warmupServer(Rpc serverRpc, Party clientParty, PayablePsiConfig config, int taskId)
        throws MpcAbortException, IOException {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        PayablePsiServer psiServer = PayablePsiFactory.createServer(serverRpc, clientParty, config);
        psiServer.setTaskId(taskId);
        psiServer.setParallel(false);
        psiServer.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", psiServer.ownParty().getPartyName());
        psiServer.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        psiServer.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", psiServer.ownParty().getPartyName());
        psiServer.payablePsi(serverElementSet, WARMUP_SET_SIZE);
        psiServer.getRpc().synchronize();
        psiServer.getRpc().reset();
        LOGGER.info("(warmup) {} finish", psiServer.ownParty().getPartyName());
    }

    public void runServer(Rpc serverRpc, Party clientParty, PayablePsiConfig config, int taskId, boolean parallel,
                          Set<ByteBuffer> serverElementSet, int clientSetSize,
                          PrintWriter printWriter) throws MpcAbortException {
        int serverSetSize = serverElementSet.size();
        PayablePsiServer psiServer = PayablePsiFactory.createServer(serverRpc, clientParty, config);
        psiServer.setTaskId(taskId);
        psiServer.setParallel(parallel);
        psiServer.getRpc().synchronize();
        psiServer.getRpc().reset();
        LOGGER.info("{} init", psiServer.ownParty().getPartyName());
        stopWatch.start();
        psiServer.init(serverSetSize, clientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = psiServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = psiServer.getRpc().getPayloadByteLength();
        long initSendByteLength = psiServer.getRpc().getSendByteLength();
        psiServer.getRpc().synchronize();
        psiServer.getRpc().reset();
        LOGGER.info("{} execute", psiServer.ownParty().getPartyName());
        stopWatch.start();
        psiServer.payablePsi(serverElementSet, clientSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = psiServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = psiServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = psiServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = psiServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + psiServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        psiServer.getRpc().synchronize();
        psiServer.getRpc().reset();
        LOGGER.info("{} finish", psiServer.ownParty().getPartyName());
    }

    @Override
    public void runParty2(Rpc clientRpc, Party serverParty) throws MpcAbortException, IOException {
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + TASK_NAME
            + "_" + payablePsiMainType
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
            runClient(clientRpc, serverParty, config, taskId, parallel, clientElementSet, serverSetSize, printWriter);
            taskId++;
        }
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readClientElementSet(int setSize, int elementByteLength) throws IOException {
        LOGGER.info("Client read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            Files.newInputStream(Paths.get(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, elementByteLength))),
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

    private void warmupClient(Rpc clientRpc, Party serverParty, PayablePsiConfig config, int taskId)
        throws MpcAbortException, IOException {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        PayablePsiClient psiClient = PayablePsiFactory.createClient(clientRpc, serverParty, config);
        psiClient.setTaskId(taskId);
        psiClient.setParallel(false);
        psiClient.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", psiClient.ownParty().getPartyName());
        psiClient.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        psiClient.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", psiClient.ownParty().getPartyName());
        psiClient.payablePsi(clientElementSet, WARMUP_SET_SIZE);
        // 同步并等待5秒钟，保证对方执行完毕
        psiClient.getRpc().synchronize();
        psiClient.getRpc().reset();
        LOGGER.info("(warmup) {} finish", psiClient.ownParty().getPartyName());
    }

    public void runClient(Rpc clientRpc, Party serverParty, PayablePsiConfig config, int taskId, boolean parallel,
                          Set<ByteBuffer> clientElementSet, int serverSetSize,
                          PrintWriter printWriter) throws MpcAbortException {
        int clientSetSize = clientElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        PayablePsiClient psiClient = PayablePsiFactory.createClient(clientRpc, serverParty, config);
        psiClient.setTaskId(taskId);
        psiClient.setParallel(parallel);
        // 启动测试
        psiClient.getRpc().synchronize();
        psiClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", psiClient.ownParty().getPartyName());
        stopWatch.start();
        psiClient.init(clientSetSize, serverSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = psiClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = psiClient.getRpc().getPayloadByteLength();
        long initSendByteLength = psiClient.getRpc().getSendByteLength();
        psiClient.getRpc().synchronize();
        psiClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", psiClient.ownParty().getPartyName());
        stopWatch.start();
        psiClient.payablePsi(clientElementSet, serverSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = psiClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = psiClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = psiClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = psiClient.ownParty().getPartyId()
            + "\t" + clientSetSize
            + "\t" + serverSetSize
            + "\t" + psiClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        psiClient.getRpc().synchronize();
        psiClient.getRpc().reset();
        LOGGER.info("{} finish", psiClient.ownParty().getPartyName());
    }
}
