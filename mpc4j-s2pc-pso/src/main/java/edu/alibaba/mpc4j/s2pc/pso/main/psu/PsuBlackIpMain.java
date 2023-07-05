package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuServer;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * PSU_BLACK_IP主函数。
 *
 * @author Weiran Liu
 * @date 2021/09/14
 */
public class PsuBlackIpMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsuBlackIpMain.class);
    /**
     * 协议类型名称
     */
    public static final String PTO_TYPE_NAME = "PSU_BLACK_IP";
    /**
     * 秒表
     */
    private final StopWatch stopWatch;
    /**
     * 配置参数
     */
    private final Properties properties;
    /**
     * 服务端输入
     */
    private Set<ByteBuffer> serverElementSet;
    /**
     * 服务端集合大小
     */
    private int serverSetSize;
    /**
     * 客户端输入
     */
    private Set<ByteBuffer> clientElementSet;
    /**
     * 客户端集合大小
     */
    private int clientSetSize;

    public PsuBlackIpMain(Properties properties) {
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

    public void readInputFiles() throws IOException {
        // 读取输入文件
        String serverInputPath = PropertiesUtils.readString(properties, "psu_black_ip_server_input");
        LOGGER.info("Read server input file from: {}", serverInputPath);
        serverElementSet = PsuBlackIpUtils.readBlackIpSet(serverInputPath);
        serverSetSize = serverElementSet.size();
        LOGGER.info("Server contains {} IPs", serverSetSize);

        String clientInputPath = PropertiesUtils.readString(properties, "psu_black_ip_client_input");
        LOGGER.info("Read client input file from: {}", clientInputPath);
        clientElementSet = PsuBlackIpUtils.readBlackIpSet(clientInputPath);
        clientSetSize = clientElementSet.size();
        LOGGER.info("Client contains {} IPs", clientSetSize);
    }

    private void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read PSU config", serverRpc.ownParty().getPartyName());
        PsuConfig config = PsuConfigUtils.createPsuConfig(properties);
        // 读取输入文件
        readInputFiles();
        // 创建统计结果文件
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
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
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        PsuServer psuServer = PsuFactory.createServer(serverRpc, clientParty, config);
        psuServer.setTaskId(taskId);
        psuServer.setParallel(true);
        // 启动测试
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", psuServer.ownParty().getPartyName());
        stopWatch.start();
        psuServer.init(serverSetSize, clientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = psuServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = psuServer.getRpc().getPayloadByteLength();
        long initSendByteLength = psuServer.getRpc().getSendByteLength();
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", psuServer.ownParty().getPartyName());
        stopWatch.start();
        psuServer.psu(serverElementSet, clientSetSize, PsuBlackIpUtils.IP_BYTE_LENGTH);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = psuServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = psuServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = psuServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = psuServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + psuServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        psuServer.destroy();
        LOGGER.info("{} finish", psuServer.ownParty().getPartyName());
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read PSU config", clientRpc.ownParty().getPartyName());
        PsuConfig config = PsuConfigUtils.createPsuConfig(properties);
        // 读取输入文件
        readInputFiles();
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // 建立连接
        clientRpc.connect();
        // 启动测试
        int taskId = 0;
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        PsuClient psuClient = PsuFactory.createClient(clientRpc, serverParty, config);
        psuClient.setTaskId(taskId);
        psuClient.setParallel(true);
        // 启动测试
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", psuClient.ownParty().getPartyName());
        stopWatch.start();
        psuClient.init(clientSetSize, serverSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = psuClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = psuClient.getRpc().getPayloadByteLength();
        long initSendByteLength = psuClient.getRpc().getSendByteLength();
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", psuClient.ownParty().getPartyName());
        stopWatch.start();
        psuClient.psu(clientElementSet, serverSetSize, PsuBlackIpUtils.IP_BYTE_LENGTH);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = psuClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = psuClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = psuClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = psuClient.ownParty().getPartyId()
            + "\t" + clientSetSize
            + "\t" + serverSetSize
            + "\t" + psuClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        psuClient.destroy();
        LOGGER.info("{} finish", psuClient.ownParty().getPartyName());
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }
}
