package edu.alibaba.mpc4j.common.structure.lpn.dual.excoder;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.lpn.dual.expander.NonSysExpanderCoder;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;

/**
 * non-systematic EC coder.
 *
 * @author Weiran Liu
 * @date 2024/6/18
 */
public class NonSysEcCoder implements ExCoder {
    /**
     * default seed: block(33333, 33333)
     */
    private static final byte[] DEFAULT_ORIG_SEED = ByteBuffer.allocate(Long.BYTES + Long.BYTES)
        .putLong(9996754675674599L).putLong(56756745976768754L).array();
    /**
     * default flip seed: ~seed, used for accumulate twice
     */
    private static final byte[] DEFAULT_FLIP_SEED = BytesUtils.not(DEFAULT_ORIG_SEED, CommonConstants.BLOCK_BIT_LENGTH);
    /**
     * environment
     */
    private final EnvType envType;
    /**
     * non-systematic expander coder
     */
    private final NonSysExpanderCoder expanderCoder;
    /**
     * The message size of the code, i.e., k.
     */
    private final int k;
    /**
     * The codeword size of the code, i.e., n.
     */
    private final int n;
    /**
     * accumulator weight
     */
    private final int accumulatorWeight;
    /**
     * accumulator byte wight
     */
    private final int accumulatorByteWeight;

    /**
     * Creates an EC coder.
     *
     * @param k                 k.
     * @param n                 n.
     * @param accumulatorWeight accumulator weight.
     * @param expanderWeight    expander weight.
     */
    public NonSysEcCoder(int k, int n, int accumulatorWeight, int expanderWeight) {
        this(EnvType.STANDARD, k, n, accumulatorWeight, expanderWeight);
    }

    /**
     * Creates an EA coder.
     *
     * @param envType           environment.
     * @param k                 k.
     * @param n                 n.
     * @param accumulatorWeight accumulator weight.
     * @param expanderWeight    expander weight.
     */
    public NonSysEcCoder(EnvType envType, int k, int n, int accumulatorWeight, int expanderWeight) {
        this.envType = envType;
        MathPreconditions.checkInRange("accumulator_weight", accumulatorWeight, 1, n - k);
        Preconditions.checkArgument(accumulatorWeight % Byte.SIZE == 0);
        this.accumulatorWeight = accumulatorWeight;
        accumulatorByteWeight = accumulatorWeight / Byte.SIZE;
        expanderCoder = new NonSysExpanderCoder(envType, k, n, expanderWeight);
        this.k = k;
        this.n = n;
    }

    @Override
    public int getCodeSize() {
        return n;
    }

    @Override
    public int getMessageSize() {
        return k;
    }

    @Override
    public void setParallel(boolean parallel) {
        expanderCoder.setParallel(parallel);
    }

    @Override
    public boolean getParallel() {
        return expanderCoder.getParallel();
    }

    @Override
    public boolean[] dualEncode(boolean[] es) {
        MathPreconditions.checkEqual("n", "inputs.length", n, es.length);
        boolean[] ws = new boolean[n];
        System.arraycopy(es, 0, ws, 0, n);
        // accumulate
        accumulate(ws, es, DEFAULT_ORIG_SEED);
        accumulate(ws, es, DEFAULT_FLIP_SEED);
        // expand
        return expanderCoder.dualEncode(ws);
    }

