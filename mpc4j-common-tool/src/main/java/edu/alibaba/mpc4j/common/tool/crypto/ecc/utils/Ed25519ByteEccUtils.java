package edu.alibaba.mpc4j.common.tool.crypto.ecc.utils;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.math.ec.rfc7748.X25519Field;
import org.bouncycastle.math.raw.Interleave;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat256;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Locale;

/**
 * ED25519字节椭圆曲线，所有数据使用小端表示。ED25519原理可参考下述论文：
 * <p>
 * 《深入理解 Ed25519: 原理与速度》（https://crypto-in-action.github.io/intro-ed25519/190930-ed25519-theory-speed.pdf）
 * </p>
 * ED25519实现由Bouncy Castle实现改造而来，参见：
 * <p>
 * org.bouncycastle.math.ec.rfc8032.Ed25519.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/8/27
 */
@SuppressWarnings("SuspiciousNameCombination")
public class Ed25519ByteEccUtils {
    /**
     * Curve25519有限域
     */
    private static class Curve25519Field extends X25519Field {

    }

    /**
     * 点的整数长度
     */
    private static final int POINT_INTS = 8;
    /**
     * 点的字节长度
     */
    public static final int POINT_BYTES = POINT_INTS * 4;
    /**
     * 幂指数的整数长度
     */
    private static final int SCALAR_INTS = 8;
    /**
     * 幂指数的字节长度
     */
    public static final int SCALAR_BYTES = SCALAR_INTS * 4;
    /**
     * Fp中的p = 2^{255} - 19 = 7FFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFED
     */
    private static final int[] P = new int[]{
        0xFFFFFFED, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0x7FFFFFFF
    };
    /**
     * 阶（十进制表示）l = 2^{252} + 27742317777372353535851937790883648493 =
     * 10000000 00000000 00000000 00000000 14DEF9DE A2F79CD6 5812631A 5CF5D3ED
     */
    private static final int[] L = new int[]{
        0x5CF5D3ED, 0x5812631A, 0xA2F79CD6, 0x14DEF9DE, 0x00000000, 0x00000000, 0x00000000, 0x10000000
    };

