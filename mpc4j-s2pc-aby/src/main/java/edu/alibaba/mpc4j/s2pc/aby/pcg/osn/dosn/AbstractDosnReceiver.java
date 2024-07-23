package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;

/**
 * abstract Decision OSN receiver.
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public abstract class AbstractDosnReceiver extends AbstractTwoPartyPto implements DosnReceiver {
    /**
     * config
     */
    protected final DosnConfig config;
    /**
     * input byte length
     */
    protected int byteLength;
    /**
     * input vector length
     */
    protected int num;
    /**
     * permutation π
     */
    protected int[] pi;

    protected AbstractDosnReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, DosnConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int[] pi, int byteLength) {
        checkInitialized();
        MathPreconditions.checkGreaterOrEqual("byteLength", byteLength, CommonConstants.STATS_BYTE_LENGTH);
        this.byteLength = byteLength;
        Preconditions.checkArgument(PermutationNetworkUtils.validPermutation(pi));
        MathPreconditions.checkGreater("num", pi.length, 1);
        num = pi.length;
        this.pi = pi;
        extraInfo++;
    }
}
