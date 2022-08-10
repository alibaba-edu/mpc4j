package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为174比特，码字（codeword）为660比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch174By660Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch174By660Coder() {
        super(174, 660);
    }

    /**
     * 编码实例
     */
    private static final Bch174By660Coder INSTANCE = new Bch174By660Coder();

    public static Bch174By660Coder getInstance() {
        return INSTANCE;
    }
}
