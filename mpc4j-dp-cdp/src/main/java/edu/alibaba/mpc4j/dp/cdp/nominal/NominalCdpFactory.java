package edu.alibaba.mpc4j.dp.cdp.nominal;

/**
 * 枚举CDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
public class NominalCdpFactory {

    /**
     * 私有构造函数
     */
    private NominalCdpFactory() {
        // empty
    }

    /**
     * 构造枚举CDP机制。
     *
     * @param nominalCdpConfig 配置项。
     * @return 枚举CDP机制。
     */
    public static NominalCdp createInstance(NominalCdpConfig nominalCdpConfig) {
        if (nominalCdpConfig instanceof ExpCdpConfig) {
            ExpCdp expCdp = new ExpCdp();
            expCdp.setup(nominalCdpConfig);
            return expCdp;
        } else if (nominalCdpConfig instanceof Base2ExpCdpConfig) {
            Base2ExpCdp base2ExpCdp = new Base2ExpCdp();
            base2ExpCdp.setup(nominalCdpConfig);
            return base2ExpCdp;
        }
        throw new IllegalArgumentException("Invalid NominalCdpConfig: " + nominalCdpConfig.getClass().getSimpleName());
    }
}
