package edu.alibaba.mpc4j.s2pc.upso.upsu;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * abstract UPSU receiver.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public abstract class AbstractUpsuReceiver extends AbstractTwoPartyPto implements UpsuReceiver {
    /**
     * max sender element size
     */
    protected int maxSenderElementSize;
    /**
     * receiver element list
     */
    protected List<ByteBuffer> receiverElementList;
    /**
     * sender element size
     */
    protected int senderElementSize;
    /**
     * receiver element size
     */
    protected int receiverElementSize;
    /**
     * bot element bytebuffer
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * element byte length
     */
    protected int elementByteLength;

    protected AbstractUpsuReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, UpsuConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(Set<ByteBuffer> receiverElementSet, int maxSenderElementSize, int elementByteLength) {
        MathPreconditions.checkPositive("max sender element size", maxSenderElementSize);
        this.maxSenderElementSize = maxSenderElementSize;
        MathPreconditions.checkPositive("max receiver element size", receiverElementSet.size());
        this.receiverElementSize = receiverElementSet.size();
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        this.botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        this.receiverElementList = receiverElementSet.stream()
            .peek(yi -> Preconditions.checkArgument(!yi.equals(botElementByteBuffer), "yi must not equal ⊥"))
            .peek(yi -> Preconditions.checkArgument(yi.array().length == elementByteLength))
            .collect(Collectors.toCollection(ArrayList::new));
        this.elementByteLength = elementByteLength;
        initState();
    }

    protected void setPtoInput(int senderElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("sender element size", senderElementSize, maxSenderElementSize);
        this.senderElementSize = senderElementSize;
        extraInfo++;
    }
}