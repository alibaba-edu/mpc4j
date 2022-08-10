package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为090比特，码字（codeword）为495比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch090By495Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch090By495Coder() {
        super(90, 495);
    }

    /**
     * 编码实例
     */
    private static final Bch090By495Coder INSTANCE = new Bch090By495Coder();

    public static Bch090By495Coder getInstance() {
        return INSTANCE;
    }
}
