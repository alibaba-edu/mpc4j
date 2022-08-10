package edu.alibaba.mpc4j.dp.ldp.nominal.encode;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 直接编码（Direct Encoding）LD机制算法。方案描述参见下述论文：
 * <p>
 * Wang, Tianhao, Jeremiah Blocki, Ninghui Li, and Somesh Jha. Locally differentially private protocols for frequency
 * estimation. USENIX Security 2017, pp. 729-745. 2017.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
class DirectEncodeLdp implements EncodeLdp {
    /**
     * 配置项
     */
    private DirectEncodeLdpConfig directEncodeLdpConfig;
    /**
     * 不翻转的概率
     */
    private double p;
    /**
     * 翻转概率
     */
    private double q;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof DirectEncodeLdpConfig;
        directEncodeLdpConfig = (DirectEncodeLdpConfig)ldpConfig;
        // 设置翻转概率
        int d = directEncodeLdpConfig.getLabelSize();
        double epsilon = directEncodeLdpConfig.getBaseEpsilon();
        p = Math.exp(epsilon) / (Math.exp(epsilon) + d - 1);
        q = 1.0 / (Math.exp(epsilon) + d - 1);
    }

    @Override
    public String randomize(String value) {
        assert directEncodeLdpConfig.getLabelSet().contains(value) : "Value is not in the label set: " + value;
        double randomSample = directEncodeLdpConfig.getRandom().nextDouble();
        // 采样[0, d)之间的随机整数值
        int d = directEncodeLdpConfig.getLabelSize();
        int randomIndex = directEncodeLdpConfig.getRandom().nextInt(d);
        if (randomSample > p - q) {
            // 答复随机值
            return directEncodeLdpConfig.getLabelArrayList().get(randomIndex);
        } else {
            // 答复真实值
            return value;
        }
    }

    @Override
    public double getEpsilon() {
        return directEncodeLdpConfig.getBaseEpsilon();
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        directEncodeLdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public LdpConfig getLdpConfig() {
        return directEncodeLdpConfig;
    }
}
