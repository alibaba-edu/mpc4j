package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.stream.IntStream;

/**
 * Decision OSN party output.
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public class DosnPartyOutput {
    /**
     * share vector.
     */
    private final byte[][] shareVector;
    /**
     * element byte length.
     */
    private final int byteLength;

    public DosnPartyOutput(byte[][] shareVector) {
        MathPreconditions.checkGreater("shareVector.length", shareVector.length, 1);
        byteLength = shareVector[0].length;
        MathPreconditions.checkGreaterOrEqual("byte_length", byteLength, CommonConstants.STATS_BYTE_LENGTH);
        IntStream.range(0, shareVector.length).forEach(i -> {
            byte[] share = shareVector[i];
            MathPreconditions.checkEqual(
                "byte_length", i + "-th shareVector.length", byteLength, share.length
            );
        });
        this.shareVector = shareVector;
    }

    /**
     * Gets share.
     *
     * @param index index.
     * @return share.
     */
    public byte[] getShare(int index) {
        return shareVector[index];
    }

    /**
     * Gets share vector.
     *
     * @return share vector.
     */
    public byte[][] getShareVector() {
        return shareVector;
    }

    /**
     * Gets share byte length.
     *
     * @return share byte length.
     */
    public int getByteLength() {
        return byteLength;
    }

    /**
     * Gets number of elements.
     *
     * @return number of elements.
     */
    public int getN() {
        return shareVector.length;
    }
}
