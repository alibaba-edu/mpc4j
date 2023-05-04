package edu.alibaba.mpc4j.s2pc.upso.upsi;

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
import java.util.stream.Collectors;

/**
 * 非平衡PSI协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/14
 */
public abstract class AbstractUpsiServer<T> extends AbstractTwoPartyPto implements UpsiServer<T> {
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
    }

    protected void setInitInput(int maxClientElementSize) {
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientElementSize) {
        checkInitialized();
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        MathPreconditions.checkPositive("serverElementSize", serverElementSet.size());
        serverElementArrayList = serverElementSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(xi -> Preconditions.checkArgument(!xi.equals(botElementByteBuffer), "xi must not equal ⊥"))
            .collect(Collectors.toCollection(ArrayList::new));
        serverElementSize = serverElementSet.size();
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        byteArrayObjectMap = new HashMap<>(clientElementSize);
        serverElementSet.forEach(serverElement ->
            byteArrayObjectMap.put(ByteBuffer.wrap(ObjectUtils.objectToByteArray(serverElement)), serverElement)
        );
        extraInfo++;
    }
}