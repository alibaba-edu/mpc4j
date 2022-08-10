package edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpMaxLisFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H3CuckooTable;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 3哈希-两核椭圆曲线OVDM实现。原始构造来自于下述论文的Section 4.1: OKVS based on a 3-Hash Garbled Cuckoo Table：
 * Garimella G, Pinkas B, Rosulek M, et al. Oblivious Key-Value Stores and Amplification for Private Set
 * Intersection. CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
 * <p>
 * 根据下述论文将其改成了OVDM版本。
 * Rindal P, Schoppmann P. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. To appear in EUROCRYPT 2021.
 *
 * @author Weiran Liu
 * @date 2021/09/10
 */
public class H3TcGctEccOvdm<T> extends AbstractEccOvdm<T> {
    /**
     * 3哈希-两核乱码布谷鸟表需要4个哈希函数：3个布谷鸟哈希的哈希函数，1个右侧哈希函数
     */
    static final int HASH_NUM = 4;
    /**
     * 3哈希-两核乱码布谷鸟表左侧编码放大系数
     */
    private static final double LEFT_EPSILON = 1.3;
    /**
     * 3哈希-两核乱码布谷鸟表右侧编码放大系数
     */
    private static final double RIGHT_EPSILON = 0.5;
    /**
     * 左侧编码比特长度，等于1.3 * n，向上取整为Byte.SIZE的整数倍
     */
    private final int lm;
    /**
     * 右侧编码比特长度，等于0.5 * log(n) + λ，向上取整为Byte.SIZE的整数倍
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
     * 布谷鸟哈希的第3个哈希函数
     */
    private final Prf h3;
    /**
     * 用于计算右侧r(x)的哈希函数
     */
    private final Prf hr;
    /**
     * 数据到h1的映射表
     */
    private Map<T, Integer> dataH1Map;
    /**
     * 数据到h2的映射表
     */
    private Map<T, Integer> dataH2Map;
    /**
     * 数据到h3的映射表
     */
    private Map<T, Integer> dataH3Map;
    /**
     * 数据到hr的映射表
     */
    private Map<T, boolean[]> dataHrMap;

    H3TcGctEccOvdm(EnvType envType, Ecc ecc, int n, byte[][] keys) {
        super(ecc, n, getLm(n) + getRm(n));
        lm = getLm(n);
        rm = getRm(n);
        h1 = PrfFactory.createInstance(envType, Integer.BYTES);
        h1.setKey(keys[0]);
        h2 = PrfFactory.createInstance(envType, Integer.BYTES);
        h2.setKey(keys[1]);
        h3 = PrfFactory.createInstance(envType, Integer.BYTES);
        h3.setKey(keys[2]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[3]);
    }

    /**
     * 返回不同的哈希值。
     *
     * @param message 消息。
     * @return 不同的哈希值。
     */
    private int[] hashDistinctValues(byte[] message) {
        int[] hValues = new int[3];
        hValues[0] = h1.getInteger(0, message, lm);
        // 得到与h1Value取值不同的h2Value
        int h2Index = 0;
        do {
            hValues[1] = h2.getInteger(h2Index, message, lm);
            h2Index++;
        } while (hValues[1] == hValues[0]);
        // 得到与h1Value和h2Value取值不同的h3Value
        int h3Index = 0;
        do {
            hValues[2] = h3.getInteger(h3Index, message, lm);
            h3Index++;
        } while (hValues[2] == hValues[0] || hValues[2] == hValues[1]);

        return hValues;
    }

