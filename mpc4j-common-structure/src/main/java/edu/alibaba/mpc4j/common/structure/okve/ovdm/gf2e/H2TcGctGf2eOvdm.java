package edu.alibaba.mpc4j.common.structure.okve.ovdm.gf2e;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTable;
import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 2哈希-两核GF(2^l)-OVDM实现。原始构造来自论文：
 * Rindal P, Schoppmann P. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. To appear in EUROCRYPT 2021.
 *
 * @author Weiran Liu
 * @date 2021/09/27
 */
class H2TcGctGf2eOvdm<T> extends AbstractGf2eOvdm<T> implements SparseGf2eOvdm<T> {
    /**
     * 2 sparse hashes
     */
    private static final int SPARSE_HASH_NUM = 2;
    /**
     * 2哈希-两核椭圆曲线OVDM所需的哈希函数密钥数量。
     */
    static int HASH_NUM = SPARSE_HASH_NUM + 1;
    /**
     * 2哈希-两核椭圆曲线OVDM所对应的ε
     */
    private static final double EPSILON = 0.4;
    /**
     * 左侧编码比特长度，等于(2 + ε) * n，向上取整为Byte.SIZE的整数倍
     */
    private final int lm;
    /**
     * 右侧编码比特长度，等于(1 + ε) * log(n) + λ，向上取整为Byte.SIZE的整数倍
     */
    private final int rm;
    /**
     * 布谷鸟哈希的第1个哈希函数
     */
    private final Prf h1;
    /**
     * 布谷鸟哈希的第2个哈希函数
     */
    private final Prf h2;
    /**
     * 用于计算右侧r(x)的哈希函数
     */
    private final Prf hr;
    /**
     * 2-core图查找器
     */
    private final CuckooTableTcFinder<T> tcFinder;
    /**
     * 数据到h1的映射表
     */
    private TObjectIntMap<T> dataH1Map;
    /**
     * 数据到h2的映射表
     */
    private TObjectIntMap<T> dataH2Map;
    /**
     * 数据到hr的映射表
     */
    private Map<T, boolean[]> dataHrMap;

