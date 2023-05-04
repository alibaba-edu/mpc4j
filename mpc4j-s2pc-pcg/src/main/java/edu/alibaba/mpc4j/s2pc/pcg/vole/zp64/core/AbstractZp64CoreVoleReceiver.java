package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;

/**
 * Abstract Zp64-core VOLE receiver.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public abstract class AbstractZp64CoreVoleReceiver extends AbstractTwoPartyPto implements Zp64CoreVoleReceiver {
    /**
     * Δ
     */
    protected long delta;
    /**
     * the Zp64 instance
     */
    protected Zp64 zp64;
    /**
     * l
     */
    protected int l;
    /**
     * max num
     */
    private int maxNum;
    /**
     * num
     */
    protected int num;

    protected AbstractZp64CoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Zp64CoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(Zp64 zp64, long delta, int maxNum) {
        this.zp64 = zp64;
        l = zp64.getL();
        Preconditions.checkArgument(
            zp64.validateRangeElement(delta),
            "Δ must be in range [0, %s): %s", zp64.getRangeBound(), delta
        );
        this.delta = delta;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }
}

