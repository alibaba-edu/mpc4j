package edu.alibaba.mpc4j.common.tool.okve.okvs;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTable;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 2哈希-两核乱码布谷鸟表。原始构造来自于：
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020.
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/09/05
 */
public class H2TcGctBinaryOkvs<T> extends AbstractBinaryOkvs<T> {
    /**
     * 2哈希-两核乱码布谷鸟表需要3个哈希函数：2个布谷鸟哈希的哈希函数，1个右侧哈希函数
     */
    static final int HASH_NUM = 3;
    /**
     * 2哈希-两核乱码布谷鸟表对应的ε = 0.4
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

    H2TcGctBinaryOkvs(EnvType envType, int n, int l, byte[][] keys, CuckooTableTcFinder<T> tcFinder) {
        super(envType, n, getLm(n) + getRm(n), l);
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
        // 这里不能验证storage每一行的长度，否则整体算法复杂度会变为O(n^2)
        assert storage.length == getM();
        // 不直接使用mapToRow映射，而是人工计算，这样效率更高
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int h1Value = h1.getInteger(keyBytes, lm);
        int h2Value = h2.getInteger(keyBytes, lm);
        byte[] rxBytes = hr.getBytes(keyBytes);
        boolean[] rxBinary = BinaryUtils.byteArrayToBinary(rxBytes);
        byte[] valueBytes = new byte[lByteLength];
        if (h1Value != h2Value) {
            BytesUtils.xori(valueBytes, storage[h1Value]);
            BytesUtils.xori(valueBytes, storage[h2Value]);
        } else {
            // 如果两个哈希结果相同，则只计算一次异或
            BytesUtils.xori(valueBytes, storage[h1Value]);
        }
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (rxBinary[rmIndex]) {
                BytesUtils.xori(valueBytes, storage[lm + rmIndex]);
            }
        }
        return valueBytes;
    }

    @Override
    public OkvsType getOkvsType() {
        if (tcFinder instanceof CuckooTableSingletonTcFinder) {
            return OkvsType.H2_SINGLETON_GCT;
        } else if (tcFinder instanceof H2CuckooTableTcFinder) {
            return OkvsType.H2_TWO_CORE_GCT;
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
        Set<Integer> coreVertexSet = coreDataSet.stream()
            .map(h2CuckooTable::getVertices)
            .flatMap(Arrays::stream)
            .collect(Collectors.toSet());
        // 生成矩阵，矩阵中包含右侧的全部解，以及2-core中的全部解
        byte[][] storage = generateStorage(keyValueMap, coreVertexSet, coreDataSet);
        // 将矩阵拆分为L || R
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
            int source = removedDataVertices[0];
            int target = removedDataVertices[1];
            boolean[] rx = dataHrMap.get(removedData);
            byte[] innerProduct = BytesUtils.innerProduct(rightStorage, lByteLength, rx);
            byte[] valueBytes = keyValueMap.get(removedData);
            BytesUtils.xori(innerProduct, valueBytes);
            if (source == target) {
                // 起点和终点一致，只设置一个即可
                if (leftStorage[source] == null) {
                    leftStorage[source] = innerProduct;
                } else {
                    // 顶点不为空，不可能出现这种情况
                    throw new IllegalStateException(removedData + "：(" + source + ", " + target + ")均不为空");
                }
            } else {
                // 起点和终点不一致，有4种情况
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
                    leftStorage[target] =innerProduct;
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
        System.arraycopy(leftStorage, 0, storage, 0, leftStorage.length);
        // 不应该再有没有更新的矩阵行了
        for (byte[] bytes : storage) {
            assert bytes != null;
        }

        return storage;
    }

    private byte[][] generateStorage(Map<T, byte[]> keyValueMap, Set<Integer> coreVertexSet, Set<T> coreDataSet) {
        // initialize variables L = {l_1, ..., l_m}, R = (r_1, ..., r_{χ + λ})
        byte[][] vectorX = new byte[m][];
        if (coreDataSet.size() > m) {
            throw new ArithmeticException("Back Edge数量 = " + coreDataSet.size() + "，超过了给定的最大值，无解");
        }
        // 2-core图不为空，求解线性方程组
        if (coreDataSet.size() != 0) {
            int size = coreDataSet.size();
            // 矩阵M有2-core边数量的行，2.4n + 1.4 * log(n) + λ列
            byte[][][] matrixM = new byte[size][m][];
            byte[][] vectorY = new byte[size][];
            int rowIndex = 0;
            for (T coreData : coreDataSet) {
                int h1Value = dataH1Map.get(coreData);
                int h2Value = dataH2Map.get(coreData);
                boolean[] lx = new boolean[lm];
                lx[h1Value] = true;
                lx[h2Value] = true;
                boolean[] rx = dataHrMap.get(coreData);
                for (int columnIndex = 0; columnIndex < lm; columnIndex++) {
                    matrixM[rowIndex][columnIndex] = lx[columnIndex] ? gf2e.createOne() : gf2e.createZero();
                }
                for (int columnIndex = 0; columnIndex < rm; columnIndex++) {
                    matrixM[rowIndex][lm + columnIndex] = rx[columnIndex] ? gf2e.createOne() : gf2e.createZero();
                }
                vectorY[rowIndex] = BytesUtils.clone(keyValueMap.get(coreData));
                rowIndex++;
            }
            SystemInfo systemInfo = gf2eLinearSolver.solve(matrixM, vectorY, vectorX, true);
            if (systemInfo.compareTo(SystemInfo.Inconsistent) == 0) {
                throw new ArithmeticException("无法完成编码过程，线性系统无解");
            }
        } else {
            // 不存在环路，所有vectorX均设置为0，注意这里不能设置为空
            Arrays.fill(vectorX, gf2e.createZero());
        }
        byte[][] matrix = new byte[m][];
        for (Integer vertex : coreVertexSet) {
            // 把2-core图的边所对应的矩阵设置好
            matrix[vertex] = BytesUtils.clone(vectorX[vertex]);
        }
        // 把R的矩阵设置好，R矩阵不可能为空
        System.arraycopy(vectorX, lm, matrix, lm, rm);
        return matrix;
    }

    /**
     * 给定待编码的键值对个数，计算左侧映射比特长度。
     *
     * @param n 待编码的键值对个数。
     * @return 左侧哈希比特长度，向上取整为Byte.SIZE的整数倍。
     */
    static int getLm(int n) {
        // 根据论文的表2， lm = (2 + ε) * n = 2.4n，向上取整到Byte.SIZE的整数倍
        return CommonUtils.getByteLength((int)Math.ceil((2 + EPSILON) * n)) * Byte.SIZE;
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
            (int)Math.ceil((1 + EPSILON) * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
    }

    /**
     * 生成2哈希-布谷鸟图。
     *
     * @param keyValueMap 键值对映射。
     * @return 2哈希-布谷鸟图。
     */
    H2CuckooTable<T> generateCuckooTable(Map<T, byte[]> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        // 构造2哈希-布谷鸟图
        H2CuckooTable<T> h2CuckooTable = new H2CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            h2CuckooTable.addData(new Integer[] {h1Value, h2Value}, key);
        }
        return h2CuckooTable;
    }

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }
}
