package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import cc.redberry.rings.linear.LinearSolver;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTable;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2GctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.tool.EccLinearSolver;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract DOKVS using garbled cuckoo table with 2 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
abstract class AbstractH2GctEccDokvs<T> extends AbstractEccDokvs<T> implements SparseEccDokvs<T> {
    /**
     * left m, i.e., sparse part.
     */
    private final int lm;
    /**
     * right m, i.e., dense part.
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
    protected final CuckooTableTcFinder<T> tcFinder;
    /**
     * ECC linear solver
     */
    private final EccLinearSolver linearSolver;
    /**
     * key -> h1
     */
    private TObjectIntMap<T> dataH1Map;
    /**
     * key -> h2
     */
    private TObjectIntMap<T> dataH2Map;
    /**
     * key -> hr
     */
    private Map<T, boolean[]> dataHrMap;

    AbstractH2GctEccDokvs(EnvType envType, Ecc ecc, int n, int lm, int rm,
                          byte[][] keys, CuckooTableTcFinder<T> tcFinder, SecureRandom secureRandom) {
        super(envType, ecc, n, lm + rm, secureRandom);
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, H2GctDokvsUtils.HASH_KEY_NUM);
        this.lm = lm;
        this.rm = rm;
        hl = PrfFactory.createInstance(envType, Integer.BYTES * H2GctDokvsUtils.SPARSE_HASH_NUM);
        hl.setKey(keys[0]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[1]);
        this.tcFinder = tcFinder;
        linearSolver = new EccLinearSolver(ecc, secureRandom);
    }

    @Override
    public int sparsePositionRange() {
        return lm;
    }

    @Override
    public int[] sparsePositions(T key) {
        return H2GctDokvsUtils.sparsePositions(hl, key, lm);
    }

    @Override
    public int sparsePositionNum() {
        return H2GctDokvsUtils.SPARSE_HASH_NUM;
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
    public ECPoint decode(ECPoint[] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        assert (tcFinder instanceof CuckooTableSingletonTcFinder || tcFinder instanceof H2CuckooTableTcFinder);
        int[] sparsePositions = sparsePositions(key);
        boolean[] binaryDensePositions = binaryDensePositions(key);
        ECPoint value = ecc.getInfinity();
        // h1 and h2 must be distinct
        value = ecc.add(value, storage[sparsePositions[0]]);
        value = ecc.add(value, storage[sparsePositions[1]]);
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (binaryDensePositions[rmIndex]) {
                value = ecc.add(value, storage[lm + rmIndex]);
            }
        }
        return value;
    }

    @Override
    public ECPoint[] encode(Map<T, ECPoint> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        // construct maps
        Set<T> keySet = keyValueMap.keySet();
        int keySize = keySet.size();
        dataH1Map = new TObjectIntHashMap<>(keySize);
        dataH2Map = new TObjectIntHashMap<>(keySize);
        dataHrMap = new ConcurrentHashMap<>(keySize);
        keySet.forEach(key -> {
            int[] sparsePositions = sparsePositions(key);
            boolean[] binaryDensePositions = binaryDensePositions(key);
            dataH1Map.put(key, sparsePositions[0]);
            dataH2Map.put(key, sparsePositions[1]);
            dataHrMap.put(key, binaryDensePositions);
        });
        // generate cuckoo table with 2 hash functions
        H2CuckooTable<T> h2CuckooTable = generateCuckooTable(keyValueMap);
        // find two-core graph
        tcFinder.findTwoCore(h2CuckooTable);
        // construct matrix based on two-core graph
        Set<T> coreDataSet = tcFinder.getRemainedDataSet();
        // generate storage that contains all solutions in the right part and involved left part.
        TIntSet coreVertexSet = new TIntHashSet(keySet.size());
        coreDataSet.stream().map(h2CuckooTable::getVertices).forEach(coreVertexSet::addAll);
        ECPoint[] storage = doublyEncode
            ? generateDoublyStorage(keyValueMap, coreVertexSet, coreDataSet)
            : generateFreeStorage(keyValueMap, coreVertexSet, coreDataSet);
        // split D = L || R
        ECPoint[] leftStorage = new ECPoint[lm];
        ECPoint[] rightStorage = new ECPoint[rm];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // back-fill
        Stack<T> removedDataStack = tcFinder.getRemovedDataStack();
        Stack<int[]> removedDataVerticesStack = tcFinder.getRemovedDataVertices();
        Map<T, ECPoint> removedDataInnerProductMap = removedDataStack.stream()
            .collect(Collectors.toMap(Function.identity(), removedData -> {
                boolean[] rx = dataHrMap.get(removedData);
                ECPoint rightInnerProduct = ecc.innerProduct(rightStorage, rx);
                ECPoint value = keyValueMap.get(removedData);
                return ecc.subtract(value, rightInnerProduct);
            }));
        while (!removedDataStack.empty()) {
            T removedData = removedDataStack.pop();
            int[] removedDataVertices = removedDataVerticesStack.pop();
            int source = removedDataVertices[0];
            int target = removedDataVertices[1];
            ECPoint innerProduct = removedDataInnerProductMap.get(removedData);
            // all positions in the sparse part are distinct
            assert source != target;
            if (leftStorage[source] == null && leftStorage[target] == null) {
                // case 1: left and right are all null
                leftStorage[source] = ecc.randomPoint(secureRandom);
                leftStorage[target] = ecc.subtract(innerProduct, leftStorage[source]);
            } else if (leftStorage[source] == null) {
                // case 2: left is null
                leftStorage[source] = ecc.subtract(innerProduct, leftStorage[target]);
            } else if (leftStorage[target] == null) {
                // case 3: right is null
                leftStorage[target] = ecc.subtract(innerProduct, leftStorage[source]);
            } else {
                throw new IllegalStateException(removedData + ":(" + source + ", " + target + ") are all full, error");
            }
        }
        // fill randomness in the left part
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                if (doublyEncode) {
                    leftStorage[vertex] = ecc.randomPoint(secureRandom);
                } else {
                    leftStorage[vertex] = ecc.getInfinity();
                }
            }
        }
        // update storage
        System.arraycopy(leftStorage, 0, storage, 0, lm);
        return storage;
    }

    private H2CuckooTable<T> generateCuckooTable(Map<T, ECPoint> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        H2CuckooTable<T> h2CuckooTable = new H2CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            h2CuckooTable.addData(new int[]{h1Value, h2Value}, key);
        }
        return h2CuckooTable;
    }

    private ECPoint[] generateDoublyStorage(Map<T, ECPoint> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        ECPoint[] storage = new ECPoint[m];
        // Let d˜ = |R| and abort if d˜ > d + rm
        int dTilde = coreDataSet.size();
        int d = coreVertexSet.size();
        if (dTilde == 0) {
            // d˜ = 0, we do not need to solve equations, fill random variables.
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = ecc.randomPoint(secureRandom));
            return storage;
        }
        if (dTilde > d + rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + ", d + rm = " + (d + rm) + " no solutions");
        }
        // Let M˜' ∈ {0,1}^{d˜ × (d + rm)} be the sub-matrix of M˜ obtained by taking the row indexed by R.
        BigInteger[][] tildePrimeMatrix = new BigInteger[dTilde][d + rm];
        ECPoint[] vectorY = new ECPoint[dTilde];
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
            boolean[] rxBinary = dataHrMap.get(data);
            for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                tildePrimeMatrix[tildePrimeMatrixRowIndex][d + rmIndex] = rxBinary[rmIndex] ? zp.createOne() : zp.createZero();
            }
            // clone Y
            vectorY[tildePrimeMatrixRowIndex] = keyValueMap.get(data);
            tildePrimeMatrixRowIndex++;
        }
        // Using Gaussian elimination solve the system
        // M˜* (P_{m' + C_1}, ..., P_{m' + C_{d˜})^T = (v'_{R_1}, ..., v'_{R_{d˜})^T.
        ECPoint[] vectorX = new ECPoint[d + rm];
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

    private ECPoint[] generateFreeStorage(Map<T, ECPoint> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        int d = coreVertexSet.size();
        if (dTilde > d + rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + ", d + rm = " + (d + rm) + " no solutions");
        }
        if (dTilde == 0) {
            ECPoint[] storage = new ECPoint[m];
            // d˜ = 0, we do not need to solve equations, fill 0 variables.
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = ecc.getInfinity());
            return storage;
        } else {
            // we need to solve equations
            BigInteger[][] matrixM = new BigInteger[dTilde][m];
            ECPoint[] vectorX = new ECPoint[m];
            ECPoint[] vectorY = new ECPoint[dTilde];
            int rowIndex = 0;
            for (T coreData : coreDataSet) {
                Arrays.fill(matrixM[rowIndex], zp.createZero());
                int h1Value = dataH1Map.get(coreData);
                int h2Value = dataH2Map.get(coreData);
                boolean[] rx = dataHrMap.get(coreData);
                matrixM[rowIndex][h1Value] = zp.createOne();
                matrixM[rowIndex][h2Value] = zp.createOne();
                for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                    matrixM[rowIndex][lm + rmIndex] = rx[rmIndex] ? zp.createOne() : zp.createZero();
                }
                // clone Y
                vectorY[rowIndex] = keyValueMap.get(coreData);
                rowIndex++;
            }
            LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(matrixM, vectorY, vectorX);
            // Although d˜ > d + rm, we cannot find solution with a negligible probability since the matrix is not full rank
            if (!systemInfo.equals(LinearSolver.SystemInfo.Consistent)) {
                throw new ArithmeticException("There is no solution, the linear system does not have full rank");
            }
            ECPoint[] storage = new ECPoint[m];
            // set left part
            for (int vertex : coreVertexSet.toArray()) {
                storage[vertex] = vectorX[vertex];
            }
            // set right part
            System.arraycopy(vectorX, lm, storage, lm, rm);
            return storage;
        }
    }
}
