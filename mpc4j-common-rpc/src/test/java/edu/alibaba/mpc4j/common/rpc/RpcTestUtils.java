package edu.alibaba.mpc4j.common.rpc;

import java.math.BigInteger;

/**
 * RPC测试工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class RpcTestUtils {

    private RpcTestUtils() {
        // empty
    }

    /**
     * 空数据包
     */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    /**
     * 整数
     */
    public static final int[] INT_ARRAY = new int[] {
        -1, 0, 1, Integer.MIN_VALUE, Integer.MAX_VALUE,
    };
    /**
     * 浮点数
     */
    public static final double[] DOUBLE_ARRAY = new double[] {
        -1.0, 0.0, 1.0, Double.MIN_VALUE, Double.MAX_VALUE,
    };
    /**
     * 大整数
     */
    public static final BigInteger[] BIGINTEGER_ARRAY = new BigInteger[] {
        // -1
        BigInteger.ONE.negate(),
        // 0
        BigInteger.ZERO,
        // 1
        BigInteger.ONE,
        // long最小值
        BigInteger.valueOf(Long.MIN_VALUE),
        // long最大值
        BigInteger.valueOf(Long.MAX_VALUE),
    };
    /**
     * 字节数组
     */
    public static final byte[][] BYTES_ARRAY = new byte[][] {
        new byte[] {(byte)0x01,},
        new byte[] {(byte)0x11,},
        new byte[] {(byte)0xAA,},
        new byte[] {(byte)0xBB,},
        new byte[] {(byte)0xCC,},
        new byte[] {(byte)0xDD,},
        new byte[] {(byte)0xEE,},
        new byte[] {(byte)0xFF,},
        new byte[] {(byte)0x11, (byte)0x11,},
        new byte[] {(byte)0x22, (byte)0x22, (byte)0x22,},
        new byte[] {(byte)0x33, (byte)0x33, (byte)0x33,},
        new byte[] {(byte)0x44, (byte)0x44, (byte)0x44, (byte)0x44,},
        new byte[] {(byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55,},
        new byte[] {(byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66, (byte)0x66,},
        new byte[] {(byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77, (byte)0x77,},
        new byte[] {(byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88, (byte)0x88,},
        new byte[] {(byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99, (byte)0x99,},
        new byte[] {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA,},
        new byte[] {(byte)0xBB, (byte)0xBB, (byte)0xBB, (byte)0xBB, (byte)0xBB, (byte)0xBB,},
        new byte[] {(byte)0xCC, (byte)0xCC, (byte)0xCC, (byte)0xCC, (byte)0xCC,},
        new byte[] {(byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,},
        new byte[] {(byte)0xEE, (byte)0xEE, (byte)0xEE,},
        new byte[] {(byte)0xFF, (byte)0xFF,},
    };
}
