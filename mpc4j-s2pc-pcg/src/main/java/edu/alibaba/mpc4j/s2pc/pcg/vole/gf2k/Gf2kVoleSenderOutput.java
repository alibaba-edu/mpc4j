package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * GF2K-VOLE协议发送方输出。发送方得到(x, t)，满足t = q + Δ · x（Δ和q由接收方持有）。
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2kVoleSenderOutput {
    /**
     * x_i
     */
    private byte[][] x;
    /**
     * t_i
     */
    private byte[][] t;

    /**
     * 创建发送方输出。
     *
     * @param x x_i。
     * @param t t_i。
     * @return 发送方输出。
     */
    public static Gf2kVoleSenderOutput create(byte[][] x, byte[][] t) {
        Gf2kVoleSenderOutput senderOutput = new Gf2kVoleSenderOutput();
        assert x.length > 0 : "# of x must be greater than 0";
        assert x.length == t.length : "# of x must be equal to # of q";
        senderOutput.x = Arrays.stream(x)
            .peek(xi -> {
                assert xi.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        senderOutput.t = Arrays.stream(t)
            .peek(ti -> {
                assert ti.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);

        return senderOutput;
    }

    /**
     * 创建长度为0的发送方输出。
     *
     * @return 长度为0的发送方输出。
     */
    public static Gf2kVoleSenderOutput createEmpty() {
        Gf2kVoleSenderOutput senderOutput = new Gf2kVoleSenderOutput();
        senderOutput.x = new byte[0][];
        senderOutput.t = new byte[0][];

        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private Gf2kVoleSenderOutput() {
        // empty
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public Gf2kVoleSenderOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 切分x
        byte[][] subX = new byte[length][];
        byte[][] remainX = new byte[num - length][];
        System.arraycopy(x, 0, subX, 0, length);
        System.arraycopy(x, length, remainX, 0, num - length);
        x = remainX;
        // 切分t
        byte[][] subT = new byte[length][];
        byte[][] remainT = new byte[num - length][];
        System.arraycopy(t, 0, subT, 0, length);
        System.arraycopy(t, length, remainT, 0, num - length);
        t = remainT;

        return Gf2kVoleSenderOutput.create(subX, subT);
    }

    /**
     * 将当前输出结果数量减少至给定的数量。
     *
     * @param length 给定的数量。
     */
    public void reduce(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "reduce length = " + length + " must be in range (0, " + num + "]";
        if (length < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            byte[][] remainX = new byte[length][];
            System.arraycopy(x, 0, remainX, 0, length);
            x = remainX;
            byte[][] remainT = new byte[length][];
            System.arraycopy(t, 0, remainT, 0, length);
            t = remainT;
        }
    }

    /**
     * 合并两个发送方输出。
     *
     * @param that 另一个发送方输出。
     */
    public void merge(Gf2kVoleSenderOutput that) {
        // 合并x
        byte[][] mergeX = new byte[this.x.length + that.x.length][];
        System.arraycopy(this.x, 0, mergeX, 0, this.x.length);
        System.arraycopy(that.x, 0, mergeX, this.x.length, that.x.length);
        x = mergeX;
        // 合并t
        byte[][] mergeT = new byte[this.t.length + that.t.length][];
        System.arraycopy(this.t, 0, mergeT, 0, this.t.length);
        System.arraycopy(that.t, 0, mergeT, this.t.length, that.t.length);
        t = mergeT;
    }

    /**
     * 返回x_i。
     *
     * @param index 索引值。
     * @return x_i。
     */
    public byte[] getX(int index) {
        return x[index];
    }

    /**
     * 返回x。
     *
     * @return x。
     */
    public byte[][] getX() {
        return x;
    }

    /**
     * 返回t_i。
     *
     * @param index 索引值。
     * @return t_i。
     */
    public byte[] getT(int index) {
        return t[index];
    }

    /**
     * 返回t。
     *
     * @return t。
     */
    public byte[][] getT() {
        return t;
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return x.length;
    }
}
