package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract Single Share Translation receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public abstract class AbstractSstReceiver extends AbstractTwoPartyPto implements SstReceiver {
    /**
     * config
     */
    protected final SstConfig config;
    /**
     * n
     */
    protected int num;
    /**
     * element byte length
     */
    protected int byteLength;
    /**
     * number of COTs
     */
    protected int cotNum;

    protected AbstractSstReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SstConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int num, int byteLength) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        MathPreconditions.checkPositive("byte_length", byteLength);
        this.byteLength = byteLength;
        cotNum = SstFactory.getPrecomputeNum(config, num);
        extraInfo++;
    }

    protected void setPtoInput(int num, int byteLength, CotSenderOutput preSenderOutput) {
        setPtoInput(num, byteLength);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preSenderOutput.getNum(), SstFactory.getPrecomputeNum(config, num)
            );
        }
    }
}
