package edu.alibaba.mpc4j.s2pc.pso.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * PSO主函数工具类。
 *
 * @author Weiran Liu
 * @date 2022/5/15
 */
public class PsoMainUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);

    private PsoMainUtils() {
        // empty
    }

    /**
     * 设置通信接口。
     *
     * @param properties 配置项。
     * @return 通信接口。
     */
    public static Rpc setRpc(Properties properties) {
        // 构建参与方信息
        Set<NettyParty> nettyPartySet = new HashSet<>(2);
        Map<String, NettyParty> nettyPartyMap = new HashMap<>(2);
        // 初始化服务端
        String serverName = Preconditions.checkNotNull(
            properties.getProperty("server_name"), "Please set server_name"
        );
        String serverIp = Preconditions.checkNotNull(
            properties.getProperty("server_ip"), "Please set server_ip"
        );
        int serverPort = Integer.parseInt(Preconditions.checkNotNull(
            properties.getProperty("server_port", "Please set server_port")
        ));
        NettyParty serverNettyParty = new NettyParty(0, serverName, serverIp, serverPort);
        nettyPartySet.add(serverNettyParty);
        nettyPartyMap.put(serverName, serverNettyParty);
        // 初始化客户端
        String clientName = Preconditions.checkNotNull(
            properties.getProperty("client_name"), "Please set client_name"
        );
        String clientIp = Preconditions.checkNotNull(
            properties.getProperty("client_ip"), "Please set client_ip"
        );
        int clientPort = Integer.parseInt(Preconditions.checkNotNull(
            properties.getProperty("client_port"), "Please set client_port"
        ));
        NettyParty clientNettyParty = new NettyParty(1, clientName, clientIp, clientPort);
        nettyPartySet.add(clientNettyParty);
        nettyPartyMap.put(clientName, clientNettyParty);
        // 获得自己的参与方信息
        String ownName = Preconditions.checkNotNull(
            properties.getProperty("own_name"), "Please set own_name"
        );
        NettyParty ownParty = Preconditions.checkNotNull(
            nettyPartyMap.get(ownName), "own_name must be %s or %s", serverName, clientName
        );
        if (ownName.equals(serverName)) {
            LOGGER.info("own_name = {} for party_id = 0", serverName);
        } else {
            LOGGER.info("own_name = {} for party_id = 1", clientName);
        }
        return new NettyRpc(ownParty, nettyPartySet);
    }

    /**
     * 读取集合大小。
     *
     * @param properties 配置项。
     * @return 集合大小。
     */
    public static int[] readSetSizes(Properties properties) {
        String logSetSizeString = Preconditions.checkNotNull(
            properties.getProperty("log_set_size"), "Please set log_set_size"
        );
        int[] setSizes = Arrays.stream(logSetSizeString.split(","))
            .mapToInt(Integer::parseInt)
            .peek(logSetSize -> Preconditions.checkArgument(
                logSetSize > 0 && logSetSize < Integer.SIZE,
                "log(n) must be in range (%s, %s]", 0, Integer.SIZE))
            .map(logSetSize -> 1 << logSetSize)
            .toArray();
        LOGGER.info("setSizes = {}", Arrays.toString(setSizes));

        return setSizes;
    }

    /**
     * 读取max(k)。
     *
     * @param properties 配置项。
     * @return max(k)。
     */
    public static int[] readMaxKs(Properties properties) {
        String maxKsString = Preconditions.checkNotNull(
            properties.getProperty("max_k"), "Please set max_k"
        );
        int[] maxKs = Arrays.stream(maxKsString.split(","))
            .mapToInt(Integer::parseInt)
            .peek(logSetSize -> Preconditions.checkArgument(
                logSetSize > 0 && logSetSize < Integer.SIZE,
                "max(k) must be in range (%s, %s]", 0, Integer.SIZE))
            .toArray();
        LOGGER.info("max(k) = {}", Arrays.toString(maxKs));
        return maxKs;
    }

    /**
     * 读取元素字节长度。
     *
     * @param properties 配置项。
     * @return 元素字节长度。
     */
    public static int readElementByteLength(Properties properties) {
        int elementByteLength = Integer.parseInt(Preconditions.checkNotNull(
            properties.getProperty("element_byte_length"), "Please set element_byte_length"
        ));
        Preconditions.checkArgument(elementByteLength > 0,
            "elementByteLength must be greater than 0: %s", elementByteLength
        );
        return elementByteLength;
    }
}
