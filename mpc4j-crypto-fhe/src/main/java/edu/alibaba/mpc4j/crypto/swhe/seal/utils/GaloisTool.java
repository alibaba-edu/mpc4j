package edu.alibaba.mpc4j.crypto.swhe.seal.utils;


import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.swhe.seal.zq.Common;
import edu.alibaba.mpc4j.crypto.swhe.seal.zq.UintArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.swhe.seal.zq.UintCore;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Galois operation tool class.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/galois.h">
 * galois.h
 * </a>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/11
 */
public class GaloisTool {
    /**
     * Galois automorphism generator
     */
    private static final int GENERATOR = 3;
    /**
     * coeff_count_power, i.e., k such that n = 2^k
     */
    private int coeffCountPower = 0;
    /**
     * coeff_count, i.e, n
     */
    private int coeffCount = 0;
    /**
     * Galois automorphism permutation tables
     */
    private int[][] permutationTables;

    /**
     * Creates a Galois tool.
     *
     * @param coeffCountPower k such that n = 2^k.
     */
    public GaloisTool(int coeffCountPower) {
        initialize(coeffCountPower);
    }

    /**
     * Initializes a Galois tool.
     *
     * @param coeffCountPower k such that n = 2^k.
     */
    private void initialize(int coeffCountPower) {
        if (coeffCountPower < UintCore.getPowerOfTwo(Constants.SEAL_POLY_MOD_DEGREE_MIN) ||
            coeffCountPower > UintCore.getPowerOfTwo(Constants.SEAL_POLY_MOD_DEGREE_MAX)
        ) {
            throw new IllegalArgumentException("coeffCountPower out of range");
        }
        this.coeffCountPower = coeffCountPower;
        coeffCount = 1 << coeffCountPower;
        permutationTables = new int[coeffCount][coeffCount];
    }

    /**
     * Generated NTT table for a specific Galois element.
     *
     * @param galoisElt   the Galois element.
     * @param results     the NTT table to overwrite.
     * @param resultIndex the index of NTT table.
     */
    private void generateTableNtt(long galoisElt, int[][] results, int resultIndex) {
        // verify: (1) galois element k is an odd number (so that k ∈ Z_i^*, where i = 2N); (2) k < 2N.
        assert (galoisElt & 1) > 0 && (galoisElt < 2 * (1L << coeffCountPower));
        if (results[resultIndex] == null) {
            return;
        }
        results[resultIndex] = new int[coeffCount];
        int tempPtr = 0;
        int coeffCountMinusOne = coeffCount - 1;
        for (int i = coeffCount; i < (coeffCount << 1); i++) {
            int reversed = Common.reverseBits(i, coeffCountPower + 1);
            long indexRaw = (galoisElt * (long) reversed) >>> 1;
            indexRaw &= coeffCountMinusOne;
            results[resultIndex][tempPtr++] = Common.reverseBits((int) indexRaw, coeffCountPower);
        }
    }

    /**
     * Compute the Galois element corresponding to a given rotation step.
     *
     * @param step a rotation step.
     * @return the Galois element.
     */
    public int getEltFromStep(int step) {
        int n = coeffCount;
        int m32 = Common.mulSafe(n, 2, true);
        if (step == 0) {
            return m32 - 1;
        } else {
            // Extract sign of steps. When step is positive, the rotation
            // is to the left; when step is negative, it is to the right.
            boolean sign = step < 0;
            int posStep = Math.abs(step);
            if (posStep >= (n >>> 1)) {
                throw new IllegalArgumentException("step count too large");
            }

            posStep &= (m32 - 1);
            if (sign) {
                step = (n >>> 1) - posStep;
            } else {
                step = posStep;
            }

            // Construct Galois element for row rotation
            int galoisElt = 1;
            while (step-- > 0) {
                galoisElt *= GENERATOR;
                galoisElt &= (m32 - 1);
            }
            return galoisElt;
        }
    }

    /**
     * Compute the Galois elements corresponding to a vector of given rotation steps.
     *
     * @param steps the steps.
     * @return the Galois elements.
     */
    public int[] getEltsFromSteps(int[] steps) {
        return Arrays.stream(steps).map(this::getEltFromStep).toArray();
    }

    /**
     * Compute a vector of all necessary galois_elts.
     *
     * @return all necessary galois_elts.
     */
    public int[] getEltsAll() {
        int m = (int) ((long) coeffCount << 1);
        // there are 2 * log(n) - 1 galois_elements.
        int[] galoisElts = new int[2 * (coeffCountPower - 1) + 1];

        int galoisEltPtr = 0;
        // Generate Galois keys for m - 1 (X -> X^{m-1})
        galoisElts[galoisEltPtr++] = m - 1;

        // Generate Galois key for power of generator_ mod m (X -> X^{3^k}) and
        // for negative power of generator_ mod m (X -> X^{-3^k})
        long posPower = GENERATOR;
        long[] temp = new long[1];
        UintArithmeticSmallMod.tryInvertUintMod(GENERATOR, m, temp);
        long negPower = temp[0];
        for (int i = 0; i < coeffCountPower - 1; i++) {
            galoisElts[galoisEltPtr++] = (int) posPower;
            posPower *= posPower;
            posPower &= (m - 1);
            galoisElts[galoisEltPtr++] = (int) negPower;
            negPower *= negPower;
            negPower &= (m - 1);
        }

        return galoisElts;
    }

