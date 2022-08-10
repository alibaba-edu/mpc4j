package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 乱码布隆过滤器（Garbled Bloom Filter, GBF）的OKVS方案。原始论文：
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol. CCS 2013.
 * ACM, 2013 pp. 789-800.
 *
 * @author Weiran Liu
 * @date 2021/09/06
 */
public class GbfBinaryOkvs<T> extends AbstractBinaryOkvs<T> {
    /**
     * 乱码布隆过滤器需要40个哈希函数
     */
    static int HASH_NUM = CommonConstants.STATS_BIT_LENGTH;
    /**
     * 乱码布隆过滤器哈希函数
     */
    private final Prf[] prfs;

    public GbfBinaryOkvs(EnvType envType, int n, int l, byte[][] keys) {
        super(envType, n, getM(n), l);
        assert keys.length == HASH_NUM;
        prfs = IntStream.range(0, HASH_NUM)
            .mapToObj(hashIndex -> {
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(keys[hashIndex]);
                return hash;
            })
            .toArray(Prf[]::new);
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap) throws ArithmeticException {
        // 键值对的总数量小于等于n，之所以不写为等于n，是因为后续PSI方案中两边的数量可能不相等。不验证映射值的长度，提高性能。
        assert keyValueMap.size() <= n;
        Set<T> keySet = keyValueMap.keySet();
        // 依次计算每个键值的映射结果，并根据计算结果将真实值秘密分享给存储器
        byte[][] storage = new byte[m][];
        for (T key : keySet) {
            byte[] finalShare = BytesUtils.clone(keyValueMap.get(key));
            assert finalShare.length == lByteLength;
            byte[] keyBytes = ObjectUtils.objectToByteArray(key);
            int[] positions = Arrays.stream(prfs)
                .mapToInt(prf -> prf.getInteger(keyBytes, m))
                .distinct()
                .toArray();
            int emptySlot = -1;
            for (int position : positions) {
                if (storage[position] == null && emptySlot == -1) {
                    // 如果找到了一个空位置，就锁定此空位置（reserve the location for finalShare）
                    emptySlot = position;
                } else if (storage[position] == null) {
                    // 如果当前位置为空，则随机选择一个分享值（generate a new share）
                    storage[position] = new byte[lByteLength];
                    secureRandom.nextBytes(storage[position]);
                    BytesUtils.xori(finalShare, storage[position]);
                } else {
                    // 如果当前位置已经有分享值，则更新最终分享值（reuse a share）
                    BytesUtils.xori(finalShare, storage[position]);
                }
            }
            if (emptySlot == -1) {
                // 如果迭代一轮后，没有找到空位置，则抛出异常，此种情况发生的概率为1 - 2^{-λ}
                throw new ArithmeticException("Failed to encode Key-Value Map, cannot find empty slot");
            }
            storage[emptySlot] = finalShare;
        }
        // 把剩余的空位置都填充上随机元素
        for (int i = 0; i < m; i++) {
            if (storage[i] == null) {
                storage[i] = new byte[lByteLength];
                secureRandom.nextBytes(storage[i]);
            }
        }

        return storage;
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // 这里不能验证storage每一行的长度，否则整体算法复杂度会变为O(n^2)
        assert storage.length == getM();
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        // 先计算所有可能的位置，位置可能有重复，因此要去重
        int[] positions = Arrays.stream(prfs)
            .mapToInt(prf -> prf.getInteger(keyBytes, m))
            .distinct()
            .toArray();
        // 计算输出值
        byte[] value = new byte[lByteLength];
        for (int position : positions) {
            BytesUtils.xori(value, storage[position]);
        }
        return value;
    }

    @Override
    public OkvsType getOkvsType() {
        return OkvsType.GBF;
    }

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }

    /**
     * 给定待编码的键值对个数，计算映射比特长度。
     *
     * @param n 插入的key-value对个数。
     * @return 最优的m。
     */
    static int getM(int n) {
        assert n > 0;
        // m = n / ln(2) * 统计安全常数，向上取整到Byte.SIZE的整数倍
        return CommonUtils.getByteLength((int)Math.ceil(n * CommonConstants.STATS_BIT_LENGTH / Math.log(2)))
            * Byte.SIZE;
    }
}