    /**
     * 无穷远点：00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001，小端表示
     */
    public static final byte[] POINT_INFINITY = new byte[]{
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    /**
     * 基点B：66666666 66666666 66666666 66666666 66666666 66666666 66666666 66666658，小端表示
     */
    public static final byte[] POINT_B = new byte[]{
        0x58, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66,
        0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66,
    };

    /**
     * 基点B的x坐标，Bx = 216936D3 CD6E53FE C0A4E231 FDD6DC5C 692CC760 9525A7B2 C9562D60 8F25D51A =
     * <p>0010 0001 0110 1001 0011 0110 1101 0011</p>
     * <p>1100 1101 0110 1110 0101 0011 1111 1110</p>
     * <p>1100 0000 1010 0100 1110 0010 0011 0001</p>
     * <p>1111 1101 1101 0110 1101 1100 0101 1100</p>
     * <p>0110 1001 0010 1100 1100 0111 0110 0000</p>
     * <p>1001 0101 0010 0101 1010 0111 1011 0010</p>
     * <p>1100 1001 0101 0110 0010 1101 0110 0000</p>
     * <p>1000 1111 0010 0101 1101 0101 0001 1010</p>
     * 为了计算方便，实现时每个int只存储一部分比特信息，即组合为：
     * <p>        0 0100 0010 1101 0010 0110 1101</p>
     * <p>       10 1001 1110 0110 1011 0111 0010</p>
     * <p>        1 0011 1111 1110 1100 0000 1010</p>
     * <p>       01 0011 1000 1000 1100 0111 1111</p>
     * <p>       01 1101 0110 1101 1100 0101 1100</p>
     * <p>        0 1101 0010 0101 1001 1000 1110</p>
     * <p>       11 0000 0100 1010 1001 0010 1101</p>
     * <p>        0 0111 1011 0010 1100 1001 0101</p>
     * <p>       01 1000 1011 0101 1000 0010 0011</p>
     * <p>       11 0010 0101 1101 0101 0001 1010</p>
     * 即：
     * <p>0042D26D 029E6B72 013FEC0A 01388C7F 01D6DC5C 00D2598E 0304A92D 007B2C95 018B5823 0325D51A</p>
     */
    private static final int[] B_X_INT_COORD = new int[]{
        0x0325D51A, 0x018B5823, 0x007B2C95, 0x0304A92D, 0x00D2598E,
        0x01D6DC5C, 0x01388C7F, 0x013FEC0A, 0x029E6B72, 0x0042D26D
    };
    /**
     * 基点B的y坐标，By = 66666666 66666666 66666666 66666666 66666666 66666666 66666666 66666658 =
     * <p>0110 0110 0110 0110 0110 0110 0110 0110</p>
     * <p>0110 0110 0110 0110 0110 0110 0110 0110</p>
     * <p>0110 0110 0110 0110 0110 0110 0110 0110</p>
     * <p>0110 0110 0110 0110 0110 0110 0110 0110</p>
     * <p>0110 0110 0110 0110 0110 0110 0110 0110</p>
     * <p>0110 0110 0110 0110 0110 0110 0110 0110</p>
     * <p>0110 0110 0110 0110 0110 0110 0110 0110</p>
     * <p>0110 0110 0110 0110 0110 0110 0101 1000</p>
     * 为了计算方便，实现时每个int只存储一部分比特信息，即组合为：
     * <p>        0 1100 1100 1100 1100 1100 1100</p>
     * <p>       11 0011 0011 0011 0011 0011 0011</p>
     * <p>        0 0110 0110 0110 0110 0110 0110</p>
     * <p>       01 1001 1001 1001 1001 1001 1001</p>
     * <p>       10 0110 0110 0110 0110 0110 0110</p>
     * <p>        0 1100 1100 1100 1100 1100 1100</p>
     * <p>       11 0011 0011 0011 0011 0011 0011</p>
     * <p>        0 0110 0110 0110 0110 0110 0110</p>
     * <p>       01 1001 1001 1001 1001 1001 1001</p>
     * <p>       10 0110 0110 0110 0110 0101 1000</p>
     * 即：
     * <p>00CCCCCC 03333333 00666666 01999999 02666666 00CCCCCC 03333333 00666666 01999999 02666658</p>
     */
    private static final int[] B_Y_INT_COORD = new int[]{
        0x02666658, 0x01999999, 0x00666666, 0x03333333, 0x00CCCCCC,
        0x02666666, 0x01999999, 0x00666666, 0x03333333, 0x00CCCCCC,
    };
    /**
     * d = -121665 / 121666 = 52036CEE 2B6FFE73 8CC74079 7779E898 00700A4D 4141D8AB 75EB4DCA 135978A3 =
     * <p>0101 0010 0000 0011 0110 1100 1110 1110</p>
     * <p>0010 1011 0110 1111 1111 1110 0111 0011</p>
     * <p>1000 1100 1100 0111 0100 0000 0111 1001</p>
     * <p>0111 0111 0111 1001 1110 1000 1001 1000</p>
     * <p>0000 0000 0111 0000 0000 1010 0100 1101</p>
     * <p>0100 0001 0100 0001 1101 1000 1010 1011</p>
     * <p>0111 0101 1110 1011 0100 1101 1100 1010</p>
     * <p>0001 0011 0101 1001 0111 1000 1010 0011</p>
     * 为了计算方便，实现时每个int只存储一部分比特信息，即组合为：
     * <p>        0 1010 0100 0000 0110 1101 1001</p>
     * <p>       11 0111 0001 0101 1011 0111 1111</p>
     * <p>        1 1110 0111 0011 1000 1100 1100</p>
     * <p>       01 1101 0000 0001 1110 0101 1101</p>
     * <p>       11 0111 1001 1110 1000 1001 1000</p>
     * <p>        0 0000 0000 1110 0000 0001 0100</p>
     * <p>       10 0110 1010 0000 1010 0000 1110</p>
     * <p>        1 1000 1010 1011 0111 0101 1110</p>
     * <p>       10 1101 0011 0111 0010 1000 0100</p>
     * <p>       11 0101 1001 0111 1000 1010 0011</p>
     * 即：00A406D9 03715B7F 01E738CC 01D01E5D 0379E898 0000E014 026A0A0E 018AB75E 02D37284 035978A3
     */
    private static final int[] C_D_INT_VALUE = new int[]{
        0x035978A3, 0x02D37284, 0x018AB75E, 0x026A0A0E, 0x0000E014,
        0x0379E898, 0x01D01E5D, 0x01E738CC, 0x03715B7F, 0x00A406D9
    };
    /**
     * 2d = 2 * -121665 / 121666 = 2406D9DC 56DFFCE7 198E80F2 EEF3D130 00E0149A 8283B156 EBD69B94 26B2F159 =
     * <p>0010 0100 0000 0110 1101 1001 1101 1100</p>
     * <p>0101 0110 1101 1111 1111 1100 1110 0111</p>
     * <p>0001 1001 1000 1110 1000 0000 1111 0010</p>
     * <p>1110 1110 1111 0011 1101 0001 0011 0000</p>
     * <p>0000 0000 1110 0000 0001 0100 1001 1010</p>
     * <p>1000 0010 1000 0011 1011 0001 0101 0110</p>
     * <p>1110 1011 1101 0110 1001 1011 1001 0100</p>
     * <p>0010 0110 1011 0010 1111 0001 0101 1001</p>
     * 为了计算方便，实现时每个int只存储一部分比特信息，即组合为：
     * <p>        0 0100 1000 0000 1101 1011 0011</p>
     * <p>       10 1110 0010 1011 0110 1111 1111</p>
     * <p>        1 1100 1110 0111 0001 1001 1000</p>
     * <p>       11 1010 0000 0011 1100 1011 1011</p>
     * <p>       10 1111 0011 1101 0001 0011 0000</p>
     * <p>        0 0000 0001 1100 0000 0010 1001</p>
     * <p>       00 1101 0100 0001 0100 0001 1101</p>
     * <p>        1 0001 0101 0110 1110 1011 1101</p>
     * <p>       01 1010 0110 1110 0101 0000 1001</p>
     * <p>       10 1011 0010 1111 0001 0101 1001</p>
     * 即：00480DB3 02E2B6FF 01CE7198 03A03CBB 02F3D130 0001C029 00D4141D 01156EBD 01A6E509 02B2F159
     */
    private static final int[] C_2D_INT_VALUE = new int[]{
        0x02B2F159, 0x01A6E509, 0x01156EBD, 0x00D4141D, 0x0001C029,
        0x02F3D130, 0x03A03CBB, 0x01CE7198, 0x02E2B6FF, 0x00480DB3
    };
    /**
     * 4d = 4 * -121665 / 121666 = 480DB3B8 ADBFF9CE 331D01E5 DDE7A260 01C02935 050762AD D7AD3728 4D65E2B2 =
     * <p>0100 1000 0000 1101 1011 0011 1011 1000</p>
     * <p>1010 1101 1011 1111 1111 1001 1100 1110</p>
     * <p>0011 0011 0001 1101 0000 0001 1110 0101</p>
     * <p>1101 1101 1110 0111 1010 0010 0110 0000</p>
     * <p>0000 0001 1100 0000 0010 1001 0011 0101</p>
     * <p>0000 0101 0000 0111 0110 0010 1010 1101</p>
     * <p>1101 0111 1010 1101 0011 0111 0010 1000</p>
     * <p>0100 1101 0110 0101 1110 0010 1011 0010</p>
     * 为了计算方便，实现时每个int只存储一部分比特信息，即组合为：
     * <p>      0 1001 0000 0001 1011 0110 0111</p>
     * <p>     01 1100 0101 0110 1101 1111 1111</p>
     * <p>      1 1001 1100 1110 0011 0011 0001</p>
     * <p>     11 0100 0000 0111 1001 0111 0111</p>
     * <p>     01 1110 0111 1010 0010 0110 0000</p>
     * <p>      0 0000 0011 1000 0000 0101 0010</p>
     * <p>     01 1010 1000 0010 1000 0011 1011</p>
     * <p>      0 0010 1010 1101 1101 0111 1010</p>
     * <p>     11 0100 1101 1100 1010 0001 0011</p>
     * <p>     01 0110 0101 1110 0010 1011 0010</p>
     * 即：00901B67 01C56DFF 019CE331 03407977 01E7A260 00380520 01A8283B 002ADD7A 034DCA13 0165E2B2
     */
    private static final int[] C_4D_INT_VALUE = new int[]{
        0x0165E2B2, 0x034DCA13, 0x002ADD7A, 0x01A8283B, 0x00038052,
        0x01E7A260, 0x03407977, 0x019CE331, 0x01C56DFF, 0x00901B67};
    /**
     * 协因子
     */
    public static final byte[] SCALAR_COFACTOR = new byte[]{
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08,
    };
    /**
     * l = 2^{252} + 27742317777372353535851937790883648493
     */
    public static final BigInteger N = BigInteger.ONE.shiftLeft(252)
        .add(new BigInteger("27742317777372353535851937790883648493"));
    /**
     * 幂指数-1
     */
    public static final byte[] NEG_SCALAR_ONE;

    static {
        BigInteger negate = BigInteger.ONE.negate().mod(N);
        NEG_SCALAR_ONE = BigIntegerUtils.nonNegBigIntegerToByteArray(negate, POINT_BYTES);
        BytesUtils.innerReverseByteArray(NEG_SCALAR_ONE);
    }

    /**
     * 将大整数表示的幂指数转换为字节数组。
     *
     * @param k 幂指数。
     * @return 转换结果。
     */
    public static byte[] toByteK(BigInteger k) {
        assert BigIntegerUtils.greaterOrEqual(k, BigInteger.ZERO) && BigIntegerUtils.less(k, N) :
            "k must be in range [0, " + N.toString().toUpperCase(Locale.ROOT) + "): " + k;
        byte[] byteK = BigIntegerUtils.nonNegBigIntegerToByteArray(k, SCALAR_BYTES);
        BytesUtils.innerReverseByteArray(byteK);
        return byteK;
    }

    /**
     * WNAF（W-ary Non-Adjacent Form）预计算参数
     */
    private static final int PRECOMP_BLOCKS = 8;
    private static final int PRECOMP_TEETH = 4;
    private static final int PRECOMP_SPACING = 8;
    private static final int PRECOMP_POINTS = 1 << (PRECOMP_TEETH - 1);
    private static final int PRECOMP_MASK = PRECOMP_POINTS - 1;
    /**
     * 基点预计算锁
     */
    private static final Object PRE_COMPUTE_BASE_LOCK = new Object();
    /**
     * 预计算基点
     */
    private static int[] precompBase = null;

    /**
     * 累计坐标表示，假定仿射表示为(x, y)，则扩展表示为(X, Y, Z, U, V)，x = X / Z, y = Y / Z, T = XY / Z, Z ≠ 0, UV = T
     */
    private static class PointAccum {
        int[] x = Curve25519Field.create();
        int[] y = Curve25519Field.create();
        int[] z = Curve25519Field.create();
        int[] u = Curve25519Field.create();
        int[] v = Curve25519Field.create();
    }

    /**
     * 仿射坐标表示，即把椭圆曲线点表示为满足-x^2 + y^2 = 1 - 121665 / 121666 * x^2 y^2的(x, y)
     */
    private static class PointAffine {
        int[] x = Curve25519Field.create();
        int[] y = Curve25519Field.create();
    }

    /**
     * 扩展坐标表示，假定仿射表示为(x, y)，则扩展表示为(X, Y, Z, T)，x = X / Z, y = Y / Z, T = XY / Z, Z ≠ 0
     */
    private static class PointExt {
        int[] x = Curve25519Field.create();
        int[] y = Curve25519Field.create();
        int[] z = Curve25519Field.create();
        int[] t = Curve25519Field.create();
    }

    /**
     * 预计算坐标表示
     */
    private static class PointPrecomp {
        int[] ypxh = Curve25519Field.create();
        int[] ymxh = Curve25519Field.create();
        int[] xyd = Curve25519Field.create();
    }

    /**
     * 检查输入的坐标是否为有效的椭圆曲线点。
     *
     * @param x 坐标x。
     * @param y 坐标y。
     * @return 如果是有效坐标，则返回非0值（可以为负数），否则返回0。
     */
    private static int checkPoint(int[] x, int[] y) {
        int[] t = Curve25519Field.create();
        int[] u = Curve25519Field.create();
        int[] v = Curve25519Field.create();
        // u = x^2
        Curve25519Field.sqr(x, u);
        // v = y^2
        Curve25519Field.sqr(y, v);
        // t = u * v = x^2 * y^2
        Curve25519Field.mul(u, v, t);
        // v = -x^2 + y^2
        Curve25519Field.sub(v, u, v);
        // t = d * t = -121665 / 121666 * x^2 * y^2
        Curve25519Field.mul(t, C_D_INT_VALUE, t);
        // t = t + 1 = 1 - 121665 / 121666 * x^2 * y^2
        Curve25519Field.addOne(t);
        // t = t - v = 1 - 121665 / 121666 * x^2 * y^2 - (-x^2 + y^2)
        Curve25519Field.sub(t, v, t);
        // 归一化后验证结果是否为0
        Curve25519Field.normalize(t);

        return Curve25519Field.isZero(t);
    }

    /**
     * 检查输入的字节数组是否为有效的Fp群元素。
     *
     * @param p 输入的字节数组。
     * @return 如果为有效Fp群元素，则返回{@code true}，否则返回{@code false}。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean checkFp(byte[] p) {
        if (p == null || p.length != POINT_BYTES) {
            return false;
        }
        int[] t = new int[POINT_INTS];
        decode32(p, t, POINT_INTS);
        // 消除最高的符号位后，验证结果小于质数p
        t[POINT_INTS - 1] &= 0x7FFFFFFF;
        return !Nat256.gte(t, P);
    }

    private static int checkPoint(int[] x, int[] y, int[] z) {
        int[] t = Curve25519Field.create();
        int[] u = Curve25519Field.create();
        int[] v = Curve25519Field.create();
        int[] w = Curve25519Field.create();

        Curve25519Field.sqr(x, u);
        Curve25519Field.sqr(y, v);
        Curve25519Field.sqr(z, w);
        Curve25519Field.mul(u, v, t);
        Curve25519Field.sub(v, u, v);
        Curve25519Field.mul(v, w, v);
        Curve25519Field.sqr(w, w);
        Curve25519Field.mul(t, C_D_INT_VALUE, t);
        Curve25519Field.add(t, w, t);
        Curve25519Field.sub(t, v, t);
        Curve25519Field.normalize(t);

        return Curve25519Field.isZero(t);
    }

    public static boolean validPoint(byte[] p) {
        if (p == null || p.length != POINT_BYTES) {
            return false;
        }
        byte[] py = BytesUtils.clone(p);
        if (!checkFp(py)) {
            return false;
        }
        int x0 = (py[POINT_BYTES - 1] & 0x80) >>> 7;
        // 恢复坐标y
        py[POINT_BYTES - 1] &= 0x7F;
        int[] pyInt = Curve25519Field.create();
        int[] pxInt = Curve25519Field.create();
        Curve25519Field.decode(py, 0, pyInt);
        // 恢复坐标x
        int[] u = Curve25519Field.create();
        int[] v = Curve25519Field.create();
        // u = y^2
        Curve25519Field.sqr(pyInt, u);
        // v = d * u = -121665 / 121666 * y^2
        Curve25519Field.mul(C_D_INT_VALUE, u, v);
        // u = y^2 - 1
        Curve25519Field.subOne(u);
        // v = 1 - 121665 / 121666 * y^2
        Curve25519Field.addOne(v);
        // 计算x
        if (!Curve25519Field.sqrtRatioVar(u, v, pxInt)) {
            return false;
        }
        // x进行归一化处理
        Curve25519Field.normalize(pxInt);
        // 不能符号位是负数，但是x的取值为0
        return x0 != 1 || !Curve25519Field.isZeroVar(pxInt);
    }

    /**
     * 将字节数组偏移量开始后的每32比特解码为1个整数，放置在给定整数数组中。
     *
     * @param bs   字节数组。
     * @param n    整数数组。
     * @param nLen 转换的整数长度。
     */
    private static void decode32(byte[] bs, int[] n, int nLen) {
        for (int i = 0; i < nLen; ++i) {
            n[i] = ByteEccUtils.decodeInt32(bs, i * Integer.BYTES);
        }
    }

    private static void decodePointVar(byte[] p, PointAffine r) {
        byte[] py = BytesUtils.clone(p);
        if (!checkFp(py)) {
            throw new IllegalArgumentException("Invalid point p = " + Hex.toHexString(p));
        }
        // 恢复坐标y
        int x0 = (py[POINT_BYTES - 1] & 0x80) >>> 7;
        py[POINT_BYTES - 1] &= 0x7F;
        Curve25519Field.decode(py, 0, r.y);
        // 恢复坐标x
        int[] u = Curve25519Field.create();
        int[] v = Curve25519Field.create();
        // u = y^2
        Curve25519Field.sqr(r.y, u);
        // v = d * u = -121665 / 121666 * y^2
        Curve25519Field.mul(C_D_INT_VALUE, u, v);
        // u = y^2 - 1
        Curve25519Field.subOne(u);
        // v = 1 - 121665 / 121666 * y^2
        Curve25519Field.addOne(v);
        // 计算x
        if (!Curve25519Field.sqrtRatioVar(u, v, r.x)) {
            throw new IllegalArgumentException("Invalid point p = " + Hex.toHexString(p));
        }
        // x进行归一化处理
        Curve25519Field.normalize(r.x);
        // 不能符号位是负数，但是x的取值为0
        if (x0 == 1 && Curve25519Field.isZeroVar(r.x)) {
            throw new IllegalArgumentException("Invalid point p = " + Hex.toHexString(p));
        }
        // 设置x的符号位
        if ((x0 != (r.x[0] & 1))) {
            Curve25519Field.negate(r.x, r.x);
        }
    }

    private static void decodeScalar(byte[] k, int[] n) {
        decode32(k, n, SCALAR_INTS);
    }

    /**
     * 编码累计点，放置在指定字节数组中。
     *
     * @param p 累积点。
     * @param r 指定字节数组。
     * @return 如果成功，则返回非0值，否则返回0.
     */
    private static int encodePoint(PointAccum p, byte[] r) {
        assert r.length == POINT_BYTES : "r.length must be equal to " + POINT_BYTES + ": " + r.length;
        int[] x = Curve25519Field.create();
        int[] y = Curve25519Field.create();

        Curve25519Field.inv(p.z, y);
        Curve25519Field.mul(p.x, y, x);
        Curve25519Field.mul(p.y, y, y);
        Curve25519Field.normalize(x);
        Curve25519Field.normalize(y);

        int result = checkPoint(x, y);

        Curve25519Field.encode(y, r, 0);
        // y的最高位永远为0，因此利用y的最高位编码点
        r[POINT_BYTES - 1] |= ((x[0] & 1) << 7);

        return result;
    }

    /**
     * 计算幂指数的WNAF表示，窗口长度固定为5。
     *
     * @param n 幂指数。
     * @return 幂指数的WNAF表示。
     */
    private static byte[] getWnafVar(int[] n) {
        assert 0 <= n[SCALAR_INTS - 1] && n[SCALAR_INTS - 1] <= L[SCALAR_INTS - 1];

        int[] t = new int[SCALAR_INTS * 2];
        {
            int tPos = t.length, c = 0;
            int i = SCALAR_INTS;
            while (--i >= 0) {
                int next = n[i];
                t[--tPos] = (next >>> 16) | (c << 16);
                t[--tPos] = c = next;
            }
        }

        byte[] ws = new byte[253];

        final int lead = 32 - 5;

        int j = 0, carry = 0;
        for (int i = 0; i < t.length; ++i, j -= 16) {
            int word = t[i];
            while (j < 16) {
                int word16 = word >>> j;
                int bit = word16 & 1;

                if (bit == carry) {
                    ++j;
                    continue;
                }

                int digit = (word16 | 1) << lead;
                carry = digit >>> 31;

                ws[(i << 4) + j] = (byte) (digit >> lead);

                j += 5;
            }
        }
        return ws;
    }

    /**
     * 计算r = r + p。
     *
     * @param r 点r。
     * @param p 点p。
     */
    public static void pointAdd(byte[] r, byte[] p) {
        PointAffine pointAffline = new PointAffine();
        decodePointVar(p, pointAffline);
        PointExt pointExt = pointCopy(pointAffline);
        PointAffine resultAffline = new PointAffine();
        decodePointVar(r, resultAffline);
        PointAccum resultAccum = new PointAccum();
        pointCopy(resultAffline, resultAccum);
        pointAdd(pointExt, resultAccum);
        encodePoint(resultAccum, r);
    }

    /**
     * 计算r = p + r。
     *
     * @param p 扩展点p。
     * @param r 累积点r。
     */
    private static void pointAdd(PointExt p, PointAccum r) {
        int[] a = Curve25519Field.create();
        int[] b = Curve25519Field.create();
        int[] c = Curve25519Field.create();
        int[] d = Curve25519Field.create();
        int[] e = r.u;
        int[] f = Curve25519Field.create();
        int[] g = Curve25519Field.create();
        int[] h = r.v;

        Curve25519Field.apm(r.y, r.x, b, a);
        Curve25519Field.apm(p.y, p.x, d, c);
        Curve25519Field.mul(a, c, a);
        Curve25519Field.mul(b, d, b);
        Curve25519Field.mul(r.u, r.v, c);
        Curve25519Field.mul(c, p.t, c);
        Curve25519Field.mul(c, C_2D_INT_VALUE, c);
        Curve25519Field.mul(r.z, p.z, d);
        Curve25519Field.add(d, d, d);
        Curve25519Field.apm(b, a, h, e);
        Curve25519Field.apm(d, c, g, f);
        Curve25519Field.carry(g);
        Curve25519Field.mul(e, f, r.x);
        Curve25519Field.mul(g, h, r.y);
        Curve25519Field.mul(f, g, r.z);
    }

    /**
     * 计算r = -r。
     *
     * @param r 点r。
     */
    public static void pointNegate(byte[] r) {
        r[POINT_BYTES - 1] ^= (1 << 7);
    }

    /**
     * 计算r = r - p。
     *
     * @param p 点p。
     * @param r 点r。
     */
    public static void pointSubtract(byte[] r, byte[] p) {
        PointAffine pointAffline = new PointAffine();
        decodePointVar(p, pointAffline);
        PointExt pointExt = pointCopy(pointAffline);
        PointAffine resultAffline = new PointAffine();
        decodePointVar(r, resultAffline);
        PointAccum resultAccum = new PointAccum();
        pointCopy(resultAffline, resultAccum);
        pointSubtract(pointExt, resultAccum);
        encodePoint(resultAccum, r);
    }

    private static void pointSubtract(PointExt p, PointAccum r) {
        int[] a = Curve25519Field.create();
        int[] b = Curve25519Field.create();
        int[] c = Curve25519Field.create();
        int[] d = Curve25519Field.create();
        int[] e = r.u;
        int[] f = Curve25519Field.create();
        int[] g = Curve25519Field.create();
        int[] h = r.v;

        Curve25519Field.apm(r.y, r.x, b, a);
        Curve25519Field.apm(p.y, p.x, c, d);
        Curve25519Field.mul(a, c, a);
        Curve25519Field.mul(b, d, b);
        Curve25519Field.mul(r.u, r.v, c);
        Curve25519Field.mul(c, p.t, c);
        Curve25519Field.mul(c, C_2D_INT_VALUE, c);
        Curve25519Field.mul(r.z, p.z, d);
        Curve25519Field.add(d, d, d);
        Curve25519Field.apm(b, a, h, e);
        Curve25519Field.apm(d, c, f, g);
        Curve25519Field.carry(g);
        Curve25519Field.mul(e, f, r.x);
        Curve25519Field.mul(g, h, r.y);
        Curve25519Field.mul(f, g, r.z);
    }

    /**
     * 计算r = r + p。
     *
     * @param negate q是否取反（如果为{@code true}，则计算的是r = r - p。
     * @param p      扩展点p。
     * @param r      扩展点r。
     */
    private static void pointAddVar(boolean negate, PointExt p, PointAccum r) {
        int[] a = Curve25519Field.create();
        int[] b = Curve25519Field.create();
        int[] c = Curve25519Field.create();
        int[] d = Curve25519Field.create();
        int[] e = r.u;
        int[] f = Curve25519Field.create();
        int[] g = Curve25519Field.create();
        int[] h = r.v;

        int[] nc, nd, nf, ng;
        if (negate) {
            nc = d;
            nd = c;
            nf = g;
            ng = f;
        } else {
            nc = c;
            nd = d;
            nf = f;
            ng = g;
        }

        Curve25519Field.apm(r.y, r.x, b, a);
        Curve25519Field.apm(p.y, p.x, nd, nc);
        Curve25519Field.mul(a, c, a);
        Curve25519Field.mul(b, d, b);
        Curve25519Field.mul(r.u, r.v, c);
        Curve25519Field.mul(c, p.t, c);
        Curve25519Field.mul(c, C_2D_INT_VALUE, c);
        Curve25519Field.mul(r.z, p.z, d);
        Curve25519Field.add(d, d, d);
        Curve25519Field.apm(b, a, h, e);
        Curve25519Field.apm(d, c, ng, nf);
        Curve25519Field.carry(ng);
        Curve25519Field.mul(e, f, r.x);
        Curve25519Field.mul(g, h, r.y);
        Curve25519Field.mul(f, g, r.z);
    }

    /**
     * 计算r = p + q。
     *
     * @param negate q是否取反（如果为{@code true}，则计算的是r = p - q。
     * @param p      扩展点p。
     * @param q      扩展点q。
     * @param r      扩展点r。
     */
    private static void pointAddVar(boolean negate, PointExt p, PointExt q, PointExt r) {
        int[] a = Curve25519Field.create();
        int[] b = Curve25519Field.create();
        int[] c = Curve25519Field.create();
        int[] d = Curve25519Field.create();
        int[] e = Curve25519Field.create();
        int[] f = Curve25519Field.create();
        int[] g = Curve25519Field.create();
        int[] h = Curve25519Field.create();

        int[] nc, nd, nf, ng;
        if (negate) {
            nc = d;
            nd = c;
            nf = g;
            ng = f;
        } else {
            nc = c;
            nd = d;
            nf = f;
            ng = g;
        }

        Curve25519Field.apm(p.y, p.x, b, a);
        Curve25519Field.apm(q.y, q.x, nd, nc);
        Curve25519Field.mul(a, c, a);
        Curve25519Field.mul(b, d, b);
        Curve25519Field.mul(p.t, q.t, c);
        Curve25519Field.mul(c, C_2D_INT_VALUE, c);
        Curve25519Field.mul(p.z, q.z, d);
        Curve25519Field.add(d, d, d);
        Curve25519Field.apm(b, a, h, e);
        Curve25519Field.apm(d, c, ng, nf);
        Curve25519Field.carry(ng);
        Curve25519Field.mul(e, f, r.x);
        Curve25519Field.mul(g, h, r.y);
        Curve25519Field.mul(f, g, r.z);
        Curve25519Field.mul(e, h, r.t);
    }

    /**
     * r = r + p，其中p为预计算点。
     *
     * @param p 预计算点p。
     * @param r 计算结果r。
     */
    private static void pointAddPrecomp(PointPrecomp p, PointAccum r) {
        int[] a = Curve25519Field.create();
        int[] b = Curve25519Field.create();
        int[] c = Curve25519Field.create();
        int[] e = r.u;
        int[] f = Curve25519Field.create();
        int[] g = Curve25519Field.create();
        int[] h = r.v;

        Curve25519Field.apm(r.y, r.x, b, a);
        Curve25519Field.mul(a, p.ymxh, a);
        Curve25519Field.mul(b, p.ypxh, b);
        Curve25519Field.mul(r.u, r.v, c);
        Curve25519Field.mul(c, p.xyd, c);
        Curve25519Field.apm(b, a, h, e);
        Curve25519Field.apm(r.z, c, g, f);
        Curve25519Field.carry(g);
        Curve25519Field.mul(e, f, r.x);
        Curve25519Field.mul(g, h, r.y);
        Curve25519Field.mul(f, g, r.z);
    }

    private static PointExt pointCopy(PointAccum p) {
        PointExt r = new PointExt();
        Curve25519Field.copy(p.x, 0, r.x, 0);
        Curve25519Field.copy(p.y, 0, r.y, 0);
        Curve25519Field.copy(p.z, 0, r.z, 0);
        Curve25519Field.mul(p.u, p.v, r.t);
        return r;
    }

    private static PointExt pointCopy(PointAffine p) {
        PointExt r = new PointExt();
        Curve25519Field.copy(p.x, 0, r.x, 0);
        Curve25519Field.copy(p.y, 0, r.y, 0);
        pointExtendXY(r);
        return r;
    }

    private static PointExt pointCopy(PointExt p) {
        PointExt r = new PointExt();
        pointCopy(p, r);
        return r;
    }

    private static void pointCopy(PointAffine p, PointAccum r) {
        Curve25519Field.copy(p.x, 0, r.x, 0);
        Curve25519Field.copy(p.y, 0, r.y, 0);
        pointExtendXY(r);
    }

    private static void pointCopy(PointExt p, PointExt r) {
        Curve25519Field.copy(p.x, 0, r.x, 0);
        Curve25519Field.copy(p.y, 0, r.y, 0);
        Curve25519Field.copy(p.z, 0, r.z, 0);
        Curve25519Field.copy(p.t, 0, r.t, 0);
    }

    private static void pointDouble(PointAccum r) {
        int[] a = Curve25519Field.create();
        int[] b = Curve25519Field.create();
        int[] c = Curve25519Field.create();
        int[] e = r.u;
        int[] f = Curve25519Field.create();
        int[] g = Curve25519Field.create();
        int[] h = r.v;

        Curve25519Field.sqr(r.x, a);
        Curve25519Field.sqr(r.y, b);
        Curve25519Field.sqr(r.z, c);
        Curve25519Field.add(c, c, c);
        Curve25519Field.apm(a, b, h, g);
        Curve25519Field.add(r.x, r.y, e);
        Curve25519Field.sqr(e, e);
        Curve25519Field.sub(h, e, e);
        Curve25519Field.add(c, g, f);
        Curve25519Field.carry(f);
        Curve25519Field.mul(e, f, r.x);
        Curve25519Field.mul(g, h, r.y);
        Curve25519Field.mul(f, g, r.z);
    }

    /**
     * 将只设置好(x, y)的累积点扩充为累积点。
     *
     * @param p 只设置好(x, y)的累积点。
     */
    private static void pointExtendXY(PointAccum p) {
        // z = 1
        Curve25519Field.one(p.z);
        // u = x, v = y，这样可以满足uv = t = xy
        Curve25519Field.copy(p.x, 0, p.u, 0);
        Curve25519Field.copy(p.y, 0, p.v, 0);
    }

    /**
     * 将只设置好(x, y)的扩展点扩充为扩展点。
     *
     * @param p 只设置好(x, y)的扩展点。
     */
    private static void pointExtendXY(PointExt p) {
        // z = 1
        Curve25519Field.one(p.z);
        // t = xy
        Curve25519Field.mul(p.x, p.y, p.t);
    }

    /**
     * 计算YZ。
     *
     * @param k 幂指数k。
     * @param y 坐标y。
     * @param z 坐标z。
     */
    static void scalarMultBaseYZ(byte[] k, int[] y, int[] z) {
        PointAccum p = new PointAccum();
        scalarBaseMul(k, p);
        if (0 == checkPoint(p.x, p.y, p.z)) {
            throw new IllegalStateException();
        }
        Curve25519Field.copy(p.y, 0, y, 0);
        Curve25519Field.copy(p.z, 0, z, 0);
    }

    private static void pointLookup(int block, int index, PointPrecomp p) {
        int off = block * PRECOMP_POINTS * 3 * Curve25519Field.SIZE;

        for (int i = 0; i < PRECOMP_POINTS; ++i) {
            int cond = ((i ^ index) - 1) >> 31;
            Curve25519Field.cmov(cond, precompBase, off, p.ypxh, 0);
            off += Curve25519Field.SIZE;
            Curve25519Field.cmov(cond, precompBase, off, p.ymxh, 0);
            off += Curve25519Field.SIZE;
            Curve25519Field.cmov(cond, precompBase, off, p.xyd, 0);
            off += Curve25519Field.SIZE;
        }
    }

    /**
     * 预计算扩展点p，窗口长度固定为8。
     *
     * @param p 扩展点p。
     * @return 预计算结果。
     */
    private static PointExt[] pointPrecomputeVar(PointExt p) {
        // d = 2p
        PointExt d = new PointExt();
        pointAddVar(false, p, p, d);
        PointExt[] table = new PointExt[8];
        // 将查找表第1个点设置为p
        table[0] = pointCopy(p);
        // 后续查找表每一个元素为前一个查找表元素加上d，即查找表格式为[p, 3p, 5p, 7p, 9p, 11p, 13p, 15p]
        for (int i = 1; i < 8; ++i) {
            pointAddVar(false, table[i - 1], d, table[i] = new PointExt());
        }
        return table;
    }

    /**
     * 设置无穷远点。
     *
     * @param p 设置结果。
     */
    private static void pointSetNeutral(PointAccum p) {
        // 无穷远点为(x, y) = (0, 1)
        Curve25519Field.zero(p.x);
        Curve25519Field.one(p.y);
        // z = 1
        Curve25519Field.one(p.z);
        // t = x * y = 0, u * v = t，设置为u = 0, v = 1
        Curve25519Field.zero(p.u);
        Curve25519Field.one(p.v);
    }

    /**
     * 设置无穷远点。
     *
     * @param p 设置结果。
     */
    private static void pointSetNeutral(PointExt p) {
        // 无穷远点为(x, y) = (0, 1)
        Curve25519Field.zero(p.x);
        Curve25519Field.one(p.y);
        // z = 1
        Curve25519Field.one(p.z);
        // t = x * y = 0
        Curve25519Field.zero(p.t);
    }

    /**
     * 预计算基点。
     */
    public static void precomputeBase() {
        synchronized (PRE_COMPUTE_BASE_LOCK) {
            if (precompBase != null) {
                return;
            }
            // Precomputed table for the base point in verification ladder
            {
                PointExt b = new PointExt();
                Curve25519Field.copy(B_X_INT_COORD, 0, b.x, 0);
                Curve25519Field.copy(B_Y_INT_COORD, 0, b.y, 0);
                pointExtendXY(b);
            }
            PointAccum p = new PointAccum();
            Curve25519Field.copy(B_X_INT_COORD, 0, p.x, 0);
            Curve25519Field.copy(B_Y_INT_COORD, 0, p.y, 0);
            pointExtendXY(p);

            precompBase = Curve25519Field.createTable(PRECOMP_BLOCKS * PRECOMP_POINTS * 3);

            int off = 0;
            for (int b = 0; b < PRECOMP_BLOCKS; ++b) {
                PointExt[] ds = new PointExt[PRECOMP_TEETH];

                PointExt sum = new PointExt();
                pointSetNeutral(sum);

                for (int t = 0; t < PRECOMP_TEETH; ++t) {
                    PointExt q = pointCopy(p);
                    pointAddVar(true, sum, q, sum);
                    pointDouble(p);

                    ds[t] = pointCopy(p);

                    if (b + t != PRECOMP_BLOCKS + PRECOMP_TEETH - 2) {
                        for (int s = 1; s < PRECOMP_SPACING; ++s) {
                            pointDouble(p);
                        }
                    }
                }

                PointExt[] points = new PointExt[PRECOMP_POINTS];
                int k = 0;
                points[k++] = sum;

                for (int t = 0; t < (PRECOMP_TEETH - 1); ++t) {
                    int size = 1 << t;
                    for (int j = 0; j < size; ++j, ++k) {
                        pointAddVar(false, points[k - size], ds[t], points[k] = new PointExt());
                    }
                }

                int[] cs = Curve25519Field.createTable(PRECOMP_POINTS);

                {
                    int[] u = Curve25519Field.create();
                    Curve25519Field.copy(points[0].z, 0, u, 0);
                    Curve25519Field.copy(u, 0, cs, 0);

                    int i = 0;
                    while (++i < PRECOMP_POINTS) {
                        Curve25519Field.mul(u, points[i].z, u);
                        Curve25519Field.copy(u, 0, cs, i * Curve25519Field.SIZE);
                    }

                    Curve25519Field.add(u, u, u);
                    Curve25519Field.invVar(u, u);
                    --i;

                    int[] t = Curve25519Field.create();

                    while (i > 0) {
                        int j = i--;
                        Curve25519Field.copy(cs, i * Curve25519Field.SIZE, t, 0);
                        Curve25519Field.mul(t, u, t);
                        Curve25519Field.copy(t, 0, cs, j * Curve25519Field.SIZE);
                        Curve25519Field.mul(u, points[j].z, u);
                    }

                    Curve25519Field.copy(u, 0, cs, 0);
                }

                for (int i = 0; i < PRECOMP_POINTS; ++i) {
                    PointExt q = points[i];

                    int[] x = Curve25519Field.create();
                    int[] y = Curve25519Field.create();

                    Curve25519Field.copy(cs, i * Curve25519Field.SIZE, y, 0);

                    Curve25519Field.mul(q.x, y, x);
                    Curve25519Field.mul(q.y, y, y);

                    PointPrecomp r = new PointPrecomp();
                    Curve25519Field.apm(y, x, r.ypxh, r.ymxh);
                    Curve25519Field.mul(x, y, r.xyd);
                    Curve25519Field.mul(r.xyd, C_4D_INT_VALUE, r.xyd);

                    Curve25519Field.normalize(r.ypxh);
                    Curve25519Field.normalize(r.ymxh);

                    Curve25519Field.copy(r.ypxh, 0, precompBase, off);
                    off += Curve25519Field.SIZE;
                    Curve25519Field.copy(r.ymxh, 0, precompBase, off);
                    off += Curve25519Field.SIZE;
                    Curve25519Field.copy(r.xyd, 0, precompBase, off);
                    off += Curve25519Field.SIZE;
                }
            }
        }
    }

    /**
     * 计算k·P。
     *
     * @param k 幂指数k。
     * @param p 点p。
     * @param r 计算结果。
     */
    private static void scalarMul(byte[] k, PointAffine p, PointAccum r) {
        int[] nk = new int[SCALAR_INTS];
        decodeScalar(k, nk);
        // 将幂指数展开为WNAF格式
        byte[] wnafp = getWnafVar(nk);
        // 预计算点p，构建查找表
        PointExt[] tp = pointPrecomputeVar(pointCopy(p));
        // 将结果初始化为无穷远点
        pointSetNeutral(r);
        // 注意阶是l = 2^252 + 27742317777372353535851937790883648493，因此只需要循环253比特
        for (int bit = 252; ; ) {
            int wp = wnafp[bit];
            if (wp != 0) {
                int sign = wp >> 31;
                int index = (wp ^ sign) >>> 1;

                pointAddVar((sign != 0), tp[index], r);
            }

            if (--bit < 0) {
                break;
            }

            pointDouble(r);
        }
    }

    /**
     * 计算k·P。
     *
     * @param k 幂指数k。
     * @param p 点p。
     * @param r 计算结果。
     */
    public static void scalarMulEncoded(byte[] k, byte[] p, byte[] r) {
        // 解码点
        PointAffine pointAffline = new PointAffine();
        decodePointVar(p, pointAffline);
        // 计算结果
        PointAccum result = new PointAccum();
        scalarMul(k, pointAffline, result);
        // 编码点
        if (0 == encodePoint(result, r)) {
            throw new IllegalStateException();
        }
    }

    /**
     * 计算k·G。
     *
     * @param k 幂指数k。
     * @param r 计算结果。
     */
    private static void scalarBaseMul(byte[] k, PointAccum r) {
        precomputeBase();

        int[] n = new int[SCALAR_INTS];
        decodeScalar(k, n);
        // Recode the scalar into signed-digit form, then group comb bits in each block
        {
            //int c1 =
            Nat.cadd(SCALAR_INTS, ~n[0] & 1, n, L, n);
            //int c2 =
            Nat.shiftDownBit(SCALAR_INTS, n, 1);

            for (int i = 0; i < SCALAR_INTS; ++i) {
                n[i] = Interleave.shuffle2(n[i]);
            }
        }

        PointPrecomp p = new PointPrecomp();

        pointSetNeutral(r);

        int cOff = (PRECOMP_SPACING - 1) * PRECOMP_TEETH;
        for (; ; ) {
            for (int b = 0; b < PRECOMP_BLOCKS; ++b) {
                int w = n[b] >>> cOff;
                int sign = (w >>> (PRECOMP_TEETH - 1)) & 1;
                int abs = (w ^ -sign) & PRECOMP_MASK;

                pointLookup(b, abs, p);

                Curve25519Field.cswap(sign, p.ypxh, p.ymxh);
                Curve25519Field.cnegate(sign, p.xyd);

                pointAddPrecomp(p, r);
            }

            if ((cOff -= PRECOMP_TEETH) < 0) {
                break;
            }

            pointDouble(r);
        }
    }

    /**
     * 计算k·G。
     *
     * @param k 幂指数k。
     * @param r 计算结果。
     */
    public static void scalarBaseMulEncoded(byte[] k, byte[] r) {
        PointAccum p = new PointAccum();
        scalarBaseMul(k, p);
        if (0 == encodePoint(p, r)) {
            throw new IllegalStateException();
        }
    }
}
