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
                if (ceilLogN < 25 && ceilLogN > 13) {
                    return new OnlineLdpcCreator(codeType, ceilLogN);
                } else {
                    throw new IllegalArgumentException("Type: " + type.name() + "is not ready for ceilLogN :" + ceilLogN);
                }
            default:
                throw new IllegalArgumentException("Type: " + type.name() + "is not supported");
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
        if (ceilLogN >= 14 && ceilLogN <= 24) {
            return new OnlineLdpcCreator(codeType, ceilLogN);
        } else {
            return new FullLdpcCreator(codeType, ceilLogN);
        }
    }
}
