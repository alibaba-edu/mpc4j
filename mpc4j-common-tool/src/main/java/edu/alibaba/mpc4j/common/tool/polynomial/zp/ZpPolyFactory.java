package edu.alibaba.mpc4j.common.tool.polynomial.zp;

/**
 * Zp多项式插值工厂类。
 *
 * @author Weiran Liu
 * @date 2021/06/05
 */
public class ZpPolyFactory {
    /**
     * 私有构造函数。
     */
    private ZpPolyFactory() {
        // empty
    }

    /**
     * Zp多项式插值类型。
     */
    public enum ZpPolyType {
        /**
         * NTL库插值
         */
        NTL,
        /**
         * Rings实现的牛顿插值
         */
        RINGS_NEWTON,
        /**
         * JDK实现的牛顿插值
         */
        JDK_NEWTON,
        /**
         * Rings实现的拉格朗日插值
         */
        RINGS_LAGRANGE,
        /**
         * JDK实现的拉格朗日插值
         */
        JDK_LAGRANGE,
    }

    /**
     * Zp二叉树多项式插值类型。
     */
    public enum ZpTreePolyType {
        /**
         * NTL实现的二叉树插值
         */
        NTL_TREE,
        /**
         * Rings实现的二叉树插值
         */
        RINGS_TREE,
    }

    /**
     * 创建多项式插值实例。
     *
     * @param type 多项式插值类型。
     * @param l    有限域比特长度。
     * @return 多项式插值实例。
     */
    public static ZpPoly createInstance(ZpPolyType type, int l) {
        switch (type) {
            case NTL:
                return new NtlZpPoly(l);
            case RINGS_NEWTON:
                return new RingsNewtonZpPoly(l);
            case JDK_NEWTON:
                return new JdkNewtonZpPoly(l);
            case RINGS_LAGRANGE:
                return new RingsLagrangeZpPoly(l);
            case JDK_LAGRANGE:
                return new JdkLagrangeZpPoly(l);
            default:
                throw new IllegalArgumentException("Invalid " + ZpPolyType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建二叉树多项式插值实例。
     *
     * @param type 二叉树多项式插值类型。
     * @param l    有限域比特长度。
     * @return 二叉树多项式插值实例。
     */
    public static ZpTreePoly createTreeInstance(ZpTreePolyType type, int l) {
        switch (type) {
            case RINGS_TREE:
                return new RingsZpTreePoly(l);
            case NTL_TREE:
                return new NtlTreeZpPoly(l);
            default:
                throw new IllegalArgumentException("Invalid " + ZpTreePolyType.class.getSimpleName() + ": " + type.name());
        }
    }
}
