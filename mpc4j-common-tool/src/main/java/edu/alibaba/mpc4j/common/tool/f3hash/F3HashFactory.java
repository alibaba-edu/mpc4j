package edu.alibaba.mpc4j.common.tool.f3hash;

import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory.LongHashType;

/**
 * F3Hash factory
 *
 * @author Feng Han
 * @date 2024/10/20
 */
public class F3HashFactory {

    /**
     * 私有构造函数。
     */
    private F3HashFactory() {
        // empty
    }

    /**
     * 哈希函数类型
     */
    public enum F3HashType {
        /**
         * method with longHash
         */
        LONG_F3_HASH,
    }

    /**
     * 创建哈希函数实例。
     *
     * @param hashType 哈希函数类型。
     * @return 哈希函数实例。
     */
    public static F3Hash createInstance(F3HashType f3hashType, LongHashType hashType) {
        switch (f3hashType) {
            case LONG_F3_HASH:
                return new LongF3Hash(hashType);
            default:
                throw new IllegalArgumentException("Invalid " + F3HashType.class.getSimpleName() + ": " + f3hashType.name());
        }
    }

    /**
     * 创建默认哈希函数实例。
     *
     * @return 哈希函数实例。
     */
    public static F3Hash createDefaultInstance() {
        return new LongF3Hash(LongHashType.BOB_HASH_64);
    }

}
