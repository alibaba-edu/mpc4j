package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;

/**
 * abstract Random OSN receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public abstract class AbstractRosnReceiver extends AbstractTwoPartyPto implements RosnReceiver {
    /**
     * config
     */
    protected final RosnConfig config;
    /**
     * num
     */
    protected int num;
    /**
     * input byte length
     */
    protected int byteLength;
    /**
     * permutation π
     */
    protected int[] pi;

    protected AbstractRosnReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, RosnConfig config) {
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
        MathPreconditions.checkGreater("n", pi.length, 1);
        num = pi.length;
        this.pi = pi;
        extraInfo++;
    }
}
