package edu.alibaba.mpc4j.s2pc.pir.main.ccpsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiServer;
import org.apache.commons.lang3.time.StopWatch;
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
 * CCPSI main.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class CcpsiMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(CcpsiMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "CCPSI_TASK";
    /**
     * protocol name
     */
    public static final String PTO_TYPE_NAME = "CCPSI_PTO";
    /**
     * warmup element byte length
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
    /**
     * warmup server set size
     */
    private static final int WARMUP_SERVER_SET_SIZE = 1 << 10;
    /**
     * warmup client set size
     */
    private static final int WARMUP_CLIENT_SET_SIZE = 1 << 5;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;
    /**
     * properties
     */
    private final Properties properties;

    public CcpsiMain(Properties properties) {
        this.properties = properties;
        stopWatch = new StopWatch();
    }

    public void run() throws Exception {
        Rpc ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "server", "client");
        if (ownRpc.ownParty().getPartyId() == 0) {
            runServer(ownRpc, ownRpc.getParty(1));
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            runClient(ownRpc, ownRpc.getParty(0));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }

    private void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        int elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int[] clientLogSetSizes = PropertiesUtils.readLogIntArray(properties, "client_log_set_size");
        Preconditions.checkArgument(
            serverLogSetSizes.length == clientLogSetSizes.length,
            "# of server log_set_size = %s, $ of client log_set_size = %s, they must be equal",
            serverLogSetSizes.length, clientLogSetSizes.length
        );
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] clientSetSizes = Arrays.stream(clientLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        CcpsiConfig config = CcpsiConfigUtils.createCcpsiConfig(properties);
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementByteLength * Byte.SIZE
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num\tSilent"
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
            runServer(serverRpc, clientParty, config, taskId, serverElementSet, clientSetSize, printWriter);
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

    private void warmupServer(Rpc serverRpc, Party clientParty, CcpsiConfig config, int taskId) throws Exception {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        CcpsiServer<ByteBuffer> ccpsiServer = CcpsiFactory.createServer(serverRpc, clientParty, config);
        ccpsiServer.setTaskId(taskId);
        ccpsiServer.setParallel(false);
        ccpsiServer.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", ccpsiServer.ownParty().getPartyName());
        ccpsiServer.init(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE);
        ccpsiServer.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", ccpsiServer.ownParty().getPartyName());
        ccpsiServer.psi(serverElementSet, WARMUP_CLIENT_SET_SIZE);
        ccpsiServer.getRpc().synchronize();
        ccpsiServer.getRpc().reset();
        ccpsiServer.destroy();
        LOGGER.info("(warmup) {} finish", ccpsiServer.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, CcpsiConfig config, int taskId,
                           Set<ByteBuffer> serverElementSet, int clientSetSize, PrintWriter printWriter)
        throws MpcAbortException {
        int serverSetSize = serverElementSet.size();
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        CcpsiServer<ByteBuffer> ccpsiServer = CcpsiFactory.createServer(serverRpc, clientParty, config);
        ccpsiServer.setTaskId(taskId);
        ccpsiServer.setParallel(true);
        ccpsiServer.getRpc().synchronize();
        ccpsiServer.getRpc().reset();
        LOGGER.info("{} init", ccpsiServer.ownParty().getPartyName());
        stopWatch.start();
        ccpsiServer.init(serverSetSize, clientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = ccpsiServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = ccpsiServer.getRpc().getPayloadByteLength();
        long initSendByteLength = ccpsiServer.getRpc().getSendByteLength();
        ccpsiServer.getRpc().synchronize();
        ccpsiServer.getRpc().reset();
        LOGGER.info("{} execute", ccpsiServer.ownParty().getPartyName());
        stopWatch.start();
        ccpsiServer.psi(serverElementSet, clientSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = ccpsiServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = ccpsiServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = ccpsiServer.getRpc().getSendByteLength();
        String info = ccpsiServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + ccpsiServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + silent
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        ccpsiServer.getRpc().synchronize();
        ccpsiServer.getRpc().reset();
        ccpsiServer.destroy();
        LOGGER.info("{} finish", ccpsiServer.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        int elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int[] clientLogSetSizes = PropertiesUtils.readLogIntArray(properties, "client_log_set_size");
        Preconditions.checkArgument(
            serverLogSetSizes.length == clientLogSetSizes.length,
            "# of server log_set_size = %s, $ of client log_set_size = %s, they must be equal",
            serverLogSetSizes.length, clientLogSetSizes.length
        );
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] clientSetSizes = Arrays.stream(clientLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        CcpsiConfig config = CcpsiConfigUtils.createCcpsiConfig(properties);
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementByteLength * Byte.SIZE
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num\tSilent"
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
            Set<ByteBuffer> clientElementSet = readClientElementSet(clientSetSize, elementByteLength);
            runClient(clientRpc, serverParty, config, taskId, clientElementSet, serverSetSize, printWriter);
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

    private void warmupClient(Rpc clientRpc, Party serverParty, CcpsiConfig config, int taskId) throws Exception {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        CcpsiClient<ByteBuffer> ccpsiClient = CcpsiFactory.createClient(clientRpc, serverParty, config);
        ccpsiClient.setTaskId(taskId);
        ccpsiClient.setParallel(false);
        ccpsiClient.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", ccpsiClient.ownParty().getPartyName());
        ccpsiClient.init(WARMUP_CLIENT_SET_SIZE, WARMUP_SERVER_SET_SIZE);
        ccpsiClient.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", ccpsiClient.ownParty().getPartyName());
        ccpsiClient.psi(clientElementSet, WARMUP_SERVER_SET_SIZE);
        ccpsiClient.getRpc().synchronize();
        ccpsiClient.getRpc().reset();
        ccpsiClient.destroy();
        LOGGER.info("(warmup) {} finish", ccpsiClient.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, CcpsiConfig config, int taskId,
                           Set<ByteBuffer> clientElementSet, int serverSetSize, PrintWriter printWriter)
        throws MpcAbortException {
        int clientSetSize = clientElementSet.size();
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        CcpsiClient<ByteBuffer> ccpsiClient = CcpsiFactory.createClient(clientRpc, serverParty, config);
        ccpsiClient.setTaskId(taskId);
        ccpsiClient.setParallel(true);
        ccpsiClient.getRpc().synchronize();
        ccpsiClient.getRpc().reset();
        LOGGER.info("{} init", ccpsiClient.ownParty().getPartyName());
        stopWatch.start();
        ccpsiClient.init(clientSetSize, serverSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = ccpsiClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = ccpsiClient.getRpc().getPayloadByteLength();
        long initSendByteLength = ccpsiClient.getRpc().getSendByteLength();
        ccpsiClient.getRpc().synchronize();
        ccpsiClient.getRpc().reset();
        LOGGER.info("{} execute", ccpsiClient.ownParty().getPartyName());
        stopWatch.start();
        ccpsiClient.psi(clientElementSet, serverSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = ccpsiClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = ccpsiClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = ccpsiClient.getRpc().getSendByteLength();
        String info = ccpsiClient.ownParty().getPartyId()
            + "\t" + clientSetSize
            + "\t" + serverSetSize
            + "\t" + ccpsiClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + silent
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        ccpsiClient.getRpc().synchronize();
        ccpsiClient.getRpc().reset();
        ccpsiClient.destroy();
        LOGGER.info("{} finish", ccpsiClient.ownParty().getPartyName());
    }
}
