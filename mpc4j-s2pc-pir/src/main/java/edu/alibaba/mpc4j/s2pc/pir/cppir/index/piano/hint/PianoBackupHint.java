package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * backup hint for PIANO PIR, which contains a PRF key and a parity under the punctured index.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoBackupHint extends AbstractPianoHint {
    /**
     * punctured chunk index
     */
    private final int puncturedChunkId;

    /**
     * Creates a hint with a random hint ID.
     *
     * @param fixedKeyPrp  fixed key PRP.
     * @param chunkSize    chunk size.
     * @param chunkNum     chunk num.
     * @param l            parity bit length.
     * @param secureRandom the random state.
     */
    public PianoBackupHint(FixedKeyPrp fixedKeyPrp,
                           int chunkSize, int chunkNum, int l, int puncturedChunkId, SecureRandom secureRandom) {
        super(fixedKeyPrp, chunkSize, chunkNum, l);
        MathPreconditions.checkNonNegativeInRange("puncturedChunkId", puncturedChunkId, chunkNum);
        this.puncturedChunkId = puncturedChunkId;
        secureRandom.nextBytes(hintId);
    }

    /**
     * Gets the punctured chunk index.
     *
     * @return the punctured chunk index.
     */
    public int getPuncturedChunkId() {
        return puncturedChunkId;
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        if (chunkId == puncturedChunkId) {
            return -1;
        } else {
            return getInteger(chunkId);
        }
    }

    @Override
    public int[] expandOffsets() {
        int[] offsets = getIntegers();
        offsets[puncturedChunkId] = -1;
        return offsets;
    }

    @Override
    public int[] expandPrpBlockOffsets(int blockChunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", blockChunkId, chunkNum);
        int[] offsets = getPrpBlockIntegers(blockChunkId);
        if (puncturedChunkId - blockChunkId >= 0 && puncturedChunkId - blockChunkId < offsets.length) {
            offsets[puncturedChunkId - blockChunkId] = -1;
        }
        return offsets;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(chunkSize)
            .append(chunkNum)
            .append(l)
            .append(hintId)
            .append(parity)
            .append(puncturedChunkId)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PianoBackupHint that)) {
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
            .append(this.parity, that.parity)
            .append(this.puncturedChunkId, that.puncturedChunkId)
            .isEquals();
    }
}
