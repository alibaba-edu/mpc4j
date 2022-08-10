package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为150比特，码字（codeword）为616比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch150By616Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch150By616Coder() {
        super(150, 616);
    }

    /**
     * 编码实例
     */
    private static final Bch150By616Coder INSTANCE = new Bch150By616Coder();

    public static Bch150By616Coder getInstance() {
        return INSTANCE;
    }
}
