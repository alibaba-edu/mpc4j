package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Abstract ZP-core VOLE sender.
 *
 * @author Hanwen Feng
 * @date 2022/06/13
 */
public abstract class AbstractZpCoreVoleSender extends AbstractTwoPartyPto implements ZpCoreVoleSender {
    /**
     * the Zp instance
     */
    protected Zp zp;
    /**
     * l
     */
    protected int l;
    /**
     * prime byte length
     */
    protected int primeByteLength;
    /**
     * max num
     */
    private int maxNum;
    /**
     * x
     */
    protected BigInteger[] x;
    /**
     * num
     */
    protected int num;

    protected AbstractZpCoreVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, ZpCoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(Zp zp, int maxNum) {
        this.zp = zp;
        l = zp.getL();
        primeByteLength = zp.getElementByteLength();
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(BigInteger[] x) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", x.length, maxNum);
        num = x.length;
        this.x = Arrays.stream(x)
            .peek(xi ->
                Preconditions.checkArgument(
                    zp.validateElement(xi), "xi must be in range [0, %s): %s", zp.getPrime(), xi
                )
            )
            .toArray(BigInteger[]::new);
        extraInfo++;
    }
}
