package edu.alibaba.mpc4j.common.structure.lpn.dual.expander;

import edu.alibaba.mpc4j.common.structure.lpn.dual.DualLpnCoder;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

/**
 * The abstract coder for the expander matrix B. The construction comes from Definition 3.2 of the paper:
 * <p>
 * Elette Boyle, Geoffroy Couteau, Niv Gilboa, Yuval Ishai, Lisa Kohl, Nicolas Resch, and Peter Scholl. Correlated
 * pseudorandomness from expand-accumulate codes. CRYPTO 2022, pp. 603-633. Cham: Springer Nature Switzerland, 2022.
 * </p>
 * B has messageSize rows and codeSize columns. It is sampled uniformly with fixed weight named expanderWeight. The
 * implementation is inspired from
 * <p>
 * https://github.com/osu-crypto/libOTe/blob/master/libOTe/Tools/ExConvCode/Expander.h
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
abstract class AbstractExpanderCoder implements DualLpnCoder {
    /**
     * default seed: block(33333, 33333)
     */
    protected static final byte[] DEFAULT_SEED = ByteBuffer.allocate(Long.BYTES + Long.BYTES)
        .putLong(33333).putLong(33333).array();
    /**
     * regular seed mask: block(342342134, 23421341)
     */
    protected static final byte[] REGULAR_SEED_MASK = ByteBuffer.allocate(Long.BYTES + Long.BYTES)
        .putLong(342342134L).putLong(23421341L).array();
    /**
     * The message size of the code, i.e., k.
     */
    protected final int k;
    /**
     * The codeword size of the code, i.e., n.
     */
    protected final int n;
    /**
     * expander weight
     */
    protected final int expanderWeight;
    /**
     * matrix
     */
    private final int[][] matrix;
    /**
     * parallel encoding
     */
    private boolean parallel;

    AbstractExpanderCoder(EnvType envType, int k, int n, int expanderWeight) {
        MathPreconditions.checkPositive("k", k);
        MathPreconditions.checkGreater("n", n, k);
        MathPreconditions.checkInRange("expander_weight", expanderWeight, 1, n);
        this.k = k;
        this.n = n;
        this.expanderWeight = expanderWeight;
        matrix = generateMatrix(envType);
    }

    /**
     * Generates matrix B.
     *
     * @param envType environment.
     * @return matrix B.
     */
    protected abstract int[][] generateMatrix(EnvType envType);

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
    public boolean[] dualEncode(boolean[] es) {
        MathPreconditions.checkEqual("n", "inputs.length", n, es.length);
        boolean[] ws = new boolean[k];
        IntStream rowIntStream = IntStream.range(0, k);
        rowIntStream = parallel ? rowIntStream.parallel() : rowIntStream;
        rowIntStream.forEach(i -> {
            for (int j = 0; j < matrix[i].length; j++) {
                int position = matrix[i][j];
                ws[i] ^= es[position];
            }
        });
        return ws;
    }

    @Override
    public byte[][] dualEncode(byte[][] es) {
        MathPreconditions.checkEqual("n", "inputs.length", n, es.length);
        int byteL = es[0].length;
        // we do not need to verify input length, xori will verify that
        IntStream rowIntStream = IntStream.range(0, k);
        rowIntStream = parallel ? rowIntStream.parallel() : rowIntStream;
        return rowIntStream
            .mapToObj(i -> {
                byte[] w = new byte[byteL];
                for (int j = 0; j < matrix[i].length; j++) {
                    int position = matrix[i][j];
                    BytesUtils.xori(w, es[position]);
                }
                return w;
            })
            .toArray(byte[][]::new);
    }

    /**
     * Gets the expander matrix B.
     *
     * @return the expander matrix B.
     */
    public int[][] getMatrix() {
        return matrix;
    }
}
