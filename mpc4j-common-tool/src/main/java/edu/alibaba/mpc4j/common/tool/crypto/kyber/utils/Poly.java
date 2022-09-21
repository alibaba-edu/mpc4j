package edu.alibaba.mpc4j.common.tool.crypto.kyber.utils;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.engine.KyberEngineHelper;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberParams;

import java.util.Arrays;

/**
 * Polynomial and Polynomial Vector Utility class. Modified from:
 * <p>
 * https://github.com/fisherstevenk/kyberJCE/blob/main/src/main/java/com/swiftcryptollc/crypto/provider/kyber/Poly.java
 * </p>
 * The modification is for removing unnecessary import packages.
 *
 * @author Steven K Fisher, Sheng Hu, Weiran Liu
 */
public final class Poly {
    /**
     * Apply the conditional subtraction of Q (KyberParams) to each coefficient of a polynomial.
     *
     * @param poly polynomial.
     */
    public static void inPolyConditionalSubQ(short[] poly) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            poly[i] = ByteOps.conditionalSubQ(poly[i]);
        }
    }

    /**
     * Applies the conditional subtraction of Q (KyberParams) to each coefficient of each element of a vector of
     * polynomials.
     *
     * @param polyVector polynomial vector.
     */
    public static void inPolyVectorCoefficientSubQ(short[][] polyVector) {
        for (short[] r : polyVector) {
            Poly.inPolyConditionalSubQ(r);
        }
    }

    /**
     * Add two polynomials. The result is set in the input polynomial A.
     *
     * @param polyA polynomial A.
     * @param polyB polynomial B.
     */
    public static void inPolyAdd(short[] polyA, short[] polyB) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            polyA[i] = (short) (polyA[i] + polyB[i]);
        }
    }

    /**
     * Add two polynomial vectors. The result is set in the input polynomial vector A.
     *
     * @param polyVectorA polynomial vector A.
     * @param polyVectorB polynomial vector B.
     */
    public static void inPolyVectorAdd(short[][] polyVectorA, short[][] polyVectorB) {
        for (int i = 0; i < polyVectorA.length; i++) {
            Poly.inPolyAdd(polyVectorA[i], polyVectorB[i]);
        }
    }

    /**
     * Subtract two polynomials. The result is a newly created polynomial.
     *
     * @param polyA polynomial A.
     * @param polyB polynomial B.
     * @return Subtract polynomial.
     */
    public static short[] polySub(short[] polyA, short[] polyB) {
        short[] polyC = new short[polyA.length];
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            polyC[i] = (short) (polyA[i] - polyB[i]);
        }
        return polyC;
    }

    /**
     * Subtract two polynomials. The result is set in the input polynomial A.
     *
     * @param polyA polynomial A.
     * @param polyB polynomial B.
     */
    public static void inPolySub(short[] polyA, short[] polyB) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            polyA[i] = (short) (polyA[i] - polyB[i]);
        }
    }

    /**
     * Subtract two polynomial vectors. The result is set in the input polynomial vector A.
     *
     * @param polyVectorA polynomial vector A.
     * @param polyVectorB polynomial vector B.
     */
    public static void inPolyVectorSub(short[][] polyVectorA, short[][] polyVectorB) {
        for (int i = 0; i < polyVectorA.length; i++) {
            Poly.inPolySub(polyVectorA[i], polyVectorB[i]);
        }
    }

    /**
     * Performs an in-place conversion of all coefficients of a polynomial from the normal domain to the Montgomery domain.
     *
     * @param poly polynomial.
     */
    public static void inPolyToMontgomery(short[] poly) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            poly[i] = ByteOps.montgomeryReduce(poly[i] * 1353);
        }
    }

    /**
     * Apply Barrett reduction to all coefficients of this polynomial.
     *
     * @param poly polynomial.
     */
    public static void inPolyBarrettReduce(short[] poly) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            poly[i] = ByteOps.barrettReduce(poly[i]);
        }
    }

    /**
     * Applies Barrett reduction to each coefficient of each element of a vector
     * of polynomials.
     *
     * @param polyVector polynomial vector.
     */
    public static void inPolyVectorBarrettReduce(short[][] polyVector) {
        for (short[] r : polyVector) {
            Poly.inPolyBarrettReduce(r);
        }
    }

    /**
     * Serialize a polynomial in to a byte array.
     *
     * @param poly polynomial.
     * @return serialized polynomial.
     */
    public static byte[] polyToByteArray(short[] poly) {
        int t0, t1;
        byte[] r = new byte[KyberParams.POLY_BYTES];
        Poly.inPolyConditionalSubQ(poly);
        for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_TWO; i++) {
            t0 = (poly[2 * i] & 0xFFFF);
            t1 = ((int) (poly[2 * i + 1]) & 0xFFFF);
            r[3 * i] = (byte) (t0);
            r[3 * i + 1] = (byte) ((t0 >> 8) | (t1 << 4));
            r[3 * i + 2] = (byte) (t1 >> 4);
        }
        return r;
    }

    /**
     * Serialize a polynomial vector to a byte array.
     *
     * @param polyVector polynomial vector.
     * @return serialized polynomial vector.
     */
    public static byte[] polyVectorToByteArray(short[][] polyVector) {
        int polyNum = polyVector.length;
        byte[] r = new byte[polyNum * KyberParams.POLY_BYTES];
        for (int i = 0; i < polyNum; i++) {
            byte[] byteA = polyToByteArray(polyVector[i]);
            System.arraycopy(byteA, 0, r, i * KyberParams.POLY_BYTES, byteA.length);
        }
        return r;
    }

    /**
     * De-serialize a byte array into a polynomial.
     *
     * @param polyByteArray serialized polynomial.
     * @return polynomial.
     */
    public static short[] polyFromByteArray(byte[] polyByteArray) {
        short[] r = new short[KyberParams.PARAMS_N];
        for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_TWO; i++) {
            r[2 * i] = (short) ((((polyByteArray[3 * i] & 0xFF)) | ((polyByteArray[3 * i + 1] & 0xFF) << 8)) & 0xFFF);
            r[2 * i + 1] = (short) ((((polyByteArray[3 * i + 1] & 0xFF) >> 4) | ((polyByteArray[3 * i + 2] & 0xFF) << 4)) & 0xFFF);
        }
        return r;
    }

    /**
     * Deserialize a byte array into a polynomial vector.
     *
     * @param polyVectorByteArray serialized polynomial vector.
     * @return polynomial vector.
     */
    public static short[][] polyVectorFromBytes(byte[] polyVectorByteArray) {
        int polyNum = polyVectorByteArray.length / KyberParams.POLY_BYTES;
        short[][] r = new short[polyNum][KyberParams.POLY_BYTES];
        for (int i = 0; i < polyNum; i++) {
            int start = (i * KyberParams.POLY_BYTES);
            int end = (i + 1) * KyberParams.POLY_BYTES;
            r[i] = Poly.polyFromByteArray(Arrays.copyOfRange(polyVectorByteArray, start, end));
        }
        return r;
    }

    /**
     * Convert a 32-byte message to a polynomial. Note that the message byte length can be greater than 32, but only
     * first 32-byte message is converted to a polynomial.
     *
     * @param message message.
     * @return polynomial.
     */
    public static short[] polyFromMessage(byte[] message) {
        short[] poly = new short[KyberParams.PARAMS_N];
        short mask;
        for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_EIGHT; i++) {
            for (int j = 0; j < KyberParams.MATH_EIGHT; j++) {
                // 这里乘-1，是为了将1转换为-1，以在做and运算是变成-1的补码，即16个1，再去计算and
                mask = (short) (-1 * (short) (((message[i] & 0xFF) >> j) & 1));
                poly[8 * i + j] = (short) (mask & (short) ((KyberParams.PARAMS_Q + 1) / 2));
            }
        }
        return poly;
    }

    /**
     * Convert a polynomial to a 32-byte message.
     *
     * @param poly polynomial.
     * @return message.
     */
    public static byte[] polyToMessage(short[] poly) {
        byte[] message = new byte[KyberParams.SYM_BYTES];
        int t;
        inPolyConditionalSubQ(poly);
        for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_EIGHT; i++) {
            message[i] = 0;
            for (int j = 0; j < KyberParams.MATH_EIGHT; j++) {
                // message[i] = (a * 2 + Q / 2) / Q & 1
                t = ((((((int) (poly[8 * i + j])) << 1) + (KyberParams.PARAMS_Q / 2)) / KyberParams.PARAMS_Q) & 1);
                message[i] = (byte) (message[i] | (t << j));
            }
        }
        return message;
    }

    /**
     * Create a new empty polynomial vector.
     *
     * @param vectorLength polynomial vector length.
     * @return new empty polynomial vector.
     */
    public static short[][] createEmptyPolyVector(int vectorLength) {
        return new short[vectorLength][KyberParams.POLY_BYTES];
    }

    /**
     * Computes an in-place neg-cyclic number-theoretic transform (NTT) of a polynomial.
     * Input is assumed normal order. Output is assumed bit-revered order
     *
     * @param poly polynomial.
     */
    public static void inPolyNtt(short[] poly) {
        Ntt.inNtt(poly);
    }

    /**
     * Applies forward number-theoretic transforms (NTT) to all elements of a vector of polynomial.
     *
     * @param polyVector polynomial vector.
     */
    public static void inPolyVectorNtt(short[][] polyVector) {
        for (short[] poly : polyVector) {
            Poly.inPolyNtt(poly);
        }
    }

    /**
     * Computes an in-place inverse of a neg-cyclic number-theoretic transform (NTT) of a polynomial and multiplies
     * by Montgomery factor 2^16.
     * <p>
     * Input is assumed bit-revered order. Output is assumed normal order.
     * </p>
     *
     * @param nttPoly NTT polynomial.
     */
    public static void inPolyInvNttMontgomery(short[] nttPoly) {
        Ntt.inInvNtt(nttPoly);
    }

    /**
     * Applies the inverse number-theoretic transform (NTT) to all elements of a vector of polynomials and multiplies
     * by Montgomery factor 2^16.
     *
     * @param nttPolyVector NTT polynomial vector.
     */
    public static void inPolyVectorInvNttMontgomery(short[][] nttPolyVector) {
        for (short[] nttPoly : nttPolyVector) {
            Poly.inPolyInvNttMontgomery(nttPoly);
        }
    }

    /**
     * Multiply two polynomials in the number-theoretic transform (NTT) domain.
     *
     * @param polyA NTT polynomial A.
     * @param polyB NTT polynomial B.
     * @return multiplication polynomial.
     */
    public static short[] polyBaseMulMontgomery(short[] polyA, short[] polyB) {
        for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_FOUR; i++) {
            short[] rx = Ntt.baseMultiplier(
                polyA[4 * i], polyA[4 * i + 1],
                polyB[4 * i], polyB[4 * i + 1],
                Ntt.NTT_ZETAS[64 + i]
            );
            short[] ry = Ntt.baseMultiplier(
                polyA[4 * i + 2], polyA[4 * i + 3],
                polyB[4 * i + 2], polyB[4 * i + 3],
                (short) (-1 * Ntt.NTT_ZETAS[64 + i])
            );
            polyA[4 * i] = rx[0];
            polyA[4 * i + 1] = rx[1];
            polyA[4 * i + 2] = ry[0];
            polyA[4 * i + 3] = ry[1];
        }
        return polyA;
    }

    /**
     * Pointwise-multiplies elements of the given polynomial-vectors, accumulates the results, and then multiplies by 2^-16.
     *
     * @param nttPolyVectorA NTT polynomial vector A.
     * @param nttPolyVectorB NTT polynomial vector B.
     * @return accumulated montgomery polynomial.
     */
    public static short[] polyVectorPointWiseAccMontgomery(short[][] nttPolyVectorA, short[][] nttPolyVectorB) {
        short[] r = Poly.polyBaseMulMontgomery(nttPolyVectorA[0], nttPolyVectorB[0]);
        for (int i = 1; i < nttPolyVectorA.length; i++) {
            short[] t = Poly.polyBaseMulMontgomery(nttPolyVectorA[i], nttPolyVectorB[i]);
            Poly.inPolyAdd(r, t);
        }
        Poly.inPolyBarrettReduce(r);
        return r;
    }

    /**
     * Performs lossy compression and serialization of a polynomial.
     *
     * @param poly    polynomial.
     * @param paramsK parameter K, can be 2, 3 or 4.
     * @return compressed polynomial.
     */
    public static byte[] compressPoly(short[] poly, int paramsK) {
        byte[] t = new byte[8];
        Poly.inPolyConditionalSubQ(poly);
        int rr = 0;
        byte[] r;
        switch (paramsK) {
            case 2:
            case 3:
                r = new byte[KyberParams.POLY_COMPRESSED_BYTES_768];
                for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_EIGHT; i++) {
                    for (int j = 0; j < KyberParams.MATH_EIGHT; j++) {
                        t[j] = (byte) (((((poly[8 * i + j]) << 4) + (KyberParams.PARAMS_Q / 2)) / (KyberParams.PARAMS_Q)) & 15);
                    }
                    r[rr] = (byte) (t[0] | (t[1] << 4));
                    r[rr + 1] = (byte) (t[2] | (t[3] << 4));
                    r[rr + 2] = (byte) (t[4] | (t[5] << 4));
                    r[rr + 3] = (byte) (t[6] | (t[7] << 4));
                    rr = rr + 4;
                }
                break;
            case 4:
                r = new byte[KyberParams.POLY_COMPRESSED_BYTES_1024];
                for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_EIGHT; i++) {
                    for (int j = 0; j < KyberParams.MATH_EIGHT; j++) {
                        t[j] = (byte) (((((poly[8 * i + j]) << 5) + (KyberParams.PARAMS_Q / 2)) / (KyberParams.PARAMS_Q)) & 31);
                    }
                    r[rr] = (byte) ((t[0]) | (t[1] << 5));
                    r[rr + 1] = (byte) ((t[1] >> 3) | (t[2] << 2) | (t[3] << 7));
                    r[rr + 2] = (byte) ((t[3] >> 1) | (t[4] << 4));
                    r[rr + 3] = (byte) ((t[4] >> 4) | (t[5] << 1) | (t[6] << 6));
                    r[rr + 4] = (byte) ((t[6] >> 2) | (t[7] << 3));
                    rr = rr + 5;
                }
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        return r;
    }

    /**
     * Perform a lossy compression and serialization of a vector of polynomials.
     *
     * @param polyVector polynomial vector.
     * @param paramsK    parameter K, can be 2, 3 or 4.
     * @return compressed polynomial vector.
     */
    public static byte[] compressPolyVector(short[][] polyVector, int paramsK) {
        Poly.inPolyVectorCoefficientSubQ(polyVector);
        int rr = 0;
        byte[] r;
        long[] t;
        switch (paramsK) {
            case 2:
                r = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_512];
                break;
            case 3:
                r = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_768];
                break;
            case 4:
                r = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_1024];
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        switch (paramsK) {
            case 2:
            case 3:
                t = new long[4];
                for (int i = 0; i < paramsK; i++) {
                    for (int j = 0; j < KyberParams.PARAMS_N / KyberParams.MATH_FOUR; j++) {
                        for (int k = 0; k < KyberParams.MATH_FOUR; k++) {
                            t[k] = (((((long) (polyVector[i][4 * j + k]) << 10) + (long) (KyberParams.PARAMS_Q / 2))
                                / (long) (KyberParams.PARAMS_Q)) & 0x3ff);
                        }
                        r[rr] = (byte) (t[0]);
                        r[rr + 1] = (byte) ((t[0] >> 8) | (t[1] << 2));
                        r[rr + 2] = (byte) ((t[1] >> 6) | (t[2] << 4));
                        r[rr + 3] = (byte) ((t[2] >> 4) | (t[3] << 6));
                        r[rr + 4] = (byte) ((t[3] >> 2));
                        rr = rr + 5;
                    }
                }
                break;
            case 4:
                t = new long[8];
                for (int i = 0; i < paramsK; i++) {
                    for (int j = 0; j < KyberParams.PARAMS_N / KyberParams.MATH_EIGHT; j++) {
                        for (int k = 0; k < KyberParams.MATH_EIGHT; k++) {
                            t[k] = (((((long) (polyVector[i][8 * j + k]) << 11) + (long) (KyberParams.PARAMS_Q / 2))
                                / (long) (KyberParams.PARAMS_Q)) & 0x7ff);
                        }
                        r[rr] = (byte) ((t[0]));
                        r[rr + 1] = (byte) ((t[0] >> 8) | (t[1] << 3));
                        r[rr + 2] = (byte) ((t[1] >> 5) | (t[2] << 6));
                        r[rr + 3] = (byte) ((t[2] >> 2));
                        r[rr + 4] = (byte) ((t[2] >> 10) | (t[3] << 1));
                        r[rr + 5] = (byte) ((t[3] >> 7) | (t[4] << 4));
                        r[rr + 6] = (byte) ((t[4] >> 4) | (t[5] << 7));
                        r[rr + 7] = (byte) ((t[5] >> 1));
                        r[rr + 8] = (byte) ((t[5] >> 9) | (t[6] << 2));
                        r[rr + 9] = (byte) ((t[6] >> 6) | (t[7] << 5));
                        r[rr + 10] = (byte) ((t[7] >> 3));
                        rr = rr + 11;
                    }
                }
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        return r;
    }

    /**
     * De-serialize and decompress a polynomial.
     * <p>
     * Compression is lossy so the resulting polynomial will not match the original polynomial.
     * </p>
     *
     * @param compressedPoly compressed polynomial.
     * @param paramsK        parameter K, can be 2, 3 or 4.
     * @return polynomial.
     */
    public static short[] decompressPoly(byte[] compressedPoly, int paramsK) {
        short[] r = new short[KyberParams.PARAMS_N];
        switch (paramsK) {
            case 2:
            case 3:
                for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_TWO; i++) {
                    r[2 * i] = (short) ((((compressedPoly[i] & 15) * KyberParams.PARAMS_Q) + 8) >> 4);
                    r[2 * i + 1] = (short) (((((compressedPoly[i] & 0xFF) >> 4) * KyberParams.PARAMS_Q) + 8) >> 4);
                }
                break;
            case 4:
                int aa = 0;
                long[] t = new long[8];
                for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_EIGHT; i++) {
                    t[0] = ((compressedPoly[aa] & 0xFF));
                    t[1] = (long) ((byte) (((compressedPoly[aa] & 0xFF) >> 5))
                        | (byte) ((compressedPoly[aa + 1] & 0xFF) << 3)) & 0xFF;
                    t[2] = (long) ((compressedPoly[aa + 1] & 0xFF) >> 2) & 0xFF;
                    t[3] = (long) ((byte) (((compressedPoly[aa + 1] & 0xFF) >> 7))
                        | (byte) ((compressedPoly[aa + 2] & 0xFF) << 1)) & 0xFF;
                    t[4] = (long) ((byte) (((compressedPoly[aa + 2] & 0xFF) >> 4))
                        | (byte) ((compressedPoly[aa + 3] & 0xFF) << 4)) & 0xFF;
                    t[5] = (long) ((compressedPoly[aa + 3] & 0xFF) >> 1) & 0xFF;
                    t[6] = (long) ((byte) (((compressedPoly[aa + 3] & 0xFF) >> 6))
                        | (byte) ((compressedPoly[aa + 4] & 0xFF) << 2)) & 0xFF;
                    t[7] = ((long) ((compressedPoly[aa + 4] & 0xFF) >> 3)) & 0xFF;
                    aa = aa + 5;
                    for (int j = 0; j < KyberParams.MATH_EIGHT; j++) {
                        r[8 * i + j] = (short) ((((t[j] & 31) * (KyberParams.PARAMS_Q)) + 16) >> 5);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        return r;
    }

    /**
     * De-serialize and decompress a vector of polynomials.
     * <p>
     * Since the compress is lossy, the results will not be exactly the same as he original vector of polynomials.
     * </p>
     *
     * @param compressedPolyVector compressed polynomial vector.
     * @param paramsK              parameter K, can be 2, 3 or 4.
     * @return polynomial vector.
     */
    public static short[][] decompressPolyVector(byte[] compressedPolyVector, int paramsK) {
        short[][] r = new short[paramsK][KyberParams.POLY_BYTES];
        int aa = 0;
        int[] t;
        switch (paramsK) {
            case 2:
            case 3:
                // has to be unsigned
                t = new int[4];
                for (int i = 0; i < paramsK; i++) {
                    for (int j = 0; j < (KyberParams.PARAMS_N / KyberParams.MATH_FOUR); j++) {
                        t[0] = ((compressedPolyVector[aa] & 0xFF) | ((compressedPolyVector[aa + 1] & 0xFF) << 8));
                        t[1] = ((compressedPolyVector[aa + 1] & 0xFF) >> 2) | ((compressedPolyVector[aa + 2] & 0xFF) << 6);
                        t[2] = ((compressedPolyVector[aa + 2] & 0xFF) >> 4) | ((compressedPolyVector[aa + 3] & 0xFF) << 4);
                        t[3] = ((compressedPolyVector[aa + 3] & 0xFF) >> 6) | ((compressedPolyVector[aa + 4] & 0xFF) << 2);
                        aa = aa + 5;
                        for (int k = 0; k < KyberParams.MATH_FOUR; k++) {
                            r[i][4 * j + k] = (short) (((long) (t[k] & 0x3FF) * (long) (KyberParams.PARAMS_Q) + 512) >> 10);
                        }
                    }
                }
                break;
            case 4:
                // has to be unsigned
                t = new int[8];
                for (int i = 0; i < paramsK; i++) {
                    for (int j = 0; j < (KyberParams.PARAMS_N / KyberParams.MATH_EIGHT); j++) {
                        t[0] = ((compressedPolyVector[aa] & 0xff) | ((compressedPolyVector[aa + 1] & 0xff) << 8));
                        t[1] = (((compressedPolyVector[aa + 1] & 0xff) >> 3) | ((compressedPolyVector[aa + 2] & 0xff) << 5));
                        t[2] = (((compressedPolyVector[aa + 2] & 0xff) >> 6) | ((compressedPolyVector[aa + 3] & 0xff) << 2)
                            | ((compressedPolyVector[aa + 4] & 0xff) << 10));
                        t[3] = (((compressedPolyVector[aa + 4] & 0xff) >> 1) | ((compressedPolyVector[aa + 5] & 0xff) << 7));
                        t[4] = (((compressedPolyVector[aa + 5] & 0xff) >> 4) | ((compressedPolyVector[aa + 6] & 0xff) << 4));
                        t[5] = (((compressedPolyVector[aa + 6] & 0xff) >> 7) | ((compressedPolyVector[aa + 7] & 0xff) << 1)
                            | ((compressedPolyVector[aa + 8] & 0xff) << 9));
                        t[6] = (((compressedPolyVector[aa + 8] & 0xff) >> 2) | ((compressedPolyVector[aa + 9] & 0xff) << 6));
                        t[7] = (((compressedPolyVector[aa + 9] & 0xff) >> 5) | ((compressedPolyVector[aa + 10] & 0xff) << 3));
                        aa = aa + 11;
                        for (int k = 0; k < KyberParams.MATH_EIGHT; k++) {
                            r[i][8 * j + k] = (short) (((long) (t[k] & 0x7FF) * (long) (KyberParams.PARAMS_Q) + 1024) >> 11);
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        return r;
    }

    /**
     * Generate a deterministic noise polynomial from a seed and nonce. The polynomial output will be close to a
     * centered binomial distribution.
     *
     * @param seed    the seed.
     * @param nonce   the nonce.
     * @param paramsK parameter K, can be 2, 3 or 4.
     * @return noisy polynomial
     */
    public static short[] getNoisePoly(byte[] seed, byte nonce, int paramsK) {
        int prfByteLength;
        switch (paramsK) {
            case 2:
                prfByteLength = KyberParams.ETA_512 * KyberParams.PARAMS_N / 4;
                break;
            case 3:
            case 4:
                prfByteLength = KyberParams.ETA_768_1024 * KyberParams.PARAMS_N / 4;
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        byte[] p = KyberEngineHelper.generatePrfByteArray(prfByteLength, seed, nonce);
        return ByteOps.generateCbdPoly(p, paramsK);
    }


}
