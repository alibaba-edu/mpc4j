package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为156比特，码字（codeword）为627比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch156By627Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch156By627Coder() {
        super(156, 627);
    }

    /**
     * 编码实例
     */
    private static final Bch156By627Coder INSTANCE = new Bch156By627Coder();

    public static Bch156By627Coder getInstance() {
        return INSTANCE;
    }
}
