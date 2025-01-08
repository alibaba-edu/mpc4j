package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Abstract partial ST receiver
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public abstract class AbstractPstReceiver extends AbstractTwoPartyPto implements PstReceiver {
    /**
     * config
     */
    protected final PstConfig config;
    /**
     * batch num
     */
    protected int batchNum;
    /**
     * each num
     */
    protected int eachNum;
    /**
     * element byte length
     */
    protected int byteLength;
    /**
     * number of COTs
     */
    protected int cotNum;

    protected AbstractPstReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, PstConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int batchNum, int eachNum, int byteLength) {
        checkInitialized();
        MathPreconditions.checkPositive("batchNum", batchNum);
        this.batchNum = batchNum;
        Preconditions.checkArgument(IntMath.isPowerOfTwo(eachNum), "eachNum must be a power of 2: %s", eachNum);
        this.eachNum = eachNum;
        MathPreconditions.checkPositive("byte_length", byteLength);
        this.byteLength = byteLength;
        cotNum = PstFactory.getPrecomputeNum(config, batchNum, eachNum);
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, eachNum, byteLength);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preSenderOutput.getNum(), PstFactory.getPrecomputeNum(config, batchNum, eachNum)
            );
        }
    }
}
