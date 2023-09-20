package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * programmed primary hint for PIANO.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoProgrammedPrimaryHint extends AbstractPianoHint implements PianoPrimaryHint {
    /**
     * programmed chunk ID
     */
    private final int programmedChunkId;
    /**
     * programmed offset
     */
    private final int programmedOffset;

    /**
     * Creates a hint from a backup hint.
     *
     * @param backupHint backup hint.
     * @param x          programmed x.
     * @param parity     programmed parity.
     */
    public PianoProgrammedPrimaryHint(PianoBackupHint backupHint, int x, byte[] parity) {
        super(backupHint.chunkSize, backupHint.chunkNum, backupHint.l);
        MathPreconditions.checkNonNegativeInRange("x", x, chunkSize * chunkNum);
        programmedChunkId = x / chunkSize;
        MathPreconditions.checkEqual(
            "programmedChunkId", "puncturedChunkId",
            programmedChunkId, backupHint.getPuncturedChunkId()
        );
        programmedOffset = Math.abs(x % chunkSize);
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(parity, byteL, l));
        BytesUtils.xori(hintId, backupHint.hintId);
        // newly generated parity is 0, just xor the backup hint parity
        BytesUtils.xori(this.parity, backupHint.parity);
        // xor x's parity
        BytesUtils.xori(this.parity, parity);
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        if (chunkId == programmedChunkId) {
            return programmedOffset;
        } else {
            return getInteger(chunkId);
        }
    }
}
