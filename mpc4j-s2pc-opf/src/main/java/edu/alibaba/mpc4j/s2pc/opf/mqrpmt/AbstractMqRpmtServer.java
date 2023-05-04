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
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * mqRPMT协议服务端抽象类。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public abstract class AbstractMqRpmtServer extends AbstractTwoPartyPto implements MqRpmtServer {
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


    protected AbstractMqRpmtServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, MqRpmtConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        MathPreconditions.checkGreater("maxServerElementSize", maxServerElementSize, 1);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkGreater("maxClientElementSize", maxClientElementSize, 1);
        this.maxClientElementSize = maxClientElementSize;
        extraInfo++;
        initState();
    }

    protected void setPtoInput(Set<ByteBuffer> serverElementSet, int clientElementSize) {
        checkInitialized();
        MathPreconditions.checkGreater("serverElementSetSize", serverElementSet.size(), 1);
        MathPreconditions.checkLessOrEqual("serverElementSetSize", serverElementSet.size(), maxServerElementSize);
        serverElementArrayList = serverElementSet.stream()
            .peek(element ->
                Preconditions.checkArgument(
                    !element.equals(BOT_ELEMENT_BYTE_BUFFER), "element must not equal ⊥"
                )
            )
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(serverElementArrayList, secureRandom);
        serverElementSize = serverElementSet.size();
        MathPreconditions.checkGreater("clientElementSize", clientElementSize, 1);
        MathPreconditions.checkLessOrEqual("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        extraInfo++;
    }
}
