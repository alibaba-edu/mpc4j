package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import org.junit.Assert;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SparseBitMatrixTestUtils {
    /**
     * 私有构造函数。
     */
    private SparseBitMatrixTestUtils() {
        // empty
    }

    static SparseBitMatrix createRandom(int cols, int rows, int weight, SecureRandom secureRandom) {
        ArrayList<SparseBitVector> colsList = IntStream.range(0, cols)
            .mapToObj(colIndex -> createRandomSparseBitVector(rows, weight, secureRandom))
            .collect(Collectors.toCollection(ArrayList::new));
        return SparseBitMatrix.creatFromColsList(colsList);
    }

    static SparseBitVector createRandomSparseBitVector(int bitSize, int size, SecureRandom secureRandom) {
        HashSet<Integer> indexSet = new HashSet<>();
        int[] indexesArray = new int[size];
        for (int i = 0; i < size; i++) {
            int index = secureRandom.nextInt(bitSize);
            while (!indexSet.add(index)) {
                index = (index + 1) % bitSize;
            }
            indexesArray[i] = index;
        }
        Arrays.sort(indexesArray);
        return SparseBitVector.createUnCheck(indexesArray, bitSize);
    }

    static LowerTriangularSparseBitMatrix createRandomLowerTriangular(int size, int weight, SecureRandom secureRandom) {
        ArrayList<SparseBitVector> colsList = IntStream.range(0, size)
            .mapToObj(colIndex -> createRandomLowerBitVector(size, weight, colIndex, secureRandom))
            .collect(Collectors.toCollection(ArrayList::new));
        return LowerTriangularSparseBitMatrix.create(colsList);
    }

    static byte[][] generateRandomExtendFieldVector(int dimension, int filedLength, SecureRandom secureRandom) {
        return IntStream.range(0, dimension)
            .mapToObj(index -> {
                byte[] output = new byte[filedLength];
                secureRandom.nextBytes(output);
                return output;
            })
            .toArray(byte[][]::new);
    }

    static boolean[] generateRandomBitVector(int dimension, SecureRandom secureRandom) {
        boolean[] outputs = new boolean[dimension];
        for (int i = 0; i < dimension; i++){
            outputs[i] = secureRandom.nextBoolean();
        }
        return outputs;
    }

    private static SparseBitVector createRandomLowerBitVector(int bitSize, int size, int colIndex, SecureRandom secureRandom) {
        HashSet<Integer> indexSet = new HashSet<>();
        size = Math.min(bitSize - colIndex, size);
        int[] indexesArray = new int[size];
        indexesArray[0] = colIndex;
        indexSet.add(colIndex);
        for (int i = 1; i < size; i++) {
            int index = secureRandom.nextInt(bitSize - colIndex) + colIndex;
            while (!indexSet.add(index)) {
                index = secureRandom.nextInt(bitSize - colIndex) + colIndex;
            }
            indexesArray[i] = index;
        }
        Arrays.sort(indexesArray);
        Assert.assertEquals(indexesArray[0], colIndex);
        return SparseBitVector.create(indexesArray, bitSize);
    }

}
