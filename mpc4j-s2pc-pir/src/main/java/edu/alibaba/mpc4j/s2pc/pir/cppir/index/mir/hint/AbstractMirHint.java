package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;

import java.nio.ByteBuffer;

/**
 * abstract hint for MIR.
 *
 * @author Weiran Liu
 * @date 2023/8/30
 */
public abstract class AbstractMirHint implements MirHint {
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

    protected AbstractMirHint(FixedKeyPrp fixedKeyPrp, int chunkSize, int chunkNum, int l) {
        this.fixedKeyPrp = fixedKeyPrp;
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
        int prpBlockId = chunkId / PRP_BLOCK_OFFSET_NUM;
        int prpBlockIndex = Math.abs(chunkId % PRP_BLOCK_OFFSET_NUM);
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) prpBlockId)
            // 1 for "offset"
            .putShort((short) 1)
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
                // 1 for "offset"
                .putShort((short) 1)
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
            // 1 for "offset"
            .putShort((short) 1)
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

    /**
     * Gets an integer value based on the hint ID and the chunk ID.
     *
     * @param chunkId chunk ID.
     * @return an integer value
     */
    protected int getIntegerForMedian(int chunkId) {
        int prpBlockId = chunkId / PRP_BLOCK_INT_NUM;
        int prpBlockIndex = Math.abs(chunkId % PRP_BLOCK_INT_NUM);
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) prpBlockId)
            // 0 for "select"
            .putShort((short) 0)
            .array();
        byte[] prpOutput = fixedKeyPrp.prp(prpInput);
        int j = prpBlockIndex * 4;
        // return an integer value
        return ((prpOutput[j] & 0xFF) << 24) + ((prpOutput[j + 1] & 0xFF) << 16) +
            ((prpOutput[j + 2] & 0xFF) << 8) + (prpOutput[j + 3] & 0xFF);
    }

    protected int[] getIntegersForMedian() {
        int chunkNum = getChunkNum();
        int[] integers = new int[chunkNum];
        int index = 0;
        int prpBlockId = 0;
        boolean done = false;
        while (!done) {
            byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
                .put(hintId)
                .putShort((short) prpBlockId)
                // 0 for "select"
                .putShort((short) 0)
                .array();
            byte[] prpOutput = fixedKeyPrp.prp(prpInput);
            for (int prpBlockIndex = 0, j = 0; prpBlockIndex < PRP_BLOCK_INT_NUM; prpBlockIndex++, j += 4) {
                integers[index] = ((prpOutput[j] & 0xFF) << 24) + ((prpOutput[j + 1] & 0xFF) << 16) +
                    ((prpOutput[j + 2] & 0xFF) << 8) + (prpOutput[j + 3] & 0xFF);
                index++;
                if (index >= chunkNum) {
                    done = true;
                    break;
                }
            }
            prpBlockId++;
        }
        return integers;
    }

    protected int[] getPrpBlockIntegersForMedian(int blockChunkId) {
        assert blockChunkId % PRP_BLOCK_OFFSET_NUM == 0;
        int prpBlockId = blockChunkId / PRP_BLOCK_OFFSET_NUM;
        int num = (prpBlockId + 1) * PRP_BLOCK_OFFSET_NUM >= chunkNum ? chunkNum - prpBlockId * PRP_BLOCK_OFFSET_NUM : PRP_BLOCK_OFFSET_NUM;
        int[] integers = new int[num];
        // each PRP block contains two double block IDs
        int leftPrpBlockId = blockChunkId / PRP_BLOCK_INT_NUM;
        int rightPrpBlockId = leftPrpBlockId + 1;
        int index = 0;
        // left block
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) leftPrpBlockId)
            // 0 for "select"
            .putShort((short) 0)
            .array();
        byte[] prpOutput = fixedKeyPrp.prp(prpInput);
        for (int prpBlockIndex = 0; prpBlockIndex < PRP_BLOCK_INT_NUM && prpBlockIndex < num; prpBlockIndex++) {
            int j = prpBlockIndex * 4;
            integers[index] = (((prpOutput[j] & 0xFF) << 24) + ((prpOutput[j + 1] & 0xFF) << 16)
                + ((prpOutput[j + 2] & 0xFF) << 8) + (prpOutput[j + 3] & 0xFF));
            index++;
            if (index >= num) {
                break;
            }
        }
        if (index == num) {
            return integers;
        }
        // right block
        prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintId)
            .putShort((short) rightPrpBlockId)
            // 0 for "select"
            .putShort((short) 0)
            .array();
        prpOutput = fixedKeyPrp.prp(prpInput);
        for (int prpBlockIndex = 0; prpBlockIndex < PRP_BLOCK_INT_NUM && prpBlockIndex < num; prpBlockIndex++) {
            int j = prpBlockIndex * 4;
            integers[index] = (((prpOutput[j] & 0xFF) << 24) + ((prpOutput[j + 1] & 0xFF) << 16)
                + ((prpOutput[j + 2] & 0xFF) << 8) + (prpOutput[j + 3] & 0xFF));
            index++;
            if (index >= num) {
                break;
            }
        }
        return integers;
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
    protected abstract int getCutoff();
}
