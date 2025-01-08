package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

/**
 * hint for PIANO.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public interface PianoHint {
    /**
     * each AES block contains CommonConstants.BLOCK_BYTE_LENGTH / 2 offsets.
     */
    int PRP_BLOCK_OFFSET_NUM = CommonConstants.BLOCK_BYTE_LENGTH / 2;

    /**
     * Gets chunk size.
     *
     * @return chunk size.
     */
    int getChunkSize();

    /**
     * Gets chunk num.
     *
     * @return chunk num.
     */
    int getChunkNum();

    /**
     * Gets parity bit length.
     *
     * @return parity bit length.
     */
    int getL();

    /**
     * Gets parity byte length.
     *
     * @return parity byte length.
     */
    int getByteL();

    /**
     * Gets the parity.
     *
     * @return the parity.
     */
    byte[] getParity();

    /**
     * Inplace XOR the current parity with the other parity.
     *
     * @param otherParity other parity.
     */
    void xori(byte[] otherParity);

    /**
     * Expands the offset for the given chunk ID.
     *
     * @param chunkId chunk ID.
     * @return the offset of the given chunk ID.
     */
    int expandOffset(int chunkId);

    /**
     * Expands all offsets for all chunks. The total number of indexes are chunkNum.
     *
     * @return all offsets for all chunks.
     */
    int[] expandOffsets();

    /**
     * Expands block offsets for the given chunk ID.
     *
     * @return block offsets for the given chunk ID.
     */
    int[] expandPrpBlockOffsets(int blockChunkId);
}
