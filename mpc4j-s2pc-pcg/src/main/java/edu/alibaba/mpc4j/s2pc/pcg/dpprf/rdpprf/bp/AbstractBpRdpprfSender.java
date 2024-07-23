package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract batch-point RDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractBpRdpprfSender extends AbstractTwoPartyPto implements BpRdpprfSender {
    /**
     * config
     */
    protected final BpRdpprfConfig config;
    /**
     * batch num
     */
    protected int batchNum;
    /**
     * n
     */
    protected int eachNum;
    /**
     * log(n)
     */
    protected int eachLogNum;

    protected AbstractBpRdpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, BpRdpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int batchNum, int eachNum) {
        checkInitialized();
        MathPreconditions.checkPositive("batchNum", batchNum);
        this.batchNum = batchNum;
        MathPreconditions.checkPositive("eachNum", eachNum);
        this.eachNum = eachNum;
        eachLogNum = LongUtils.ceilLog2(eachNum, 1);
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int eachNum, CotSenderOutput preSenderOutput) {
        setPtoInput(batchNum, eachNum);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preSenderOutput.getNum(), BpRdpprfFactory.getPrecomputeNum(config, batchNum, eachNum)
            );
        }
    }
}
