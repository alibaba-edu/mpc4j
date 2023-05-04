package edu.alibaba.mpc4j.s2pc.pso.psu;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

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
public abstract class AbstractPsuClient extends AbstractTwoPartyPto implements PsuClient {
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
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        MathPreconditions.checkGreater("maxClientElementSize", maxClientElementSize, 1);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkGreater("maxServerElementSize", maxServerElementSize, 1);
        this.maxServerElementSize = maxServerElementSize;
        initState();
    }

    protected void setPtoInput(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength) {
        checkInitialized();
        MathPreconditions.checkGreaterOrEqual("elementByteLength", elementByteLength, CommonConstants.STATS_BYTE_LENGTH);
        this.elementByteLength = elementByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        MathPreconditions.checkGreater("clientElementSize", clientElementSet.size(), 1);
        MathPreconditions.checkLessOrEqual("clientElementSize", clientElementSet.size(), maxClientElementSize);
        clientElementSize = clientElementSet.size();
        clientElementArrayList = clientElementSet.stream()
            .peek(yi -> {
                MathPreconditions.checkEqual("yi.length", "elementByteLength", yi.array().length, elementByteLength);
                Preconditions.checkArgument(!yi.equals(botElementByteBuffer), "element must not equal ⊥");
            })
            .collect(Collectors.toCollection(ArrayList::new));
        MathPreconditions.checkGreater("serverElementSize", serverElementSize, 1);
        MathPreconditions.checkLessOrEqual("serverElementSize", serverElementSize, maxServerElementSize);
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }
}
