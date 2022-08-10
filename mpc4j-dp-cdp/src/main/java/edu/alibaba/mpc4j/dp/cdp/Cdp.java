package edu.alibaba.mpc4j.dp.cdp;

/**
 * CDP机制接口。
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
public interface Cdp {
    /**
     * 根据配置项初始化算法。
     *
     * @param cdpConfig 配置项。
     */
    void setup(CdpConfig cdpConfig);

    /**
     * 返回配置项。
     *
     * @return 配置项。
     */
    CdpConfig getCdpConfig();

    /**
     * 返回CDP机制所实现的ε。
     *
     * @return ε值。
     */
    double getEpsilon();

    /**
     * 返回差分隐私机制所实现的δ。
     *
     * @return δ值。
     */
    double getDelta();

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
    default String getMechanismName() {
        return "(ε = " + getEpsilon() + ", δ = " + getDelta() + ")-" + getClass().getSimpleName();
    }
}