    H2TcGctGf2eOvdm(EnvType envType, int l, int n, byte[][] keys, CuckooTableTcFinder<T> tcFinder) {
        super(l, n, getLm(n) + getRm(n));
        assert (tcFinder instanceof CuckooTableSingletonTcFinder || tcFinder instanceof H2CuckooTableTcFinder);
        lm = getLm(n);
        rm = getRm(n);
        h1 = PrfFactory.createInstance(envType, Integer.BYTES);
        h1.setKey(keys[0]);
        h2 = PrfFactory.createInstance(envType, Integer.BYTES);
        h2.setKey(keys[1]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[2]);
        this.tcFinder = tcFinder;
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
    public byte[] decode(byte[][] storage, T key) {
        assert storage.length == getM();
        int[] sparsePositions = sparsePositions(key);
        boolean[] densePositions = densePositions(key);
        byte[] value = new byte[byteL];
        // h1 and h2 must be distinct
        BytesUtils.xori(value, storage[sparsePositions[0]]);
        BytesUtils.xori(value, storage[sparsePositions[1]]);
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (densePositions[rmIndex]) {
                BytesUtils.xori(value, storage[lm + rmIndex]);
            }
        }
        assert BytesUtils.isFixedReduceByteArray(value, byteL, l);
        return value;
    }

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }

    @Override
    public Gf2eOvdmFactory.Gf2eOvdmType getGf2xOvdmType() {
        if (tcFinder instanceof CuckooTableSingletonTcFinder) {
            return Gf2eOvdmFactory.Gf2eOvdmType.H2_SINGLETON_GCT;
        } else if (tcFinder instanceof H2CuckooTableTcFinder) {
            return Gf2eOvdmFactory.Gf2eOvdmType.H2_TWO_CORE_GCT;
        } else {
            throw new IllegalStateException("Invalid TcFinder:" + tcFinder.getClass().getSimpleName());
        }
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap) throws ArithmeticException {
        assert keyValueMap.size() <= n;
        keyValueMap.values().forEach(x -> {
            assert BytesUtils.isFixedReduceByteArray(x, byteL, l);
        });
        // 构造数据到哈希值的查找表
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
        // 生成2哈希-布谷鸟图
        H2CuckooTable<T> h2CuckooTable = generateCuckooTable(keyValueMap);
        // 找到2-core图
        tcFinder.findTwoCore(h2CuckooTable);
        // 根据2-core图的所有数据和所有边构造矩阵
        Set<T> coreDataSet = tcFinder.getRemainedDataSet();
        // 生成矩阵，矩阵中包含右侧的全部解，以及2-core中的全部解
        byte[][] storage = generateStorage(keyValueMap, coreDataSet);
        // 将矩阵拆分为L || D
        byte[][] leftStorage = new byte[lm][];
        byte[][] rightStorage = new byte[rm][];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // 从栈中依次弹出数据，为相应节点赋值
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
            if (source == target) {
                // 起点和终点一致，只设置一个即可
                if (leftStorage[source] == null) {
                    leftStorage[source] = innerProduct;
                } else {
                    // 顶点不为空，不可能出现这种情况
                    throw new IllegalStateException(removedData + "：(" + source + ", " + target + ")均不为空");
                }
            } else {
                // 起点和重点不一致，有4种情况
                if (leftStorage[source] == null && leftStorage[target] == null) {
                    // 情况1：左右都为空
                    leftStorage[source] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                    BytesUtils.xori(innerProduct, leftStorage[source]);
                    leftStorage[target] = innerProduct;
                } else if (leftStorage[source] == null) {
                    // 情况2：左端点为空，右端点不为空
                    BytesUtils.xori(innerProduct, leftStorage[target]);
                    leftStorage[source] = innerProduct;
                } else if (leftStorage[target] == null) {
                    // 情况3：左端点不为空，右端点为空
                    BytesUtils.xori(innerProduct, leftStorage[source]);
                    leftStorage[target] = innerProduct;
                } else {
                    // 左右端点都不为空，实现存在问题
                    throw new IllegalStateException(removedData + "左右顶点同时有数据，算法实现有误");
                }
            }
        }
        // 左侧矩阵补充随机数
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                leftStorage[vertex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
        // 更新矩阵
        System.arraycopy(leftStorage, 0, storage, 0, lm);
        // 不应该再有没有更新的矩阵行了
        for (byte[] bytes : storage) {
            assert bytes != null;
        }

        return storage;
    }

    private byte[][] generateStorage(Map<T, byte[]> keyValueMap, Set<T> coreDataSet) {
        // 初始化OVDM存储器，所有位置设置为空
        byte[][] storage = new byte[m][];
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        // 如果没有2-core边，则补充的边都设置为随机数
        if (dTilde == 0) {
            IntStream.range(lm, lm + rm).forEach(index ->
                storage[index] = BytesUtils.randomByteArray(byteL, l, secureRandom)
            );
            return storage;
        }
        if (dTilde > rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + "，d + λ + " + rm + "，线性系统无解");
        }
        // Let M˜' ∈ {0, 1}^{d˜ × (d + λ)} be the sub-matrix of M˜ obtained by taking the row indexed by R.
        int dByteTilde = CommonUtils.getByteLength(dTilde);
        int dOffsetTilde = dByteTilde * Byte.SIZE - dTilde;
        byte[][] tildePrimeMatrix = new byte[rm][dByteTilde];
        int tildePrimeMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                BinaryUtils.setBoolean(tildePrimeMatrix[rmIndex], dOffsetTilde + tildePrimeMatrixRowIndex, rxBinary[rmIndex]);
            }
            tildePrimeMatrixRowIndex++;
        }
        // let M˜* be the sub-matrix of M˜ containing an invertible d˜ × d˜ matrix,
        // and C ⊂ [d + λ] index the corresponding columns of M˜.
        TIntSet setC = maxLisFinder.getLisRows(tildePrimeMatrix, dTilde);
        int[] cArray = setC.toArray();
        int size = setC.size();
        int byteSize = CommonUtils.getByteLength(size);
        int offsetSize = byteSize * Byte.SIZE - size;
        byte[][] tildeStarMatrix = new byte[dTilde][byteSize];
        int tildeStarMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            int rmIndex = 0;
            for (int r : cArray) {
                BinaryUtils.setBoolean(tildeStarMatrix[tildeStarMatrixRowIndex], offsetSize + rmIndex, rxBinary[r]);
                rmIndex++;
            }
            tildeStarMatrixRowIndex++;
        }
        // Let C' = {j | i \in R, M'_{i, j} = 1} ∪ ([d + λ] \ C + m')
        TIntSet setPrimeC = new TIntHashSet(dTilde * 2 + rm / 2);
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
        for (int primeIndexC : setPrimeC.toArray()) {
            storage[primeIndexC] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        // For i ∈ R, define v'_i = v_i - (MP), where P_i is assigned to be zero if unassigned.
        byte[][] vectorY = new byte[dTilde][];
        int coreRowIndex = 0;
        for (T data : coreDataSet) {
            int h1Value = dataH1Map.get(data);
            int h2Value = dataH2Map.get(data);
            boolean[] rx = dataHrMap.get(data);
            byte[] mp = new byte[byteL];
            if (storage[h1Value] == null) {
                storage[h1Value] = new byte[byteL];
            }
            if (storage[h2Value] == null) {
                storage[h2Value] = new byte[byteL];
            }
            // h1 and h2 must be distinct
            BytesUtils.xori(mp, storage[h1Value]);
            BytesUtils.xori(mp, storage[h2Value]);
            for (int rxIndex = 0; rxIndex < rx.length; rxIndex++) {
                if (rx[rxIndex]) {
                    if (storage[lm + rxIndex] == null) {
                        storage[lm + rxIndex] = new byte[byteL];
                    }
                    BytesUtils.xori(mp, storage[lm + rxIndex]);
                }
            }
            vectorY[coreRowIndex] = BytesUtils.xor(keyValueMap.get(data), mp);
            coreRowIndex++;
        }
        // Using Gaussian elimination solve the system
        // M˜* (P_{m' + C_1}, ..., P_{m' + C_{d˜})^T = (v'_{R_1}, ..., v'_{R_{d˜})^T.
        byte[][] vectorX = new byte[size][];
        SystemInfo systemInfo = linearSolver.freeSolve(tildeStarMatrix, size, vectorY, vectorX);
        if (systemInfo.compareTo(SystemInfo.Inconsistent) == 0) {
            throw new ArithmeticException("无法完成编码过程，线性系统无解");
        }
        // 将求解结果更新到matrix里面
        int xVectorIndex = 0;
        for (int cIndex : cArray) {
            storage[lm + cIndex] = BytesUtils.clone(vectorX[xVectorIndex]);
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
    private H2CuckooTable<T> generateCuckooTable(Map<T, byte[]> keyValueMap) {
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
