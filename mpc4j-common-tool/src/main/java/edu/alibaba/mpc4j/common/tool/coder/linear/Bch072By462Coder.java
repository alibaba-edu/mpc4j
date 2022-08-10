package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为072比特，码字（codeword）为462比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch072By462Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch072By462Coder() {
        super(72, 462);
    }

    /**
     * 编码实例
     */
    private static final Bch072By462Coder INSTANCE = new Bch072By462Coder();

    public static Bch072By462Coder getInstance() {
        return INSTANCE;
    }
}
