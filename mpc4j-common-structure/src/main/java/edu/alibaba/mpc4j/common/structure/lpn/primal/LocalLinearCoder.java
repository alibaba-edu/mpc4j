package edu.alibaba.mpc4j.common.structure.lpn.primal;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
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
     * code size (n)
     */
    private final int n;
    /**
     * matrix
     */
    private final int[][] matrix;
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
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        parallel = false;
        // sets PRP
        Prp prp = PrpFactory.createInstance(envType);
        prp.setKey(seed);
        matrix = IntStream.range(0, n)
            .mapToObj(i -> {
                ByteBuffer blockByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH);
                // block tmp[3]
                ByteBuffer indexByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH * BLOCK_NUM);
                for (int blockIndex = 0; blockIndex < BLOCK_NUM; blockIndex++) {
                    // tmp[m] = makeBlock(i, m)
                    byte[] block = blockByteBuffer
                        .putInt(0, i)
                        .putInt(CommonConstants.BLOCK_BYTE_LENGTH / 2, blockIndex)
                        .array();
                    // prp->permute_block(tmp, 3)
                    indexByteBuffer.put(prp.prp(block));
                }
                int[] randomRow = IntUtils.byteArrayToIntArray(indexByteBuffer.array());
                int[] sparseRow = new int[D];
                for (int j = 0; j < D; j++) {
                    sparseRow[j] = Math.abs(randomRow[j] % k);
                }
                return sparseRow;
            })
            .toArray(int[][]::new);
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

    @Override
    public boolean[] encode(boolean[] es) {
        MathPreconditions.checkEqual("k", "inputs.length", k, es.length);
        boolean[] ws = new boolean[n];
        IntStream rowIndexIntStream = IntStream.range(0, n);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        rowIndexIntStream.forEach(rowIndex -> {
            for (int j = 0; j < D; j++) {
                int position = matrix[rowIndex][j];
                ws[rowIndex] ^= es[position];
            }
        });
        return ws;
    }

    @Override
    public byte[][] encode(byte[][] es) {
        MathPreconditions.checkEqual("k", "inputs.length", k, es.length);
        int byteL = es[0].length;
        // we do not need to verify input length, xori will verify that
        IntStream rowIndexIntStream = IntStream.range(0, n);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        return rowIndexIntStream
            .mapToObj(rowIndex -> {
                byte[] w = new byte[byteL];
                for (int j = 0; j < D; j++) {
                    int position = matrix[rowIndex][j];
                    BytesUtils.xori(w, es[position]);
                }
                return w;
            })
            .toArray(byte[][]::new);
    }
}
