package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract Batched Share Translation receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public abstract class AbstractBstReceiver extends AbstractTwoPartyPto implements BstReceiver {
    /**
     * config
     */
    protected final BstConfig config;
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

    protected AbstractBstReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BstConfig config) {
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
        MathPreconditions.checkPositive("n", eachNum);
        this.eachNum = eachNum;
        MathPreconditions.checkPositive("byte_length", byteLength);
        this.byteLength = byteLength;
        cotNum = BstFactory.getPrecomputeNum(config, batchNum, eachNum);
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, eachNum, byteLength);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preSenderOutput.getNum(), BstFactory.getPrecomputeNum(config, batchNum, eachNum)
            );
        }
    }
}
