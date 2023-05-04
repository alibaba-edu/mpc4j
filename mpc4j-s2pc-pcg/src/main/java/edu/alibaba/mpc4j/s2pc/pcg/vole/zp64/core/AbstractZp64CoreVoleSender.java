package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;

import java.util.Arrays;

/**
 * Abstract ZP64-core VOLE sender.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public abstract class AbstractZp64CoreVoleSender extends AbstractTwoPartyPto implements Zp64CoreVoleSender {
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
     * x
     */
    protected long[] x;
    /**
     * num
     */
    protected int num;

    protected AbstractZp64CoreVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Zp64CoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(Zp64 zp64, int maxNum) {
        this.zp64 = zp64;
        l = zp64.getL();
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(long[] x) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", x.length, maxNum);
        num = x.length;
        this.x = Arrays.stream(x)
            .peek(xi ->
                Preconditions.checkArgument(
                    zp64.validateElement(xi), "xi must be in range [0, %s): %s", zp64.getPrime(), xi
                )
            )
            .toArray();
        extraInfo++;
    }
}
