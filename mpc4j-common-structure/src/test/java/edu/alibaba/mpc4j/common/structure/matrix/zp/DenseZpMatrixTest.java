package edu.alibaba.mpc4j.common.structure.matrix.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * square dense Zp matrix test.
 *
 * @author Weiran Liu
 * @date 2023/6/19
 */
@RunWith(Parameterized.class)
public class DenseZpMatrixTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 100;
    /**
     * l
     */
    private static final int L = CommonConstants.STATS_BIT_LENGTH;
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
     * Zp instance
     */
    private final Zp zp;

    public DenseZpMatrixTest(String name, int size) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.size = size;
        zp = ZpFactory.createInstance(EnvType.STANDARD, L);
    }

    @Test
    public void testIdentityInvertible() {
        List<BigInteger[]> identityRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                BigInteger[] row = new BigInteger[size];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn == iRow) {
                        row[iColumn] = zp.createOne();
                    } else {
                        row[iColumn] = zp.createZero();
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(identityRows, SECURE_RANDOM);
            BigInteger[][] data = identityRows.toArray(new BigInteger[0][]);
            DenseZpMatrix matrix = DenseZpMatrix.fromDense(zp, data);
            DenseZpMatrix iMatrix = matrix.inverse();
            Assert.assertTrue(DenseZpMatrix.isIdentity(matrix.multiply(iMatrix)));
        }
    }

    @Test
    public void testIdentityIrreversible() {
        List<BigInteger[]> identityRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                BigInteger[] row = new BigInteger[size];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn == iRow) {
                        row[iColumn] = zp.createOne();
                    } else {
                        row[iColumn] = zp.createZero();
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(identityRows, SECURE_RANDOM);
            BigInteger[][] data = BigIntegerUtils.clone(identityRows.toArray(new BigInteger[0][]));
            // set a random row to be 0
            int zeroRowIndex = SECURE_RANDOM.nextInt(size);
            data[zeroRowIndex] = IntStream.range(0, size).mapToObj(index -> zp.createZero()).toArray(BigInteger[]::new);
            DenseZpMatrix matrix = DenseZpMatrix.fromDense(zp, data);
            Assert.assertThrows(ArithmeticException.class, matrix::inverse);
        }
    }

    @Test
    public void testRandomUpperTriangleInvertible() {
        // upper-triangle matrix must be invertible
        List<BigInteger[]> upperTriangleRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                BigInteger[] row = new BigInteger[size];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn < iRow) {
                        row[iColumn] = zp.createZero();
                    } else {
                        row[iColumn] = zp.createNonZeroRandom(SECURE_RANDOM);
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(upperTriangleRows, SECURE_RANDOM);
            BigInteger[][] data = upperTriangleRows.toArray(new BigInteger[0][]);
            DenseZpMatrix matrix = DenseZpMatrix.fromDense(zp, data);
            DenseZpMatrix iMatrix = matrix.inverse();
            Assert.assertTrue(DenseZpMatrix.isIdentity(matrix.multiply(iMatrix)));
        }
    }

    @Test
    public void testRandomUpperTriangleIrreversible() {
        // upper-triangle matrix must be invertible
        List<BigInteger[]> upperTriangleRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                BigInteger[] row = new BigInteger[size];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn < iRow) {
                        row[iColumn] = zp.createZero();
                    } else {
                        row[iColumn] = zp.createNonZeroRandom(SECURE_RANDOM);
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(upperTriangleRows, SECURE_RANDOM);
            BigInteger[][] data = upperTriangleRows.toArray(new BigInteger[0][]);
            // set a random row to be 0
            int zeroRowIndex = SECURE_RANDOM.nextInt(size);
            data[zeroRowIndex] = IntStream.range(0, size).mapToObj(index -> zp.createZero()).toArray(BigInteger[]::new);
            DenseZpMatrix matrix = DenseZpMatrix.fromDense(zp, data);
            Assert.assertThrows(ArithmeticException.class, matrix::inverse);
        }
    }

    @Test
    public void testRandomLowerTriangleInvertible() {
        // lower-triangle matrix must be invertible
        List<BigInteger[]> lowerTriangleRows = IntStream.range(0, size)
            .mapToObj(iRow -> {
                BigInteger[] row = new BigInteger[size];
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    if (iColumn <= iRow) {
                        row[iColumn] = zp.createNonZeroRandom(SECURE_RANDOM);
                    } else {
                        row[iColumn] = zp.createZero();
                    }
                }
                return row;
            })
            .collect(Collectors.toList());
        for (int round = 0; round < RANDOM_ROUND; round++) {
            Collections.shuffle(lowerTriangleRows, SECURE_RANDOM);
            BigInteger[][] data = lowerTriangleRows.toArray(new BigInteger[0][]);
            DenseZpMatrix matrix = DenseZpMatrix.fromDense(zp, data);
            DenseZpMatrix iMatrix = matrix.inverse();
            Assert.assertTrue(DenseZpMatrix.isIdentity(matrix.multiply(iMatrix)));
        }
    }
}
