package edu.alibaba.mpc4j.common.structure.okve.ovdm.zp;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.ovdm.zp.ZpOvdmFactory.ZpOvdmType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.structure.okve.tool.ZpMaxLisFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTable;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.utils.*;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 2-hash-two-core-OVDM in ZP.
 *
 * @author Weiran Liu
 * @date 2021/10/01
 */
class H2TcGctZpOvdm<T> extends AbstractZpOvdm<T> implements SparseZpOvdm<T> {
    /**
     * 2 sparse hashes
     */
    private static final int SPARSE_HASH_NUM = 2;
    /**
     * hash num
     */
    static int HASH_NUM = SPARSE_HASH_NUM + 1;
    /**
     * ε
     */
    private static final double EPSILON = 0.4;
    /**
     * left m = (2 + ε) * n, flooring to lm % Byte.SIZE == 0
     */
    private final int lm;
    /**
     * right m = (1 + ε) * log(n) + λ, flooring to rm % Byte.SIZE == 0
     */
    private final int rm;
    /**
     * the first hash
     */
    private final Prf h1;
    /**
     * the second hash
     */
    private final Prf h2;
    /**
     * the right hash for r(x)
     */
    private final Prf hr;
    /**
     * 2-core finder
     */
    private final CuckooTableTcFinder<T> tcFinder;
    /**
     * max linear independent system finder
     */
    private final ZpMaxLisFinder maxLisFinder;
    /**
     * data -> h1
     */
    private TObjectIntMap<T> dataH1Map;
    /**
     * data -> h2
     */
    private TObjectIntMap<T> dataH2Map;
    /**
     * data -> hr
     */
    private Map<T, boolean[]> dataHrMap;


    H2TcGctZpOvdm(EnvType envType, BigInteger prime, int n, byte[][] keys, CuckooTableTcFinder<T> tcFinder) {
        super(envType, prime, n, getLm(n) + getRm(n));
        Preconditions.checkArgument(
            tcFinder instanceof CuckooTableSingletonTcFinder || tcFinder instanceof H2CuckooTableTcFinder
        );
        lm = getLm(n);
        rm = getRm(n);
        h1 = PrfFactory.createInstance(envType, Integer.BYTES);
        h1.setKey(keys[0]);
        h2 = PrfFactory.createInstance(envType, Integer.BYTES);
        h2.setKey(keys[1]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[2]);
        this.tcFinder = tcFinder;
        maxLisFinder = new ZpMaxLisFinder(ZpFactory.createInstance(envType, prime));
    }

