package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为132比特，码字（codeword）为583比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch132By583Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch132By583Coder() {
        super(132, 583);
    }

    /**
     * 编码实例
     */
    private static final Bch132By583Coder INSTANCE = new Bch132By583Coder();

    public static Bch132By583Coder getInstance() {
        return INSTANCE;
    }
}
