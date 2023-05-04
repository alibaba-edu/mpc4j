package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Single single-point GF2K VOLE sender output.
 * <p>
 * The sender gets (x, t) with t = q + Δ·x, where Δ and q is owned by the receiver, and only the α-th x is non-zero.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Gf2kSspVoleSenderOutput implements PcgPartyOutput {
    /**
     * α
     */
    private int alpha;
    /**
     * single x
     */
    private byte[] x;
    /**
     * t array
     */
    private byte[][] ts;

    /**
     * Creates a sender output.
     *
     * @param alpha α.
     * @param x     x.
     * @param ts    t array.
     * @return a sender output.
     */
    public static Gf2kSspVoleSenderOutput create(int alpha, byte[] x, byte[][] ts) {
        Gf2kSspVoleSenderOutput receiverOutput = new Gf2kSspVoleSenderOutput();
        assert ts.length > 0 : "# of t must be greater than 0: " + ts.length;
        assert alpha >= 0 && alpha < ts.length : "α must be in range [0, " + ts.length + "): " + alpha;
        receiverOutput.alpha = alpha;
        assert x.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "x must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(x);
        receiverOutput.x = BytesUtils.clone(x);
        receiverOutput.ts = Arrays.stream(ts)
            .peek(t -> {
                assert t.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "t must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(t);
            })
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private Gf2kSspVoleSenderOutput() {
        // empty
    }

    /**
     * Gets α.
     *
     * @return α.
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Gets x.
     *
     * @return x.
     */
    public byte[] getX() {
        return x;
    }

    /**
     * Gets t.
     *
     * @param index the index.
     * @return t.
     */
    public byte[] getT(int index) {
        return ts[index];
    }

    @Override
    public int getNum() {
        return ts.length;
    }
}
