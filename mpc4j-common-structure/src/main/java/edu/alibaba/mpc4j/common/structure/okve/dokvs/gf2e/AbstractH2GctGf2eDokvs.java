package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import cc.redberry.rings.linear.LinearSolver;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2GctDokvsUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTable;
import edu.alibaba.mpc4j.common.structure.okve.tool.BinaryLinearSolver;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.IntStream;

/**
 * abstract DOKVS using garbled cuckoo table with 2 hash functions. The non-doubly construction is from the following
 * paper:
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 * The doubly-obliviousness construction is form the following paper:
 * <p>
 * Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: fast OPRF and circuit-PSI from vector-OLE. EUROCRYPT 2021,
 * pp. 901-930. Cham: Springer International Publishing, 2021.
 * </p>
 * Here we use blazing fast encoding introduced in the following paper:
 * <p>
 * Raghuraman, Srinivasan, and Peter Rindal. Blazing fast PSI from improved OKVS and subfield VOLE. ACM CCS 2022,
 * pp. 2505-2517.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
abstract class AbstractH2GctGf2eDokvs<T> extends AbstractGf2eDokvs<T> implements SparseGf2eDokvs<T> {
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
     * binary linear solver
     */
    private final BinaryLinearSolver linearSolver;
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

    AbstractH2GctGf2eDokvs(EnvType envType, int n, int lm, int rm, int l,
                           byte[][] keys, CuckooTableTcFinder<T> tcFinder, SecureRandom secureRandom) {
        super(n, lm + rm, l, secureRandom);
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, H2GctDokvsUtils.HASH_KEY_NUM);
        this.lm = lm;
        this.rm = rm;
        hl = PrfFactory.createInstance(envType, Integer.BYTES * H2GctDokvsUtils.SPARSE_HASH_NUM);
        hl.setKey(keys[0]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[1]);
        this.tcFinder = tcFinder;
        linearSolver = new BinaryLinearSolver(l, secureRandom);
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
    public byte[] decode(byte[][] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        assert (tcFinder instanceof CuckooTableSingletonTcFinder || tcFinder instanceof H2CuckooTableTcFinder);
        int[] sparsePositions = sparsePositions(key);
        boolean[] binaryDensePositions = binaryDensePositions(key);
        byte[] value = new byte[byteL];
        // h1 and h2 must be distinct
        BytesUtils.xori(value, storage[sparsePositions[0]]);
        BytesUtils.xori(value, storage[sparsePositions[1]]);
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (binaryDensePositions[rmIndex]) {
                BytesUtils.xori(value, storage[lm + rmIndex]);
            }
        }
        assert BytesUtils.isFixedReduceByteArray(value, byteL, l);
        return value;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        keyValueMap.values().forEach(x -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(x, byteL, l)));
        // construct maps
        Set<T> keySet = keyValueMap.keySet();
        int keySize = keySet.size();
        dataH1Map = new TObjectIntHashMap<>(keySize);
        dataH2Map = new TObjectIntHashMap<>(keySize);
        dataHrMap = new HashMap<>(keySize);
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
        byte[][] storage = doublyEncode
            ? generateDoublyStorage(keyValueMap, coreVertexSet, coreDataSet)
            : generateFreeStorage(keyValueMap, coreVertexSet, coreDataSet);
        // split D = L || R
        byte[][] leftStorage = new byte[lm][];
        byte[][] rightStorage = new byte[rm][];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // back-fill
        Stack<T> removedDataStack = tcFinder.getRemovedDataStack();
        Stack<int[]> removedDataVerticesStack = tcFinder.getRemovedDataVertices();
        while (!removedDataStack.empty()) {
            T removedData = removedDataStack.pop();
            int[] removedDataVertices = removedDataVerticesStack.pop();
            int source = removedDataVertices[0];
            int target = removedDataVertices[1];
            boolean[] rx = dataHrMap.get(removedData);
            byte[] innerProduct = BytesUtils.innerProduct(rightStorage, byteL, rx);
            byte[] value = keyValueMap.get(removedData);
            BytesUtils.xori(innerProduct, value);
            // all positions in the sparse part are distinct
            assert source != target;
            if (leftStorage[source] == null && leftStorage[target] == null) {
                // case 1: left and right are all null
                leftStorage[source] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                BytesUtils.xori(innerProduct, leftStorage[source]);
                leftStorage[target] = innerProduct;
            } else if (leftStorage[source] == null) {
                // case 2: left is null
                BytesUtils.xori(innerProduct, leftStorage[target]);
                leftStorage[source] = innerProduct;
            } else if (leftStorage[target] == null) {
                // case 3: right is null
                BytesUtils.xori(innerProduct, leftStorage[source]);
                leftStorage[target] = innerProduct;
            } else {
                throw new IllegalStateException(removedData + ":(" + source + ", " + target + ") are all full, error");
            }
        }
        // fill randomness in the left part
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                if (doublyEncode) {
                    leftStorage[vertex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                } else {
                    leftStorage[vertex] = new byte[byteL];
                }
            }
        }
        // update storage
        System.arraycopy(leftStorage, 0, storage, 0, lm);
        return storage;
    }

    private H2CuckooTable<T> generateCuckooTable(Map<T, byte[]> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        H2CuckooTable<T> h2CuckooTable = new H2CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            h2CuckooTable.addData(new int[]{h1Value, h2Value}, key);
        }
        return h2CuckooTable;
    }

    private byte[][] generateDoublyStorage(Map<T, byte[]> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        byte[][] storage = new byte[m][];
        // Let d˜ = |R| and abort if d˜ > d + rm
        int dTilde = coreDataSet.size();
        int d = coreVertexSet.size();
        if (dTilde == 0) {
            // d˜ = 0, we do not need to solve equations, fill random variables.
            IntStream.range(lm, lm + rm).forEach(index ->
                storage[index] = BytesUtils.randomByteArray(byteL, l, secureRandom)
            );
            return storage;
        }
        if (dTilde > d + rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + ", d + rm = " + (d + rm) + " no solutions");
        }
        // Let M˜' ∈ {0,1}^{d˜ × (d + rm)} be the sub-matrix of M˜ obtained by taking the row indexed by R.
        int columnTildeBytes = CommonUtils.getByteLength(d + rm);
        int columnTildeOffset = columnTildeBytes * Byte.SIZE - (d + rm);
        byte[][] tildePrimeMatrix = new byte[dTilde][columnTildeBytes];
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
            BinaryUtils.setBoolean(tildePrimeMatrix[tildePrimeMatrixRowIndex], columnTildeOffset + coreVertexMap.get(h1), true);
            int h2 = dataH2Map.get(data);
            BinaryUtils.setBoolean(tildePrimeMatrix[tildePrimeMatrixRowIndex], columnTildeOffset + coreVertexMap.get(h2), true);
            boolean[] rxBinary = dataHrMap.get(data);
            for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                BinaryUtils.setBoolean(tildePrimeMatrix[tildePrimeMatrixRowIndex], columnTildeOffset + d + rmIndex, rxBinary[rmIndex]);
            }
            vectorY[tildePrimeMatrixRowIndex] = BytesUtils.clone(keyValueMap.get(data));
            tildePrimeMatrixRowIndex++;
        }
        // Using Gaussian elimination solve the system
        // M˜* (P_{m' + C_1}, ..., P_{m' + C_{d˜})^T = (v'_{R_1}, ..., v'_{R_{d˜})^T.
        byte[][] vectorX = new byte[d + rm][];
        LinearSolver.SystemInfo systemInfo = linearSolver.fullSolve(tildePrimeMatrix, d + rm, vectorY, vectorX);
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
            // d˜ = 0, we do not need to solve equations, fill 0 variables.
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = new byte[byteL]);
            return storage;
        } else {
            // we need to solve equations
            byte[][] matrixM = new byte[dTilde][byteM];
            byte[][] vectorX = new byte[m][];
            byte[][] vectorY = new byte[dTilde][];
            int rowIndex = 0;
            for (T coreData : coreDataSet) {
                int h1Value = dataH1Map.get(coreData);
                int h2Value = dataH2Map.get(coreData);
                boolean[] rx = dataHrMap.get(coreData);
                BinaryUtils.setBoolean(matrixM[rowIndex], h1Value, true);
                BinaryUtils.setBoolean(matrixM[rowIndex], h2Value, true);
                for (int columnIndex = 0; columnIndex < rm; columnIndex++) {
                    BinaryUtils.setBoolean(matrixM[rowIndex], lm + columnIndex, rx[columnIndex]);
                }
                vectorY[rowIndex] = BytesUtils.clone(keyValueMap.get(coreData));
                rowIndex++;
            }
            LinearSolver.SystemInfo systemInfo = linearSolver.freeSolve(matrixM, m, vectorY, vectorX);
            // Although d˜ > d + rm, we cannot find solution with a negligible probability since the matrix is not full rank
            if (!systemInfo.equals(LinearSolver.SystemInfo.Consistent)) {
                throw new ArithmeticException("There is no solution, the linear system does not have full rank");
            }
            byte[][] storage = new byte[m][];
            // set left part
            for (int vertex : coreVertexSet.toArray()) {
                storage[vertex] = BytesUtils.clone(vectorX[vertex]);
            }
            // set right part
            System.arraycopy(vectorX, lm, storage, lm, rm);
            return storage;
        }
    }
}
