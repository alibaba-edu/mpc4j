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
 * abstract UPSU sender.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public abstract class AbstractUpsuSender extends AbstractTwoPartyPto implements UpsuSender {
    /**
     * max sender element size
     */
    protected int maxSenderElementSize;
    /**
     * sender element list
     */
    protected List<ByteBuffer> senderElementList;
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

    protected AbstractUpsuSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, UpsuConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(int maxSenderElementSize, int receiverElementSize) {
        MathPreconditions.checkPositive("max sender element size", maxSenderElementSize);
        this.maxSenderElementSize = maxSenderElementSize;
        MathPreconditions.checkPositive("receiver element size", receiverElementSize);
        this.receiverElementSize = receiverElementSize;
        initState();
    }

    protected void setPtoInput(Set<ByteBuffer> senderElementSet, int elementByteLength) {
        checkInitialized();
        byte[] botElementByteArray = new byte[elementByteLength];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        this.botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        MathPreconditions.checkPositiveInRangeClosed("sender element size", senderElementSet.size(), maxSenderElementSize);
        this.senderElementList = senderElementSet.stream()
            .peek(xi -> Preconditions.checkArgument(!xi.equals(this.botElementByteBuffer), "xi must not equal ⊥"))
            .peek(xi -> Preconditions.checkArgument(xi.array().length == elementByteLength))
            .collect(Collectors.toCollection(ArrayList::new));

        this.senderElementSize = senderElementSet.size();
        this.elementByteLength = elementByteLength;
        extraInfo++;
    }
}