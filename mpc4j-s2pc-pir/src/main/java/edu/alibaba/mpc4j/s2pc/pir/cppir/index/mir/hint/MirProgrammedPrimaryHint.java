package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * programmed primary hint for MIR.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class MirProgrammedPrimaryHint extends AbstractMirHint implements MirPrimaryHint {
    /**
     * the cutoff ^v
     */
    protected final int cutoff;
    /**
     * programmed index
     */
    private final int programmedIndex;
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
     * the hint has been amended
     */
    private boolean amended;

    /**
     * Creates a hint from a backup hint.
     *
     * @param backupHint backup hint.
     * @param x          programmed x.
     */
    public MirProgrammedPrimaryHint(MirBackupHint backupHint, int x) {
        super(backupHint.fixedKeyPrp, backupHint.chunkSize, backupHint.chunkNum, backupHint.l);
        MathPreconditions.checkNonNegativeInRange("x", x, chunkSize * chunkNum);
        // add index x to the subset as the extra index
        programmedIndex = x;
        extraChunkId = x / chunkSize;
        extraOffset = x % chunkSize;
        // the client picks the half that does not select the Chunk ID l that index x belongs to
        flip = backupHint.containsChunkId(extraChunkId);
        BytesUtils.xori(hintId, backupHint.hintId);
        // initialize ^v and e
        cutoff = backupHint.getCutoff();
        // initialize the parity to zero
        this.parity = new byte[byteL];
        if (flip) {
            // if < is redefined to be "greater than", xor the right backup hint parity
            BytesUtils.xori(this.parity, backupHint.getRightParity());
        } else {
            // if < is still "less than", xor the left backup hint parity
            BytesUtils.xori(this.parity, backupHint.getLeftParity());
        }
        amended = false;
    }

    @Override
    public boolean containsChunkId(int chunkId) {
        // the straightforward case is that the extra index e_j equals i
        if (chunkId == extraChunkId) {
            return true;
        }
        // The other case is the selection process involving the median cutoff. For each hint j, the client computes
        int vl = getIntegerForMedian(chunkId);
        return flip == (vl >= cutoff);
    }

    @Override
    public BitVector containsChunks() {
        int[] vs = getIntegersForMedian();
        BitVector contains = BitVectorFactory.createZeros(chunkNum);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (chunkId == extraChunkId) {
                contains.set(chunkId, true);
            } else {
                if (!flip) {
                    if (vs[chunkId] < cutoff) {
                        contains.set(chunkId, true);
                    }
                } else {
                    if (vs[chunkId] >= cutoff) {
                        contains.set(chunkId, true);
                    }
                }
            }
        }
        return contains;
    }

    @Override
    public boolean[] containsChunks(int blockChunkId) {
        int[] vs = getPrpBlockIntegersForMedian(blockChunkId);
        boolean[] contains = new boolean[vs.length];
        for (int j = 0; j < vs.length; j++) {
            int chunkId = blockChunkId + j;
            if (chunkId == extraChunkId) {
                contains[j] = true;
            } else {
                contains[j] = (flip == (vs[j] >= cutoff));
            }
        }
        return contains;
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
    public int[] expandOffsets() {
        int[] offsets = getIntegers();
        offsets[extraChunkId] = extraOffset;
        return offsets;
    }

    @Override
    public int[] expandPrpBlockOffsets(int blockChunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", blockChunkId, chunkNum);
        int[] offsets = getPrpBlockIntegers(blockChunkId);
        if (extraChunkId - blockChunkId >= 0 && extraChunkId - blockChunkId < offsets.length) {
            offsets[extraChunkId - blockChunkId] = extraOffset;
        }
        return offsets;
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

    @Override
    protected int getCutoff() {
        return cutoff;
    }
}
