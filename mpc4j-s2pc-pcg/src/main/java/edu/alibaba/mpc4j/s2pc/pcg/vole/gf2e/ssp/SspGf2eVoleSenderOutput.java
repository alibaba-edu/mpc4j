package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.ssp;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Single single-point VOLE sender output.
 * <p>
 * The sender gets (x, t) with t = q + Δ·x, where Δ and q is owned by the receiver, and only the α-th x is non-zero.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SspGf2eVoleSenderOutput implements PcgPartyOutput {
    /**
     * the GF2E instance
     */
    private Gf2e gf2e;
    /**
     * α
     */
    private int alpha;
    /**
     * x array
     */
    private byte[][] xs;
    /**
     * t array
     */
    private byte[][] ts;

    /**
     * Creates a sender output.
     *
     * @param alpha α.
     * @param xs    x array.
     * @param ts    t array.
     * @return a sender output.
     */
    public static SspGf2eVoleSenderOutput create(Gf2e gf2e, int alpha, byte[][] xs, byte[][] ts) {
        SspGf2eVoleSenderOutput receiverOutput = new SspGf2eVoleSenderOutput();
        receiverOutput.gf2e = gf2e;
        assert xs.length > 0 : "# of x must be greater than 0: " + xs.length;
        int num = xs.length;
        assert ts.length == num : "# of t must be equal to " + num + ": " + ts.length;
        assert alpha >= 0 && alpha < xs.length : "α must be in range [0, " + num + "): " + alpha;
        receiverOutput.alpha = alpha;
        receiverOutput.xs = Arrays.stream(xs)
            .peek(x -> {
                assert gf2e.validateElement(x) : "x must be in range [0, 2^" + gf2e.getL() + "): " + Hex.toHexString(x);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        receiverOutput.ts = Arrays.stream(ts)
            .peek(t -> {
                assert gf2e.validateElement(t) : "t must be in range [0, 2^" + gf2e.getL() + "): " + Hex.toHexString(t);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private SspGf2eVoleSenderOutput() {
        // empty
    }

    /**
     * Gets the GF2E instance.
     *
     * @return the GF2E instance.
     */
    public Gf2e getGf2e() {
        return gf2e;
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
     * @param index the index.
     * @return x.
     */
    public byte[] getX(int index) {
        return xs[index];
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
        return xs.length;
    }
}
