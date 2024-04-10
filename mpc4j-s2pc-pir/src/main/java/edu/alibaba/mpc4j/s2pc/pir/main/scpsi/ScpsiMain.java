package edu.alibaba.mpc4j.s2pc.pir.main.scpsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiServer;
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
 * SCPSI main.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class ScpsiMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScpsiMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "SCPSI_TASK";
    /**
     * protocol name
     */
    public static final String PTO_TYPE_NAME = "SCPSI_PTO";
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

    public ScpsiMain(Properties properties) {
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
        ScpsiConfig config = ScpsiConfigUtils.createScpsiConfig(properties);
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(
                serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength
            );
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

    private void warmupServer(Rpc serverRpc, Party clientParty, ScpsiConfig config, int taskId) throws Exception {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        ScpsiServer<ByteBuffer> scpsiServer = ScpsiFactory.createServer(serverRpc, clientParty, config);
        scpsiServer.setTaskId(taskId);
        scpsiServer.setParallel(false);
        scpsiServer.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", scpsiServer.ownParty().getPartyName());
        scpsiServer.init(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE);
        scpsiServer.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", scpsiServer.ownParty().getPartyName());
        scpsiServer.psi(serverElementSet, WARMUP_CLIENT_SET_SIZE);
        scpsiServer.getRpc().synchronize();
        scpsiServer.getRpc().reset();
        scpsiServer.destroy();
        LOGGER.info("(warmup) {} finish", scpsiServer.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, ScpsiConfig config, int taskId,
                           Set<ByteBuffer> serverElementSet, int clientSetSize, PrintWriter printWriter)
        throws MpcAbortException {
        int serverSetSize = serverElementSet.size();
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        ScpsiServer<ByteBuffer> scpsiServer = ScpsiFactory.createServer(serverRpc, clientParty, config);
        scpsiServer.setTaskId(taskId);
        scpsiServer.setParallel(true);
        scpsiServer.getRpc().synchronize();
        scpsiServer.getRpc().reset();
        LOGGER.info("{} init", scpsiServer.ownParty().getPartyName());
        stopWatch.start();
        scpsiServer.init(serverSetSize, clientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = scpsiServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = scpsiServer.getRpc().getPayloadByteLength();
        long initSendByteLength = scpsiServer.getRpc().getSendByteLength();
        scpsiServer.getRpc().synchronize();
        scpsiServer.getRpc().reset();
        LOGGER.info("{} execute", scpsiServer.ownParty().getPartyName());
        stopWatch.start();
        scpsiServer.psi(serverElementSet, clientSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = scpsiServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = scpsiServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = scpsiServer.getRpc().getSendByteLength();
        String info = scpsiServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + scpsiServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + silent
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        scpsiServer.getRpc().synchronize();
        scpsiServer.getRpc().reset();
        scpsiServer.destroy();
        LOGGER.info("{} finish", scpsiServer.ownParty().getPartyName());
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
        ScpsiConfig config = ScpsiConfigUtils.createScpsiConfig(properties);
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(
                serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength
            );
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

    private void warmupClient(Rpc clientRpc, Party serverParty, ScpsiConfig config, int taskId) throws Exception {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        ScpsiClient<ByteBuffer> scpsiClient = ScpsiFactory.createClient(clientRpc, serverParty, config);
        scpsiClient.setTaskId(taskId);
        scpsiClient.setParallel(false);
        scpsiClient.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", scpsiClient.ownParty().getPartyName());
        scpsiClient.init(WARMUP_CLIENT_SET_SIZE, WARMUP_SERVER_SET_SIZE);
        scpsiClient.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", scpsiClient.ownParty().getPartyName());
        scpsiClient.psi(clientElementSet, WARMUP_SERVER_SET_SIZE);
        scpsiClient.getRpc().synchronize();
        scpsiClient.getRpc().reset();
        scpsiClient.destroy();
        LOGGER.info("(warmup) {} finish", scpsiClient.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, ScpsiConfig config, int taskId,
                           Set<ByteBuffer> clientElementSet, int serverSetSize, PrintWriter printWriter)
        throws MpcAbortException {
        int clientSetSize = clientElementSet.size();
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        ScpsiClient<ByteBuffer> scpsiClient = ScpsiFactory.createClient(clientRpc, serverParty, config);
        scpsiClient.setTaskId(taskId);
        scpsiClient.setParallel(true);
        scpsiClient.getRpc().synchronize();
        scpsiClient.getRpc().reset();
        LOGGER.info("{} init", scpsiClient.ownParty().getPartyName());
        stopWatch.start();
        scpsiClient.init(clientSetSize, serverSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = scpsiClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = scpsiClient.getRpc().getPayloadByteLength();
        long initSendByteLength = scpsiClient.getRpc().getSendByteLength();
        scpsiClient.getRpc().synchronize();
        scpsiClient.getRpc().reset();
        LOGGER.info("{} execute", scpsiClient.ownParty().getPartyName());
        stopWatch.start();
        scpsiClient.psi(clientElementSet, serverSetSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = scpsiClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = scpsiClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = scpsiClient.getRpc().getSendByteLength();
        String info = scpsiClient.ownParty().getPartyId()
            + "\t" + clientSetSize
            + "\t" + serverSetSize
            + "\t" + scpsiClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + silent
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        scpsiClient.getRpc().synchronize();
        scpsiClient.getRpc().reset();
        scpsiClient.destroy();
        LOGGER.info("{} finish", scpsiClient.ownParty().getPartyName());
    }
}
