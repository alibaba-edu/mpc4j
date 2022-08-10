package edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpMaxLisFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTable;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 2哈希-两核椭圆曲线OVDM实现。原始构造来自论文：
 * Rindal P, Schoppmann P. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. To appear in EUROCRYPT 2021.
 *
 * @author Weiran Liu
 * @date 2021/09/09
 */
class H2TcGctEccOvdm<T> extends AbstractEccOvdm<T> {
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

    H2TcGctEccOvdm(EnvType envType, Ecc ecc, int n, byte[][] keys, CuckooTableTcFinder<T> tcFinder) {
        super(ecc, n, getLm(n) + getRm(n));
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
    public ECPoint decode(ECPoint[] storage, T key) {
        assert storage.length == getM();
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int h1Value = h1.getInteger(keyBytes, lm);
        int h2Value = h2.getInteger(keyBytes, lm);
        boolean[] rxBinary = BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
        ECPoint value = ecc.getInfinity();
        // 如果两个哈希结果相同，则只计算一次加法
        if (h1Value != h2Value) {
            value = value.add(storage[h1Value]).add(storage[h2Value]);
        } else {
            value = value.add(storage[h1Value]);
        }
        // 计算内积
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (rxBinary[rmIndex]) {
                value = value.add(storage[lm + rmIndex]);
            }
        }
        return value.normalize();
    }

    @Override
    public EccOvdmFactory.EccOvdmType getEccOvdmType() {
        if (tcFinder instanceof CuckooTableSingletonTcFinder) {
            return EccOvdmFactory.EccOvdmType.H2_SINGLETON_GCT;
        } else if (tcFinder instanceof H2CuckooTableTcFinder) {
            return EccOvdmFactory.EccOvdmType.H2_TWO_CORE_GCT;
        } else {
            throw new IllegalStateException("Invalid TcFinder:" + tcFinder.getClass().getSimpleName());
        }
    }

    @Override
    public ECPoint[] encode(Map<T, ECPoint> keyValueMap) throws ArithmeticException {
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
        ECPoint[] storage = generateStorage(keyValueMap, coreDataSet);
        // 将矩阵拆分为L || D
        ECPoint[] leftStorage = new ECPoint[lm];
        ECPoint[] rightStorage = new ECPoint[rm];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // 从栈中依次弹出数据，为相应节点赋值
        Stack<T> removedDataStack = tcFinder.getRemovedDataStack();
        Stack<Integer[]> removedDataVerticesStack = tcFinder.getRemovedDataVertices();
        // 先计算右侧内积结果
        Map<T, ECPoint> removedDataInnerProductMap = removedDataStack.stream()
            .collect(Collectors.toMap(Function.identity(), removedData -> {
                boolean[] rx = dataHrMap.get(removedData);
                ECPoint rightInnerProduct = ecc.innerProduct(rx, rightStorage);
                ECPoint value = keyValueMap.get(removedData);
                return value.subtract(rightInnerProduct);
            }));
        while (!removedDataStack.empty()) {
            T removedData = removedDataStack.pop();
            Integer[] removedDataVertices = removedDataVerticesStack.pop();
            Integer source = removedDataVertices[0];
            Integer target = removedDataVertices[1];
            ECPoint innerProduct = removedDataInnerProductMap.get(removedData);
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
                    leftStorage[source] = ecc.randomPoint(secureRandom);
                    leftStorage[target] = innerProduct.subtract(leftStorage[source]);
                } else if (leftStorage[source] == null) {
                    // 情况2：左端点为空，右端点不为空
                    leftStorage[source] = innerProduct.subtract(leftStorage[target]);
                } else if (leftStorage[target] == null) {
                    // 情况3：左端点不为空，右端点为空
                    leftStorage[target] = innerProduct.subtract(leftStorage[source]);
                } else {
                    // 左右端点都不为空，实现存在问题
                    throw new IllegalStateException(removedData + "左右顶点同时有数据，算法实现有误");
                }
            }
        }
        // 左侧矩阵补充随机数
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                leftStorage[vertex] = ecc.randomPoint(secureRandom);
            }
        }
        // 更新矩阵
        System.arraycopy(leftStorage, 0, storage, 0, lm);
        // 不应该再有没有更新的矩阵行了
        for (ECPoint point : storage) {
            assert point != null;
        }
        return storage;
    }

    private ECPoint[] generateStorage(Map<T, ECPoint> keyValueMap, Set<T> coreDataSet) {
        // 初始化OVDM存储器，所有位置设置为空
        ECPoint[] storage = new ECPoint[m];
        // Let d˜ = |R| and abort if d˜ > d + λ
        int dTilde = coreDataSet.size();
        // 如果没有2-core边，则补充的边都设置为随机数
        if (dTilde == 0) {
            for (int rowIndex = lm; rowIndex < lm + rm; rowIndex++) {
                storage[rowIndex] = ecc.randomPoint(secureRandom);
            }
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
        // Otherwise let M˜* be one such matrix and C ⊂ [d + λ] index the corresponding columns of M˜.
        ZpMaxLisFinder zpMaxLisFinder = new ZpMaxLisFinder(ecc.getN(), tildePrimeMatrix);
        Set<Integer> setC = zpMaxLisFinder.getLisRows();
        BigInteger[][] tildeStarMatrix = new BigInteger[dTilde][setC.size()];
        int tildeStarMatrixRowIndex = 0;
        for (T data : coreDataSet) {
            boolean[] rxBinary = dataHrMap.get(data);
            int rmIndex = 0;
            for (Integer r : setC) {
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
            storage[primeIndexC] = ecc.randomPoint(secureRandom);
        }
        // For i ∈ R, define v'_i = v_i - (MP), where P_i is assigned to be zero if unassigned.
        ECPoint[] vectorY = new ECPoint[dTilde];
        int coreRowIndex = 0;
        for (T data : coreDataSet) {
            int h1Value = dataH1Map.get(data);
            int h2Value = dataH2Map.get(data);
            boolean[] rx = dataHrMap.get(data);
            ECPoint mp = ecc.getInfinity();
            if (storage[h1Value] == null) {
                storage[h1Value] = ecc.getInfinity();
            }
            if (storage[h2Value] == null) {
                storage[h2Value] = ecc.getInfinity();
            }
            if (h1Value == h2Value) {
                mp = mp.add(storage[h1Value]);
            } else {
                mp = mp.add(storage[h1Value]).add(storage[h2Value]);
            }
            for (int rxIndex = 0; rxIndex < rx.length; rxIndex++) {
                if (rx[rxIndex]) {
                    if (storage[lm + rxIndex] == null) {
                        storage[lm + rxIndex] = ecc.getInfinity();
                    }
                    mp = mp.add(storage[lm + rxIndex]);
                }
            }
            vectorY[coreRowIndex] = keyValueMap.get(data).subtract(mp);
            coreRowIndex++;
        }
        // Using Gaussian elimination solve the system
        // M˜* (P_{m' + C_1}, ..., P_{m' + C_{d˜})^T = (v'_{R_1}, ..., v'_{R_{d˜})^T.
        ECPoint[] vectorX = new ECPoint[setC.size()];
        SystemInfo systemInfo = eccLinearSolver.solve(tildeStarMatrix, vectorY, vectorX, true);
        if (systemInfo.compareTo(SystemInfo.Inconsistent) == 0) {
            throw new ArithmeticException("无法完成编码过程，线性系统无解");
        }
        // 将求解结果更新到matrix里面
        int xVectorIndex = 0;
        for (int cIndex : setC) {
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
    H2CuckooTable<T> generateCuckooTable(Map<T, ECPoint> keyValueMap) {
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

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }
}
