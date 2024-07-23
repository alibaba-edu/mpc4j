package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import java.util.stream.IntStream;

/**
 * abstract pre-computed OSN sender.
 *
 * @author Feng Han
 * @date 2024/05/08
 */
public abstract class AbstractPosnSender extends AbstractTwoPartyPto implements PosnSender {
    /**
     * num
     */
    protected int num;
    /**
     * pre-computed osn result
     */
    protected RosnSenderOutput preRosnSenderOutput;

    protected AbstractPosnSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, PosnConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(byte[][] inputVector, RosnSenderOutput preRosnSenderOutput) {
        checkInitialized();
        MathPreconditions.checkGreater("num", inputVector.length, 1);
        this.num = inputVector.length;
        int byteLength = inputVector[0].length;
        MathPreconditions.checkEqual("byteLength", "posn byte length", byteLength, preRosnSenderOutput.getByteLength());
        IntStream.range(1, inputVector.length).forEach(i -> {
            byte[] input = inputVector[i];
            MathPreconditions.checkEqual("byteLength", i + "-th input.length", byteLength, input.length);
        });
        this.preRosnSenderOutput = preRosnSenderOutput;
    }
}
