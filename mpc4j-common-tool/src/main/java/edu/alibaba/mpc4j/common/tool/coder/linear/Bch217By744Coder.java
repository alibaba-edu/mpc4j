package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为217比特，码字（codeword）为744比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/16
 */
public class Bch217By744Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch217By744Coder() {
        super(217, 744);
    }

    /**
     * 编码实例
     */
    private static final Bch217By744Coder INSTANCE = new Bch217By744Coder();

    public static Bch217By744Coder getInstance() {
        return INSTANCE;
    }
}
