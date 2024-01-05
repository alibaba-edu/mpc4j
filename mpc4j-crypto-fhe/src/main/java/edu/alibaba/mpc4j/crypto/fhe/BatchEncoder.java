package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

import java.util.Arrays;

import static edu.alibaba.mpc4j.crypto.fhe.context.SealContext.*;

/**
 * Provides functionality for CRT batching. If the polynomial modulus degree is N, and
 * the plaintext modulus is a prime number T such that T is congruent to 1 modulo 2N,
 * then BatchEncoder allows the plaintext elements to be viewed as 2-by-(N/2)
 * matrices of integers modulo T. Homomorphic operations performed on such encrypted
 * matrices are applied coefficient (slot) wise, enabling powerful SIMD functionality
 * for computations that are vectorizable. This functionality is often called "batching"
 * in the homomorphic encryption literature.
 * <p>
 * Mathematically speaking, if the polynomial modulus is X^N+1, N is a power of two, and
 * plain_modulus is a prime number T such that 2N divides T-1, then integers modulo T
 * contain a primitive 2N-th root of unity and the polynomial X^N+1 splits into n distinct
 * linear factors as X^N+1 = (X-a_1)*...*(X-a_N) mod T, where the constants a_1, ..., a_n
 * are all the distinct primitive 2N-th roots of unity in integers modulo T. The Chinese
 * Remainder Theorem (CRT) states that the plaintext space Z_T[X]/(X^N+1) in this case is
 * isomorphic (as an algebra) to the N-fold direct product of fields Z_T. The isomorphism
 * is easy to compute explicitly in both directions, which is what this class does.
 * Furthermore, the Galois group of the extension is (Z/2NZ)* ~= Z/2Z x Z/(N/2) whose
 * action on the primitive roots of unity is easy to describe. Since the batching slots
 * correspond 1-to-1 to the primitive roots of unity, applying Galois automorphisms on the
 * plaintext act by permuting the slots. By applying generators of the two cyclic
 * subgroups of the Galois group, we can effectively view the plaintext as a 2-by-(N/2)
 * matrix, and enable cyclic row rotations, and column rotations (row swaps).
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/batchencoder.h
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/10/5
 */
public class BatchEncoder {

    /**
     * context
     */
    private final SealContext context;
    /**
     * slot num
     */
    private final int slots;
    /**
     * roots of unity
     */
    private final long[] rootsOfUnity;
    /**
     * matrix reverse position index map
     */
    private int[] matrixReversePositionIndexMap;

    /**
     * Creates a BatchEncoder. It is necessary that the encryption parameters given through
     * the SEALContext object support batching.
     *
     * @param context the SEALContext.
     */
    public BatchEncoder(SealContext context) {
        // Verify parameters
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }

        ContextData contextData = context.firstContextData();
        if (contextData.parms().scheme() != SchemeType.BFV && contextData.parms().scheme() != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        if (!contextData.qualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters are not valid for batching");
        }

        this.context = context;

        // Set the slot count
        slots = contextData.parms().polyModulusDegree();

        // Reserve space for all the primitive roots
        rootsOfUnity = new long[slots];

        // Fill the vector of roots of unity with all distinct odd powers of generator.
        // These are all the primitive (2*slots_)-th roots of unity in integers modulo
        // parms.plain_modulus().
        populateRootsOfUnityVector(contextData);

        // Populate matrix representation index map
        populateMatrixRepsIndexMap();
    }

