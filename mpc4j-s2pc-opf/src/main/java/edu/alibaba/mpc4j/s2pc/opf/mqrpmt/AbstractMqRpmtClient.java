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
 * abstract mqRPMT client.
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public abstract class AbstractMqRpmtClient extends AbstractTwoPartyPto implements MqRpmtClient {
    /**
     * ⊥
     */
    protected static final ByteBuffer BOT_ELEMENT_BYTE_BUFFER;

    static {
        byte[] botElementByteArray = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        BOT_ELEMENT_BYTE_BUFFER = ByteBuffer.wrap(botElementByteArray);
    }

    /**
     * mqRPMT config
     */
    protected final MqRpmtConfig mqRpmtConfig;
    /**
     * max client element size.
     */
    private int maxClientElementSize;
    /**
     * max server element size.
     */
    protected int maxServerElementSize;
    /**
     * client element array list.
     */
    protected ArrayList<ByteBuffer> clientElementArrayList;
    /**
     * client element size.
     */
    protected int clientElementSize;
    /**
     * server element size.
     */
    protected int serverElementSize;

    protected AbstractMqRpmtClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, MqRpmtConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        mqRpmtConfig = config;
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
