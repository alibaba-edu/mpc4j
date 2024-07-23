package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 1-out-of-n OT sender output, where n = 2^l. The sender gets r_0, r_1, ..., r_{n - 1}.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class LnotSenderOutput implements MergedPcgPartyOutput {
    /**
     * choice bit length
     */
    private final int l;
    /**
     * maximal choice
     */
    private final int n;
    /**
     * rs array
     */
    private byte[][][] rsArray;

    /**
     * Creates a sender output.
     *
     * @param l       choice bit length.
     * @param rsArray rs array.
     * @return a sender output.
     */
    public static LnotSenderOutput create(int l, byte[][][] rsArray) {
        LnotSenderOutput senderOutput = new LnotSenderOutput(l);
        senderOutput.rsArray = Arrays.stream(rsArray)
            .peek(rs -> {
                MathPreconditions.checkEqual("n", "rs.length", senderOutput.n, rs.length);
                Arrays.stream(rs).forEach(r ->
                    MathPreconditions.checkEqual("r.length", "λ", r.length, CommonConstants.BLOCK_BYTE_LENGTH)
                );
            })
            .toArray(byte[][][]::new);

        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @param l choice bit length.
     * @return an empty sender output.
     */
    public static LnotSenderOutput createEmpty(int l) {
        LnotSenderOutput senderOutput = new LnotSenderOutput(l);
        senderOutput.rsArray = new byte[0][][];
        return senderOutput;
    }

    /**
     * Creates a random sender output.
     *
     * @param num          num.
     * @param l            choice bit length.
     * @param secureRandom random state.
     * @return a random sender output.
     */
    public static LnotSenderOutput createRandom(int num, int l, SecureRandom secureRandom) {
        LnotSenderOutput senderOutput = new LnotSenderOutput(l);
        senderOutput.rsArray = IntStream.range(0, num)
            .mapToObj(i -> BytesUtils.randomByteArrayVector(senderOutput.n, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom))
            .toArray(byte[][][]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private LnotSenderOutput(int l) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, IntUtils.MAX_L);
        this.l = l;
        this.n = (1 << l);
    }

    @Override
    public LnotSenderOutput copy() {
        LnotSenderOutput copy = new LnotSenderOutput(l);
        copy.rsArray = BytesUtils.clone(rsArray);
        return copy;
    }

    @Override
    public LnotSenderOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        byte[][][] rsSubArray = new byte[splitNum][][];
        byte[][][] rsRemainArray = new byte[num - splitNum][][];
        System.arraycopy(rsArray, num - splitNum, rsSubArray, 0, splitNum);
        System.arraycopy(rsArray, 0, rsRemainArray, 0, num - splitNum);
        rsArray = rsRemainArray;

        return LnotSenderOutput.create(l, rsSubArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
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
        MathPreconditions.checkEqual("this.l", "that.l", this.l, that.l);
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(l)
            .append(rsArray)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LnotSenderOutput that) {
            return new EqualsBuilder()
                .append(this.l, that.l)
                .append(this.rsArray, that.rsArray)
                .isEquals();
        }
        return false;
    }
}
