package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 关键词索引PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public abstract class AbstractKwPirServer<T> extends AbstractTwoPartyPto implements KwPirServer<T> {
    /**
     * 服务端关键词数组
     */
    protected ArrayList<ByteBuffer> keywordArrayList;
    /**
     * 关键词字节数组和关键词对象映射
     */
    protected Map<ByteBuffer, T> byteArrayObjectMap;
    /**
     * 服务端关键词数量
     */
    protected int keywordSize;
    /**
     * 标签字节长度
     */
    protected int labelByteLength;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * 关键词和标签映射
     */
    protected Map<T, ByteBuffer> keywordLabelMap;

    protected AbstractKwPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, KwPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Map<T, ByteBuffer> keywordLabelMap, int labelByteLength) {
        MathPreconditions.checkPositive("labelByteLength", labelByteLength);
        this.labelByteLength = labelByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        MathPreconditions.checkPositive("keywordNum", keywordLabelMap.size());
        this.keywordLabelMap = keywordLabelMap;
        keywordSize = keywordLabelMap.size();
        Iterator<Entry<T, ByteBuffer>> iterator = keywordLabelMap.entrySet().iterator();
        Set<T> serverElementSet = new HashSet<>();
        while (iterator.hasNext()) {
            Entry<T, ByteBuffer> entry = iterator.next();
            T item = entry.getKey();
            serverElementSet.add(item);
        }
        keywordArrayList = serverElementSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(xi -> Preconditions.checkArgument(!xi.equals(botElementByteBuffer), "xi must not equal ⊥"))
            .collect(Collectors.toCollection(ArrayList::new));
        byteArrayObjectMap = new HashMap<>(keywordSize);
        keywordLabelMap.forEach((key, value) ->
            byteArrayObjectMap.put(ByteBuffer.wrap(ObjectUtils.objectToByteArray(key)), key)
        );
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
