package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * NTT tables used for fast polynomial multiplication in the ring Z[x] / (x^n + 1), where n = 2^k is a power of 2.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/ntt.h#L69
 * <p></p>
 * You can read the following paper to see why we need to maintain some parameters in the NTT table.
 * <p>
 * Longa, Patrick, and Michael Naehrig. Speeding up the number theoretic transform for faster ideal lattice-based
 * cryptography. CANS 2016, pp. 124-139.
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/27
 */
public class NttTables {
    /**
     * Creates multiple NTT tables based on multiple modulus and stores in nttTables.
     *
     * @param coeffCountPower k, where n = 2^k.
     * @param modulusArray    modulus array.
     * @param nttTables       where to store the created NTT tables.
     */
    public static void createNttTables(int coeffCountPower, Modulus[] modulusArray, NttTables[] nttTables) {
        assert modulusArray.length == nttTables.length;
        for (int i = 0; i < modulusArray.length; i++) {
            nttTables[i] = new NttTables(coeffCountPower, modulusArray[i]);
        }
    }

    /**
     * the 2n-th primitive root of unity ψ mod q
     */
    private long root;
    /**
     * ψ^{-1} mod q
     */
    private long invRoot;
    /**
     * k
     */
    private int coeffCountPower;
    /**
     * n = 2^k
     */
    private int coeffCount;
    /**
     * modulus
     */
    private Modulus modulus;
    /**
     * n^(-1) modulo q
     */
    private MultiplyUintModOperand invDegreeModulo;
    /**
     * ψ^0, ψ^{ 1}, ψ^{ 2}, ..., ψ^{ (n - 1)} in the bit-reverse order of [0, n).
     */
    private MultiplyUintModOperand[] rootPowers;
    /**
     * ψ^0, ψ^{-1}, ψ^{-2}, ..., ψ^{-(n - 1)} in the shift bit-reverse order of [0, -n).
     */
    private MultiplyUintModOperand[] invRootPowers;
    /**
     * lazy modulo operation
     */
    private ModArithLazy modArithLazy;
    /**
     * NTT handler
     */
    NttHandler nttHandler;

    /**
     * Creates an NTT table with the given k and modulus q.
     *
     * @param coeffCountPower k, where n = 2^k.
     * @param modulus         modulus q.
     */
    public NttTables(int coeffCountPower, Modulus modulus) {
        initialize(coeffCountPower, modulus);
    }

    private void initialize(int coeffCountPower, Modulus modulus) {
        assert coeffCountPower >= UintCore.getPowerOfTwo(Constants.SEAL_POLY_MOD_DEGREE_MIN)
            && coeffCountPower <= UintCore.getPowerOfTwo(Constants.SEAL_POLY_MOD_DEGREE_MAX);
        // k
        this.coeffCountPower = coeffCountPower;
        // n = 2^k
        coeffCount = 1 << coeffCountPower;
        // modulus q
        this.modulus = modulus;

        long[] temp = new long[1];
        // find ψ, the minimal primitive 2n-th root of unity mod q
        if (!Numth.tryMinimalPrimitiveRoot(2L * coeffCount, modulus, temp)) {
            throw new IllegalArgumentException("invalid modulus");
        }
        root = temp[0];
        // compute ψ^{-1}
        if (!Numth.tryInvertUintMod(root, modulus.value(), temp)) {
            throw new IllegalArgumentException("invalid modulus");
        }
        invRoot = temp[0];
        // Populate ψ^0, ψ^1, ..., ψ^{n - 1} in bit-reverse order.
        rootPowers = new MultiplyUintModOperand[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            rootPowers[i] = new MultiplyUintModOperand();
        }
        MultiplyUintModOperand rootTemp = new MultiplyUintModOperand();
        rootTemp.set(root, modulus);
        long power = root;
        // compute ψ^{ 1}, ψ^{ 2}, ..., ψ^{ (n - 1)} and stores in bit-reverse order of (i).
        for (int i = 1; i < coeffCount; i++) {
            rootPowers[Common.reverseBits(i, coeffCountPower)].set(power, modulus);
            power = UintArithmeticSmallMod.multiplyUintMod(power, rootTemp, modulus);
        }
        // set ψ^0 = 1
        rootPowers[0].set(1, modulus);
        // Populate ψ^0, ψ^{-1}, ..., ψ^{-(n - 1)} in bit-reverse order.
        invRootPowers = new MultiplyUintModOperand[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            invRootPowers[i] = new MultiplyUintModOperand();
        }
        rootTemp.set(invRoot, modulus);
        power = invRoot;
        for (int i = 1; i < coeffCount; i++) {
            // this is not symmetric with rootPowers, since we need to reverse bits of (-i) instead of (i).
            invRootPowers[Common.reverseBits(i - 1, coeffCountPower) + 1].set(power, modulus);
            power = UintArithmeticSmallMod.multiplyUintMod(power, rootTemp, modulus);
        }
        // set ψ^0 = 1
        invRootPowers[0].set(1, modulus);
        // Compute n^(-1) modulo q.
        if (!Numth.tryInvertUintMod(coeffCount, modulus.value(), temp)) {
            throw new IllegalArgumentException("invalid modulus");
        }
        invDegreeModulo = new MultiplyUintModOperand();
        invDegreeModulo.set(temp[0], modulus);
        modArithLazy = new ModArithLazy(modulus);
        nttHandler = new NttHandler(modArithLazy);
    }

