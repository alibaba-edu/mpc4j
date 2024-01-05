package edu.alibaba.mpc4j.common.structure.lpn.primal;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

/**
 * d-local linear coder.
 * <p></p>
 * The generation matrix of d-LLC is an n * k bit matrix. In each row, only d < k positions are 1, others are all 0.
 * <p></p>
 * The implementation is based on the following code in emp-ot:
 * <p></p>
 * https://github.com/emp-toolkit/emp-ot/blob/master/emp-ot/ferret/lpn_f2.h
 *
 * @author Weiran Liu
 * @date 2022/01/31
 */
public class LocalLinearCoder implements PrimalLpnCoder {
    /**
     * in each row, d < k positions are 1.
     */
    private static final int D = 10;
    /**
     * number of blocks to generate d positions.
     */
    private static final int BLOCK_NUM = (int) Math.ceil((double) D * Integer.BYTES / CommonConstants.BLOCK_BYTE_LENGTH);
    /**
     * message size (k)
     */
    private final int k;
    /**
     * message size (k) in bytes
     */
    private final int byteK;
    /**
     * offset for k
     */
    private final int offsetK;
    /**
     * code size (n)
     */
    private final int n;
    /**
     * pseudo-random permutation
     */
    private final Prp prp;
    /**
     * parallel encoding
     */
    private boolean parallel;

    /**
     * Creates the d-local linear coder.
     *
     * @param k    message size (k).
     * @param n    code size (n).
     * @param seed seed.
     */
    public LocalLinearCoder(int k, int n, byte[] seed) {
        this(EnvType.STANDARD, k, n, seed);
    }

    /**
     * Creates the d-local linear coder.
     *
     * @param envType environment.
     * @param k       message size.
     * @param n       code size.
     * @param seed    seed.
     */
    public LocalLinearCoder(EnvType envType, int k, int n, byte[] seed) {
        MathPreconditions.checkGreater("k", k, D);
        this.k = k;
        byteK = CommonUtils.getByteLength(k);
        offsetK = byteK * Byte.SIZE - k;
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        parallel = false;
        // sets PRP
        prp = PrpFactory.createInstance(envType);
        prp.setKey(seed);
    }

    @Override
    public boolean[] encode(boolean[] e) {
        MathPreconditions.checkEqual("k", "inputs.length", k, e.length);
        boolean[] w = new boolean[n];
        IntStream rowIndexIntStream = IntStream.range(0, n);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        rowIndexIntStream.forEach(rowIndex -> {
            int[] sparseRow = generateSparseRow(rowIndex);
            for (int j = 0; j < D; j++) {
                int position = Math.abs(sparseRow[j] % k);
                w[rowIndex] ^= e[position];
            }
        });
        return w;
    }

    @Override
    public byte[] encode(byte[] e) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(e, byteK, k), "e.length must be " + k);
        boolean[] w = new boolean[n];
        IntStream rowIndexIntStream = IntStream.range(0, n);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        rowIndexIntStream.forEach(rowIndex -> {
            int[] sparseRow = generateSparseRow(rowIndex);
            for (int j = 0; j < D; j++) {
                int position = Math.abs(sparseRow[j] % k);
                w[rowIndex] ^= BinaryUtils.getBoolean(e, offsetK + position);
            }
        });
        return BinaryUtils.binaryToRoundByteArray(w);
    }

    @Override
    public byte[][] encode(byte[][] e) {
        MathPreconditions.checkEqual("k", "inputs.length", k, e.length);
        int inputByteLength = e[0].length;
        // we do not need to verify input length, xori will verify that
        IntStream rowIndexIntStream = IntStream.range(0, n);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        return rowIndexIntStream
            .mapToObj(rowIndex -> {
                int[] sparseRow = generateSparseRow(rowIndex);
                byte[] output = new byte[inputByteLength];
                for (int j = 0; j < D; j++) {
                    int position = Math.abs(sparseRow[j] % k);
                    BytesUtils.xori(output, e[position]);
                }
                return output;
            })
            .toArray(byte[][]::new);
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
        this.parallel = parallel;
    }

    @Override
    public boolean getParallel() {
        return parallel;
    }

    private int[] generateSparseRow(int rowIndex) {
        // block tmp[3]
        ByteBuffer indexByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH * BLOCK_NUM);
        // 不重复分配内存
        ByteBuffer blockByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH);
        for (int blockIndex = 0; blockIndex < BLOCK_NUM; blockIndex++) {
            // tmp[m] = makeBlock(i, m)
            byte[] block = blockByteBuffer
                .putInt(0, rowIndex)
                .putInt(CommonConstants.BLOCK_BYTE_LENGTH / 2, blockIndex)
                .array();
            // prp->permute_block(tmp, 3)
            indexByteBuffer.put(prp.prp(block));
        }
        return IntUtils.byteArrayToIntArray(indexByteBuffer.array());
    }
}
