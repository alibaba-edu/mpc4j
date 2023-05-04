package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * 1-out-of-n OT sender output, where n = 2^l. The sender gets r_0, r_1, ..., r_{n - 1}.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class LnotSenderOutput implements MergedPcgPartyOutput {
    /**
     * the choice bit length
     */
    private final int l;
    /**
     * the maximal choice
     */
    private final int n;
    /**
     * rs array
     */
    private byte[][][] rsArray;

    /**
     * Creates a sender output.
     *
     * @param l       the choice bit length.
     * @param rsArray the rs array.
     * @return a sender output.
     */
    public static LnotSenderOutput create(int l, byte[][][] rsArray) {
        LnotSenderOutput senderOutput = new LnotSenderOutput(l);
        assert rsArray.length > 0 : "# of rs must be greater than 0: " + rsArray.length;
        senderOutput.rsArray = Arrays.stream(rsArray)
            .peek(rs -> {
                assert rs.length == senderOutput.n : "# of r must be equal to " + senderOutput.n + ": " + rs.length;
                Arrays.stream(rs).forEach(r -> {
                    assert r.length == CommonConstants.BLOCK_BYTE_LENGTH;
                });
            })
            .toArray(byte[][][]::new);

        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @param l the choice bit length.
     * @return an empty sender output.
     */
    public static LnotSenderOutput createEmpty(int l) {
        LnotSenderOutput senderOutput = new LnotSenderOutput(l);
        senderOutput.rsArray = new byte[0][][];

        return senderOutput;
    }

    /**
     * private constructor.
     */
    private LnotSenderOutput(int l) {
        assert l > 0 && l <= IntUtils.MAX_L : "l must be in range (0, " + IntUtils.MAX_L + "]: " + l;
        this.l = l;
        this.n = (1 << l);
    }

    @Override
    public LnotSenderOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        byte[][][] rsSubArray = new byte[splitNum][][];
        byte[][][] rsRemainArray = new byte[num - splitNum][][];
        System.arraycopy(rsArray, 0, rsSubArray, 0, splitNum);
        System.arraycopy(rsArray, splitNum, rsRemainArray, 0, num - splitNum);
        rsArray = rsRemainArray;

        return LnotSenderOutput.create(l, rsSubArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // we need to reduce only if reduceNum is less than the current num.
            byte[][][] rsRemainArray = new byte[reduceNum][][];
            System.arraycopy(rsArray, 0, rsRemainArray, 0, reduceNum);
            rsArray = rsRemainArray;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        LnotSenderOutput that = (LnotSenderOutput) other;
        assert this.l == that.l : "l mismatch";
        byte[][][] mergeRsArray = new byte[this.rsArray.length + that.rsArray.length][][];
        System.arraycopy(this.rsArray, 0, mergeRsArray, 0, this.rsArray.length);
        System.arraycopy(that.rsArray, 0, mergeRsArray, this.rsArray.length, that.rsArray.length);
        rsArray = mergeRsArray;
    }

    @Override
    public int getNum() {
        return rsArray.length;
    }

    /**
     * Gets Rb.
     *
     * @param index  the index.
     * @param choice the choice.
     * @return Rb.
     */
    public byte[] getRb(int index, int choice) {
        return rsArray[index][choice];
    }

    /**
     * Gets rs.
     *
     * @param index the index.
     * @return rs.
     */
    public byte[][] getRs(int index) {
        return rsArray[index];
    }

    /**
     * Gets the choice bit length.
     *
     * @return the choice bit length.
     */
    public int getL() {
        return l;
    }

    /**
     * Gets the maximal choice.
     *
     * @return maximal choice.
     */
    public int getN() {
        return n;
    }
}
