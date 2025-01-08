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
     * programmed index
     */
    private final int programmedIndex;
    /**
     * programmed chunk ID
     */
    private final int programmedChunkId;
    /**
     * programmed offset
     */
    private final int programmedOffset;
    /**
     * the hint has been amended
     */
    private boolean amended;

    /**
     * Creates a hint from a backup hint without knowing the amended parity.
     *
     * @param backupHint backup hint.
     * @param x          programmed x.
     */
    public PianoProgrammedPrimaryHint(PianoBackupHint backupHint, int x) {
        super(backupHint.fixedKeyPrp, backupHint.chunkSize, backupHint.chunkNum, backupHint.l);
        MathPreconditions.checkNonNegativeInRange("x", x, chunkSize * chunkNum);
        this.programmedIndex = x;
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
        amended = false;
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

    @Override
    public int[] expandOffsets() {
        int[] offsets = getIntegers();
        offsets[programmedChunkId] = programmedOffset;
        return offsets;
    }

    @Override
    public int[] expandPrpBlockOffsets(int blockChunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", blockChunkId, chunkNum);
        int[] offsets = getPrpBlockIntegers(blockChunkId);
        if (programmedChunkId - blockChunkId >= 0 && programmedChunkId - blockChunkId < offsets.length) {
            offsets[programmedChunkId - blockChunkId] = programmedOffset;
        }
        return offsets;
    }

    @Override
    public int getAmendIndex() {
        if (amended) {
            return -1;
        } else {
            return programmedIndex;
        }
    }

    @Override
    public void amendParity(byte[] parity) {
        if (amended) {
            throw new RuntimeException("It is not necessary to amend a previously amended primary hint");
        } else {
            BytesUtils.xori(this.parity, parity);
            amended = true;
        }
    }
}
