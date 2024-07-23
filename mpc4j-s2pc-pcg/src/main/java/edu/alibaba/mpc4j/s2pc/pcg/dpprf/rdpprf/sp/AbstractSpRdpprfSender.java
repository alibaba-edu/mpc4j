package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract single-point RDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractSpRdpprfSender extends AbstractTwoPartyPto implements SpRdpprfSender {
    /**
     * config
     */
    protected final SpRdpprfConfig config;
    /**
     * n
     */
    protected int num;
    /**
     * log(n)
     */
    protected int logNum;

    protected AbstractSpRdpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, SpRdpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("n", num);
        this.num = num;
        logNum = SpRdpprfFactory.getPrecomputeNum(config, num);
        extraInfo++;
    }

    protected void setPtoInput(int num, CotSenderOutput preSenderOutput) {
        setPtoInput(num);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preSenderOutput.getNum(), SpRdpprfFactory.getPrecomputeNum(config, num)
            );
        }
    }
}
