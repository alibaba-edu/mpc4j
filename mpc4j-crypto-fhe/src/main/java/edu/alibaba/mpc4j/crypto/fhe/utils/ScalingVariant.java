package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.Plaintext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.zq.MultiplyUintModOperand;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

import java.util.Arrays;

/**
 * This class provides some scaling methods.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/scalingvariant.cpp
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/26
 */
public class ScalingVariant {
    /**
     * private constructor.
     */
    private ScalingVariant() {
        // empty
    }

    /**
     * Computes round(plain * q/t) + poly.
     *
     * @param plain       plaintext.
     * @param contextData context data.
     * @param destination the poly to overwrite.
     * @param n           coefficient count.
     * @param pos         destination start position.
     */
    public static void multiplyAddPlainWithScalingVariant(Plaintext plain, SealContext.ContextData contextData,
                                                          long[] destination, int pos, int n) {
        EncryptionParameters parms = contextData.parms();
        int plainCoeffCount = plain.coeffCount();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        Modulus plainModulus = contextData.parms().plainModulus();
        // q / t mod q_i
        MultiplyUintModOperand[] coeffDivPlainModulus = contextData.coeffDivPlainModulus();
        // (plain_modulus + 1) / 2
        long plainUpperHalfThreshold = contextData.plainUpperHalfThreshold();
        // q mod t
        long qModT = contextData.coeffModulusModPlainModulus();
        assert plainCoeffCount <= parms.polyModulusDegree();
        assert n == parms.polyModulusDegree();
        // Coefficients of plain m multiplied by coeff_modulus q, divided by plain_modulus t,
        // and rounded to the nearest integer (rounded up in case of a tie). Equivalent to
        // floor((q * m + floor((t+1) / 2)) / t).
        long[] prod = new long[2];
        long[] numerator = new long[2];
        long[] fix = new long[2];

        for (int i = 0; i < plainCoeffCount; i++) {
            Arrays.fill(prod, 0);
            Arrays.fill(numerator, 0);
            // (q mod t) * m[i]
            UintArithmetic.multiplyUint64(plain.getData()[i], qModT, prod);
            // lower (q mod t) * m[i] + (t+1)/2
            long carry = UintArithmetic.addUint64(prod[0], plainUpperHalfThreshold, numerator);
            // higher carry is 0 or 1
            numerator[1] = prod[1] + carry;
            // compute fix[0] = floor(numerator / t)
            Arrays.fill(fix, 0);
            UintArithmetic.divideUint128Inplace(numerator, plainModulus.value(), fix);
            // Add to ciphertext: floor(q / t) * m + increment
            for (int j = 0; j < coeffModulusSize; j++) {
                long scaledRoundedHalf = UintArithmeticSmallMod.multiplyAddUintMod(
                    plain.getData()[i], coeffDivPlainModulus[j], fix[0], coeffModulus[j]);
                destination[pos + j * n + i] = UintArithmeticSmallMod.addUintMod(
                    destination[pos + j * n + i], scaledRoundedHalf, coeffModulus[j]
                );
            }
        }
    }

    /**
     * Computes round(plain * q/t) - poly.
     *
     * @param plain       plaintext.
     * @param contextData context data.
     * @param destination the poly to overwrite.
     * @param n           coefficient count.
     * @param pos         destination start position.
     */
    public static void multiplySubPlainWithScalingVariant(Plaintext plain, ContextData contextData,
                                                          long[] destination, int pos, int n) {
        EncryptionParameters parms = contextData.parms();
        int plainCoeffCount = plain.coeffCount();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        Modulus plainModulus = contextData.parms().plainModulus();
        MultiplyUintModOperand[] coeffDivPlainModulus = contextData.coeffDivPlainModulus();
        long plianUpperHalfThreshold = contextData.plainUpperHalfThreshold();
        long qModT = contextData.coeffModulusModPlainModulus();

        assert plainCoeffCount <= parms.polyModulusDegree();
        assert n == parms.polyModulusDegree();

        // Coefficients of plain m multiplied by coeff_modulus q, divided by plain_modulus t,
        // and rounded to the nearest integer (rounded up in case of a tie). Equivalent to
        // floor((q * m + floor((t+1) / 2)) / t).
        long[] prod = new long[2];
        long[] numerator = new long[2];
        long[] fix = new long[2];

        for (int i = 0; i < plainCoeffCount; i++) {
            Arrays.fill(prod, 0);
            Arrays.fill(numerator, 0);
            // Compute numerator = (q mod t) * m[i] + (t+1)/2
            UintArithmetic.multiplyUint64(plain.getData()[i], qModT, prod);
            // carry is 0 or 1, lower + half
            long carry = UintArithmetic.addUint64(prod[0], plianUpperHalfThreshold, numerator);
            // higher carry is 0 or 1
            numerator[1] = prod[1] + carry;
            // Compute fix[0] = floor(numerator / t)
            Arrays.fill(fix, 0);
            UintArithmetic.divideUint128Inplace(numerator, plainModulus.value(), fix);

            // Add to ciphertext: floor(q / t) * m + increment
            for (int j = 0; j < coeffModulusSize; j++) {
                long scaledRoundedHalf = UintArithmeticSmallMod.multiplyAddUintMod(
                    plain.getData()[i], coeffDivPlainModulus[j], fix[0], coeffModulus[j]
                );
                destination[pos + j * n + i] = UintArithmeticSmallMod.subUintMod(
                    destination[pos + j * n + i], scaledRoundedHalf, coeffModulus[j]
                );
            }
        }
    }
}
