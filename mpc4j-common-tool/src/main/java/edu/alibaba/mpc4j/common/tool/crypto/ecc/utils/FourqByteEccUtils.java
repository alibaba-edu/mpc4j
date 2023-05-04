package edu.alibaba.mpc4j.common.tool.crypto.ecc.utils;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.math.BigInteger;
import java.util.Locale;

/**
 * FourQ byte ecc utilities.
 *
 * @author Qixian Zhou
 * @date 2023/4/6
 */
public class FourqByteEccUtils {
    /**
     * 点的整数长度, FourQ中，完整点用(x, y): 64-byte来表示，其中x, y ∈ F_p^2, p = 2^127 - 1。
     * x = a + bi, y = a + bi, a, b \in F_p，所以x需要32-byte，y需要32-byte。我们这里的实现均考虑点的压缩表示，即32-byte。
     */
    private static final int POINT_INTS = 8;
    /**
     * 点的字节长度
     */
    public static final int POINT_BYTES = POINT_INTS * 4;
    /**
     * 幂指数的整数长度, 参考FourQlib中FourQ_params.h中curve_order的大小是32-byte。
     */
    private static final int SCALAR_INTS = 8;
    /**
     * 幂指数的字节长度
     */
    public static final int SCALAR_BYTES = SCALAR_INTS * 4;

    /**
     * Order of FourQ, Reference the FourQ_params.h in FourQlib:
     * static const uint64_t curve_order[4] = { 0x2FB2540EC7768CE7, 0xDFBD004DFE0F7999, 0xF05397829CBC14E5, 0x0029CBC14E5E0A72 };
     * Not that this is Little Endian format. So we can compute:
     * curve_order[0] + curve_order[1]*(2**64) + curve_order[0] + curve_order[2]*((2**64)**2) + curve_order[3]*((2**64)**3)
     * to get the Order.
     */
    public static final BigInteger N = new BigInteger("73846995687063900142583536357581573884798075859800097461294096333596429543");

    /**
     * BasePoint
     * 参考 FourQ_params.h 中的
     * static const uint64_t GENERATOR_x[4] = { 0x286592AD7B3833AA, 0x1A3472237C2FB305, 0x96869FB360AC77F6, 0x1E1F553F2878AA9C };
     * static const uint64_t GENERATOR_y[4] = { 0xB924A2462BCBB287, 0x0E3FEE9BA120785A, 0x49A7C344844C8B5C, 0x6E1C4AF8630E0242 };
     * 这里是在 encode 后的结果
     */
    public static final byte[] POINT_B = new byte[]{
        (byte) 0x87, (byte) 0xb2, (byte) 0xcb, (byte) 0x2b, (byte) 0x46, (byte) 0xa2, (byte) 0x24, (byte) 0xb9,
        (byte) 0x5a, (byte) 0x78, (byte) 0x20, (byte) 0xa1, (byte) 0x9b, (byte) 0xee, (byte) 0x3f, (byte) 0x0e,
        (byte) 0x5c, (byte) 0x8b, (byte) 0x4c, (byte) 0x84, (byte) 0x44, (byte) 0xc3, (byte) 0xa7, (byte) 0x49,
        (byte) 0x42, (byte) 0x02, (byte) 0x0e, (byte) 0x63, (byte) 0xf8, (byte) 0x4a, (byte) 0x1c, (byte) 0x6e,
    };

    /**
     * 无穷远点 x:(0, 0) y:(1, 0) 这里压缩表示为32-bytes
     */
    public static final byte[] POINT_INFINITY = new byte[]{
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    /**
     * Convert k to byte[], little-endian representation.
     *
     * @param k k.
     * @return k in bytes.
     */
    public static byte[] toByteK(BigInteger k) {
        assert BigIntegerUtils.greaterOrEqual(k, BigInteger.ZERO) && BigIntegerUtils.less(k, N) :
            "k must be in range [0, " + N.toString().toUpperCase(Locale.ROOT) + "): " + k;
        byte[] byteK = BigIntegerUtils.nonNegBigIntegerToByteArray(k, SCALAR_BYTES);
        BytesUtils.innerReverseByteArray(byteK);
        return byteK;
    }
}
