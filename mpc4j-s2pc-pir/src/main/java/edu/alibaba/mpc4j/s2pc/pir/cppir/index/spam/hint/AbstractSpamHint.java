package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;

/**
 * abstract hint for SPAM.
 *
 * @author Weiran Liu
 * @date 2023/8/30
 */
public abstract class AbstractSpamHint implements SpamHint {
    /**
     * PRP with a fixed key
     */
    protected static final Prp PRP = PrpFactory.createInstance(PrpType.JDK_AES);

    static {
        PRP.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    }

    /**
     * chunk size
     */
    protected final int chunkSize;
    /**
     * chunk num
     */
    protected final int chunkNum;
    /**
     * parity bit length
     */
    protected final int l;
    /**
     * parity byte length
     */
    protected final int byteL;
    /**
     * hint ID
     */
    protected final byte[] hintId;

    protected AbstractSpamHint(int chunkSize, int chunkNum, int l) {
        MathPreconditions.checkPositive("chunkSize", chunkSize);
        this.chunkSize = chunkSize;
        MathPreconditions.checkPositive("chunkNum", chunkNum);
        // we require chunk num is an even number
        Preconditions.checkArgument(chunkNum % 2 == 0);
        this.chunkNum = chunkNum;
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // initialize the hint ID
        // the PRG input is "Hint ID || (short) Chunk ID || (short) 0" or "Hint ID || (short) Chunk ID || (short) 1"
        hintId = new byte[CommonConstants.BLOCK_BYTE_LENGTH - Short.BYTES - Short.BYTES];
    }

    /**
     * Gets an integer value based on the hint ID and the chunk ID.
     *
     * @param chunkId chunk ID.
     * @return the integer.
     */
    protected int getInteger(int chunkId) {
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) chunkId)
            // 1 for "offset"
            .putShort((short) 1)
            .array();
        return Math.abs(ByteBuffer.wrap(PRP.prp(prpInput)).getInt() % chunkSize);
    }

    /**
     * Gets a double value based on the hint ID and the chunk ID.
     *
     * @param chunkId chunk ID.
     * @return a double value in range [0, 1).
     */
    protected double getDouble(int chunkId) {
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) chunkId)
            // 0 for "select"
            .putShort((short) 0)
            .array();
        // return a positive double value
        return (double) Math.abs(ByteBuffer.wrap(PRP.prp(prpInput)).getLong()) / Long.MAX_VALUE;
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public int getChunkNum() {
        return chunkNum;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    /**
     * Gets the cutoff value ^v.
     *
     * @return the cutoff value ^v.
     */
    protected abstract double getCutoff();
}