    /**
     * Creates a plaintext from a given matrix. This function "batches" a given matrix
     * of integers modulo the plaintext modulus into a plaintext element, and stores
     * the result in the destination parameter. The input vector must have size at most equal
     * to the degree of the polynomial modulus. The first half of the elements represent the
     * first row of the matrix, and the second half represent the second row. The numbers
     * in the matrix can be at most equal to the plaintext modulus for it to represent
     * a valid plaintext.
     *
     * @param values      the matrix of integers modulo plaintext modulus to batch.
     * @param destination the plaintext polynomial to overwrite with the result.
     */
    public void encode(long[] values, Plaintext destination) {
        ContextData contextData = context.firstContextData();

        // Validate input parameters
        int valuesMatrixSize = values.length;
        if (valuesMatrixSize > slots) {
            throw new IllegalArgumentException("values_matrix size is too large");
        }

        // Validate the i-th input
        assert Arrays.stream(values).allMatch(n -> n < contextData.parms().plainModulus().value());

        // Set destination to full size
        destination.resize(slots);
        destination.setParmsId(ParmsId.parmsIdZero());

        // First write the values to destination coefficients.
        // Read in top row, then bottom row.
        for (int i = 0; i < valuesMatrixSize; i++) {
            destination.set(matrixReversePositionIndexMap[i], values[i]);
        }
        for (int i = valuesMatrixSize; i < slots; i++) {
            destination.set(matrixReversePositionIndexMap[i], 0);
        }

        // Transform destination using inverse of negacyclic NTT
        // Note: We already performed bit-reversal when reading in the matrix
        // represent plaintext in coefficient values.
        NttTool.inverseNttNegacyclicHarvey(destination.getData(), contextData.plainNttTables());
    }

    /**
     * Creates a plaintext from a given matrix. This function "batches" a given matrix
     * of integers modulo the plaintext modulus into a plaintext element, and stores
     * the result in the destination parameter. The input vector must have size at most equal
     * to the degree of the polynomial modulus. The first half of the elements represent the
     * first row of the matrix, and the second half represent the second row. The numbers
     * in the matrix can be at most equal to the plaintext modulus for it to represent
     * a valid plaintext.
     *
     * @param values      the matrix of integers modulo plaintext modulus to batch.
     * @param destination the plaintext polynomial to overwrite with the result.
     */
    public void encodeInt64(long[] values, Plaintext destination) {
        ContextData contextData = context.firstContextData();

        // Validate input parameters
        int valuesMatrixSize = values.length;
        if (valuesMatrixSize > slots) {
            throw new IllegalArgumentException("values_matrix size is too large");
        }

        long modulus = contextData.parms().plainModulus().value();
        long plainModulusDivTwo = modulus >> 1;

        // Validate the i-th input
        for (long matrix : values) {
            assert !Common.unsignedGt(Math.abs(matrix), plainModulusDivTwo);
        }

        // Set destination to full size
        destination.resize(slots);
        destination.setParmsId(ParmsId.parmsIdZero());

        // First write the values to destination coefficients.
        // Read in top row, then bottom row.
        long value;
        for (int i = 0; i < valuesMatrixSize; i++) {
            value = (values[i] < 0) ? (modulus + values[i]) : values[i];
            destination.set(matrixReversePositionIndexMap[i], value);
        }
        for (int i = valuesMatrixSize; i < slots; i++) {
            destination.set(matrixReversePositionIndexMap[i], 0);
        }

        // Transform destination using inverse of negacyclic NTT
        // Note: We already performed bit-reversal when reading in the matrix
        // represent plaintext in coefficient values.
        NttTool.inverseNttNegacyclicHarvey(destination.getData(), contextData.plainNttTables());
    }

    /**
     * Inverse of encode. This function "unbatches" a given plaintext into a matrix
     * of integers modulo the plaintext modulus, and stores the result in the destination
     * parameter. The input plaintext must have degrees less than the polynomial modulus,
     * and coefficients less than the plaintext modulus, i.e. it must be a valid plaintext
     * for the encryption parameters.
     *
     * @param plain       the plaintext polynomial to unbatch.
     * @param destination the matrix to be overwritten with the values in the slots.
     */
    public void decode(Plaintext plain, long[] destination) {
        if (!ValCheck.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }
        if (plain.isNttForm()) {
            throw new IllegalArgumentException("plain cannot be in NTT form");
        }

        ContextData contextData = context.firstContextData();

        // Set destination size
        assert destination.length == slots;

        // Never include the leading zero coefficient (if present)
        int plainCoeffCount = Math.min(plain.coeffCount(), slots);

        long[] tempDest = new long[slots];

        // Make a copy of poly
        System.arraycopy(plain.getData(), 0, tempDest, 0, plainCoeffCount);

        // Transform destination using negacyclic NTT.
        NttTool.nttNegacyclicHarvey(tempDest, contextData.plainNttTables());

        // Read top row, then bottom row
        for (int i = 0; i < slots; i++) {
            destination[i] = tempDest[matrixReversePositionIndexMap[i]];
        }
    }

