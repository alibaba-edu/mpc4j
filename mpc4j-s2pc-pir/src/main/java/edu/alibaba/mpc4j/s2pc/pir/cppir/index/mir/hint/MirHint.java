package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * hint for MIR.
 *
 * @author Weiran Liu
 * @date 2023/8/30
 */
public interface MirHint {
    /**
     * each AES block contains CommonConstants.BLOCK_BYTE_LENGTH / 2 offsets.
     */
    int PRP_BLOCK_OFFSET_NUM = CommonConstants.BLOCK_BYTE_LENGTH / 2;
    /**
     * each AES block contains CommonConstants.BLOCK_BYTE_LENGTH / 4 integers.
     */
    int PRP_BLOCK_INT_NUM = CommonConstants.BLOCK_BYTE_LENGTH / 4;

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
     * @param blockChunkId block chunk ID.
     * @return block offsets for the given chunk ID.
     */
    int[] expandPrpBlockOffsets(int blockChunkId);

    /**
     * Gets if the backup hint contains the given chunk ID.
     *
     * @param chunkId chunk ID.
     * @return return true if the backup hint contains the given chunk ID.
     */
    boolean containsChunkId(int chunkId);

    /**
     * Gets if each chunk ID is contained in the hints.
     *
     * @return each chunk ID is contained in the hints.
     */
    BitVector containsChunks();

    /**
     * Gets if each chunk ID is contained in the hints.
     *
     * @param blockChunkId block chunk ID.
     * @return each chunk ID is contained in the hints.
     */
    boolean[] containsChunks(int blockChunkId);
}
