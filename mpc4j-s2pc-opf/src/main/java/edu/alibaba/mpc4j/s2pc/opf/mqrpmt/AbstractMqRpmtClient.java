package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

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
 * mqRPMT协议客户端抽象类。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public abstract class AbstractMqRpmtClient extends AbstractTwoPartyPto implements MqRpmtClient {
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
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        MathPreconditions.checkGreater("maxClientElementSize", maxClientElementSize, 1);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkGreater("maxServerElementSize", maxServerElementSize, 1);
        this.maxServerElementSize = maxServerElementSize;
        extraInfo++;
        initState();
    }

    protected void setPtoInput(Set<ByteBuffer> clientElementSet, int serverElementSize) {
        checkInitialized();
        MathPreconditions.checkGreater("clientElementSize", clientElementSet.size(), 1);
        MathPreconditions.checkLessOrEqual("clientElementSize", clientElementSet.size(), maxClientElementSize);
        clientElementArrayList = clientElementSet.stream()
            .peek(element ->
                Preconditions.checkArgument(
                    !element.equals(BOT_ELEMENT_BYTE_BUFFER), "element must not equal ⊥"
                )
            )
            .collect(Collectors.toCollection(ArrayList::new));
        clientElementSize = clientElementSet.size();
        MathPreconditions.checkGreater("serverElementSetSize", serverElementSize, 1);
        MathPreconditions.checkLessOrEqual("serverElementSetSize", serverElementSize, maxServerElementSize);
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }
}
