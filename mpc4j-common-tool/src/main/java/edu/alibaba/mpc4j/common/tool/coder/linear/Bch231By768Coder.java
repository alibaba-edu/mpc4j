package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为231比特，码字（codeword）为768比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/16
 */
public class Bch231By768Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch231By768Coder() {
        super(231, 768);
    }

    /**
     * 编码实例
     */
    private static final Bch231By768Coder INSTANCE = new Bch231By768Coder();

    public static Bch231By768Coder getInstance() {
        return INSTANCE;
    }
}
