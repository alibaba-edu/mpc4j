package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;

import java.math.BigInteger;

/**
 * Abstract Zp-core VOLE receiver.
 *
 * @author Hanwen Feng
 * @date 2022/06/13
 */
public abstract class AbstractZpCoreVoleReceiver extends AbstractTwoPartyPto implements ZpCoreVoleReceiver {
    /**
     * Δ
     */
    protected BigInteger delta;
    /**
     * the Zp instance
     */
    protected Zp zp;
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

    protected AbstractZpCoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, ZpCoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(Zp zp, BigInteger delta, int maxNum) {
        this.zp = zp;
        l = zp.getL();
        Preconditions.checkArgument(
            zp.validateRangeElement(delta),
            "Δ must be in range [0, %s): %s", zp.getRangeBound(), delta
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
