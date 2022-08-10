package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为144比特，码字（codeword）为605比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch144By605Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch144By605Coder() {
        super(144, 605);
    }

    /**
     * 编码实例
     */
    private static final Bch144By605Coder INSTANCE = new Bch144By605Coder();

    public static Bch144By605Coder getInstance() {
        return INSTANCE;
    }
}
