package edu.alibaba.mpc4j.s2pc.pcg.vole.z2;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;

/**
 * Z_2-VOLE协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/6/11
 */
public class Z2VoleSenderOutput {
    /**
     * 数量
     */
    private int num;
    /**
     * 字节数量
     */
    private int byteNum;
    /**
     * x_i
     */
    private BigInteger x;
    /**
     * t_i
     */
    private BigInteger t;

    /**
     * 创建发送方输出。
     *
     * @param num 数量。
     * @param x   x_i。
     * @param t   t_i。
     * @return 发送方输出。
     */
    public static Z2VoleSenderOutput create(int num, byte[] x, byte[] t) {
        Z2VoleSenderOutput senderOutput = new Z2VoleSenderOutput();
        assert num > 0 : "# must be greater than 0";
        senderOutput.num = num;
        senderOutput.byteNum = CommonUtils.getByteLength(num);
        assert x.length == senderOutput.byteNum && BytesUtils.isReduceByteArray(x, num);
        senderOutput.x = BigIntegerUtils.byteArrayToNonNegBigInteger(x);
        assert t.length == senderOutput.byteNum && BytesUtils.isReduceByteArray(t, num);
        senderOutput.t = BigIntegerUtils.byteArrayToNonNegBigInteger(t);

        return senderOutput;
    }

    /**
     * 创建发送方输出。
     *
     * @param num 数量。
     * @param x   x_i。
     * @param t   t_i。
     * @return 发送方输出。
     */
    private static Z2VoleSenderOutput create(int num, BigInteger x, BigInteger t) {
        assert num > 0 : "# must be greater than 0";
        assert BigIntegerUtils.nonNegative(x) && x.bitLength() <= num;
        assert BigIntegerUtils.nonNegative(t) && t.bitLength() <= num;

        Z2VoleSenderOutput senderOutput = new Z2VoleSenderOutput();
        senderOutput.num = num;
        senderOutput.byteNum = CommonUtils.getByteLength(num);
        senderOutput.x = x;
        senderOutput.t = t;
        return senderOutput;
    }

    /**
     * 创建长度为0的发送方输出。
     *
     * @return 发送方输出。
     */
    public static Z2VoleSenderOutput createEmpty() {
        Z2VoleSenderOutput senderOutput = new Z2VoleSenderOutput();
        senderOutput.num = 0;
        senderOutput.byteNum = 0;
        senderOutput.x = BigInteger.ZERO;
        senderOutput.t = BigInteger.ZERO;

        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private Z2VoleSenderOutput() {
        // empty
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public Z2VoleSenderOutput split(int length) {
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 切分方法：分别对2^length取模数和取余数，模数作为split结果，余数作为剩余结果
        BigInteger mask = BigInteger.ONE.shiftLeft(length).subtract(BigInteger.ONE);
        // 由于模数一定是2^length格式，因此可以用位运算更高效地实现
        BigInteger splitX = x.and(mask);
        BigInteger splitQ = t.and(mask);
        // 更新剩余的数据
        x = x.shiftRight(length);
        t = t.shiftRight(length);
        num = num - length;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);
        return Z2VoleSenderOutput.create(length, splitX, splitQ);
    }

    /**
     * 将当前输出结果数量减少至给定的数量。
     *
     * @param length 给定的数量。
     */
    public void reduce(int length) {
        assert length > 0 && length <= num : "reduce length = " + length + " must be in range (0, " + num + "]";
        if (length < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            BigInteger mask = BigInteger.ONE.shiftLeft(length).subtract(BigInteger.ONE);
            x = x.and(mask);
            t = t.and(mask);
            // 更改长度
            num = length;
            byteNum = CommonUtils.getByteLength(length);
        }
    }

    /**
     * 合并两个发送方输出。
     *
     * @param that 另一个发送方输出。
     */
    public void merge(Z2VoleSenderOutput that) {
        // 移位
        x = x.shiftLeft(that.num).add(that.x);
        t = t.shiftLeft(that.num).add(that.t);
        // 更新长度
        num += that.num;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);
    }

    /**
     * 返回x。
     *
     * @return x。
     */
    public byte[] getX() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(x, byteNum);
    }

    /**
     * 返回随机分量x的二进制字符串表示。
     *
     * @return 随机分量x的二进制字符串表示。
     */
    public String getStringX() {
        if (num == 0) {
            return "";
        }
        StringBuilder stringX = new StringBuilder(x.toString(2));
        while (stringX.length() < num) {
            stringX.insert(0, "0");
        }
        return stringX.toString();
    }

    /**
     * 返回t。
     *
     * @return t。
     */
    public byte[] getT() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(t, byteNum);
    }

    /**
     * 返回随机分量t的二进制字符串表示。
     *
     * @return 随机分量t的二进制字符串表示。
     */
    public String getStringT() {
        if (num == 0) {
            return "";
        }
        StringBuilder stringQ = new StringBuilder(t.toString(2));
        while (stringQ.length() < num) {
            stringQ.insert(0, "0");
        }
        return stringQ.toString();
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return num;
    }
}
