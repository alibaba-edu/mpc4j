package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * GF2K-VOLE receiver output. The receiver gets (Δ, q) with t = q + Δ · x, where x and t are owned by the sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Gf2kVoleReceiverOutput implements MergedPcgPartyOutput {
    /**
     * Δ
     */
    private byte[] delta;
    /**
     * q array
     */
    private byte[][] qs;

    /**
     * Creates a receiver output.
     *
     * @param delta Δ.
     * @param qs    q_i.
     * @return the receiver output.
     */
    public static Gf2kVoleReceiverOutput create(byte[] delta, byte[][] qs) {
        Gf2kVoleReceiverOutput receiverOutput = new Gf2kVoleReceiverOutput();
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "Δ must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(delta);
        receiverOutput.delta = BytesUtils.clone(delta);
        assert qs.length > 0 : "# of q must be greater than 0: " + qs.length;
        receiverOutput.qs = Arrays.stream(qs)
            .peek(q -> {
                assert q.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "q must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(q);
            })
            .toArray(byte[][]::new);

        return receiverOutput;
    }

    /**
     * Creates an empty receiver output.
     *
     * @param delta Δ.
     * @return an empty receiver output.
     */
    public static Gf2kVoleReceiverOutput createEmpty(byte[] delta) {
        Gf2kVoleReceiverOutput receiverOutput = new Gf2kVoleReceiverOutput();
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "Δ must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(delta);
        receiverOutput.delta = BytesUtils.clone(delta);
        receiverOutput.qs = new byte[0][];

        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private Gf2kVoleReceiverOutput() {
        // empty
    }

    @Override
    public int getNum() {
        return qs.length;
    }

    @Override
    public Gf2kVoleReceiverOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]";
        // split q
        byte[][] subQ = new byte[splitNum][];
        byte[][] remainQ = new byte[num - splitNum][];
        System.arraycopy(qs, 0, subQ, 0, splitNum);
        System.arraycopy(qs, splitNum, remainQ, 0, num - splitNum);
        qs = remainQ;

        return Gf2kVoleReceiverOutput.create(delta, subQ);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]";
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            byte[][] remainQ = new byte[reduceNum][];
            System.arraycopy(qs, 0, remainQ, 0, reduceNum);
            qs = remainQ;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Gf2kVoleReceiverOutput that = (Gf2kVoleReceiverOutput) other;
        assert Arrays.equals(this.delta, that.delta) : "merged outputs must have the same Δ";
        // merge q
        byte[][] mergeQ = new byte[this.qs.length + that.qs.length][];
        System.arraycopy(this.qs, 0, mergeQ, 0, this.qs.length);
        System.arraycopy(that.qs, 0, mergeQ, this.qs.length, that.qs.length);
        qs = mergeQ;
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    /**
     * Gets q_i.
     *
     * @param index the index.
     * @return q_i.
     */
    public byte[] getQ(int index) {
        return qs[index];
    }

    /**
     * Gets q.
     *
     * @return q.
     */
    public byte[][] getQ() {
        return qs;
    }
}
