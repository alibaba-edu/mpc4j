package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为065比特，码字（codeword）为448比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch065By448Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch065By448Coder() {
        super(65, 448);
    }

    /**
     * 编码实例
     */
    private static final Bch065By448Coder INSTANCE = new Bch065By448Coder();

    public static Bch065By448Coder getInstance() {
        return INSTANCE;
    }
}
