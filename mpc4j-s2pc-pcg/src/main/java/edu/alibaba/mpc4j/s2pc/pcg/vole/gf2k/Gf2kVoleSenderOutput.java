package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * GF2K-VOLE sender output. The sender gets (x, t) with t = q + Δ·x, where Δ and q is owned by the receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Gf2kVoleSenderOutput implements MergedPcgPartyOutput {
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
     * @param xs x_i.
     * @param ts t_i.
     * @return a sender output.
     */
    public static Gf2kVoleSenderOutput create(byte[][] xs, byte[][] ts) {
        Gf2kVoleSenderOutput senderOutput = new Gf2kVoleSenderOutput();
        assert xs.length > 0 : "# of x must be greater than 0: " + xs.length;
        assert xs.length == ts.length : "# of x must be equal to # of t (" + xs.length + " : " + ts.length + ")";
        senderOutput.xs = Arrays.stream(xs)
            .peek(x -> {
                assert x.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "x must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(x);
            })
            .toArray(byte[][]::new);
        senderOutput.ts = Arrays.stream(ts)
            .peek(t -> {
                assert t.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "t must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(t);
            })
            .toArray(byte[][]::new);

        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @return an empty sender output.
     */
    public static Gf2kVoleSenderOutput createEmpty() {
        Gf2kVoleSenderOutput senderOutput = new Gf2kVoleSenderOutput();
        senderOutput.xs = new byte[0][];
        senderOutput.ts = new byte[0][];

        return senderOutput;
    }

    /**
     * private constructor.
     */
    private Gf2kVoleSenderOutput() {
        // empty
    }

    @Override
    public int getNum() {
        return xs.length;
    }

    @Override
    public Gf2kVoleSenderOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        // split x
        byte[][] subX = new byte[splitNum][];
        byte[][] remainX = new byte[num - splitNum][];
        System.arraycopy(xs, 0, subX, 0, splitNum);
        System.arraycopy(xs, splitNum, remainX, 0, num - splitNum);
        xs = remainX;
        // split t
        byte[][] subT = new byte[splitNum][];
        byte[][] remainT = new byte[num - splitNum][];
        System.arraycopy(ts, 0, subT, 0, splitNum);
        System.arraycopy(ts, splitNum, remainT, 0, num - splitNum);
        ts = remainT;

        return Gf2kVoleSenderOutput.create(subX, subT);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            byte[][] remainX = new byte[reduceNum][];
            System.arraycopy(xs, 0, remainX, 0, reduceNum);
            xs = remainX;
            byte[][] remainT = new byte[reduceNum][];
            System.arraycopy(ts, 0, remainT, 0, reduceNum);
            ts = remainT;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Gf2kVoleSenderOutput that = (Gf2kVoleSenderOutput) other;
        // merge x
        byte[][] mergeX = new byte[this.xs.length + that.xs.length][];
        System.arraycopy(this.xs, 0, mergeX, 0, this.xs.length);
        System.arraycopy(that.xs, 0, mergeX, this.xs.length, that.xs.length);
        xs = mergeX;
        // merge t
        byte[][] mergeT = new byte[this.ts.length + that.ts.length][];
        System.arraycopy(this.ts, 0, mergeT, 0, this.ts.length);
        System.arraycopy(that.ts, 0, mergeT, this.ts.length, that.ts.length);
        ts = mergeT;
    }

    /**
     * Gets x_i.
     *
     * @param index the index.
     * @return x_i.
     */
    public byte[] getX(int index) {
        return xs[index];
    }

    /**
     * Gets x.
     *
     * @return x.
     */
    public byte[][] getX() {
        return xs;
    }

    /**
     * Gets t_i.
     *
     * @param index the index.
     * @return t_i.
     */
    public byte[] getT(int index) {
        return ts[index];
    }

    /**
     * Gets t.
     *
     * @return t.
     */
    public byte[][] getT() {
        return ts;
    }
}
