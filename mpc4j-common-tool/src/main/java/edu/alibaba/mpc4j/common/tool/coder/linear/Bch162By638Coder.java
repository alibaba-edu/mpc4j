package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为162比特，码字（codeword）为638比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch162By638Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch162By638Coder() {
        super(162, 638);
    }

    /**
     * 编码实例
     */
    private static final Bch162By638Coder INSTANCE = new Bch162By638Coder();

    public static Bch162By638Coder getInstance() {
        return INSTANCE;
    }
}
