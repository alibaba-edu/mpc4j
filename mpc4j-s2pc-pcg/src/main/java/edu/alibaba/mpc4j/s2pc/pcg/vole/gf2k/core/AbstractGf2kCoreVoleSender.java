package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Abstract GF2K-core VOLE sender.
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public abstract class AbstractGf2kCoreVoleSender extends AbstractTwoPartyPto implements Gf2kCoreVoleSender {
    /**
     * the GF2K instance
     */
    protected final Gf2k gf2k;
    /**
     * l
     */
    protected int l;
    /**
     * byteL
     */
    protected int byteL;
    /**
     * max num
     */
    private int maxNum;
    /**
     * x
     */
    protected byte[][] xs;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kCoreVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kCoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        gf2k = Gf2kFactory.createInstance(envType);
    }

    protected void setInitInput(int maxNum) {
        l = gf2k.getL();
        byteL = gf2k.getByteL();
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(byte[][] xs) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", xs.length, maxNum);
        num = xs.length;
        this.xs = Arrays.stream(xs)
            .peek(xi -> Preconditions.checkArgument(
                gf2k.validateElement(xi), "xi must be in range [0, 2^%s): %s", l, Hex.toHexString(xi)
            ))
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
