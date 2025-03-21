package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;

import java.security.SecureRandom;

/**
 * directly generated primary hint for PIANO.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoDirectPrimaryHint extends AbstractPianoHint implements PianoPrimaryHint {
    /**
     * Creates a hint with a random hint ID.
     *
     * @param fixedKeyPrp  fixed key PRP.
     * @param chunkSize    chunk size.
     * @param chunkNum     chunk num.
     * @param l            parity bit length.
     * @param secureRandom the random state.
     */
    public PianoDirectPrimaryHint(FixedKeyPrp fixedKeyPrp,
                                  int chunkSize, int chunkNum, int l, SecureRandom secureRandom) {
        super(fixedKeyPrp, chunkSize, chunkNum, l);
        secureRandom.nextBytes(hintId);
    }

    @Override
    public int expandOffset(int chunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", chunkId, chunkNum);
        return getInteger(chunkId);
    }

    @Override
    public int[] expandPrpBlockOffsets(int blockChunkId) {
        MathPreconditions.checkNonNegativeInRange("chunk ID", blockChunkId, chunkNum);
        return getPrpBlockIntegers(blockChunkId);
    }

    @Override
    public int[] expandOffsets() {
        return getIntegers();
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
