package edu.alibaba.mpc4j.common.tool.crypto.ecc.utils;

/**
 * 字节椭圆曲线工具类。
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
public final class ByteEccUtils {

    private ByteEccUtils() {
        // empty
    }

    public static long decodeLong24(byte[] in, int offset) {
        int result = in[offset++] & 0xff;
        result |= (in[offset++] & 0xff) << 8;
        result |= (in[offset] & 0xff) << 16;
        return ((long) result) & 0xffffffffL;
    }

    public static int decodeInt32(byte[] bs, int off) {
        int n = bs[off] & 0xFF;
        n |= (bs[++off] & 0xFF) << 8;
        n |= (bs[++off] & 0xFF) << 16;
        n |= bs[++off] << 24;
        return n;
    }

    public static long decodeLong32(byte[] in, int offset) {
        int result = in[offset++] & 0xff;
        result |= (in[offset++] & 0xff) << 8;
        result |= (in[offset++] & 0xff) << 16;
        result |= in[offset] << 24;
        return ((long) result) & 0xffffffffL;
    }
}
