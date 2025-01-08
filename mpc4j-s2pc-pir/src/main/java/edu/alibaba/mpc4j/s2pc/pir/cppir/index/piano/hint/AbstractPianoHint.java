package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;

import java.nio.ByteBuffer;

/**
 * abstract hint for PIANO.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractPianoHint implements PianoHint {
    /**
     * fixed key PRP
     */
    protected final FixedKeyPrp fixedKeyPrp;
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

    protected AbstractPianoHint(FixedKeyPrp fixedKeyPrp, int chunkSize, int chunkNum, int l) {
        this.fixedKeyPrp = fixedKeyPrp;
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
        int prpBlockId = chunkId / PRP_BLOCK_OFFSET_NUM;
        int prpBlockIndex = Math.abs(chunkId % PRP_BLOCK_OFFSET_NUM);
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) prpBlockId)
            .array();
        byte[] prpOutput = fixedKeyPrp.prp(prpInput);
        int j = prpBlockIndex * 2;
        int offset = ((prpOutput[j] & 0xFF) << Byte.SIZE) + (prpOutput[j + 1] & 0xFF);
        return Math.abs(offset % chunkSize);
    }

    protected int[] getIntegers() {
        int chunkNum = getChunkNum();
        int[] offsets = new int[chunkNum];
        int index = 0;
        int prpBlockId = 0;
        boolean done = false;
        while (!done) {
            byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
                .put(hintId)
                .putShort((short) prpBlockId)
                .array();
            byte[] prpOutput = fixedKeyPrp.prp(prpInput);
            for (int prpBlockIndex = 0; prpBlockIndex < PRP_BLOCK_OFFSET_NUM; prpBlockIndex++) {
                int j = prpBlockIndex * 2;
                int offset = ((prpOutput[j] & 0xFF) << Byte.SIZE) + ((prpOutput[j + 1] & 0xFF));
                offsets[index] = Math.abs(offset % chunkSize);
                index++;
                if (index >= chunkNum) {
                    done = true;
                    break;
                }
            }
            prpBlockId++;
        }
        return offsets;
    }

    protected int[] getPrpBlockIntegers(int blockChunkId) {
        assert blockChunkId % PRP_BLOCK_OFFSET_NUM == 0;
        int prpBlockId = blockChunkId / PRP_BLOCK_OFFSET_NUM;
        int num = (prpBlockId + 1) * PRP_BLOCK_OFFSET_NUM >= chunkNum ? chunkNum - prpBlockId * PRP_BLOCK_OFFSET_NUM : PRP_BLOCK_OFFSET_NUM;
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) prpBlockId)
            .array();
        byte[] prpOutput = fixedKeyPrp.prp(prpInput);
        int[] offsets = new int[num];
        for (int prpBlockIndex = 0; prpBlockIndex < num; prpBlockIndex++) {
            int j = prpBlockIndex * 2;
            int offset = ((prpOutput[j] & 0xFF) << Byte.SIZE) + (prpOutput[j + 1] & 0xFF);
            offsets[prpBlockIndex] = Math.abs(offset % chunkSize);
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
