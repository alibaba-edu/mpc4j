package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

import cc.redberry.rings.linear.LinearSolver;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3GctDokvsUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H3CuckooTable;
import edu.alibaba.mpc4j.common.structure.okve.tool.Gf2kLinearSolver;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * abstract DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
abstract class AbstractH3FieldGctGf2kDokvs<T> extends AbstractGf2kDokvs<T> implements FieldGf2kDokvs<T> {
    /**
     * number of sparse hashes
     */
    static final int SPARSE_HASH_NUM = 3;
    /**
     * number of hash keys
     */
    static final int HASH_KEY_NUM = 2;
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
     * Hr: {0, 1}^* -> {0, 1}^κ
     */
    private final Prf hr;
    /**
     * two core finder
     */
    private final CuckooTableSingletonTcFinder<T> singletonTcFinder;
    /**
     * GF2K linear solver
     */
    private final Gf2kLinearSolver linearSolver;
    /**
     * key -> h1
     */
    private Map<T, Integer> dataH1Map;
    /**
     * key -> h2
     */
    private Map<T, Integer> dataH2Map;
    /**
     * key -> h3
     */
    private Map<T, Integer> dataH3Map;
    /**
     * key -> hr
     */
    private Map<T, byte[][]> dataHrMap;

    AbstractH3FieldGctGf2kDokvs(EnvType envType, int n, int lm, int rm, byte[][] keys, SecureRandom secureRandom) {
        super(envType, n, lm + rm, secureRandom);
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, HASH_KEY_NUM);
        this.lm = lm;
        this.rm = rm;
        hl = PrfFactory.createInstance(envType, Integer.BYTES * SPARSE_HASH_NUM);
        hl.setKey(keys[0]);
        hr = PrfFactory.createInstance(envType, gf2k.getByteL());
        hr.setKey(keys[1]);
        singletonTcFinder = new CuckooTableSingletonTcFinder<>();
        linearSolver = new Gf2kLinearSolver(gf2k, secureRandom);
    }

    @Override
    public int[] sparsePositions(T key) {
        return H3GctDokvsUtils.sparsePositions(hl, key, lm);
    }

    @Override
    public byte[][] denseFields(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        byte[][] wedgeKs = new byte[rm][];
        // ^k = I(k, r), and I: {0, 1}^* → {0, 1}^κ is a random mapping.
        wedgeKs[0] = hr.getBytes(keyBytes);
        // ^row(k, r) = (^k, ^k^2, ..., ^k^(rm))
        for (int rmIndex = 1; rmIndex < rm; rmIndex++) {
            wedgeKs[rmIndex] = gf2k.mul(wedgeKs[rmIndex - 1], wedgeKs[rmIndex - 1]);
        }
        return wedgeKs;
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        int[] sparsePositions = sparsePositions(key);
        byte[][] denseFields = denseFields(key);
        byte[] value = gf2k.createZero();
        // h1, h2 and h3 must be distinct
        gf2k.addi(value, storage[sparsePositions[0]]);
        gf2k.addi(value, storage[sparsePositions[1]]);
        gf2k.addi(value, storage[sparsePositions[2]]);
        // multiply and add dense parts
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            gf2k.addi(value, gf2k.mul(denseFields[rmIndex], storage[lm + rmIndex]));
        }
        return value;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        keyValueMap.values().forEach(x -> Preconditions.checkArgument(gf2k.validateElement(x)));
        // construct maps
        Set<T> keySet = keyValueMap.keySet();
        int keySize = keySet.size();
        dataH1Map = new ConcurrentHashMap<>(keySize);
        dataH2Map = new ConcurrentHashMap<>(keySize);
        dataH3Map = new ConcurrentHashMap<>(keySize);
        dataHrMap = new ConcurrentHashMap<>(keySize);
        Stream<T> keyStream = keySet.stream();
        keyStream = parallelEncode ? keyStream.parallel() : keyStream;
        keyStream.forEach(key -> {
            int[] sparsePositions = sparsePositions(key);
            byte[][] denseFields = denseFields(key);
            dataH1Map.put(key, sparsePositions[0]);
            dataH2Map.put(key, sparsePositions[1]);
            dataH3Map.put(key, sparsePositions[2]);
            dataHrMap.put(key, denseFields);
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
        byte[][] storage = doublyEncode
            ? generateDoublyStorage(keyValueMap, coreVertexSet, coreDataSet)
            : generateFreeStorage(keyValueMap, coreVertexSet, coreDataSet);
        // split D = L || R
        byte[][] leftStorage = new byte[lm][];
        byte[][] rightStorage = new byte[rm][];
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
            byte[][] rx = dataHrMap.get(removedData);
            byte[] innerProduct = gf2k.createZero();
            for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                gf2k.addi(innerProduct, gf2k.mul(rx[rmIndex], rightStorage[rmIndex]));
            }
            byte[] value = keyValueMap.get(removedData);
            byte[] remainValue = gf2k.sub(value, innerProduct);
            fullDistinctVertices(leftStorage, remainValue, vertex0, vertex1, vertex2, removedData);
        }
        // fill randomness in the left part
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                if (doublyEncode) {
                    leftStorage[vertex] = gf2k.createRandom(secureRandom);
                } else {
                    leftStorage[vertex] = gf2k.createZero();
                }
            }
        }
        // update storage
        System.arraycopy(leftStorage, 0, storage, 0, leftStorage.length);
        return storage;
    }

    private H3CuckooTable<T> generateCuckooTable(Map<T, byte[]> keyValueMap) {
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

    private void fullDistinctVertices(byte[][] leftMatrix, byte[] remainValue,
                                      int vertex0, int vertex1, int vertex2, T removedData) {
        if (leftMatrix[vertex0] == null) {
            leftMatrix[vertex1] = (leftMatrix[vertex1] == null) ? gf2k.createRandom(secureRandom) : leftMatrix[vertex1];
            gf2k.subi(remainValue, leftMatrix[vertex1]);
            leftMatrix[vertex2] = (leftMatrix[vertex2] == null) ? gf2k.createRandom(secureRandom) : leftMatrix[vertex2];
            gf2k.subi(remainValue, leftMatrix[vertex2]);
            leftMatrix[vertex0] = remainValue;
        } else if (leftMatrix[vertex1] == null) {
            gf2k.subi(remainValue, leftMatrix[vertex0]);
            leftMatrix[vertex2] = (leftMatrix[vertex2] == null) ? gf2k.createRandom(secureRandom) : leftMatrix[vertex2];
            gf2k.subi(remainValue, leftMatrix[vertex2]);
            leftMatrix[vertex1] = remainValue;
        } else if (leftMatrix[vertex2] == null) {
            gf2k.subi(remainValue, leftMatrix[vertex0]);
            gf2k.subi(remainValue, leftMatrix[vertex1]);
            leftMatrix[vertex2] = remainValue;
        } else {
            throw new IllegalStateException(
                removedData + ":(" + vertex0 + ", " + vertex1 + ", " + vertex2 + ") are all full, error"
            );
        }
    }

    private byte[][] generateDoublyStorage(Map<T, byte[]> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        byte[][] storage = new byte[m][];
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        int d = coreVertexSet.size();
        if (dTilde == 0) {
            // d˜ = 0, we do not need to solve equations, fill random variables.
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = gf2k.createRandom(secureRandom));
            return storage;
        }
        if (dTilde > d + rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + ", d + rm = " + (d + rm) + " no solutions");
        }
        // Let M˜' ∈ {0, 1}^{d˜ × (d + rm)} be the sub-matrix of M˜ obtained by taking the row indexed by R.
        byte[][][] tildePrimeMatrix = new byte[dTilde][d + rm][gf2k.getByteL()];
        byte[][] vectorY = new byte[dTilde][];
        // construct the vertex -> index map
        int[] coreVertexArray = coreVertexSet.toArray();
        TIntIntMap coreVertexMap = new TIntIntHashMap(d);
        for (int index = 0; index < d; index++) {
            coreVertexMap.put(coreVertexArray[index], index);
        }
        int tildePrimeMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            int h1 = dataH1Map.get(data);
            tildePrimeMatrix[tildePrimeMatrixRowIndex][coreVertexMap.get(h1)] = gf2k.createOne();
            int h2 = dataH2Map.get(data);
            tildePrimeMatrix[tildePrimeMatrixRowIndex][coreVertexMap.get(h2)] = gf2k.createOne();
            int h3 = dataH3Map.get(data);
            tildePrimeMatrix[tildePrimeMatrixRowIndex][coreVertexMap.get(h3)] = gf2k.createOne();
            byte[][] rx = dataHrMap.get(data);
            System.arraycopy(rx, 0, tildePrimeMatrix[tildePrimeMatrixRowIndex], d, rm);
            vectorY[tildePrimeMatrixRowIndex] = BytesUtils.clone(keyValueMap.get(data));
            tildePrimeMatrixRowIndex++;
        }
        // Using Gaussian elimination solve the system
        // M˜* (P_{m' + C_1}, ..., P_{m' + C_{d˜})^T = (v'_{R_1}, ..., v'_{R_{d˜})^T.
        byte[][] vectorX = new byte[d + rm][];
        LinearSolver.SystemInfo systemInfo = linearSolver.fullSolve(tildePrimeMatrix, vectorY, vectorX);
        // Although d˜ > d + rm, we cannot find solution with a negligible probability since the matrix is not full rank
        if (!systemInfo.equals(LinearSolver.SystemInfo.Consistent)) {
            throw new ArithmeticException("There is no solution, the linear system does not have full rank");
        }
        // update the result into the storage
        for (int iRow = 0; iRow < d; iRow++) {
            storage[coreVertexArray[iRow]] = BytesUtils.clone(vectorX[iRow]);
        }
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            storage[lm + rmIndex] = BytesUtils.clone(vectorX[d + rmIndex]);
        }
        return storage;
    }

    private byte[][] generateFreeStorage(Map<T, byte[]> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        int d = coreVertexSet.size();
        if (dTilde > d + rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + ", d + rm = " + (d + rm) + " no solutions");
        }
        if (dTilde == 0) {
            byte[][] storage = new byte[m][];
            // d˜ = 0, we do not need to solve equations, fill 0 variables
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = gf2k.createZero());
            return storage;
        } else {
            // we need to solve equations
            byte[][][] matrixM = new byte[dTilde][m][gf2k.getByteL()];
            byte[][] vectorX = new byte[m][];
            byte[][] vectorY = new byte[dTilde][];
            int rowIndex = 0;
            for (T coreData : coreDataSet) {
                int h1Value = dataH1Map.get(coreData);
                int h2Value = dataH2Map.get(coreData);
                int h3Value = dataH3Map.get(coreData);
                byte[][] rx = dataHrMap.get(coreData);
                matrixM[rowIndex][h1Value] = gf2k.createOne();
                matrixM[rowIndex][h2Value] = gf2k.createOne();
                matrixM[rowIndex][h3Value] = gf2k.createOne();
                System.arraycopy(rx, 0, matrixM[rowIndex], lm, rm);
                vectorY[rowIndex] = BytesUtils.clone(keyValueMap.get(coreData));
                rowIndex++;
            }
            LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(matrixM, vectorY, vectorX);
            // Although d˜ > d + rm, we cannot find solution with a negligible probability since the matrix is not full rank
            if (!systemInfo.equals(LinearSolver.SystemInfo.Consistent)) {
                throw new ArithmeticException("There is no solution, the linear system does not have full rank");
            }
            byte[][] storage = new byte[m][];
            for (int vertex : coreVertexSet.toArray()) {
                // set left part
                storage[vertex] = BytesUtils.clone(vectorX[vertex]);
            }
            // set right part
            System.arraycopy(vectorX, lm, storage, lm, rm);
            return storage;
        }
    }
}
