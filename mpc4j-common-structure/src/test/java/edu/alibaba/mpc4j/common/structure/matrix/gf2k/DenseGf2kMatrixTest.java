package edu.alibaba.mpc4j.common.structure.matrix.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * dense GF(2^κ) matrix test.
 *
 * @author Weiran Liu
 * @date 2023/7/4
 */
@RunWith(Parameterized.class)
public class DenseGf2kMatrixTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 100;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        int[] sizes = new int[]{4, 7, 8, 9, 15, 16, 17, 39, 40, 41};
        // add each size
        for (int size : sizes) {
            configurations.add(new Object[]{"size = " + size + ")", size});
        }

        return configurations;
    }

    /**
     * size
     */
    private final int size;
    /**
     * GF2K instance
     */
    private final Gf2k gf2k;

    public DenseGf2kMatrixTest(String name, int size) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.size = size;
        gf2k = Gf2kFactory.createInstance(EnvType.STANDARD);
    }

    @Test
    public void testIdentityInvertible() {
        List<byte[][]> identityRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                byte[][] row = new byte[size][];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn == iRow) {
                        row[iColumn] = gf2k.createOne();
                    } else {
                        row[iColumn] = gf2k.createZero();
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(identityRows, SECURE_RANDOM);
            byte[][][] data = identityRows.toArray(new byte[0][][]);
            DenseGf2kMatrix matrix = DenseGf2kMatrix.fromDense(gf2k, data);
            DenseGf2kMatrix iMatrix = matrix.inverse();
            Assert.assertTrue(DenseGf2kMatrix.isIdentity(matrix.multiply(iMatrix)));
        }
    }

    @Test
    public void testIdentityIrreversible() {
        List<byte[][]> identityRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                byte[][] row = new byte[size][];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn == iRow) {
                        row[iColumn] = gf2k.createOne();
                    } else {
                        row[iColumn] = gf2k.createZero();
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(identityRows, SECURE_RANDOM);
            byte[][][] data = BytesUtils.clone(identityRows.toArray(new byte[0][][]));
            // set a random row to be 0
            int zeroRowIndex = SECURE_RANDOM.nextInt(size);
            data[zeroRowIndex] = IntStream.range(0, size).mapToObj(index -> gf2k.createZero()).toArray(byte[][]::new);
            DenseGf2kMatrix matrix = DenseGf2kMatrix.fromDense(gf2k, data);
            Assert.assertThrows(ArithmeticException.class, matrix::inverse);
        }
    }

    @Test
    public void testRandomUpperTriangleInvertible() {
        // upper-triangle matrix must be invertible
        List<byte[][]> upperTriangleRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                byte[][] row = new byte[size][];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn < iRow) {
                        row[iColumn] = gf2k.createZero();
                    } else {
                        row[iColumn] = gf2k.createNonZeroRandom(SECURE_RANDOM);
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(upperTriangleRows, SECURE_RANDOM);
            byte[][][] data = upperTriangleRows.toArray(new byte[0][][]);
            DenseGf2kMatrix matrix = DenseGf2kMatrix.fromDense(gf2k, data);
            DenseGf2kMatrix iMatrix = matrix.inverse();
            Assert.assertTrue(DenseGf2kMatrix.isIdentity(matrix.multiply(iMatrix)));
        }
    }

    @Test
    public void testRandomUpperTriangleIrreversible() {
        // upper-triangle matrix must be invertible
        List<byte[][]> upperTriangleRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                byte[][] row = new byte[size][];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn < iRow) {
                        row[iColumn] = gf2k.createZero();
                    } else {
                        row[iColumn] = gf2k.createNonZeroRandom(SECURE_RANDOM);
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(upperTriangleRows, SECURE_RANDOM);
            byte[][][] data = upperTriangleRows.toArray(new byte[0][][]);
            // set a random row to be 0
            int zeroRowIndex = SECURE_RANDOM.nextInt(size);
            data[zeroRowIndex] = IntStream.range(0, size).mapToObj(index -> gf2k.createZero()).toArray(byte[][]::new);
            DenseGf2kMatrix matrix = DenseGf2kMatrix.fromDense(gf2k, data);
            Assert.assertThrows(ArithmeticException.class, matrix::inverse);
        }
    }

    @Test
    public void testRandomLowerTriangleInvertible() {
        // lower-triangle matrix must be invertible
        List<byte[][]> lowerTriangleRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                byte[][] row = new byte[size][];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn <= iRow) {
                        row[iColumn] = gf2k.createNonZeroRandom(SECURE_RANDOM);
                    } else {
                        row[iColumn] = gf2k.createZero();
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(lowerTriangleRows, SECURE_RANDOM);
            byte[][][] data = lowerTriangleRows.toArray(new byte[0][][]);
            DenseGf2kMatrix matrix = DenseGf2kMatrix.fromDense(gf2k, data);
            DenseGf2kMatrix iMatrix = matrix.inverse();
            Assert.assertTrue(DenseGf2kMatrix.isIdentity(matrix.multiply(iMatrix)));
        }
    }
}
