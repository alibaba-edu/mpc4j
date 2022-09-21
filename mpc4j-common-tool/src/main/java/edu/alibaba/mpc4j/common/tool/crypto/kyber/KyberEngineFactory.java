package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.engine.KyberCcaEngine;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.engine.KyberCpaEngine;

/**
 * Kyber工厂类。
 *
 * @author Sheng Hu
 * @date 2022/09/01
 */
public class KyberEngineFactory {
    /**
     * 私有构造函数。
     */
    private KyberEngineFactory() {
        // empty
    }

    /**
     * Kyber方案枚举类
     */
    public enum KyberType {
        /**
         * Kyber的cpa实现
         */
        KYBER_CPA,
        /**
         * Kyber的cca实现
         */
        KYBER_CCA,
    }

    /**
     * 创建Kyber类型
     */
    public static KyberEngine createInstance(KyberType kyberType, int paramsK) {
        switch (kyberType) {
            case KYBER_CPA:
                return new KyberCpaEngine(paramsK);
            case KYBER_CCA:
                return new KyberCcaEngine(paramsK);
            default:
                throw new IllegalArgumentException("Invalid " + KyberEngineFactory.KyberType.class.getSimpleName() + ": " + kyberType.name());
        }
    }
}