    /**
     * Computes the index in the range of 0 to (coeff_count_ - 1) of a given Galois element.
     *
     * @param galoisElt the Galois element.
     * @return the index.
     */
    public static int getIndexFromElt(int galoisElt) {
        if ((galoisElt & 1) == 0) {
            throw new IllegalArgumentException("galois_elt is not valid");
        }
        return (galoisElt - 1) >>> 1;
    }

    /**
     * Applies Galois automorphism o coeff iter of an RNS representation.
     *
     * @param operand   operand.
     * @param galoisElt galois element.
     * @param modulus   modulus.
     * @param result    result.
     */
    private void applyGaloisCoeffIter(CoeffIterator operand, int galoisElt, Modulus modulus, CoeffIterator result) {
        assert operand != null;
        assert result != null;
        // result cannot point to the same value as operand
        assert operand != result;
        // verify: (1) galois element k is an odd number (so that k ∈ Z_i^*, where i = 2N); (2) k < 2N.
        assert (galoisElt & 1) > 0 && (galoisElt < 2 * (1L << coeffCountPower));
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        int coeffCountMinusOne = coeffCount - 1;
        // indexRaw represents i * k in the loop, and operandIndex represents i in the loop.
        int indexRaw = 0;
        int operandIndex = 0;
        for (int i = 0; i <= coeffCountMinusOne; i++) {
            int index = indexRaw & coeffCountMinusOne;
            // get the i-th coefficient a_i for x^i
            long resultValue = operand.getCoefficient(operandIndex);
            // replace x^i to x^{ik}, if ik >= N and ik mod N is odd, then flip the sign: a_i to -a_i
            if (((indexRaw >>> coeffCountPower) & 1) != 0) {
                // Explicit inline
                long nonZero = resultValue != 0 ? 1 : 0;
                resultValue = (modulusValue - resultValue) & (-nonZero);
            }
            result.setCoefficient(index, resultValue);
            indexRaw += galoisElt;
            operandIndex++;
        }
    }

    /**
     * Applies Galois automorphism on RNS representation.
     *
     * @param operand   operand.
     * @param k         operand coefficient modulus size.
     * @param galoisElt galois element.
     * @param modulus   modulus.
     * @param result    result.
     */
    public void applyGaloisRnsIter(RnsIterator operand, int k, int galoisElt, Modulus[] modulus,
                                   RnsIterator result) {
        assert operand != result;
        assert k > 0 && k == result.k;
        assert operand.n == coeffCount && result.n == coeffCount;
        IntStream.range(0, k).forEach(i ->
            applyGaloisCoeffIter(operand.coeffIterators[i], galoisElt, modulus[i], result.coeffIterators[i])
        );
    }

    /**
     * Applies Galois automorphism on RNS representation.
     *
     * @param operand   operand.
     * @param m         operand Rns representation size.
     * @param galoisElt galois element.
     * @param modulus   modulus.
     * @param result    result.
     */
    public void applyGaloisPolyIter(PolyIterator operand, int m, int galoisElt, Modulus[] modulus,
                                    PolyIterator result) {
        assert m > 0;
        assert operand.k == result.k;
        IntStream.range(0, m).forEach(r ->
            applyGaloisRnsIter(operand.rnsIterators[r], result.k, galoisElt, modulus, result.rnsIterators[r])
        );
    }

    /**
     * Applies Galois automorphism of an RNS representation.
     *
     * @param operand   operand.
     * @param galoisElt galois element.
     * @param result    result.
     */
    private void applyGaloisNttCoeffIter(CoeffIterator operand, int galoisElt, CoeffIterator result) {
        assert operand != null;
        assert result != null;
        // result cannot point to the same value as operand
        assert operand != result;
        // Verify coprime conditions.
        assert (galoisElt & 1) > 0 && (galoisElt < 2 * (1L << coeffCountPower));

        generateTableNtt(galoisElt, permutationTables, getIndexFromElt(galoisElt));
        int[] table = permutationTables[getIndexFromElt(galoisElt)];
        for (int i = 0; i < coeffCount; i++) {
            result.setCoefficient(i, operand.getCoefficient(table[i]));
        }
    }

    /**
     * Applies Galois automorphism on Poly representation under NTT form.
     *
     * @param operand   operand.
     * @param m         operand RNS representation size.
     * @param galoisElt galois element.
     * @param result    result.
     */
    private void applyGaloisNttPolyIter(PolyIterator operand, int m, int galoisElt, PolyIterator result) {
        assert m > 0;
        assert operand != null;
        assert result != null;
        // result cannot point to the same value as operand
        assert operand != result;
        // Verify coprime conditions.
        assert operand.k > 0 && operand.k == result.k;

        // perform permutation
        for (int r = 0; r < m; r++) {
            applyGaloisNttRnsIter(operand.rnsIterators[r], operand.k, galoisElt, result.rnsIterators[r]);
        }
    }

    /**
     * Applies Galois automorphism on RNS representation under NTT form.
     *
     * @param operand   operand.
     * @param k         operand coefficient modulus size.
     * @param galoisElt galois element.
     * @param result    result.
     */
    public void applyGaloisNttRnsIter(RnsIterator operand, int k, int galoisElt, RnsIterator result) {
        assert operand != null;
        assert result != null;
        // result cannot point to the same value as operand
        assert operand != result;
        // Verify coprime conditions.
        assert k > 0 && k == result.k;
        assert operand.n == coeffCount && result.n == coeffCount;
        // perform permutation
        for (int j = 0; j < k; j++) {
            applyGaloisNttCoeffIter(operand.coeffIterators[j], galoisElt, result.coeffIterators[j]);
        }
    }
}