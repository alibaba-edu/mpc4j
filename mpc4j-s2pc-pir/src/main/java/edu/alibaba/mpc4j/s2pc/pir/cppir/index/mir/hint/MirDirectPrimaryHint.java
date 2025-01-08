package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;

import java.security.SecureRandom;

/**
 * directly generated primary hint for MIR, which contains a PRF key and a parity and indexes in the set are all
 * from the PRF key.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class MirDirectPrimaryHint extends AbstractRandomCutoffMirHint implements MirPrimaryHint {
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
    public MirDirectPrimaryHint(FixedKeyPrp fixedKeyPrp, int chunkSize, int chunkNum, int l, SecureRandom secureRandom) {
        super(fixedKeyPrp, chunkSize, chunkNum, l, secureRandom);
        // We need to find a random Chunk ID among the ChunkNum / 2 unselected chunks. An easy and effective way to do
        // so is to simply keep picking random Chunk IDs and checking if the Chunk ID is already selected.
        int targetIndex = secureRandom.nextInt(chunkNum / 2);
        int countNotV = 0;
        int tryExtraChunkId = -1;
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (vs[chunkId] >= cutoff) {
                if(countNotV == targetIndex){
                    tryExtraChunkId = chunkId;
                }
                countNotV++;
            }
        }
        extraChunkId = tryExtraChunkId;
        assert countNotV == chunkNum / 2 : "|V| must be equal to " + chunkNum / 2 + ": " + countNotV;

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
        int vl = getIntegerForMedian(chunkId);
        return vl < cutoff;
    }

    @Override
    public BitVector containsChunks() {
        int[] vs = getIntegersForMedian();
        BitVector contains = BitVectorFactory.createZeros(chunkNum);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (chunkId == extraChunkId) {
                contains.set(chunkId, true);
            } else if (vs[chunkId] < cutoff) {
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
            int chunkId = j + blockChunkId;
            if (chunkId == extraChunkId) {
                contains[j] = true;
            } else {
                contains[j] = vs[j] < cutoff;
            }
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
        return getIntegers();
    }

    @Override
    public int[] expandPrpBlockOffsets(int blockChunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", blockChunkId, chunkNum);
        return getPrpBlockIntegers(blockChunkId);
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
        return -1;
    }

    @Override
    public void amendParity(byte[] parity) {
        throw new RuntimeException("It is not necessary to amend direct primary hint");
    }
}
