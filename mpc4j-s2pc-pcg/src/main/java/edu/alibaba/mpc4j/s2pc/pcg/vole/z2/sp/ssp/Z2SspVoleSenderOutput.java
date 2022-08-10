package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * Z2-SSP-VOLE协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public class Z2SspVoleSenderOutput {
    /**
     * 数量
     */
    private int num;
    /**
     * 字节数量
     */
    private int byteNum;
    /**
     * α
     */
    private int alpha;
    /**
     * t_i
     */
    private byte[] t;

    /**
     * 创建发送方输出。
     *
     * @param num   数量。
     * @param alpha α。
     * @param t     t_i。
     * @return 发送方输出。
     */
    public static Z2SspVoleSenderOutput create(int num, int alpha, byte[] t) {
        Z2SspVoleSenderOutput senderOutput = new Z2SspVoleSenderOutput();
        assert num > 0 : "# must be greater than 0";
        senderOutput.num = num;
        senderOutput.byteNum = CommonUtils.getByteLength(num);
        assert alpha >= 0 && alpha < num : "α must be in ranger [0, " + num + "): " + alpha;
        senderOutput.alpha = alpha;
        assert t.length == senderOutput.byteNum && BytesUtils.isReduceByteArray(t, num);
        senderOutput.t = BytesUtils.clone(t);

        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private Z2SspVoleSenderOutput() {
        // empty
    }

    /**
     * 返回α。
     *
     * @return α。
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * 返回t。
     *
     * @return t。
     */
    public byte[] getT() {
        return t;
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return num;
    }

    /**
     * 返回字节数量。
     *
     * @return 字节数量。
     */
    public int getByteNum() {
        return byteNum;
    }
}
