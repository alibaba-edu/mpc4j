package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * abstract SP-CDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public abstract class AbstractSpCdpprfSender extends AbstractTwoPartyPto implements SpCdpprfSender {
    /**
     * config
     */
    protected final SpCdpprfConfig config;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * n
     */
    protected int num;
    /**
     * log(n)
     */
    protected int cotNum;

    protected AbstractSpCdpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, SpCdpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(byte[] delta) {
        Preconditions.checkArgument(BlockUtils.valid(delta));
        this.delta = BlockUtils.clone(delta);
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        Preconditions.checkArgument(IntMath.isPowerOfTwo(num));
        this.num = num;
        cotNum = SpCdpprfFactory.getPrecomputeNum(config, num);
        extraInfo++;
    }

    protected void setPtoInput(int num, CotSenderOutput preSenderOutput) {
        setPtoInput(num);
        if (preSenderOutput != null) {
            // do not need to require equal Δ
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preSenderOutput.getNum(), SpCdpprfFactory.getPrecomputeNum(config, num)
            );
        }
    }
}