    /**
     * Inverse of encode. This function "unbatches" a given plaintext into a matrix
     * of integers modulo the plaintext modulus, and stores the result in the destination
     * parameter. The input plaintext must have degrees less than the polynomial modulus,
     * and coefficients less than the plaintext modulus, i.e. it must be a valid plaintext
     * for the encryption parameters.
     *
     * @param plain       the plaintext polynomial to unbatch.
     * @param destination the matrix to be overwritten with the values in the slots.
     */
    public void decodeInt64(Plaintext plain, long[] destination) {
        if (!ValCheck.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }
        if (plain.isNttForm()) {
            throw new IllegalArgumentException("plain cannot be in NTT form");
        }

        ContextData contextData = context.firstContextData();
        long modulus = contextData.parms().plainModulus().value();

        // Set destination size
        assert destination.length == slots;

        // Never include the leading zero coefficient (if present)
        int plainCoeffCount = Math.min(plain.coeffCount(), slots);

        long[] tempDest = new long[slots];

        // Make a copy of poly
        System.arraycopy(plain.getData(), 0, tempDest, 0, plainCoeffCount);

        // Transform destination using negacyclic NTT.
        NttTool.nttNegacyclicHarvey(tempDest, contextData.plainNttTables());

        // Read top row, then bottom row
        long plainModulusDivTwo = modulus >> 1;
        long curValue;
        for (int i = 0; i < slots; i++) {
            curValue = tempDest[matrixReversePositionIndexMap[i]];
            destination[i] = (Long.compareUnsigned(curValue, plainModulusDivTwo) > 0) ? (curValue - modulus) : curValue;
        }
    }

    /**
     * Compute g g^3 g^5 .... g^{2n - 1}, where g is the 2n-th primitive root of unity mod t.
     *
     * @param contextData the context data.
     */
    private void populateRootsOfUnityVector(ContextData contextData) {
        // 2n-th primitive root of unity mod t
        long root = contextData.plainNttTables().getRoot();
        Modulus modulus = contextData.parms().plainModulus();
        // g^2 mod t
        long generatorSq = UintArithmeticSmallMod.multiplyUintMod(root, root, modulus);
        rootsOfUnity[0] = root;
        // g g^3 g^5 .... g^{2n - 1}, just 2n-th roots of unity in integer mod t
        for (int i = 1; i < slots; i++) {
            rootsOfUnity[i] = UintArithmeticSmallMod.multiplyUintMod(rootsOfUnity[i - 1], generatorSq, modulus);
        }
    }

    /**
     * Stores the bit-reversed locations, isomorphic to Z_{n/2} * Z_2.
     */
    private void populateMatrixRepsIndexMap() {
        int logN = UintCore.getPowerOfTwo(slots);
        matrixReversePositionIndexMap = new int[slots];
        // Copy from the matrix to the value vectors
        int rowSize = slots >>> 1;
        int m = slots << 1;
        long gen = 3;
        long pos = 1;
        for (int i = 0; i < rowSize; i++) {
            // Position in normal bit order
            long index1 = (pos - 1) >> 1;
            long index2 = (m - pos - 1) >> 1;
            // Set the bit-reversed locations
            matrixReversePositionIndexMap[i] = (int) Common.reverseBits(index1, logN);
            matrixReversePositionIndexMap[rowSize | i] = (int) Common.reverseBits(index2, logN);
            // Next primitive root
            pos *= gen;
            pos &= (m - 1);
        }
    }

    /**
     * Returns the slot num.
     *
     * @return the slot num.
     */
    public int slotCount() {
        return slots;
    }
}
