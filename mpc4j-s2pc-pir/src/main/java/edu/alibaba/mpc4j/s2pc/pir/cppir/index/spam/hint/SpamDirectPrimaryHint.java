package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;

/**
 * directly generated primary hint for SPAM PIR, which contains a PRF key and a parity and indexes in the set are all
 * from the PRF key.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class SpamDirectPrimaryHint extends AbstractRandomCutoffSpamHint implements SpamPrimaryHint {
    /**
     * the extra one more chunk ID
     */
    private final int extraChunkId;
    /**
     * parity
     */
    private final byte[] parity;

    /**
     * Creates a hint with a random hint ID.
     *
     * @param chunkSize    chunk size.
     * @param chunkNum     chunk num.
     * @param l            parity bit length.
     * @param secureRandom the random state.
     */
    public SpamDirectPrimaryHint(int chunkSize, int chunkNum, int l, SecureRandom secureRandom) {
        super(chunkSize, chunkNum, l, secureRandom);
        // We need to find a random Chunk ID among the ChunkNum / 2 unselected chunks. An easy and effective way to do
        // so is to simply keep picking random Chunk IDs and checking if the Chunk ID is already selected.
        TIntSet vectorV = new TIntHashSet(chunkNum / 2);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (vs[chunkId] < cutoff) {
                vectorV.add(chunkId);
            }
        }
        assert vectorV.size() == chunkNum / 2 : "|V| must be equal to " + chunkNum / 2 + ": " + vectorV.size();
        int tryExtraChunkId = -1;
        boolean success = false;
        while (!success) {
            tryExtraChunkId = secureRandom.nextInt(chunkNum);
            success = (!vectorV.contains(tryExtraChunkId));
        }
        extraChunkId = tryExtraChunkId;
        // initialize the parity to zero
        parity = new byte[byteL];
        // clean vs
        vs = null;
    }

    @Override
    public boolean containsChunkId(int chunkId) {
        // the straightforward case is that the extra index e_j equals i
        if (chunkId == extraChunkId) {
            return true;
        }
        // The other case is the selection process involving the median cutoff. For each hint j, the client computes
        // v_{j, l} and checks if v_{j, l} is smaller than ^v_j. If so, it means hint j selects partition l.
        double vl = getDouble(chunkId);
        return vl < cutoff;
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        return getInteger(chunkId);
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
}
