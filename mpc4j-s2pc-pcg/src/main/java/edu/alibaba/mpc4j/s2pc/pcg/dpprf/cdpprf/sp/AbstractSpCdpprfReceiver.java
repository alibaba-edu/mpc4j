package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * abstract SP-CDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public abstract class AbstractSpCdpprfReceiver extends AbstractTwoPartyPto implements SpCdpprfReceiver {
    /**
     * config
     */
    protected final SpCdpprfConfig config;
    /**
     * α
     */
    protected int alpha;
    /**
     * num
     */
    protected int num;
    /**
     * number of COTs
     */
    protected int cotNum;

    protected AbstractSpCdpprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SpCdpprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int alpha, int num) {
        checkInitialized();
        Preconditions.checkArgument(IntMath.isPowerOfTwo(num));
        this.num = num;
        cotNum = SpCdpprfFactory.getPrecomputeNum(config, num);
        MathPreconditions.checkNonNegativeInRange("α", alpha, num);
        this.alpha = alpha;
        extraInfo++;
    }

    protected void setPtoInput(int alpha, int num, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alpha, num);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preReceiverOutput.getNum(), SpCdpprfFactory.getPrecomputeNum(config, num)
            );
        }
    }
}
