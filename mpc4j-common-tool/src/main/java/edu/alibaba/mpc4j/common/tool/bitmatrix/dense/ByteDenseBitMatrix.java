package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * 用byte[]表示的布尔矩阵。
 *
 * @author Weiran Liu, Hanwen Feng
 */
public class ByteDenseBitMatrix extends AbstractByteDenseBitMatrix{

    public static ByteDenseBitMatrix fromDense(final int columns, byte[][] byteArrays) {
        ByteDenseBitMatrix denseBitMatrix = new ByteDenseBitMatrix();
        denseBitMatrix.initFromDense(columns, byteArrays);
        return denseBitMatrix;
    }

    public static ByteDenseBitMatrix fromSparse(final int columns, int[][] positions) {
        ByteDenseBitMatrix denseBitMatrix = new ByteDenseBitMatrix();
        denseBitMatrix.initFromSparse(columns, positions);
        return denseBitMatrix;
    }

    /**
     * 私有构造函数。
     */
    private ByteDenseBitMatrix() {
        // empty
    }

    @Override
    public DenseBitMatrix add(DenseBitMatrix that) {
        return fromDense(columns, super.addToBytes(that));
    }

    @Override
    public DenseBitMatrix multiply(DenseBitMatrix that) {
        return fromDense(that.getColumns(),super.multiplyToBytes(that));
    }

    @Override
    public DenseBitMatrix transpose(EnvType envType, boolean parallel) {
        return fromDense(rows, super.transposeToBytes(envType, parallel));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ByteDenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ByteDenseBitMatrix that = (ByteDenseBitMatrix) obj;
        return new EqualsBuilder().append(this.byteBitMatrix, that.byteBitMatrix).isEquals();
    }
}
