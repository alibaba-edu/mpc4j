package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * programmed primary hint for SPAM.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class SpamProgrammedPrimaryHint extends AbstractSpamHint implements SpamPrimaryHint {
    /**
     * the cutoff ^v
     */
    private final double cutoff;
    /**
     * the extra one more chunk ID
     */
    private final int extraChunkId;
    /**
     * the extra one more offset
     */
    private final int extraOffset;
    /**
     * parity
     */
    private final byte[] parity;
    /**
     * a bit indicating whether < is redefined to be "greater than" for this hint.
     */
    private final boolean flip;

    /**
     * Creates a hint from a backup hint.
     *
     * @param backupHint backup hint.
     * @param x          programmed x.
     * @param parity     programmed parity.
     */
    public SpamProgrammedPrimaryHint(SpamBackupHint backupHint, int x, byte[] parity) {
        super(backupHint.chunkSize, backupHint.chunkNum, backupHint.l);
        MathPreconditions.checkNonNegativeInRange("x", x, chunkSize * chunkNum);
        // add index x to the subset as the extra index
        extraChunkId = x / chunkSize;
        extraOffset = x % chunkSize;
        // the client picks the half that does not select the Chunk ID l that index x belongs to
        flip = backupHint.containsChunkId(extraChunkId);
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(parity, byteL, l));
        BytesUtils.xori(hintId, backupHint.hintId);
        // initialize ^v and e
        cutoff = backupHint.getCutoff();
        // initialize the parity to zero
        this.parity = new byte[byteL];
        // xor x's parity
        BytesUtils.xori(this.parity, parity);
        if (flip) {
            // if < is redefined to be "greater than", xor the right backup hint parity
            BytesUtils.xori(this.parity, backupHint.getRightParity());
        } else {
            // if < is still "less than", xor the left backup hint parity
            BytesUtils.xori(this.parity, backupHint.getLeftParity());
        }
    }

    @Override
    public boolean containsChunkId(int chunkId) {
        // the straightforward case is that the extra index e_j equals i
        if (chunkId == extraChunkId) {
            return true;
        }
        // The other case is the selection process involving the median cutoff. For each hint j, the client computes
        // v_{j, l} and checks if v_{j, l} is < (!flip) or > (flip) ^v_j. If so, it means hint j selects partition l.
        double vl = getDouble(chunkId);
        return flip ? vl > cutoff : vl < cutoff;
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        if (chunkId == extraChunkId) {
            return extraOffset;
        } else {
            return getInteger(chunkId);
        }
    }

    @Override
    public byte[] getParity() {
        return parity;
    }

    @Override
    public void xori(byte[] otherParity) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(otherParity, byteL, l));
        BytesUtils.xori(parity, otherParity);
    }

    @Override
    protected double getCutoff() {
        return cutoff;
    }
}
