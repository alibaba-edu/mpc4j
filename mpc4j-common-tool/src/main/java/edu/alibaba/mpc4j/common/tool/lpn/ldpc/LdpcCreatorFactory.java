package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

/**
 * LdpcCreator 工厂类
 *
 * @author Hanwen Feng
 * @date 2022.3.18
 */
public class LdpcCreatorFactory {
    /**
     * 私有构造函数
     */
    private LdpcCreatorFactory() {
        //empty
    }

    /**
     * 支持的LdpcCreator类型
     */
    public enum LdpcCreatorType {
        /**
         * 完整生成
         */
        FULL,
        /**
         * 在线生成
         */
        ONLINE,
    }

    /**
     * 返回指定类型的LdpcCreator
     *
     * @param codeType Ldpc类型
     * @param ceilLogN 目标OT数量
     * @param type     LdpcCreator类型
     * @return LdpcCreator
     */
    public static LdpcCreator createLdpcCreator(LdpcCreatorUtils.CodeType codeType, int ceilLogN, LdpcCreatorType type) {
        switch (type) {
            case FULL:
                return new FullLdpcCreator(codeType, ceilLogN);
            case ONLINE:
                try {
                    return new OnlineLdpcCreator(codeType, ceilLogN);
                } catch (IllegalStateException e) {
                    System.out.println("Silver File: " + codeType.name() + "_" + ceilLogN + "is NOT ready. Starting Full LDPC Creator...");
                    return new FullLdpcCreator(codeType, ceilLogN);
                }
            default:
                throw new IllegalArgumentException("Type: " + type.name() + " is not supported");
        }
    }

    /**
     * 根据输入的ceilLogN，自动选择最优的LdpcCreator
     *
     * @param codeType Ldpc类型
     * @param ceilLogN 目标OT数量
     * @return LdpcCreator
     */
    public static LdpcCreator createLdpcCreator(LdpcCreatorUtils.CodeType codeType, int ceilLogN) {
        if (ceilLogN < LdpcCreatorUtils.MIN_LOG_N || ceilLogN > LdpcCreatorUtils.MAX_LOG_N) {
            return new FullLdpcCreator(codeType, ceilLogN);
        }
        try {
            return new OnlineLdpcCreator(codeType, ceilLogN);
        } catch (IllegalStateException e) {
            System.out.println("Silver File: " + codeType.name() + "_" + ceilLogN + " is NOT ready. Starting Full LDPC Creator...");
            return new FullLdpcCreator(codeType, ceilLogN);
        }
    }
}