    private void accumulate(boolean[] ws, boolean[] es, byte[] seed) {
        byte[] block = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        int num = n - 1 - accumulatorWeight;
        // generate randomness
        int accumulatorBlockNum = CommonUtils.getUnitNum(accumulatorByteWeight * num, CommonConstants.BLOCK_BYTE_LENGTH);
        Prp prp = PrpFactory.createInstance(envType);
        prp.setKey(seed);
        ByteBuffer byteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH * accumulatorBlockNum);
        for (int blockIndex = 0; blockIndex < accumulatorBlockNum; blockIndex++) {
            block[3] = (byte) (blockIndex >>> 24);
            block[2] = (byte) (blockIndex >>> 16);
            block[1] = (byte) (blockIndex >>> 8);
            block[0] = (byte) blockIndex;
            byteBuffer.put(prp.prp(block));
        }
        byte[] randomness = byteBuffer.array();
        for (int i = 0; i < n - 1 - accumulatorWeight; i++) {
            for (int byteJ = 0; byteJ < accumulatorByteWeight; byteJ++) {
                int offset = byteJ * Byte.SIZE;
                int j1 = i + offset + 1;
                int j2 = i + offset + 2;
                int j3 = i + offset + 3;
                int j4 = i + offset + 4;
                int j5 = i + offset + 5;
                int j6 = i + offset + 6;
                int j7 = i + offset + 7;
                int j8 = i + offset + 8;
                // j mod size
                j1 = j1 >= n ? j1 - n : j1;
                j2 = j2 >= n ? j2 - n : j2;
                j3 = j3 >= n ? j3 - n : j3;
                j4 = j4 >= n ? j4 - n : j4;
                j5 = j5 >= n ? j5 - n : j5;
                j6 = j6 >= n ? j6 - n : j6;
                j7 = j7 >= n ? j7 - n : j7;
                j8 = j8 >= n ? j8 - n : j8;
                // randomness
                byte b = randomness[accumulatorByteWeight * i + byteJ];
                if ((b & 0b00000001) != 0) {
                    ws[j1] ^= es[i];
                }
                if ((b & 0b00000010) != 0) {
                    ws[j2] ^= es[i];
                }
                if ((b & 0b00000100) != 0) {
                    ws[j3] ^= es[i];
                }
                if ((b & 0b00001000) != 0) {
                    ws[j4] ^= es[i];
                }
                if ((b & 0b00010000) != 0) {
                    ws[j5] ^= es[i];
                }
                if ((b & 0b00100000) != 0) {
                    ws[j6] ^= es[i];
                }
                if ((b & 0b01000000) != 0) {
                    ws[j7] ^= es[i];
                }
                if ((b & 0b10000000) != 0) {
                    ws[j8] ^= es[i];
                }
            }
        }
    }

    @Override
    public byte[][] dualEncode(byte[][] es) {
        MathPreconditions.checkEqual("n", "inputs.length", n, es.length);
        // here we cannot use System.arraycopy since this would be a soft copy.
        byte[][] ws = BytesUtils.clone(es);
        // accumulate
        accumulate(ws, es, DEFAULT_ORIG_SEED);
        accumulate(ws, es, DEFAULT_FLIP_SEED);
        // expand
        return expanderCoder.dualEncode(ws);
    }

    private void accumulate(byte[][] ws, byte[][] es, byte[] seed) {
        byte[] block = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        int num = n - 1 - accumulatorWeight;
        // generate randomness
        int accumulatorBlockNum = CommonUtils.getUnitNum(accumulatorByteWeight * num, CommonConstants.BLOCK_BYTE_LENGTH);
        Prp prp = PrpFactory.createInstance(envType);
        prp.setKey(seed);
        ByteBuffer byteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH * accumulatorBlockNum);
        for (int blockIndex = 0; blockIndex < accumulatorBlockNum; blockIndex++) {
            block[3] = (byte) (blockIndex >>> 24);
            block[2] = (byte) (blockIndex >>> 16);
            block[1] = (byte) (blockIndex >>> 8);
            block[0] = (byte) blockIndex;
            byteBuffer.put(prp.prp(block));
        }
        byte[] randomness = byteBuffer.array();
        for (int i = 0; i < n - 1 - accumulatorWeight; i++) {
            for (int byteJ = 0; byteJ < accumulatorByteWeight; byteJ++) {
                int offset = byteJ * Byte.SIZE;
                int j1 = i + offset + 1;
                int j2 = i + offset + 2;
                int j3 = i + offset + 3;
                int j4 = i + offset + 4;
                int j5 = i + offset + 5;
                int j6 = i + offset + 6;
                int j7 = i + offset + 7;
                int j8 = i + offset + 8;
                // j mod size
                j1 = j1 >= n ? j1 - n : j1;
                j2 = j2 >= n ? j2 - n : j2;
                j3 = j3 >= n ? j3 - n : j3;
                j4 = j4 >= n ? j4 - n : j4;
                j5 = j5 >= n ? j5 - n : j5;
                j6 = j6 >= n ? j6 - n : j6;
                j7 = j7 >= n ? j7 - n : j7;
                j8 = j8 >= n ? j8 - n : j8;
                // randomness
                byte b = randomness[accumulatorByteWeight * i + byteJ];
                if ((b & 0b00000001) != 0) {
                    BytesUtils.xori(ws[j1], es[i]);
                }
                if ((b & 0b00000010) != 0) {
                    BytesUtils.xori(ws[j2], es[i]);
                }
                if ((b & 0b00000100) != 0) {
                    BytesUtils.xori(ws[j3], es[i]);
                }
                if ((b & 0b00001000) != 0) {
                    BytesUtils.xori(ws[j4], es[i]);
                }
                if ((b & 0b00010000) != 0) {
                    BytesUtils.xori(ws[j5], es[i]);
                }
                if ((b & 0b00100000) != 0) {
                    BytesUtils.xori(ws[j6], es[i]);
                }
                if ((b & 0b01000000) != 0) {
                    BytesUtils.xori(ws[j7], es[i]);
                }
                if ((b & 0b10000000) != 0) {
                    BytesUtils.xori(ws[j8], es[i]);
                }
            }
        }
    }
}
