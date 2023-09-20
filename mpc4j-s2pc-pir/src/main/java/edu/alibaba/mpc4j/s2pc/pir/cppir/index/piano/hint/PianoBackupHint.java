package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
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
     * @param chunkSize    chunk size.
     * @param chunkNum     chunk num.
     * @param l            parity bit length.
     * @param secureRandom the random state.
     */
    public PianoBackupHint(int chunkSize, int chunkNum, int l, int puncturedChunkId, SecureRandom secureRandom) {
        super(chunkSize, chunkNum, l);
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
        Preconditions.checkArgument(chunkId != puncturedChunkId);
        return getInteger(chunkId);
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
        if (!(obj instanceof PianoBackupHint)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        PianoBackupHint that = (PianoBackupHint) obj;
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
