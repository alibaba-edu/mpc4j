package edu.alibaba.mpc4j.common.tool.okve.okvs;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTable;
import edu.alibaba.mpc4j.common.tool.okve.cuckootable.H2CuckooTableDfsDealer;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 基于深度优先遍历（Depth-First Search，DFS）的不经意键值对存储器实现。原始方案来自下述论文第30页附录C：
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020.
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/09/09
 */
class H2DfsGctBinaryOkvs<T> extends AbstractBinaryOkvs<T> {
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

    H2DfsGctBinaryOkvs(EnvType envType, int n, int l, byte[][] keys) {
        super(envType, n, getLm(n) + getRm(n), l);
        lm = getLm(n);
        rm = getRm(n);
        h1 = PrfFactory.createInstance(envType, Integer.BYTES);
        h1.setKey(keys[0]);
        h2 = PrfFactory.createInstance(envType, Integer.BYTES);
        h2.setKey(keys[1]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[2]);
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // 这里不能验证storage每一行的长度，否则整体算法复杂度会变为O(n^2)
        assert storage.length == getM();
        // 解码不使用mapToRow，而是人工计算
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int h1Value = h1.getInteger(keyBytes, lm);
        int h2Value = h2.getInteger(keyBytes, lm);
        boolean[] rxBinary = BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
        byte[] value = new byte[lByteLength];
        if (h1Value != h2Value) {
            // 如果hash1 != hash2，则正常运算，如果相等，则两个都为0，不用xor
            BytesUtils.xori(value, storage[h1Value]);
            BytesUtils.xori(value, storage[h2Value]);
        }
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (rxBinary[rmIndex]) {
                BytesUtils.xori(value, storage[lm + rmIndex]);
            }
        }

        return value;
    }

    @Override
    public OkvsType getOkvsType() {
        return OkvsType.H2_DFS_GCT;
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
        // 生成2哈希-布谷鸟表
        H2CuckooTable<T> h2CuckooTable = generateCuckooTable(keyValueMap);
        // 进行深度优先搜索，找到back edge、roots等
        H2CuckooTableDfsDealer<T> dfsDealer = new H2CuckooTableDfsDealer<>();
        dfsDealer.findCycle(h2CuckooTable);
        // 生成矩阵右半部分
        byte[][] rightMatrix = generateRightMatrix(keyValueMap, dfsDealer.getCycleEdgeDataSet());
        // 所有环路都已经处理完毕，生成矩阵左半部分
        byte[][] leftMatrix = new byte[lm][];
        Map<Integer, ArrayList<T>> rootEdgeMap = dfsDealer.getRootTraversalDataMap();
        Set<Integer> rootSet = rootEdgeMap.keySet();
        for (Integer root : rootSet) {
            if (leftMatrix[root] != null) {
                throw new IllegalStateException("重新调用DFS更新节点值时，根节点不可能已被设置，算法实现有误");
            }
            // set variable lv arbitrarily, where v is the root vertex of the traversal.
            leftMatrix[root] = new byte[this.lByteLength];
            secureRandom.nextBytes(leftMatrix[root]);
            ArrayList<T> traversalEdgeDataList = rootEdgeMap.get(root);
            for (T traversalEdgeData : traversalEdgeDataList) {
                // set lv = lu XOR <r(x_i), R> XOR y_i
                boolean[] rx = dataHrMap.get(traversalEdgeData);
                Integer[] vertices = h2CuckooTable.getVertices(traversalEdgeData);
                Integer source = vertices[0];
                Integer target = vertices[1];
                byte[] innerProduct = BytesUtils.innerProduct(rightMatrix, lByteLength, rx);
                byte[] valueBytes = keyValueMap.get(traversalEdgeData);
                BytesUtils.xori(innerProduct, valueBytes);
                // 这里不用考虑自环问题，因为自环一定是back edge
                if (leftMatrix[source] == null && leftMatrix[target] != null) {
                    // 如果左侧为空，右侧不为空，则计算左侧节点
                    leftMatrix[source] = BytesUtils.xor(leftMatrix[target], innerProduct);
                } else if (leftMatrix[target] == null && leftMatrix[source] != null) {
                    // 如果右侧为空，左侧不为空，则计算右侧节点
                    leftMatrix[target] = BytesUtils.xor(leftMatrix[source], innerProduct);
                } else {
                    throw new IllegalStateException(traversalEdgeData + "的左右两边同时为null或同时有数据，算法实现有误");
                }
            }
        }
        // 左侧矩阵补充随机数
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftMatrix[vertex] == null) {
                leftMatrix[vertex] = new byte[lByteLength];
                secureRandom.nextBytes(leftMatrix[vertex]);
            }
        }
        byte[][] matrix = new byte[lm + rm][];
        System.arraycopy(leftMatrix, 0, matrix, 0, lm);
        System.arraycopy(rightMatrix, 0, matrix, lm, rm);

        return matrix;
    }

    private byte[][] generateRightMatrix(Map<T, byte[]> keyValueMap, Set<T> cycleEdgeDataSet) {
        // initialize variables R = (r_1, ..., r_{χ + λ})
        byte[][] vectorByteX = new byte[rm][];
        if (cycleEdgeDataSet.size() != 0) {
            int size = cycleEdgeDataSet.size();
            // 存在环路，这里有个非常大的坑，求解线性方程组时要把环路中涉及到的所有边都包含进来，而不止包含back edge
            // 所以需要实现Cuckoo Graph中找涉及到back edge的环路边问题，本质上把环路边找到就是找了一个2-core
            // for each non-tree edge, solve the linear system
            byte[][][] matrixByteM = new byte[size][rm][];
            byte[][] vectorByteY = new byte[size][];
            int rowIndex = 0;
            for (T cycleEdgeData : cycleEdgeDataSet) {
                boolean[] rx = dataHrMap.get(cycleEdgeData);
                matrixByteM[rowIndex] = IntStream.range(0, rm)
                    .mapToObj(rmIndex -> rx[rmIndex] ? gf2e.createOne() : gf2e.createZero())
                    .toArray(byte[][]::new);
                vectorByteY[rowIndex] = BytesUtils.clone(keyValueMap.get(cycleEdgeData));
                rowIndex++;
            }
            SystemInfo systemInfo = gf2eLinearSolver.solve(matrixByteM, vectorByteY, vectorByteX, true);
            if (systemInfo.compareTo(SystemInfo.Inconsistent) == 0) {
                throw new ArithmeticException("ERROR: cannot encode key-value map, Linear System unsolved");
            }
        } else {
            // 不存在环路，所有vectorX均设置为0
            Arrays.fill(vectorByteX, gf2e.createZero());
        }
        // 求解结果不可能为空
        return vectorByteX;
    }

    /**
     * 生成2哈希-布谷鸟表。
     *
     * @param keyValueMap 键值对。
     * @return 2哈希-布谷鸟表。
     */
    H2CuckooTable<T> generateCuckooTable(Map<T, byte[]> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        H2CuckooTable<T> h2CuckooTable = new H2CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            h2CuckooTable.addData(new Integer[] {h1Value, h2Value}, key);
        }
        return h2CuckooTable;
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

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }
}
