package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * backup hint for MIR.
 *
 * @author Weiran Liu
 * @date 2023/8/30
 */
public class MirBackupHint extends AbstractRandomCutoffMirHint {
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
    public MirBackupHint(FixedKeyPrp fixedKeyPrp, int chunkSize, int chunkNum, int l, SecureRandom secureRandom) {
        super(fixedKeyPrp, chunkSize, chunkNum, l, secureRandom);
        // initialize the parity to zero
        leftParity = new byte[byteL];
        rightParity = new byte[byteL];
        // clean vs
        vs = null;
    }

    @Override
    public boolean containsChunkId(int chunkId) {
        int vl = getIntegerForMedian(chunkId);
        return vl < cutoff;
    }

    @Override
    public BitVector containsChunks() {
        int[] vs = getIntegersForMedian();
        BitVector contains = BitVectorFactory.createZeros(chunkNum);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (vs[chunkId] < cutoff) {
                contains.set(chunkId, true);
            }
        }
        return contains;
    }

    @Override
    public boolean[] containsChunks(int blockChunkId) {
        int[] vs = getPrpBlockIntegersForMedian(blockChunkId);
        boolean[] contains = new boolean[vs.length];
        for (int j = 0; j < vs.length; j++) {
            contains[j] = vs[j] < cutoff;
        }
        return contains;
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        return getInteger(chunkId);
    }

    @Override
    public int[] expandOffsets() {
        // We do not know which offset should be programmed. We still need to store all candidate offsets.
        return getIntegers();
    }

    @Override
    public int[] expandPrpBlockOffsets(int blockChunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", blockChunkId, chunkNum);
        // We do not know which offset should be programmed. We still need to store all candidate offsets.
        return getPrpBlockIntegers(blockChunkId);
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
        if (!(obj instanceof MirBackupHint that)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
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
