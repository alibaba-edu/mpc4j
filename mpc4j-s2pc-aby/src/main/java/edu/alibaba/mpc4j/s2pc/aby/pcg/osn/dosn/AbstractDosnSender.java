package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.stream.IntStream;

/**
 * abstract Decision OSN sender.
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public abstract class AbstractDosnSender extends AbstractTwoPartyPto implements DosnSender {
    /**
     * config
     */
    protected final DosnConfig config;
    /**
     * input vector length
     */
    protected int num;
    /**
     * input byte length
     */
    protected int byteLength;
    /**
     * input vector
     */
    protected byte[][] inputVector;

    protected AbstractDosnSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, DosnConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(byte[][] inputVector, int byteLength) {
        checkInitialized();
        this.byteLength = byteLength;
        MathPreconditions.checkGreater("num", inputVector.length, 1);
        IntStream.range(0, inputVector.length).forEach(i -> {
            byte[] input = inputVector[i];
            MathPreconditions.checkEqual("byteLength", i + "-th input.length", byteLength, input.length);
        });
        this.inputVector = inputVector;
        num = inputVector.length;
        extraInfo++;
    }
}
