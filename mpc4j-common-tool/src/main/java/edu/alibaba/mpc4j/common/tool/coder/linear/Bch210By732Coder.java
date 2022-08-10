package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为210比特，码字（codeword）为732比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch210By732Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch210By732Coder() {
        super(210, 732);
    }

    /**
     * 编码实例
     */
    private static final Bch210By732Coder INSTANCE = new Bch210By732Coder();

    public static Bch210By732Coder getInstance() {
        return INSTANCE;
    }
}