    /**
     * Copy an NTT table .
     *
     * @param copy the copied NTT table.
     */
    public NttTables(NttTables copy) {
        this.root = copy.root;
        this.invRoot = copy.invRoot;
        this.coeffCountPower = copy.coeffCountPower;
        this.coeffCount = copy.coeffCount;
        this.modulus = copy.modulus;
        this.invDegreeModulo = copy.invDegreeModulo;
        this.rootPowers = new MultiplyUintModOperand[coeffCount];
        this.invRootPowers = new MultiplyUintModOperand[coeffCount];
        System.arraycopy(copy.rootPowers, 0, this.rootPowers, 0, coeffCount);
        System.arraycopy(copy.invRootPowers, 0, this.invRootPowers, 0, coeffCount);
        modArithLazy = new ModArithLazy(modulus);
        nttHandler = new NttHandler(modArithLazy);
    }

    /**
     * Gets the number of coefficients, i.e., n = 2^k.
     *
     * @return the number of coefficients.
     */
    public int getCoeffCount() {
        return coeffCount;
    }

    /**
     * Gets the power of number of coefficients, i.e, k for n = 2^k.
     *
     * @return the power of number of coefficients.
     */
    public int getCoeffCountPower() {
        return coeffCountPower;
    }

    /**
     * Gets ψ^{-1}, where ψ is the 2n-th primitive root of unity mod q.
     *
     * @return ψ^{-1}.
     */
    public long getInvRoot() {
        return invRoot;
    }

    /**
     * Gets ψ, the 2n-th primitive root of unity mod q.
     *
     * @return ψ.
     */
    public long getRoot() {
        return root;
    }

    /**
     * Gets the lazy modulo arithmetic operation for q.
     *
     * @return the lazy modulo arithmetic operation for q.
     */
    public ModArithLazy getModArithLazy() {
        return modArithLazy;
    }

    /**
     * Gets modulus q.
     *
     * @return modulus q.
     */
    public Modulus getModulus() {
        return modulus;
    }

    /**
     * Gets the NTT handler.
     *
     * @return the NTT handler.
     */
    public NttHandler getNttHandler() {
        return nttHandler;
    }

    /**
     * Gets n^(-1) modulo q.
     *
     * @return n^(-1) modulo q.
     */
    public MultiplyUintModOperand getInvDegreeModulo() {
        return invDegreeModulo;
    }

    /**
     * Gets ψ^0, ψ^{ 1}, ψ^{ 2}, ..., ψ^{ (n - 1)} in the bit-reverse order of [0, n).
     *
     * @return ψ^0, ψ^{ 1}, ψ^{ 2}, ..., ψ^{ (n - 1)} in the bit-reverse order of [0, n).
     */
    public MultiplyUintModOperand[] getRootPowers() {
        return rootPowers;
    }

    /**
     * Gets ψ^{i}, where i is in the bit-reverse order of [0, n).
     *
     * @param index the index i.
     * @return ψ^{i}.
     */
    public MultiplyUintModOperand getRootPowers(int index) {
        return rootPowers[index];
    }

    /**
     * Gets ψ^0, ψ^{-1}, ψ^{-2}, ..., ψ^{-(n - 1)} in the bit-reverse order of [0, -n).
     *
     * @return ψ^0, ψ^{-1}, ψ^{-2}, ..., ψ^{-(n - 1)} in the bit-reverse order of [0, -n).
     */
    public MultiplyUintModOperand[] getInvRootPowers() {
        return invRootPowers;
    }

    /**
     * Gets ψ^{-i}, where i is in the bit-reverse order of [0, -n).
     *
     * @param index the index i.
     * @return ψ^{-i}.
     */
    public MultiplyUintModOperand getInvRootPowers(int index) {
        assert index < coeffCount;
        return invRootPowers[index];
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }
}
