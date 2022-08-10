package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为138比特，码字（codeword）为594比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch138By594Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch138By594Coder() {
        super(138, 594);
    }

    /**
     * 编码实例
     */
    private static final Bch138By594Coder INSTANCE = new Bch138By594Coder();

    public static Bch138By594Coder getInstance() {
        return INSTANCE;
    }
}
