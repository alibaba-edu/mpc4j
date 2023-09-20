package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * backup hint for SPAM.
 *
 * @author Weiran Liu
 * @date 2023/8/30
 */
public class SpamBackupHint extends AbstractRandomCutoffSpamHint {
    /**
     * parity for all v < ^v
     */
    private final byte[] leftParity;
    /**
     * parity for all v > ^v
     */
    private final byte[] rightParity;

    /**
     * Creates a hint with a random hint ID.
     *
     * @param chunkSize    chunk size.
     * @param chunkNum     chunk num.
     * @param l            parity bit length.
     * @param secureRandom the random state.
     */
    public SpamBackupHint(int chunkSize, int chunkNum, int l, SecureRandom secureRandom) {
        super(chunkSize, chunkNum, l, secureRandom);
        // initialize the parity to zero
        leftParity = new byte[byteL];
        rightParity = new byte[byteL];
        // clean vs
        vs = null;
    }

    @Override
    protected double getCutoff() {
        return cutoff;
    }

    @Override
    public boolean containsChunkId(int chunkId) {
        double vl = getDouble(chunkId);
        return vl < cutoff;
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        return getInteger(chunkId);
    }

    /**
     * Gets the left parity.
     *
     * @return the left parity.
     */
    public byte[] getLeftParity() {
        return leftParity;
    }

    /**
     * Inplace XOR the current left parity with the other parity.
     *
     * @param otherParity other parity.
     */
    public void xoriLeftParity(byte[] otherParity) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(otherParity, byteL, l));
        BytesUtils.xori(leftParity, otherParity);
    }

    /**
     * Gets the right parity.
     *
     * @return the right parity.
     */
    public byte[] getRightParity() {
        return rightParity;
    }

    /**
     * Inplace XOR the current right parity with the other parity.
     *
     * @param otherParity other parity.
     */
    public void xoriRightParity(byte[] otherParity) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(otherParity, byteL, l));
        BytesUtils.xori(rightParity, otherParity);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(chunkSize)
            .append(chunkNum)
            .append(l)
            .append(hintId)
            .append(cutoff)
            .append(leftParity)
            .append(rightParity)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SpamBackupHint)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        SpamBackupHint that = (SpamBackupHint) obj;
        return new EqualsBuilder()
            .append(this.chunkSize, that.chunkSize)
            .append(this.chunkNum, that.chunkNum)
            .append(this.l, that.l)
            .append(this.hintId, that.hintId)
            .append(this.cutoff, that.cutoff)
            .append(this.leftParity, that.leftParity)
            .append(this.rightParity, that.rightParity)
            .isEquals();
    }
}
