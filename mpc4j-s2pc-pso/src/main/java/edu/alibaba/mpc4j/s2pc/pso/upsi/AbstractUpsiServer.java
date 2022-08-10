package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 非平衡PSI协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/14
 */
public abstract class AbstractUpsiServer<T> extends AbstractSecureTwoPartyPto implements UpsiServer<T> {
    /**
     * 配置项
     */
    private final UpsiConfig config;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 服务端元素数组
     */
    protected ArrayList<ByteBuffer> serverElementArrayList;
    /**
     * 字节数组和对象映射
     */
    protected Map<ByteBuffer, T> byteArrayObjectMap;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractUpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, UpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        this.config = config;
    }

    @Override
    public UpsiFactory.UpsiType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(UpsiParams upsiParams) {
        assert upsiParams.maxClientSize() >= 1;
        maxClientElementSize = upsiParams.maxClientSize();
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientElementSize) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert serverElementSet.size() >= 1;
        this.serverElementArrayList = serverElementSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(senderElement -> {
                assert !senderElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        serverElementSize = serverElementSet.size();
        assert clientElementSize >= 1 && clientElementSize <= maxClientElementSize;
        this.clientElementSize = clientElementSize;
        this.byteArrayObjectMap = new HashMap<>(clientElementSize);
        serverElementSet.forEach(serverElement ->
            this.byteArrayObjectMap.put(ByteBuffer.wrap(ObjectUtils.objectToByteArray(serverElement)), serverElement)
        );
        extraInfo++;
    }
}