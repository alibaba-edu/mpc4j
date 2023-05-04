package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * 1-out-of-n OT receiver output. The receiver gets (i, r_i).
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class LnotReceiverOutput implements MergedPcgPartyOutput {
    /**
     * the choice bit length
     */
    private final int l;
    /**
     * the maximal choice
     */
    private final int n;
    /**
     * choices
     */
    private int[] choiceArray;
    /**
     * rb array
     */
    private byte[][] rbArray;

    /**
     * Creates a receiver output.
     *
     * @param l           the choice bit length.
     * @param choiceArray the choice array.
     * @param rbArray     the rb array.
     * @return a receiver output.
     */
    public static LnotReceiverOutput create(int l, int[] choiceArray, byte[][] rbArray) {
        LnotReceiverOutput receiverOutput = new LnotReceiverOutput(l);
        assert choiceArray.length > 0 : "# of choices must be greater than 0: " + choiceArray.length;
        assert choiceArray.length == rbArray.length : "# of choices must match # of rbs";
        receiverOutput.choiceArray = Arrays.stream(choiceArray)
            .peek(choice -> {
                assert choice >= 0 && choice < receiverOutput.n : "choice must be in range [0, " + receiverOutput.n + "): " + choice;
            })
            .toArray();
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * Creates an empty receiver output.
     *
     * @param l the choice bit length.
     * @return an empty receiver output.
     */
    public static LnotReceiverOutput createEmpty(int l) {
        LnotReceiverOutput receiverOutput = new LnotReceiverOutput(l);
        receiverOutput.choiceArray = new int[0];
        receiverOutput.rbArray = new byte[0][];

        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private LnotReceiverOutput(int l) {
        assert l > 0 && l <= IntUtils.MAX_L : "l must be in range (0, " + IntUtils.MAX_L + "]: " + l;
        this.l = l;
        this.n = (1 << l);
    }

    @Override
    public LnotReceiverOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        // split choice array
        int[] subChoiceArray = new int[splitNum];
        int[] remainChoiceArray = new int[num - splitNum];
        System.arraycopy(choiceArray, 0, subChoiceArray, 0, splitNum);
        System.arraycopy(choiceArray, splitNum, remainChoiceArray, 0, num - splitNum);
        choiceArray = remainChoiceArray;
        // split rb
        byte[][] subRbArray = new byte[splitNum][];
        byte[][] remainRbArray = new byte[num - splitNum][];
        System.arraycopy(rbArray, 0, subRbArray, 0, splitNum);
        System.arraycopy(rbArray, splitNum, remainRbArray, 0, num - splitNum);
        rbArray = remainRbArray;

        return LnotReceiverOutput.create(l, subChoiceArray, subRbArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // we need to reduce only if reduceNum is less than the current num.
            int[] remainChoiceArray = new int[reduceNum];
            System.arraycopy(choiceArray, 0, remainChoiceArray, 0, reduceNum);
            choiceArray = remainChoiceArray;
            byte[][] remainRbArray = new byte[reduceNum][];
            System.arraycopy(rbArray, 0, remainRbArray, 0, reduceNum);
            rbArray = remainRbArray;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        LnotReceiverOutput that = (LnotReceiverOutput) other;
        assert this.l == that.l : "l mismatch";
        // copy choice array
        int[] mergeChoiceArray = new int[this.choiceArray.length + that.choiceArray.length];
        System.arraycopy(this.choiceArray, 0, mergeChoiceArray, 0, this.choiceArray.length);
        System.arraycopy(that.choiceArray, 0, mergeChoiceArray, this.choiceArray.length, that.choiceArray.length);
        choiceArray = mergeChoiceArray;
        // copy rb
        byte[][] mergeRbArray = new byte[this.rbArray.length + that.rbArray.length][];
        System.arraycopy(this.rbArray, 0, mergeRbArray, 0, this.rbArray.length);
        System.arraycopy(that.rbArray, 0, mergeRbArray, this.rbArray.length, that.rbArray.length);
        rbArray = mergeRbArray;
    }

    @Override
    public int getNum() {
        return rbArray.length;
    }

    /**
     * Gets the choice.
     *
     * @param index the index.
     * @return the choice.
     */
    public int getChoice(int index) {
        return choiceArray[index];
    }

    /**
     * Gets Rb.
     *
     * @param index the index.
     * @return Rb.
     */
    public byte[] getRb(int index) {
        return rbArray[index];
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
