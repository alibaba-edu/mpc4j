package edu.alibaba.mpc4j.s2pc.pir.keyword;

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
 * 关键词索引PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public abstract class AbstractKwPirClient<T> extends AbstractSecureTwoPartyPto implements KwPirClient<T> {
    /**
     * 配置项
     */
    private final KwPirConfig config;
    /**
     * 客户端单次查询最大查询关键词数目
     */
    protected int maxRetrievalSize;
    /**
     * 标签字节长度
     */
    protected int labelByteLength;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * 客户端关键词数组
     */
    protected ArrayList<ByteBuffer> retrievalArrayList;
    /**
     * 关键词字节数组和关键词对象映射
     */
    protected Map<ByteBuffer, T> byteArrayObjectMap;
    /**
     * 客户端关键词数量
     */
    protected int retrievalSize;

    protected AbstractKwPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, KwPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        this.config = config;
    }

    @Override
    public KwPirFactory.KwPirType getPtoType() {
        return config.getProType();
    }

    protected void setInitInput(KwPirParams kwPirParams, int labelByteLength) {
        assert labelByteLength >= 1;
        this.labelByteLength = labelByteLength;
        maxRetrievalSize = kwPirParams.maxRetrievalSize();
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<T> clientKeywordSet) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        retrievalSize = clientKeywordSet.size();
        assert retrievalSize > 0 && retrievalSize <= maxRetrievalSize
            : "ClientKeywordSize must be in range (0, " + maxRetrievalSize + "]: " + retrievalSize;
        retrievalArrayList = clientKeywordSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(clientElement -> {
                assert !clientElement.equals(botElementByteBuffer) : "input equals ⊥";
            })
            .collect(Collectors.toCollection(ArrayList::new));
        byteArrayObjectMap = new HashMap<>(retrievalSize);
        clientKeywordSet.forEach(clientElementObject ->
            byteArrayObjectMap.put(
                ByteBuffer.wrap(ObjectUtils.objectToByteArray(clientElementObject)), clientElementObject
            )
        );
        extraInfo++;
    }
}
