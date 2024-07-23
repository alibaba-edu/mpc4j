package edu.alibaba.mpc4j.common.structure.lpn.dual.expander;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

/**
 * The coder for the (systematic) expander matrix B.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
public class SystemExpanderCoder extends AbstractExpanderCoder {
    /**
     * Creates a systematic expander coder.
     *
     * @param k              message size, i.e., k.
     * @param n              code size, i.e., n.
     * @param expanderWeight expander weight.
     */
    public SystemExpanderCoder(int k, int n, int expanderWeight) {
        this(EnvType.STANDARD, k, n, expanderWeight);
    }

    /**
     * Creates a systematic expander coder.
     *
     * @param envType        environment.
     * @param k              message size, i.e., k.
     * @param n              code size, i.e., n.
     * @param expanderWeight expander weight.
     */
    public SystemExpanderCoder(EnvType envType, int k, int n, int expanderWeight) {
        super(envType, k, n, expanderWeight);
    }

    @Override
    protected int[][] generateMatrix(EnvType envType) {
        int parity = n - k;
        // number of weight that is uniformly generated, uni = mExpanderWeight / 2
        int uniformWeight = expanderWeight / 2;
        int uniformBlockNum = CommonUtils.getUnitNum(uniformWeight, CommonConstants.BLOCK_BYTE_LENGTH / Integer.BYTES);
        Prp uniformPrp = PrpFactory.createInstance(envType);
        uniformPrp.setKey(DEFAULT_SEED);
        // number of weight that is regularly generated, reg = mExpanderWeight - uni
        int regularWeight = expanderWeight - uniformWeight;
        int regularBlockNum = CommonUtils.getUnitNum(regularWeight, CommonConstants.BLOCK_BYTE_LENGTH / Integer.BYTES);
        // step = mCodeSize / reg
        int regularStep = parity / regularWeight;
        Prp regularPrp = PrpFactory.createInstance(envType);
        regularPrp.setKey(BytesUtils.xor(DEFAULT_SEED, REGULAR_SEED_MASK));
        return IntStream.range(0, k)
            .mapToObj(i -> {
                int[] row = new int[expanderWeight + 1];
                // add the systematic point
                row[0] = i;
                ByteBuffer blockByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH);
                // generate uniform weights
                ByteBuffer uniformByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH * uniformBlockNum);
                for (int blockIndex = 0; blockIndex < uniformBlockNum; blockIndex++) {
                    byte[] block = blockByteBuffer
                        .putInt(0, i)
                        .putInt(CommonConstants.BLOCK_BYTE_LENGTH / 2, blockIndex)
                        .array();
                    uniformByteBuffer.put(uniformPrp.prp(block));
                }
                int[] uniformRow = IntUtils.byteArrayToIntArray(uniformByteBuffer.array());
                for (int j = 0; j < uniformWeight; j++) {
                    row[1 + j] = Math.abs(uniformRow[j] % parity) + k;
                }
                // generate regular weights
                ByteBuffer regularByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH * regularBlockNum);
                for (int blockIndex = 0; blockIndex < regularBlockNum; blockIndex++) {
                    byte[] block = blockByteBuffer
                        .putInt(0, i)
                        .putInt(CommonConstants.BLOCK_BYTE_LENGTH / 2, blockIndex)
                        .array();
                    regularByteBuffer.put(regularPrp.prp(block));
                }
                int[] regularRow = IntUtils.byteArrayToIntArray(regularByteBuffer.array());
                for (int j = 0; j < regularWeight; j++) {
                    row[1 + j + uniformWeight] = Math.abs(regularRow[j] % regularStep) + j * regularStep + k;
                }
                return row;
            })
            .toArray(int[][]::new);
    }
}
