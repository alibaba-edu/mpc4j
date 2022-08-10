package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为168比特，码字（codeword）为649比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch168By649Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch168By649Coder() {
        super(168, 649);
    }

    /**
     * 编码实例
     */
    private static final Bch168By649Coder INSTANCE = new Bch168By649Coder();

    public static Bch168By649Coder getInstance() {
        return INSTANCE;
    }
}
