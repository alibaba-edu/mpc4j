package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PSU协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public abstract class AbstractPsuClient extends AbstractSecureTwoPartyPto implements PsuClient {
    /**
     * 配置项
     */
    private final PsuConfig config;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 服务端最大元素数量
     */
    private int maxServerElementSize;
    /**
     * 客户端元素集合
     */
    protected ArrayList<ByteBuffer> clientElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 元素字节长度
     */
    protected int elementByteLength;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractPsuClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PsuConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        this.config = config;
    }

    @Override
    public PsuFactory.PsuType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        assert maxClientElementSize > 1 : "max client element size must be greater than 1: " + maxClientElementSize;
        this.maxClientElementSize = maxClientElementSize;
        assert maxServerElementSize > 1 : "max server element size must be greater than 1: " + maxServerElementSize;
        this.maxServerElementSize = maxServerElementSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.elementByteLength = elementByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert clientElementSet.size() > 1 && clientElementSet.size() <= maxClientElementSize
            : "client element size must be in range (1, " + maxServerElementSize + "]: " + clientElementSet.size();
        clientElementSize = clientElementSet.size();
        clientElementArrayList = clientElementSet.stream()
            .peek(receiverElement -> {
                assert receiverElement.array().length == elementByteLength;
                assert !receiverElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        assert serverElementSize > 1 && serverElementSize <= maxServerElementSize
            : "server element size must be in range (1, " + maxServerElementSize + "]: " + serverElementSize;
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }
}
