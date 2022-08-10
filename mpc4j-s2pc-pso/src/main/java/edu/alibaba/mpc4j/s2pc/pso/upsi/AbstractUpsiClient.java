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
 * 非平衡PSI协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public abstract class AbstractUpsiClient<T> extends AbstractSecureTwoPartyPto implements UpsiClient<T> {
    /**
     * 配置项
     */
    private final UpsiConfig config;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 客户端元素数组
     */
    protected ArrayList<ByteBuffer> clientElementArrayList;
    /**
     * 字节数组和对象映射
     */
    protected Map<ByteBuffer, T> byteArrayObjectMap;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractUpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, UpsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
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

    protected void setPtoInput(Set<T> clientElementSet) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        assert clientElementSet.size() >= 1 && clientElementSet.size() <= maxClientElementSize;
        this.clientElementArrayList = clientElementSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(clientElement -> {
                assert !clientElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        clientElementSize = clientElementSet.size();
        this.byteArrayObjectMap = new HashMap<>(clientElementSize);
        clientElementSet.forEach(clientElement ->
            this.byteArrayObjectMap.put(ByteBuffer.wrap(ObjectUtils.objectToByteArray(clientElement)), clientElement)
        );
        extraInfo++;
    }
}