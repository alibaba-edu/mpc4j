package edu.alibaba.mpc4j.dp.ldp;

/**
 * LDP机制接口。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/20
 */
public interface Ldp {
    /**
     * 根据配置项初始化算法。
     *
     * @param ldpConfig 配置项。
     */
    void setup(LdpConfig ldpConfig);

    /**
     * 返回配置项。
     *
     * @return 配置项。
     */
    LdpConfig getLdpConfig();

    /**
     * 重置随机数生成器的种子。如果实现的是高安全性方案，则不支持重置随机数。
     *
     * @param seed 新的种子。
     * @throws UnsupportedOperationException 如果采样算法不支持重置随机数。
     */
    void reseed(long seed) throws UnsupportedOperationException;

    /**
     * 返回机制名称。
     *
     * @return 机制名称。
     */
    String getMechanismName();
}
