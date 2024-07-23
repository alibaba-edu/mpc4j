package edu.alibaba.mpc4j.work.dpsi.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.dpsi.DpsiClient;
import edu.alibaba.mpc4j.work.dpsi.DpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.DpsiFactory;
import edu.alibaba.mpc4j.work.dpsi.DpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DPSI main.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/10/09
 */
public class DpsiMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(DpsiMain.class);
    /**
     * task name
     */
    public static final String PTO_TYPE_NAME = "DPSI";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "dpsi_pto_name";
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
     * DP-PSI main type
     */
    private final DpsiMainType dpsiMainType;
    /**
     * config
     */
    private final DpsiConfig config;

    public DpsiMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read common settings
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
        // read PTO settings
        LOGGER.info("{} read PTO settings", ownRpc.ownParty().getPartyName());
        dpsiMainType = MainPtoConfigUtils.readEnum(DpsiMainType.class, properties, PTO_NAME_KEY);
        config = DpsiConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty1(Rpc serverRpc, Party clientParty) throws MpcAbortException, IOException {
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + dpsiMainType
            + "_" + appendString
            + "_" + elementByteLength * Byte.SIZE
            + "_" + config.getEpsilon()
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        // 建立连接
        serverRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupServer(serverRpc, clientParty, config, taskId);
        taskId++;
        // 正式测试
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            Set<ByteBuffer> serverElementSet = readServerElementSet(serverSetSize, elementByteLength);
            runServer(serverRpc, clientParty, config, taskId, true, serverElementSet, clientSetSize, printWriter);
            taskId++;
            runServer(serverRpc, clientParty, config, taskId, false, serverElementSet, clientSetSize, printWriter);
            taskId++;
        }
        // 断开连接
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

    private void warmupServer(Rpc serverRpc, Party clientParty, DpsiConfig config, int taskId) throws MpcAbortException, IOException {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        DpsiServer<ByteBuffer> server = DpsiFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(false);
        server.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", server.ownParty().getPartyName());
        server.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        server.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", server.ownParty().getPartyName());
        server.psi(serverElementSet, WARMUP_SET_SIZE);
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("(warmup) {} finish", server.ownParty().getPartyName());
    }

    public void runServer(Rpc serverRpc, Party clientParty, DpsiConfig config, int taskId, boolean parallel,
                          Set<ByteBuffer> serverElementSet, int clientSetSize, PrintWriter printWriter)
        throws MpcAbortException {
        int serverSetSize = serverElementSet.size();
        DpsiServer<ByteBuffer> server = DpsiFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(parallel);
        // 启动测试
        server.getRpc().synchronize();
        server.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", server.ownParty().getPartyName());
        stopWatch.start();
        server.init(serverSetSize, clientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = server.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = server.getRpc().getPayloadByteLength();
        long initSendByteLength = server.getRpc().getSendByteLength();
        server.getRpc().synchronize();
        server.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", server.ownParty().getPartyName());
        stopWatch.start();
        server.psi(serverElementSet, clientSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = server.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = server.getRpc().getPayloadByteLength();
        long ptoSendByteLength = server.getRpc().getSendByteLength();
        // 写入统计结果
        String info = server.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + server.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("{} finish", server.ownParty().getPartyName());
    }

    @Override
    public void runParty2(Rpc clientRpc, Party serverParty) throws MpcAbortException, IOException {
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + dpsiMainType
            + "_" + appendString
            + "_" + elementByteLength * Byte.SIZE
            + "_" + config.getEpsilon()
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)"
            + "\tPrivacy Budget\tFalse Positive Rate\tFalse Negative Rate";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // 建立连接
        clientRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            // 读取输入文件
            Set<ByteBuffer> clientElementSet = readClientElementSet(clientSetSize, elementByteLength);
            Set<ByteBuffer> serverElementSet = readServerElementSet(serverSetSize, elementByteLength);
            // 多线程
            runClient(clientRpc, serverParty, config, taskId, true, clientElementSet, serverElementSet, serverSetSize,
                printWriter);
            taskId++;
            // 单线程
            runClient(clientRpc, serverParty, config, taskId, false, clientElementSet, serverElementSet, serverSetSize,
                printWriter);
            taskId++;
        }
        // 断开连接
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

    private void warmupClient(Rpc clientRpc, Party serverParty, DpsiConfig config, int taskId) throws MpcAbortException, IOException {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        DpsiClient<ByteBuffer> client = DpsiFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(false);
        client.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", client.ownParty().getPartyName());
        client.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        client.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", client.ownParty().getPartyName());
        client.psi(clientElementSet, WARMUP_SET_SIZE);
        // 同步并等待5秒钟，保证对方执行完毕
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("(warmup) {} finish", client.ownParty().getPartyName());
    }

    public void runClient(Rpc clientRpc, Party serverParty, DpsiConfig config, int taskId, boolean parallel,
                          Set<ByteBuffer> clientElementSet, Set<ByteBuffer> serverElementSet, int serverSetSize,
                          PrintWriter printWriter) throws MpcAbortException {
        int clientSetSize = clientElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        DpsiClient<ByteBuffer> client = DpsiFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(parallel);
        // 启动测试
        client.getRpc().synchronize();
        client.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", client.ownParty().getPartyName());
        stopWatch.start();
        client.init(clientSetSize, serverSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = client.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = client.getRpc().getPayloadByteLength();
        long initSendByteLength = client.getRpc().getSendByteLength();
        client.getRpc().synchronize();
        client.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", client.ownParty().getPartyName());
        stopWatch.start();
        Set<ByteBuffer> intersection = client.psi(clientElementSet, serverSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        double[] measurements = measure(serverElementSet, clientElementSet, intersection);
        long ptoDataPacketNum = client.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = client.getRpc().getPayloadByteLength();
        long ptoSendByteLength = client.getRpc().getSendByteLength();
        // 写入统计结果
        String info = client.ownParty().getPartyId()
            + "\t" + clientSetSize
            + "\t" + serverSetSize
            + "\t" + client.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength
            + "\t" + measurements[0] + "\t" + measurements[1];
        printWriter.println(info);
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("{} finish", client.ownParty().getPartyName());
    }

    private double[] measure(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, Set<ByteBuffer> actualIntersection) {
        double tp = 0;
        double fp = 0;
        double tn = 0;
        double fn = 0;
        Set<ByteBuffer> expectIntersection = new HashSet<>(serverSet);
        expectIntersection.retainAll(clientSet);
        for (ByteBuffer element : clientSet) {
            if ((actualIntersection.contains(element)) && (expectIntersection.contains(element))) {
                tp = tp + 1.0;
            } else if (!(actualIntersection.contains(element)) && (expectIntersection.contains(element))) {
                fn = fn + 1.0;
            } else if ((actualIntersection.contains(element)) && !(expectIntersection.contains(element))) {
                fp = fp + 1.0;
            } else if (!(actualIntersection.contains(element)) && !(expectIntersection.contains(element))) {
                tn = tn + 1.0;
            }
        }
        double fpr = fp / (fp + tn);
        double fnr = fn / (fn + tp);
        return new double[]{fpr, fnr};
    }

    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        String ptoType = MainPtoConfigUtils.readPtoType(properties);
        Preconditions.checkArgument(ptoType.equals(PTO_TYPE_NAME));
        DpsiMain dpsiMain = new DpsiMain(properties, ownName);
        dpsiMain.runNetty();
        System.exit(0);
    }
}
