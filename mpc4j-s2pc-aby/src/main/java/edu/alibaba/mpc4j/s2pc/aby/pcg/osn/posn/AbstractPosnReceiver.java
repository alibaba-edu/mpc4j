package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;

/**
 * abstract pre-computed OSN receiver.
 *
 * @author Feng Han
 * @date 2024/05/08
 */
public abstract class AbstractPosnReceiver extends AbstractTwoPartyPto implements PosnReceiver {
    /**
     * input vector length
     */
    protected int num;
    /**
     * permutation π
     */
    protected int[] pi;
    /**
     * pre-computed osn result
     */
    protected RosnReceiverOutput preRosnReceiverOutput;

    protected AbstractPosnReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, PosnConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int[] pi, int byteLength, RosnReceiverOutput preRosnReceiverOutput) {
        checkInitialized();
        MathPreconditions.checkGreater("num", pi.length, 1);
        this.num = pi.length;
        this.pi = pi;
        MathPreconditions.checkEqual("num", "pre compute osn number", num, preRosnReceiverOutput.getNum());
        MathPreconditions.checkNonNegative("byteLength", byteLength);
        MathPreconditions.checkEqual("byteLength", "pre compute osn byteLength", byteLength, preRosnReceiverOutput.getByteLength());
        this.preRosnReceiverOutput = preRosnReceiverOutput;
    }
}
