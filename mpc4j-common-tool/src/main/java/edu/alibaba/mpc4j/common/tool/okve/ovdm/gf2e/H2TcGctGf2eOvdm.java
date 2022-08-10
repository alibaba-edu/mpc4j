package edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eMaxLisFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTable;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 2哈希-两核GF(2^l)-OVDM实现。原始构造来自论文：
 * Rindal P, Schoppmann P. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. To appear in EUROCRYPT 2021.
 *
 * @author Weiran Liu
 * @date 2021/09/27
 */
class H2TcGctGf2eOvdm<T> extends AbstractGf2eOvdm<T> {
    /**
     * 2哈希-两核椭圆曲线OVDM所需的哈希函数密钥数量。
     */
    static int HASH_NUM = 3;
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
    private Map<T, Integer> dataH1Map;
    /**
     * 数据到h2的映射表
     */
    private Map<T, Integer> dataH2Map;
    /**
     * 数据到hr的映射表
     */
    private Map<T, boolean[]> dataHrMap;

    H2TcGctGf2eOvdm(EnvType envType, int l, int n, byte[][] keys, CuckooTableTcFinder<T> tcFinder) {
        super(envType, l, n, getLm(n) + getRm(n));
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
    public byte[] decode(byte[][] storage, T key) {
        assert storage.length == getM();
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int h1Value = h1.getInteger(keyBytes, lm);
        int h2Value = h2.getInteger(keyBytes, lm);
        boolean[] rxBinary = BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
        byte[] value = new byte[lByteLength];
        // 如果两个哈希结果相同，则只计算一次加法
        if (h1Value != h2Value) {
            BytesUtils.xori(value, storage[h1Value]);
            BytesUtils.xori(value, storage[h2Value]);
        } else {
            BytesUtils.xori(value, storage[h1Value]);
        }
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (rxBinary[rmIndex]) {
                BytesUtils.xori(value, storage[lm + rmIndex]);
            }
        }
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
        // 构造数据到哈希值的查找表
        Set<T> keySet = keyValueMap.keySet();
        dataH1Map = new HashMap<>(keySet.size());
        dataH2Map = new HashMap<>(keySet.size());
        dataHrMap = new HashMap<>(keySet.size());
        for (T key : keySet) {
            byte[] keyBytes = ObjectUtils.objectToByteArray(key);
            dataH1Map.put(key, h1.getInteger(keyBytes, lm));
            dataH2Map.put(key, h2.getInteger(keyBytes, lm));
            dataHrMap.put(key, BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes)));
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
        Stack<Integer[]> removedDataVerticesStack = tcFinder.getRemovedDataVertices();
        while (!removedDataStack.empty()) {
            T removedData = removedDataStack.pop();
            Integer[] removedDataVertices = removedDataVerticesStack.pop();
            Integer source = removedDataVertices[0];
            Integer target = removedDataVertices[1];
            boolean[] rx = dataHrMap.get(removedData);
            byte[] innerProduct = BytesUtils.innerProduct(rightStorage, lByteLength, rx);
            byte[] value = keyValueMap.get(removedData);
            BytesUtils.xori(innerProduct, value);
            if (source.equals(target)) {
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
                    leftStorage[source] = new byte[lByteLength];
                    secureRandom.nextBytes(leftStorage[source]);
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
                leftStorage[vertex] = new byte[lByteLength];
                secureRandom.nextBytes(leftStorage[vertex]);
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
            IntStream.range(lm, lm + rm).forEach(index -> {
                storage[index] = new byte[lByteLength];
                secureRandom.nextBytes(storage[index]);
            });
            return storage;
        }
        if (dTilde > rm) {
            throw new ArithmeticException("|d˜| = " + dTilde + "，d + λ + " + rm + "，线性系统无解");
        }
        // Let M˜' ∈ {0, 1}^{d˜ × (d + λ)} be the sub-matrix of M˜ obtained by taking the row indexed by R.
        byte[][][] tildePrimeMatrix = new byte[rm][dTilde][];
        int tildePrimeMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
                tildePrimeMatrix[rmIndex][tildePrimeMatrixRowIndex]
                    = rxBinary[rmIndex] ? gf2e.createOne() : gf2e.createZero();
            }
            tildePrimeMatrixRowIndex++;
        }
        // Otherwise let M˜* be one such matrix and C ⊂ [d + λ] index the corresponding columns of M˜.
        Gf2eMaxLisFinder maxLisFinder = new Gf2eMaxLisFinder(gf2e, tildePrimeMatrix);
        Set<Integer> setC = maxLisFinder.getLisRows();
        int size = setC.size();
        byte[][][] tildeStarMatrix = new byte[dTilde][size][];
        int tildeStarMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            int rmIndex = 0;
            for (Integer r : setC) {
                tildeStarMatrix[tildeStarMatrixRowIndex][rmIndex] = rxBinary[r] ? gf2e.createOne() : gf2e.createZero();
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
            storage[primeIndexC] = new byte[lByteLength];
            secureRandom.nextBytes(storage[primeIndexC]);
        }
        // For i ∈ R, define v'_i = v_i - (MP), where P_i is assigned to be zero if unassigned.
        byte[][] vectorY = new byte[dTilde][];
        int coreRowIndex = 0;
        for (T data : coreDataSet) {
            int h1Value = dataH1Map.get(data);
            int h2Value = dataH2Map.get(data);
            boolean[] rx = dataHrMap.get(data);
            byte[] mp = new byte[lByteLength];
            if (storage[h1Value] == null) {
                storage[h1Value] = new byte[lByteLength];
            }
            if (storage[h2Value] == null) {
                storage[h2Value] = new byte[lByteLength];
            }
            if (h1Value == h2Value) {
                BytesUtils.xori(mp, storage[h1Value]);
            } else {
                BytesUtils.xori(mp, storage[h1Value]);
                BytesUtils.xori(mp, storage[h2Value]);
            }
            for (int rxIndex = 0; rxIndex < rx.length; rxIndex++) {
                if (rx[rxIndex]) {
                    if (storage[lm + rxIndex] == null) {
                        storage[lm + rxIndex] = new byte[lByteLength];
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
        SystemInfo systemInfo = gf2eLinearSolver.solve(tildeStarMatrix, vectorY, vectorX, true);
        if (systemInfo.compareTo(SystemInfo.Inconsistent) == 0) {
            throw new ArithmeticException("无法完成编码过程，线性系统无解");
        }
        // 将求解结果更新到matrix里面
        int xVectorIndex = 0;
        for (int cIndex : setC) {
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
            h2CuckooTable.addData(new Integer[]{h1Value, h2Value}, key);
        }
        return h2CuckooTable;
    }
}
