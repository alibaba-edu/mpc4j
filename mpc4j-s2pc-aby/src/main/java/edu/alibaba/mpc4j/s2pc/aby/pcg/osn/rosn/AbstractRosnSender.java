package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract Random OSN sender.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public abstract class AbstractRosnSender extends AbstractTwoPartyPto implements RosnSender {
    /**
     * config
     */
    protected final RosnConfig config;
    /**
     * num
     */
    protected int num;
    /**
     * element byte length
     */
    protected int byteLength;

    protected AbstractRosnSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, RosnConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int num, int byteLength) {
        checkInitialized();
        this.byteLength = byteLength;
        MathPreconditions.checkGreater("num", num, 1);
        this.num = num;
        extraInfo++;
    }
}
