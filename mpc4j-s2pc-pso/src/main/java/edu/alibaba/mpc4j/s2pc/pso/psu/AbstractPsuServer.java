package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PSU协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public abstract class AbstractPsuServer extends AbstractSecureTwoPartyPto implements PsuServer {
    /**
     * 配置项
     */
    private final PsuConfig config;
    /**
     * 服务端最大元素数量
     */
    private int maxServerElementSize;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 服务端元素数组
     */
    protected ArrayList<ByteBuffer> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 元素字节长度
     */
    protected int elementByteLength;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractPsuServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PsuConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        this.config = config;
    }

    @Override
    public PsuType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        assert maxServerElementSize > 1 : "max server element size must be greater than 1: " + maxServerElementSize;
        this.maxServerElementSize = maxServerElementSize;
        assert maxClientElementSize > 1 : "max client element size must be greater than 1: " + maxClientElementSize;
        this.maxClientElementSize = maxClientElementSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.elementByteLength = elementByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert serverElementSet.size() > 1 && serverElementSet.size() <= maxServerElementSize
            : "server element size must be in range (1, " + maxServerElementSize + "]: " + serverElementSet.size();
        serverElementSize = serverElementSet.size();
        serverElementArrayList = serverElementSet.stream()
            .peek(senderElement -> {
                assert senderElement.array().length == elementByteLength;
                assert !senderElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        assert clientElementSize > 1 && clientElementSize <= maxClientElementSize
            : "client element size must be in range (1, " + maxClientElementSize + "]: " + clientElementSize;
        this.clientElementSize = clientElementSize;
        extraInfo++;
    }
}
