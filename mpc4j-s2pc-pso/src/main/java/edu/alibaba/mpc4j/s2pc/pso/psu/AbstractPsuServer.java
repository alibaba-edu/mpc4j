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
 * PSU协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public abstract class AbstractPsuServer extends AbstractTwoPartyPto implements PsuServer {
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
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        MathPreconditions.checkGreater("maxServerElementSize", maxServerElementSize, 1);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkGreater("maxClientElementSize", maxClientElementSize, 1);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) {
        checkInitialized();
        MathPreconditions.checkGreaterOrEqual("elementByteLength", elementByteLength, CommonConstants.STATS_BYTE_LENGTH);
        this.elementByteLength = elementByteLength;
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        MathPreconditions.checkGreater("serverElementSize", serverElementSet.size(), 1);
        MathPreconditions.checkLessOrEqual("serverElementSize", serverElementSet.size(), maxServerElementSize);
        serverElementSize = serverElementSet.size();
        serverElementArrayList = serverElementSet.stream()
            .peek(xi -> {
                MathPreconditions.checkEqual("xi.length", "elementByteLength", xi.array().length, elementByteLength);
                Preconditions.checkArgument(!xi.equals(botElementByteBuffer), "element must not equal ⊥");
            })
            .collect(Collectors.toCollection(ArrayList::new));
        MathPreconditions.checkGreater("clientElementSize", clientElementSize, 1);
        MathPreconditions.checkLessOrEqual("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        extraInfo++;
    }
}
