package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为238比特，码字（codeword）为776比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/16
 */
public class Bch238By776Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch238By776Coder() {
        super(238, 776);
    }

    /**
     * 编码实例
     */
    private static final Bch238By776Coder INSTANCE = new Bch238By776Coder();

    public static Bch238By776Coder getInstance() {
        return INSTANCE;
    }
}
