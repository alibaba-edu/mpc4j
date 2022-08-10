package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 数据字（dataword）为084比特，码字（codeword）为495比特的BCH编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch084By495Coder extends AbstractBchCoder {
    /**
     * 单例模式
     */
    private Bch084By495Coder() {
        super(84, 495);
    }

    /**
     * 编码实例
     */
    private static final Bch084By495Coder INSTANCE = new Bch084By495Coder();

    public static Bch084By495Coder getInstance() {
        return INSTANCE;
    }
}
