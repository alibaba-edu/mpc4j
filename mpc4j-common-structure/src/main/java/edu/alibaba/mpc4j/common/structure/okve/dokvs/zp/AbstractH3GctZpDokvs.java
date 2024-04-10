package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import cc.redberry.rings.linear.LinearSolver;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H3CuckooTable;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3GctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.tool.ZpLinearSolver;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

/**
 * abstract DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
abstract class AbstractH3GctZpDokvs<T> extends AbstractZpDokvs<T> implements SparseZpDokvs<T> {
    /**
     * left m, i.e., sparse part. lm = (1 + ε_l) * n, with lm % Byte.SIZE == 0.
     */
    private final int lm;
    /**
     * right m, i.e., dense part. rm = 0.5 * log(n) + λ, with rm % Byte.SIZE == 0.
     */
    private final int rm;
    /**
     * Hi: {0, 1}^* -> [0, lm)
     */
    private final Prf hl;
    /**
     * Hr: {0, 1}^* -> {0, 1}^rm
     */
    private final Prf hr;
    /**
     * two core finder
     */
    private final CuckooTableSingletonTcFinder<T> singletonTcFinder;
    /**
     * binary linear solver
     */
    private final ZpLinearSolver linearSolver;
    /**
     * key -> h1
     */
    private TObjectIntMap<T> dataH1Map;
    /**
     * key -> h2
     */
    private TObjectIntMap<T> dataH2Map;
    /**
     * key -> h3
     */
    private TObjectIntMap<T> dataH3Map;
    /**
     * key -> hr
     */
    private Map<T, boolean[]> dataHrMap;

    AbstractH3GctZpDokvs(EnvType envType, BigInteger p, int n, int lm, int rm, byte[][] keys, SecureRandom secureRandom) {
        super(envType, p, n, lm + rm, secureRandom);
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, H3GctDokvsUtils.HASH_KEY_NUM);
        this.lm = lm;
        this.rm = rm;
        hl = PrfFactory.createInstance(envType, Integer.BYTES * H3GctDokvsUtils.SPARSE_HASH_NUM);
        hl.setKey(keys[0]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[1]);
        singletonTcFinder = new CuckooTableSingletonTcFinder<>();
        linearSolver = new ZpLinearSolver(zp, secureRandom);
    }

    @Override
    public int sparsePositionRange() {
        return lm;
    }

    @Override
    public int[] sparsePositions(T key) {
        return H3GctDokvsUtils.sparsePositions(hl, key, lm);
    }

    @Override
    public int sparsePositionNum() {
        return H3GctDokvsUtils.SPARSE_HASH_NUM;
    }

    @Override
    public boolean[] binaryDensePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        return BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
    }

    @Override
    public int densePositionRange() {
        return rm;
    }

    @Override
    public BigInteger decode(BigInteger[] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        int[] sparsePositions = sparsePositions(key);
        boolean[] densePositions = binaryDensePositions(key);
        BigInteger value = zp.createZero();
        // h1, h2 and h3 must be distinct
        value = zp.add(value, storage[sparsePositions[0]]);
        value = zp.add(value, storage[sparsePositions[1]]);
        value = zp.add(value, storage[sparsePositions[2]]);
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (densePositions[rmIndex]) {
                value = zp.add(value, storage[lm + rmIndex]);
            }
        }
        return value;
    }

    @Override
    public BigInteger[] encode(Map<T, BigInteger> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        keyValueMap.values().forEach(x -> Preconditions.checkArgument(zp.validateElement(x)));
        // construct maps
        Set<T> keySet = keyValueMap.keySet();
        int keySize = keySet.size();
        dataH1Map = new TObjectIntHashMap<>(keySize);
        dataH2Map = new TObjectIntHashMap<>(keySize);
        dataH3Map = new TObjectIntHashMap<>(keySize);
        dataHrMap = new HashMap<>(keySize);
        keySet.forEach(key -> {
            int[] sparsePositions = sparsePositions(key);
            boolean[] densePositions = binaryDensePositions(key);
            dataH1Map.put(key, sparsePositions[0]);
            dataH2Map.put(key, sparsePositions[1]);
            dataH3Map.put(key, sparsePositions[2]);
            dataHrMap.put(key, densePositions);
        });
        // generate cuckoo table with 3 hash functions
        H3CuckooTable<T> h3CuckooTable = generateCuckooTable(keyValueMap);
        // find two-core graph
        singletonTcFinder.findTwoCore(h3CuckooTable);
        // construct matrix based on two-core graph
        Set<T> coreDataSet = singletonTcFinder.getRemainedDataSet();
        // generate storage that contains all solutions in the right part and involved left part.
        TIntSet coreVertexSet = new TIntHashSet(keySet.size());
        coreDataSet.stream().map(h3CuckooTable::getVertices).forEach(coreVertexSet::addAll);
        BigInteger[] storage = doublyEncode
            ? generateDoublyStorage(keyValueMap, coreVertexSet, coreDataSet)
            : generateFreeStorage(keyValueMap, coreVertexSet, coreDataSet);
        // split D = L || R
        BigInteger[] leftStorage = new BigInteger[lm];
        BigInteger[] rightStorage = new BigInteger[rm];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // back-fill
        Stack<T> removedDataStack = singletonTcFinder.getRemovedDataStack();
        Stack<int[]> removedDataVerticesStack = singletonTcFinder.getRemovedDataVertices();
        while (!removedDataStack.empty()) {
            T removedData = removedDataStack.pop();
            int[] removedDataVertices = removedDataVerticesStack.pop();
            int vertex0 = removedDataVertices[0];
            int vertex1 = removedDataVertices[1];
            int vertex2 = removedDataVertices[2];
            boolean[] rx = dataHrMap.get(removedData);
            BigInteger innerProduct = zp.innerProduct(rightStorage, rx);
            BigInteger value = keyValueMap.get(removedData);
            innerProduct = zp.sub(value, innerProduct);
            fullDistinctVertices(leftStorage, innerProduct, vertex0, vertex1, vertex2, removedData);
        }
        // fill randomness in the left part
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                if (doublyEncode) {
                    leftStorage[vertex] = zp.createNonZeroRandom(secureRandom);
                } else {
                    leftStorage[vertex] = zp.createZero();
                }
            }
        }
        // update storage
        System.arraycopy(leftStorage, 0, storage, 0, leftStorage.length);
        return storage;
    }

    private H3CuckooTable<T> generateCuckooTable(Map<T, BigInteger> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        H3CuckooTable<T> h3CuckooTable = new H3CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            int h3Value = dataH3Map.get(key);
            h3CuckooTable.addData(new int[]{h1Value, h2Value, h3Value}, key);
        }
        return h3CuckooTable;
    }

    private void fullDistinctVertices(BigInteger[] leftMatrix, BigInteger innerProduct,
                                      int vertex0, int vertex1, int vertex2, T removedData) {
        if (leftMatrix[vertex0] == null) {
            leftMatrix[vertex1] = (leftMatrix[vertex1] == null) ? zp.createNonZeroRandom(secureRandom) : leftMatrix[vertex1];
            innerProduct = zp.sub(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = (leftMatrix[vertex2] == null) ? zp.createNonZeroRandom(secureRandom) : leftMatrix[vertex2];
            innerProduct = zp.sub(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex0] = innerProduct;
        } else if (leftMatrix[vertex1] == null) {
            innerProduct = zp.sub(innerProduct, leftMatrix[vertex0]);
            leftMatrix[vertex2] = (leftMatrix[vertex2] == null) ? zp.createNonZeroRandom(secureRandom) : leftMatrix[vertex2];
            innerProduct = zp.sub(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex1] = innerProduct;
        } else if (leftMatrix[vertex2] == null) {
            innerProduct = zp.sub(innerProduct, leftMatrix[vertex0]);
            innerProduct = zp.sub(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else {
            throw new IllegalStateException(
                removedData + ":(" + vertex0 + ", " + vertex1 + ", " + vertex2 + ") are all full, error"
            );
        }
    }

    private BigInteger[] generateDoublyStorage(Map<T, BigInteger> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        BigInteger[] storage = new BigInteger[m];
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        int d = coreVertexSet.size();
        if (dTilde == 0) {
            // d˜ = 0, we do not need to solve equations, fill random variables.
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = zp.createNonZeroRandom(secureRandom));
            return storage;
        }
        if (dTilde > d + rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + ", d + rm = " + (d + rm) + " no solutions");
        }
        // Let M˜' ∈ {0, 1}^{d˜ × (d + rm)} be the sub-matrix of M˜ obtained by taking the row indexed by R.
        BigInteger[][] tildePrimeMatrix = new BigInteger[dTilde][d + rm];
        BigInteger[] vectorY = new BigInteger[dTilde];
        // construct the vertex -> index map
        int[] coreVertexArray = coreVertexSet.toArray();
        TIntIntMap coreVertexMap = new TIntIntHashMap(d);
        for (int index = 0; index < d; index++) {
            coreVertexMap.put(coreVertexArray[index], index);
        }
        int tildePrimeMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            Arrays.fill(tildePrimeMatrix[tildePrimeMatrixRowIndex], zp.createZero());
            int h1 = dataH1Map.get(data);
            tildePrimeMatrix[tildePrimeMatrixRowIndex][coreVertexMap.get(h1)] = zp.createOne();
            int h2 = dataH2Map.get(data);
            tildePrimeMatrix[tildePrimeMatrixRowIndex][coreVertexMap.get(h2)] = zp.createOne();
            int h3 = dataH3Map.get(data);
            tildePrimeMatrix[tildePrimeMatrixRowIndex][coreVertexMap.get(h3)] = zp.createOne();
            boolean[] rxBinary = dataHrMap.get(data);
            for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                tildePrimeMatrix[tildePrimeMatrixRowIndex][d + rmIndex] = rxBinary[rmIndex] ? zp.createOne() : zp.createZero();
            }
            vectorY[tildePrimeMatrixRowIndex] = keyValueMap.get(data);
            tildePrimeMatrixRowIndex++;
        }
        // Using Gaussian elimination solve the system
        // M˜* (P_{m' + C_1}, ..., P_{m' + C_{d˜})^T = (v'_{R_1}, ..., v'_{R_{d˜})^T.
        BigInteger[] vectorX = new BigInteger[d + rm];
        LinearSolver.SystemInfo systemInfo = linearSolver.fullSolve(tildePrimeMatrix, vectorY, vectorX);
        // Although d˜ > d + rm, we cannot find solution with a negligible probability since the matrix is not full rank
        if (!systemInfo.equals(LinearSolver.SystemInfo.Consistent)) {
            throw new ArithmeticException("There is no solution, the linear system does not have full rank");
        }
        // update the result into the storage
        for (int iRow = 0; iRow < d; iRow++) {
            storage[coreVertexArray[iRow]] = vectorX[iRow];
        }
        System.arraycopy(vectorX, d, storage, lm, rm);
        return storage;
    }

    private BigInteger[] generateFreeStorage(Map<T, BigInteger> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        int d = coreVertexSet.size();
        if (dTilde > d + rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + ", d + rm = " + (d + rm) + " no solutions");
        }
        if (dTilde == 0) {
            BigInteger[] storage = new BigInteger[m];
            // d˜ = 0, we do not need to solve equations, fill 0 variables
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = zp.createZero());
            return storage;
        } else {
            // we need to solve equations
            BigInteger[][] matrixM = new BigInteger[dTilde][m];
            BigInteger[] vectorX = new BigInteger[m];
            BigInteger[] vectorY = new BigInteger[dTilde];
            int rowIndex = 0;
            for (T coreData : coreDataSet) {
                Arrays.fill(matrixM[rowIndex], zp.createZero());
                int h1Value = dataH1Map.get(coreData);
                int h2Value = dataH2Map.get(coreData);
                int h3Value = dataH3Map.get(coreData);
                boolean[] rx = dataHrMap.get(coreData);
                matrixM[rowIndex][h1Value] = zp.createOne();
                matrixM[rowIndex][h2Value] = zp.createOne();
                matrixM[rowIndex][h3Value] = zp.createOne();
                for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                    matrixM[rowIndex][lm + rmIndex] = rx[rmIndex] ? zp.createOne() : zp.createZero();
                }
                vectorY[rowIndex] = keyValueMap.get(coreData);
                rowIndex++;
            }
            LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(matrixM, vectorY, vectorX);
            // Although d˜ > d + rm, we cannot find solution with a negligible probability since the matrix is not full rank
            if (!systemInfo.equals(LinearSolver.SystemInfo.Consistent)) {
                throw new ArithmeticException("There is no solution, the linear system does not have full rank");
            }
            BigInteger[] storage = new BigInteger[m];
            for (int vertex : coreVertexSet.toArray()) {
                // set left part
                storage[vertex] = vectorX[vertex];
            }
            // set right part
            System.arraycopy(vectorX, lm, storage, lm, rm);
            return storage;
        }
    }
}
