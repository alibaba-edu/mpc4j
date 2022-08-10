package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 伪随机置换工厂类。
 *
 * @author Weiran Liu
 * @date 2021/11/30
 */
public class PrpFactory {
    /**
     * 私有构造函数。
     */
    private PrpFactory() {
        // empty
    }

    /**
     * 伪随机置换类型
     */
    public enum PrpType {
        /**
         * JDK的AES
         */
        JDK_AES,
        /**
         * 本地的AES
         */
        NATIVE_AES,
        /**
         * Bouncy Castle的SM4
         */
        BC_SM4,
        /**
         * Java的byte[]实现的20轮LowMC
         */
        JDK_BYTES_LOW_MC_20,
        /**
         * Java的byte[]实现的21轮LowMC
         */
        JDK_BYTES_LOW_MC_21,
        /**
         * Java的byte[]实现的23轮LowMC
         */
        JDK_BYTES_LOW_MC_23,
        /**
         * Java的byte[]实现的32轮LowMC
         */
        JDK_BYTES_LOW_MC_32,
        /**
         * Java的byte[]实现的192轮LowMC
         */
        JDK_BYTES_LOW_MC_192,
        /**
         * Java的byte[]实现的208轮LowMC
         */
        JDK_BYTES_LOW_MC_208,
        /**
         * Java的byte[]实现的287轮LowMC
         */
        JDK_BYTES_LOW_MC_287,
        /**
         * Java的long[]实现的20轮LowMC
         */
        JDK_LONGS_LOW_MC_20,
        /**
         * Java的long[]实现的21轮LowMC
         */
        JDK_LONGS_LOW_MC_21,
        /**
         * Java的long[]实现的23轮LowMC
         */
        JDK_LONGS_LOW_MC_23,
        /**
         * Java的long[]实现的32轮LowMC
         */
        JDK_LONGS_LOW_MC_32,
        /**
         * Java的long[]实现的192轮LowMC
         */
        JDK_LONGS_LOW_MC_192,
        /**
         * Java的long[]实现的208轮LowMC
         */
        JDK_LONGS_LOW_MC_208,
        /**
         * Java的long[]实现的287轮LowMC
         */
        JDK_LONGS_LOW_MC_287,
    }

    /**
     * 创建一个新的PRP实例。
     *
     * @param prpType PRP类型。
     * @return 新的PRP实例。
     */
    public static Prp createInstance(PrpType prpType) {
        switch (prpType) {
            case JDK_AES:
                return new JdkAesPrp();
            case NATIVE_AES:
                return new NativeAesPrp();
            case BC_SM4:
                return new BcSm4Prp();
            case JDK_BYTES_LOW_MC_20:
                return new JdkBytesLowMcPrp(20);
            case JDK_BYTES_LOW_MC_21:
                return new JdkBytesLowMcPrp(21);
            case JDK_BYTES_LOW_MC_23:
                return new JdkBytesLowMcPrp(23);
            case JDK_BYTES_LOW_MC_32:
                return new JdkBytesLowMcPrp(32);
            case JDK_BYTES_LOW_MC_192:
                return new JdkBytesLowMcPrp(192);
            case JDK_BYTES_LOW_MC_208:
                return new JdkBytesLowMcPrp(208);
            case JDK_BYTES_LOW_MC_287:
                return new JdkBytesLowMcPrp(287);
            case JDK_LONGS_LOW_MC_20:
                return new JdkLongsLowMcPrp(20);
            case JDK_LONGS_LOW_MC_21:
                return new JdkLongsLowMcPrp(21);
            case JDK_LONGS_LOW_MC_23:
                return new JdkLongsLowMcPrp(23);
            case JDK_LONGS_LOW_MC_32:
                return new JdkLongsLowMcPrp(32);
            case JDK_LONGS_LOW_MC_192:
                return new JdkLongsLowMcPrp(192);
            case JDK_LONGS_LOW_MC_208:
                return new JdkLongsLowMcPrp(208);
            case JDK_LONGS_LOW_MC_287:
                return new JdkLongsLowMcPrp(287);
            default:
                throw new IllegalArgumentException("Invalid PrpType: " + prpType.name());
        }
    }

    /**
     * 创建一个新的PRP实例。
     *
     * @param envType 环境类型。
     * @return PRP实例。
     */
    public static Prp createInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
            case STANDARD_JDK:
                return new JdkAesPrp();
            case INLAND:
            case INLAND_JDK:
                return new BcSm4Prp();
            default:
                throw new IllegalArgumentException("Invalid EnvType: " + envType.name());
        }
    }
}
