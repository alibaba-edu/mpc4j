package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为076比特，码字（codeword）为511比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch076By511Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch076By511Coder() {
        super(76, 511);
    }

    /**
     * 编码实例
     */
    private static final Bch076By511Coder INSTANCE = new Bch076By511Coder();

    public static Bch076By511Coder getInstance() {
        return INSTANCE;
    }
}