    @Override
    public ECPoint decode(ECPoint[] storage, T key) {
        assert storage.length == getM();
        // 不直接使用mapToRow映射，而是人工计算，这样效率更高
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] hValues = hashDistinctValues(keyBytes);
        boolean[] rxBinary = BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
        ECPoint value = ecc.getInfinity();
        // 三个哈希结果一定不同，计算3次求和
        value = value.add(storage[hValues[0]]).add(storage[hValues[1]]).add(storage[hValues[2]]);
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (rxBinary[rmIndex]) {
                value = value.add(storage[lm + rmIndex]);
            }
        }
        return value.normalize();
    }

    @Override
    public EccOvdmFactory.EccOvdmType getEccOvdmType() {
        return EccOvdmFactory.EccOvdmType.H3_SINGLETON_GCT;
    }

    @Override
    public ECPoint[] encode(Map<T, ECPoint> keyValueMap) throws ArithmeticException {
        // 键值对的总数量小于等于n，之所以不写为等于n，是因为后续方案中两边的数量可能不相等。这里也不验证映射值的长度，提高性能。
        assert keyValueMap.size() <= n;
        // 构造数据到哈希值的查找表
        Set<T> keySet = keyValueMap.keySet();
        dataH1Map = new ConcurrentHashMap<>(keySet.size());
        dataH2Map = new ConcurrentHashMap<>(keySet.size());
        dataH3Map = new ConcurrentHashMap<>(keySet.size());
        dataHrMap = new ConcurrentHashMap<>(keySet.size());
        for (T key : keySet) {
            byte[] keyBytes = ObjectUtils.objectToByteArray(key);
            int[] hValues = hashDistinctValues(keyBytes);
            dataH1Map.put(key, hValues[0]);
            dataH2Map.put(key, hValues[1]);
            dataH3Map.put(key, hValues[2]);
            dataHrMap.put(key, BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes)));
        }
        // 生成3哈希-布谷鸟图
        H3CuckooTable<T> h3CuckooTable = generateCuckooTable(keyValueMap);
        // 找到2-core图
        CuckooTableSingletonTcFinder<T> singletonFinder = new CuckooTableSingletonTcFinder<>();
        singletonFinder.findTwoCore(h3CuckooTable);
        // 根据2-core图的所有数据和所有边构造矩阵
        Set<T> coreDataSet = singletonFinder.getRemainedDataSet();
        // 生成矩阵，矩阵中包含右侧的全部解，以及2-core中的全部解
        ECPoint[] storage = generateStorage(keyValueMap, coreDataSet);
        // 将矩阵拆分为L || R
        ECPoint[] leftStorage = new ECPoint[lm];
        ECPoint[] rightStorage = new ECPoint[rm];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // 从栈中依次弹出数据，为相应节点赋值
        Stack<T> removedDataStack = singletonFinder.getRemovedDataStack();
        Stack<Integer[]> removedDataVerticesStack = singletonFinder.getRemovedDataVertices();
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
            int vertex0 = removedDataVertices[0];
            int vertex1 = removedDataVertices[1];
            int vertex2 = removedDataVertices[2];
            ECPoint innerProduct = removedDataInnerProductMap.get(removedData);
            // 三个顶点一定不相同
            fullDistinctVertices(leftStorage, innerProduct, vertex0, vertex1, vertex2, removedData);
        }
        // 左侧矩阵补充随机数
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                leftStorage[vertex] = ecc.randomPoint(secureRandom);
            }
        }
        // 更新矩阵
        System.arraycopy(leftStorage, 0, storage, 0, leftStorage.length);
        // 不应该再有没有更新的矩阵行了
        for (ECPoint point : storage) {
            assert point != null;
        }

        return storage;
    }

    private void fullDistinctVertices(ECPoint[] leftMatrix, ECPoint innerProduct,
                                      int vertex0, int vertex1, int vertex2, T data) {
        if (leftMatrix[vertex0] == null && leftMatrix[vertex1] == null && leftMatrix[vertex2] == null) {
            // 0、1、2都为空
            leftMatrix[vertex0] = ecc.randomPoint(secureRandom);
            leftMatrix[vertex1] = ecc.randomPoint(secureRandom);
            leftMatrix[vertex2] = innerProduct.subtract(leftMatrix[vertex0]).subtract(leftMatrix[vertex1]);
        } else if (leftMatrix[vertex0] == null && leftMatrix[vertex1] == null) {
            // 0、1为空
            leftMatrix[vertex0] = ecc.randomPoint(secureRandom);
            leftMatrix[vertex1] = innerProduct.subtract(leftMatrix[vertex0]).subtract(leftMatrix[vertex2]);
        } else if (leftMatrix[vertex0] == null && leftMatrix[vertex2] == null) {
            // 0、2为空
            leftMatrix[vertex0] = ecc.randomPoint(secureRandom);
            leftMatrix[vertex2] = innerProduct.subtract(leftMatrix[vertex0]).subtract(leftMatrix[vertex1]);
        } else if (leftMatrix[vertex1] == null && leftMatrix[vertex2] == null) {
            // 1、2为空
            leftMatrix[vertex1] = ecc.randomPoint(secureRandom);
            leftMatrix[vertex2] = innerProduct.subtract(leftMatrix[vertex0]).subtract(leftMatrix[vertex1]);
        } else if (leftMatrix[vertex0] == null) {
            // 0为空
            leftMatrix[vertex0] = innerProduct.subtract(leftMatrix[vertex1]).subtract(leftMatrix[vertex2]);
        } else if (leftMatrix[vertex1] == null) {
            // 1为空
            leftMatrix[vertex1] = innerProduct.subtract(leftMatrix[vertex0]).subtract(leftMatrix[vertex2]);
        } else if (leftMatrix[vertex2] == null) {
            // 2为空
            leftMatrix[vertex2] = innerProduct.subtract(leftMatrix[vertex0]).subtract(leftMatrix[vertex1]);
        } else {
            // 三个都不为空，不可能出现这种情况
            throw new IllegalStateException(data + "的顶点(" + vertex0 + ", " + vertex1 + ", " + vertex2 + ")均不为空");
        }
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
            throw new ArithmeticException("|d˜| = " + dTilde + "，d + λ = " + rm + "，线性系统无解");
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
            setPrimeC.add(dataH3Map.get(data));
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
            int h3Value = dataH3Map.get(data);
            boolean[] rx = dataHrMap.get(data);
            ECPoint mp = ecc.getInfinity();
            if (storage[h1Value] == null) {
                storage[h1Value] = ecc.getInfinity();
            }
            if (storage[h2Value] == null) {
                storage[h2Value] = ecc.getInfinity();
            }
            if (storage[h3Value] == null) {
                storage[h3Value] = ecc.getInfinity();
            }
            // 3个哈希函数一定互不相同
            mp = mp.add(storage[h1Value]).add(storage[h2Value]).add(storage[h3Value]);
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
        // 根据论文第5.4节，lm = 1.3 * n，向上取整到Byte.SIZE的整数倍
        return CommonUtils.getByteLength((int) Math.ceil(LEFT_EPSILON * n)) * Byte.SIZE;
    }

    /**
     * 给定待编码的键值对个数，计算右侧映射比特长度。
     *
     * @param n 待编码的键值对个数。
     * @return 右侧映射比特长度。向上取整为Byte.SIZE的整数倍。
     */
    static int getRm(int n) {
        // 根据论文第5.4节，r = 0.5 * log(n) + λ，向上取整到Byte.SIZE的整数倍
        // 但当n比较小时，哈希碰撞概率较高，会导致2-core图对应边数量很多
        // 测试结果为：2^8: 186，2^9: 328, 2^10: 561, 2^11: 907，当达到2^12时，骤降为4
        int r = CommonUtils.getByteLength(
            (int) Math.ceil(RIGHT_EPSILON * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
        if (n <= 1 << 8) {
            // 小于2^8，取0.5 * log(n) + λ和n的最大值
            return CommonUtils.getByteLength(Math.max(r, n)) * Byte.SIZE;
        } else if (n <= 1 << 9) {
            // 256 < n <= 512
            return CommonUtils.getByteLength(Math.min(n, 328)) * Byte.SIZE;
        } else if (n <= 1 << 10) {
            // 512 < n <= 1024
            return CommonUtils.getByteLength(561) * Byte.SIZE;
        } else if (n <= 1 << 11) {
            // 1024 < n <= 2048
            return CommonUtils.getByteLength(907) * Byte.SIZE;
        } else {
            // n > 2048
            return r;
        }
    }

    /**
     * 生成3哈希-布谷鸟图。
     *
     * @param keyValueMap 键值对映射。
     * @return 3哈希-布谷鸟图。
     */
    H3CuckooTable<T> generateCuckooTable(Map<T, ECPoint> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        // 构造3哈希-布谷鸟图
        H3CuckooTable<T> h3CuckooTable = new H3CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            int h3Value = dataH3Map.get(key);
            h3CuckooTable.addData(new Integer[]{h1Value, h2Value, h3Value}, key);
        }
        return h3CuckooTable;
    }

    @Override
    public int getNegLogFailureProbability() {
        // 根据论文第5.4节，r = 0.5 * log(n) + λ，失败概率为2^(-29.355)
        return 29;
    }
}
