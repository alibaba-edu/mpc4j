package edu.alibaba.mpc4j.s2pc.pso.mqrpmt;

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
 * mqRPMT协议客户端抽象类。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public abstract class AbstractMqRpmtClient extends AbstractSecureTwoPartyPto implements MqRpmtClient {
    /**
     * 特殊空元素字节缓存区
     */
    protected static final ByteBuffer BOT_ELEMENT_BYTE_BUFFER;

    static {
        byte[] botElementByteArray = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        BOT_ELEMENT_BYTE_BUFFER = ByteBuffer.wrap(botElementByteArray);
    }

    /**
     * 配置项
     */
    private final MqRpmtConfig config;
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

    protected AbstractMqRpmtClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, MqRpmtConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        this.config = config;
    }

    @Override
    public MqRpmtFactory.MqRpmtType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        assert maxClientElementSize > 1;
        this.maxClientElementSize = maxClientElementSize;
        assert maxServerElementSize > 1;
        this.maxServerElementSize = maxServerElementSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<ByteBuffer> clientElementSet, int serverElementSize) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert clientElementSet.size() > 1 && clientElementSet.size() <= maxClientElementSize;
        this.clientElementArrayList = clientElementSet.stream()
            .peek(receiverElement -> {
                assert !receiverElement.equals(BOT_ELEMENT_BYTE_BUFFER) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        clientElementSize = clientElementSet.size();
        assert serverElementSize > 1 && serverElementSize <= maxServerElementSize;
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }
}
