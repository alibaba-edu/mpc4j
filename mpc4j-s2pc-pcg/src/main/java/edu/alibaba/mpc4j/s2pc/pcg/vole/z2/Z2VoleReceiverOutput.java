package edu.alibaba.mpc4j.s2pc.pcg.vole.z2;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Z_2-VOLE接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/6/11
 */
public class Z2VoleReceiverOutput {
    /**
     * 数量
     */
    private int num;
    /**
     * 字节数量
     */
    private int byteNum;
    /**
     * 关联值Δ
     */
    private boolean delta;
    /**
     * q_i
     */
    private BigInteger q;

    /**
     * 创建接收方输出。
     *
     * @param num   数量。
     * @param delta 关联值Δ。
     * @param q     q_i。
     * @return 接收方输出。
     */
    public static Z2VoleReceiverOutput create(int num, boolean delta, byte[] q) {
        Z2VoleReceiverOutput receiverOutput = new Z2VoleReceiverOutput();
        assert num > 0 : "# must be greater than 0";
        receiverOutput.num = num;
        receiverOutput.byteNum = CommonUtils.getByteLength(num);
        receiverOutput.delta = delta;
        assert q.length == receiverOutput.byteNum && BytesUtils.isReduceByteArray(q, num);
        receiverOutput.q = BigIntegerUtils.byteArrayToNonNegBigInteger(q);

        return receiverOutput;
    }

    /**
     * 创建接收方输出。
     *
     * @param num 数量。
     * @param q   q_i。
     * @return 接收方输出。
     */
    private static Z2VoleReceiverOutput create(int num, boolean delta, BigInteger q) {
        assert num > 0 : "# must be greater than 0";
        assert BigIntegerUtils.nonNegative(q) && q.bitLength() <= num;

        Z2VoleReceiverOutput receiverOutput = new Z2VoleReceiverOutput();
        receiverOutput.num = num;
        receiverOutput.byteNum = CommonUtils.getByteLength(num);
        receiverOutput.delta = delta;
        receiverOutput.q = q;
        return receiverOutput;
    }

    /**
     * 创建长度为0的接收方输出。
     *
     * @param delta 关联值Δ。
     * @return 接收方输出。
     */
    public static Z2VoleReceiverOutput createEmpty(boolean delta) {
        Z2VoleReceiverOutput receiverOutput = new Z2VoleReceiverOutput();
        receiverOutput.num = 0;
        receiverOutput.byteNum = 0;
        receiverOutput.delta = delta;
        receiverOutput.q = BigInteger.ZERO;

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private Z2VoleReceiverOutput() {
        // empty
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public Z2VoleReceiverOutput split(int length) {
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 切分方法：分别对2^length取模数和取余数，模数作为split结果，余数作为剩余结果
        BigInteger mask = BigInteger.ONE.shiftLeft(length).subtract(BigInteger.ONE);
        // 由于模数一定是2^length格式，因此可以用位运算更高效地实现
        BigInteger splitT = q.and(mask);
        // 更新剩余的数据
        q = q.shiftRight(length);
        num = num - length;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);
        return Z2VoleReceiverOutput.create(length, delta, splitT);
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
            q = q.and(mask);
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
    public void merge(Z2VoleReceiverOutput that) {
        assert this.delta == that.delta : "merged outputs must have the same Δ";
        // 移位
        q = q.shiftLeft(that.num).add(that.q);
        // 更新长度
        num += that.num;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);
    }

    /**
     * 返回关联值Δ。
     *
     * @return 关联值Δ。
     */
    public boolean getDelta() {
        return delta;
    }

    /**
     * 返回关联值Δ字节数组。
     *
     * @return 关联值Δ字节数组。
     */
    public byte[] getDeltaBytes() {
        byte[] deltaBytes = new byte[byteNum];
        if (delta) {
            Arrays.fill(deltaBytes, (byte) 0xFF);
            BytesUtils.reduceByteArray(deltaBytes, num);
        }
        return deltaBytes;
    }

    /**
     * 返回q。
     *
     * @return q。
     */
    public byte[] getQ() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(q, byteNum);
    }

    /**
     * 返回随机分量q的二进制字符串表示。
     *
     * @return 随机分量q的二进制字符串表示。
     */
    public String getStringQ() {
        if (num == 0) {
            return "";
        }
        StringBuilder stringX = new StringBuilder(q.toString(2));
        while (stringX.length() < num) {
            stringX.insert(0, "0");
        }
        return stringX.toString();
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