    @Override
    public int[] sparsePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] sparsePositions = new int[SPARSE_HASH_NUM];
        sparsePositions[0] = h1.getInteger(0, keyBytes, lm);
        // h1 and h2 are distinct
        int h2Index = 0;
        do {
            sparsePositions[1] = h2.getInteger(h2Index, keyBytes, lm);
            h2Index++;
        } while (sparsePositions[1] == sparsePositions[0]);
        return sparsePositions;
    }

    @Override
    public int sparsePositionNum() {
        return SPARSE_HASH_NUM;
    }

    @Override
    public boolean[] densePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        return BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
    }

    @Override
    public int maxDensePositionNum() {
        return rm;
    }

    @Override
    public BigInteger decode(BigInteger[] storage, T key) {
        MathPreconditions.checkEqual("storage.length", "m", storage.length, getM());
        int[] sparsePositions = sparsePositions(key);
        boolean[] densePositions = densePositions(key);
        BigInteger value = BigInteger.ZERO;
        // h1 and h2 must be distinct
        value = zp.add(zp.add(value, storage[sparsePositions[0]]), storage[sparsePositions[1]]);
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (densePositions[rmIndex]) {
                value = zp.add(value, storage[lm + rmIndex]);
            }
        }
        return value;
    }

    @Override
    public ZpOvdmType getZpOvdmType() {
        if (tcFinder instanceof CuckooTableSingletonTcFinder) {
            return ZpOvdmType.H2_SINGLETON_GCT;
        } else if (tcFinder instanceof H2CuckooTableTcFinder) {
            return ZpOvdmType.H2_TWO_CORE_GCT;
        } else {
            throw new IllegalStateException("Invalid TcFinder:" + tcFinder.getClass().getSimpleName());
        }
    }

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }

    @Override
    public BigInteger[] encode(Map<T, BigInteger> keyValueMap) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value pairs num", keyValueMap.size(), n);
        // compute hashes
        Set<T> keySet = keyValueMap.keySet();
        dataH1Map = new TObjectIntHashMap<>(keySet.size());
        dataH2Map = new TObjectIntHashMap<>(keySet.size());
        dataHrMap = new HashMap<>(keySet.size());
        for (T key : keySet) {
            int[] sparsePositions = sparsePositions(key);
            boolean[] densePositions = densePositions(key);
            dataH1Map.put(key, sparsePositions[0]);
            dataH2Map.put(key, sparsePositions[1]);
            dataHrMap.put(key, densePositions);
        }
        // generate 2-hash cuckoo table
        H2CuckooTable<T> h2CuckooTable = generateCuckooTable(keyValueMap);
        // find two-core graph
        tcFinder.findTwoCore(h2CuckooTable);
        // find two-core nodes based on the 2-core graph
        Set<T> coreDataSet = tcFinder.getRemainedDataSet();
        // generate the storages for two-core nodes
        BigInteger[] storage = generateStorage(keyValueMap, coreDataSet);
        // split the storage to L || D
        BigInteger[] leftStorage = new BigInteger[lm];
        BigInteger[] rightStorage = new BigInteger[rm];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // remove data from stack, and assign storages
        Stack<T> removedDataStack = tcFinder.getRemovedDataStack();
        Stack<int[]> removedDataVerticesStack = tcFinder.getRemovedDataVertices();
        // compute right inner product
        Map<T, BigInteger> removedDataInnerProductMap = removedDataStack.stream()
            .collect(Collectors.toMap(Function.identity(), removedData -> {
                boolean[] rx = dataHrMap.get(removedData);
                BigInteger rightInnerProduct = zp.innerProduct(rightStorage, rx);
                BigInteger value = keyValueMap.get(removedData);
                return zp.sub(value, rightInnerProduct);
            }));
        while (!removedDataStack.empty()) {
            T removedData = removedDataStack.pop();
            int[] removedDataVertices = removedDataVerticesStack.pop();
            int source = removedDataVertices[0];
            int target = removedDataVertices[1];
            removedDataInnerProductMap.get(removedData);
            BigInteger innerProduct = removedDataInnerProductMap.get(removedData);
            if (source == target) {
                // source == target, set one value
                if (leftStorage[source] == null) {
                    leftStorage[source] = innerProduct;
                } else {
                    throw new IllegalStateException(removedData + ":(" + source + ", " + target + ") all not null");
                }
            } else {
                // source != target, there are 4 possibilities
                if (leftStorage[source] == null && leftStorage[target] == null) {
                    // case 1: source and target are both empty
                    leftStorage[source] = zp.createNonZeroRandom(secureRandom);
                    leftStorage[target] = zp.sub(innerProduct, leftStorage[source]);
                } else if (leftStorage[source] == null) {
                    // case 2: source is empty, target is not empty
                    leftStorage[source] = zp.sub(innerProduct, leftStorage[target]);
                } else if (leftStorage[target] == null) {
                    // case 3: target is empty, source is not empty
                    leftStorage[target] = zp.sub(innerProduct, leftStorage[source]);
                } else {
                    // source and target are both not empty, impossible
                    throw new IllegalStateException(removedData + ":(" + source + ", " + target + ") all not null");
                }
            }
        }
        // padding random values for the left part
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                leftStorage[vertex] = zp.createNonZeroRandom(secureRandom);
            }
        }
        // update the storage
        System.arraycopy(leftStorage, 0, storage, 0, lm);
        // all storages should be not null
        for (BigInteger row : storage) {
            assert row != null;
        }
        return storage;
    }

    private BigInteger[] generateStorage(Map<T, BigInteger> keyValueMap, Set<T> coreDataSet) {
        // initialize storage, all position is null
        BigInteger[] storage = new BigInteger[m];
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        // if there is no 2-core node, pad random element for the right part
        if (dTilde == 0) {
            IntStream.range(lm, lm + rm).forEach(index -> storage[index] = zp.createNonZeroRandom(secureRandom));
            return storage;
        }
        if (dTilde > rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + "，d + λ + " + rm + "，线性系统无解");
        }
        // Let M˜' ∈ {0, 1}^{d˜ × (d + λ)} be the sub-matrix of M˜ obtained by taking the row indexed by R.
        BigInteger[][] tildePrimeMatrix = new BigInteger[rm][dTilde];
        int tildePrimeMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                tildePrimeMatrix[rmIndex][tildePrimeMatrixRowIndex]
                    = rxBinary[rmIndex] ? BigInteger.ONE : BigInteger.ZERO;
            }
            tildePrimeMatrixRowIndex++;
        }
        // Otherwise, let M˜* be one such matrix and C ⊂ [d + λ] index the corresponding columns of M˜.
        TIntSet setC = maxLisFinder.getLisRows(tildePrimeMatrix);
        int[] cArray = setC.toArray();
        BigInteger[][] tildeStarMatrix = new BigInteger[dTilde][setC.size()];
        int tildeStarMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            int rmIndex = 0;
            for (int r : cArray) {
                tildeStarMatrix[tildeStarMatrixRowIndex][rmIndex] = rxBinary[r] ? BigInteger.ONE : BigInteger.ZERO;
                rmIndex++;
            }
            tildeStarMatrixRowIndex++;
        }
        // Let C' = {j | i \in R, M'_{i, j} = 1} ∪ ([d + λ] \ C + m')
        Set<Integer> setPrimeC = new HashSet<>(dTilde * 2 + rm / 2);
        for (T data : coreDataSet) {
            setPrimeC.add(dataH1Map.get(data));
            setPrimeC.add(dataH2Map.get(data));
        }
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (!setC.contains(rmIndex)) {
                setPrimeC.add(lm + rmIndex);
            }
        }
        // For i ∈ C' assign P_i ∈ G
        for (Integer primeIndexC : setPrimeC) {
            storage[primeIndexC] = zp.createNonZeroRandom(secureRandom);
        }
        // For i ∈ R, define v'_i = v_i - (MP), where P_i is assigned to be zero if unassigned.
        BigInteger[] vectorY = new BigInteger[dTilde];
        int coreRowIndex = 0;
        for (T data : coreDataSet) {
            int h1Value = dataH1Map.get(data);
            int h2Value = dataH2Map.get(data);
            boolean[] rx = dataHrMap.get(data);
            BigInteger mp = BigInteger.ZERO;
            if (storage[h1Value] == null) {
                storage[h1Value] = BigInteger.ZERO;
            }
            if (storage[h2Value] == null) {
                storage[h2Value] = BigInteger.ZERO;
            }
            // h1 and h2 must be distinct
            mp = zp.add(mp, storage[h1Value]);
            mp = zp.add(mp, storage[h2Value]);
            for (int rxIndex = 0; rxIndex < rx.length; rxIndex++) {
                if (rx[rxIndex]) {
                    if (storage[lm + rxIndex] == null) {
                        storage[lm + rxIndex] = BigInteger.ZERO;
                    }
                    mp = zp.add(mp, storage[lm + rxIndex]);
                }
            }
            BigInteger value = keyValueMap.get(data);
            vectorY[coreRowIndex] = zp.sub(value, mp);
            coreRowIndex++;
        }
        // Using Gaussian elimination solve the system
        // M˜* (P_{m' + C_1}, ..., P_{m' + C_{d˜})^T = (v'_{R_1}, ..., v'_{R_{d˜})^T.
        BigInteger[] vectorX = new BigInteger[setC.size()];
        SystemInfo systemInfo = zpLinearSolver.freeSolve(tildeStarMatrix, vectorY, vectorX);
        if (systemInfo.compareTo(SystemInfo.Inconsistent) == 0) {
            throw new ArithmeticException("无法完成编码过程，线性系统无解");
        }
        // update solutions into the storage
        int xVectorIndex = 0;
        for (int cIndex : cArray) {
            storage[lm + cIndex] = vectorX[xVectorIndex];
            xVectorIndex++;
        }
        return storage;
    }

    /**
     * 给定待编码的键值对个数，计算左侧映射比特长度。
     *
     * @param n 待编码的键值对个数。
     * @return 左侧哈希比特长度，向上取整为Byte.SIZE的整数倍。
     */
    static int getLm(int n) {
        MathPreconditions.checkPositive("n", n);
        // 根据论文的表2， lm = (2 + ε) * n = 2.4n，向上取整到Byte.SIZE的整数倍
        return CommonUtils.getByteLength((int) Math.ceil((2 + EPSILON) * n)) * Byte.SIZE;
    }

    /**
     * 给定待编码的键值对个数，计算右侧映射比特长度。
     *
     * @param n 待编码的键值对个数。
     * @return 右侧映射比特长度。向上取整为Byte.SIZE的整数倍。
     */
    static int getRm(int n) {
        // 根据论文完整版第18页，r = (1 + ε) * log(n) + λ = 1.4 * log(n) + λ，向上取整到Byte.SIZE的整数倍
        return CommonUtils.getByteLength(
            (int) Math.ceil((1 + EPSILON) * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
    }

    /**
     * 生成2哈希-布谷鸟图。
     *
     * @param keyValueMap 编码键值对。
     * @return 2哈希-布谷鸟图。
     */
    H2CuckooTable<T> generateCuckooTable(Map<T, BigInteger> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        // 构造2哈希-布谷鸟图
        H2CuckooTable<T> h2CuckooTable = new H2CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            h2CuckooTable.addData(new int[]{h1Value, h2Value}, key);
        }
        return h2CuckooTable;
    }
}
