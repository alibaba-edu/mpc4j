package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;

/**
 * abstract hint for PIANO.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractPianoHint implements PianoHint {
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
    /**
     * parity
     */
    protected final byte[] parity;

    protected AbstractPianoHint(int chunkSize, int chunkNum, int l) {
        MathPreconditions.checkPositive("chunkSize", chunkSize);
        this.chunkSize = chunkSize;
        MathPreconditions.checkPositive("chunkNum", chunkNum);
        this.chunkNum = chunkNum;
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // initialize the hint ID, the PRG input is "Hint ID || (short) Chunk ID"
        hintId = new byte[CommonConstants.BLOCK_BYTE_LENGTH - Short.BYTES];
        // initialize the parity to zero
        parity = new byte[byteL];
    }

    /**
     * Gets the integer based on the hint ID and the chunk ID.
     *
     * @param chunkId chunk ID.
     * @return the integer.
     */
    protected int getInteger(int chunkId) {
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) chunkId)
            .array();
        return Math.abs(ByteBuffer.wrap(PRP.prp(prpInput)).getInt() % chunkSize);
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
}
