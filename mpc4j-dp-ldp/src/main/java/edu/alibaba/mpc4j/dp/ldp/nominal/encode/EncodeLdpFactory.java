package edu.alibaba.mpc4j.dp.ldp.nominal.encode;

/**
 * 编码LDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public class EncodeLdpFactory {
    /**
     * 私有构造函数
     */
    private EncodeLdpFactory() {
        // empty
    }

    /**
     * 构造编码LDP机制。
     *
     * @param encodeLdpConfig 配置项。
     * @return 编码LDP机制。
     */
    public static EncodeLdp createInstance(EncodeLdpConfig encodeLdpConfig) {
        if (encodeLdpConfig instanceof DirectEncodeLdpConfig) {
            DirectEncodeLdp directEncodeLdp = new DirectEncodeLdp();
            directEncodeLdp.setup(encodeLdpConfig);
            return directEncodeLdp;
        }
        throw new IllegalArgumentException("Illegal EncodeLdpConfig: " + encodeLdpConfig.getClass().getSimpleName());
    }
}
