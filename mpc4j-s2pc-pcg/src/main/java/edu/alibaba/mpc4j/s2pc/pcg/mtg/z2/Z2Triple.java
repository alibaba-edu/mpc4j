package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;

/**
 * 布尔三元组。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public class Z2Triple {
    /**
     * 布尔三元组数量
     */
    private int num;
    /**
     * 布尔三元组字节长度
     */
    private int byteNum;
    /**
     * 随机分量a
     */
    private BigInteger a;
    /**
     * 随机分量b
     */
    private BigInteger b;
    /**
     * 随机分量c
     */
    private BigInteger c;

    /**
     * 创建布尔三元组随机分量。
     *
     * @param num 布尔三元组数量。
     * @param a   随机分量a。
     * @param b   随机分量b。
     * @param c   随机分量c。
     */
    public static Z2Triple create(int num, byte[] a, byte[] b, byte[] c) {
        assert num > 0 : "num must be greater than 0: " + num;
        // 验证随机分量的长度
        int byteNum = CommonUtils.getByteLength(num);
        assert a.length == byteNum && BytesUtils.isReduceByteArray(a, num);
        assert b.length == byteNum && BytesUtils.isReduceByteArray(b, num);
        assert c.length == byteNum && BytesUtils.isReduceByteArray(c, num);

        // 将byte[]转换为BigInteger时已经数进行了拷贝
        Z2Triple triple = new Z2Triple();
        triple.num = num;
        triple.byteNum = byteNum;
        triple.a = BigIntegerUtils.byteArrayToNonNegBigInteger(a);
        triple.b = BigIntegerUtils.byteArrayToNonNegBigInteger(b);
        triple.c = BigIntegerUtils.byteArrayToNonNegBigInteger(c);

        return triple;
    }

    /**
     * 创建长度为0的布尔三元组随机分量。
     *
     * @return 长度为0的布尔三元组随机分量。
     */
    public static Z2Triple createEmpty() {
        Z2Triple emptyTriple = new Z2Triple();
        emptyTriple.num = 0;
        emptyTriple.byteNum = 0;
        emptyTriple.a = BigInteger.ZERO;
        emptyTriple.b = BigInteger.ZERO;
        emptyTriple.c = BigInteger.ZERO;

        return emptyTriple;
    }

    /**
     * 内部构建布尔三元组随机分量。
     *
     * @param num 布尔三元组数量。
     * @param a   随机分量a。
     * @param b   随机分量b。
     * @param c   随机分量c。
     */
    private static Z2Triple create(int num, BigInteger a, BigInteger b, BigInteger c) {
        assert num > 0 : "num must be greater than 0: " + num;
        assert BigIntegerUtils.nonNegative(a) && a.bitLength() <= num;
        assert BigIntegerUtils.nonNegative(b) && b.bitLength() <= num;
        assert BigIntegerUtils.nonNegative(c) && c.bitLength() <= num;

        Z2Triple booleanTriple = new Z2Triple();
        booleanTriple.num = num;
        booleanTriple.byteNum = CommonUtils.getByteLength(num);
        booleanTriple.a = a;
        booleanTriple.b = b;
        booleanTriple.c = c;

        return booleanTriple;
    }

    /**
     * 私有构造函数。
     */
    private Z2Triple() {
        // empty
    }

    /**
     * 返回布尔三元组数量。
     *
     * @return 布尔三元组数量。
     */
    public int getNum() {
        return num;
    }

    /**
     * 返回布尔三元组字节长度。
     *
     * @return 布尔三元组字节长度。
     */
    public int getByteNum() {
        return byteNum;
    }

    /**
     * 返回随机分量a。
     *
     * @return 随机分量a。
     */
    public byte[] getA() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(a, byteNum);
    }

    /**
     * 返回随机分量a的二进制字符串表示。
     *
     * @return 随机分量a的二进制字符串表示。
     */
    public String getStringA() {
        if (num == 0) {
            return "";
        }
        StringBuilder stringA = new StringBuilder(a.toString(2));
        while (stringA.length() < num) {
            stringA.insert(0, "0");
        }
        return stringA.toString();
    }

    /**
     * 返回随机分量b。
     *
     * @return 随机分量b。
     */
    public byte[] getB() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(b, byteNum);
    }

    /**
     * 返回随机分量b的二进制字符串表示。
     *
     * @return 随机分量b的二进制字符串表示。
     */
    public String getStringB() {
        if (num == 0) {
            return "";
        }
        StringBuilder stringB = new StringBuilder(b.toString(2));
        while (stringB.length() < num) {
            stringB.insert(0, "0");
        }
        return stringB.toString();
    }

    /**
     * 返回随机分量c。
     *
     * @return 随机分量c。
     */
    public byte[] getC() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(c, byteNum);
    }

    /**
     * 返回随机分量b的二进制字符串表示。
     *
     * @return 随机分量b的二进制字符串表示。
     */
    public String getStringC() {
        if (num == 0) {
            return "";
        }
        StringBuilder stringC = new StringBuilder(c.toString(2));
        while (stringC.length() < num) {
            stringC.insert(0, "0");
        }
        return stringC.toString();
    }

    /**
     * 从布尔三元组中切分出指定长度的子布尔三元组。
     *
     * @param length 指定切分长度。
     */
    public Z2Triple split(int length) {
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]: " + length;
        // 切分方法：分别对2^length取模数和取余数，模数作为split结果，余数作为剩余结果
        BigInteger mask = BigInteger.ONE.shiftLeft(length).subtract(BigInteger.ONE);
        // 由于模数一定是2^length格式，因此可以用位运算更高效地实现
        BigInteger splitA = a.and(mask);
        BigInteger spiltB = b.and(mask);
        BigInteger splitC = c.and(mask);
        // 更新剩余的布尔三元组
        a = a.shiftRight(length);
        b = b.shiftRight(length);
        c = c.shiftRight(length);
        num = num - length;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);
        // 返回切分出的布尔三元组
        return Z2Triple.create(length, splitA, spiltB, splitC);
    }

    /**
     * 将布尔三元组长度缩减为指定长度。
     *
     * @param length 指定缩减长度。
     */
    public void reduce(int length) {
        assert length > 0 && length <= num : "reduce length must be in range (0, " + num + "]: " + length;
        if (length < num) {
            // 缩减长度，方法为原始数据与长度对应全1比特串求AND
            BigInteger mask = BigInteger.ONE.shiftLeft(length).subtract(BigInteger.ONE);
            a = a.and(mask);
            b = b.and(mask);
            c = c.and(mask);
            // 更改长度
            num = length;
            byteNum = CommonUtils.getByteLength(length);
        }
    }

    /**
     * 合并两个布尔三元组。
     *
     * @param that 另一个布尔三元组。
     */
    public void merge(Z2Triple that) {
        // 移位
        a = a.shiftLeft(that.num).add(that.a);
        b = b.shiftLeft(that.num).add(that.b);
        c = c.shiftLeft(that.num).add(that.c);
        // 更新长度
        num += that.num;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);
    }

    @Override
    public String toString() {
        return "[" + getStringA() + ", " + getStringB() + ", " + getStringC() + "] (n = " + num + ")";
    }
}
