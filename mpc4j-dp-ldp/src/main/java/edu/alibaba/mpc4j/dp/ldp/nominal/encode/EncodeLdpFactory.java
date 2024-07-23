package edu.alibaba.mpc4j.dp.ldp.nominal.encode;

import java.util.List;

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
     * Encode_Ldp机制类型。
     */
    public enum EncodeLdpType {
        /**
         * DirectEncode机制
         */
        DE,
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

    /**
     * 构建LDP机制默认配置项。
     *
     * @param ldpType LDP机制类型。
     * @return 默认配置项。
     */
    public static EncodeLdpConfig createDefaultConfig(EncodeLdpType ldpType, double baseEpsilon, List<String> labelArrayList) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (ldpType) {
            case DE:
                return new DirectEncodeLdpConfig.Builder(baseEpsilon, labelArrayList).build();
            default:
                throw new IllegalArgumentException("Invalid LDP Mechanismm Type:" + ldpType);
        }
    }
}
