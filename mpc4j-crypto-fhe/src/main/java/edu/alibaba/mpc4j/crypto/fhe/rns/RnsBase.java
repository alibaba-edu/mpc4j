package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RNS Base Class, representing a group of co-prime moduli: (q_1, q_2, ..., q_k) with q = Π_{i = 1}^{k} q_i.
 * It provides decompose (converting x in Z_q to Z_{q_i}) and compose (converting {x_i ∈ Z_{q_i}} to x ∈ Z_q) functions.
 * The scheme comes from:
 * <p>
 * Bajard, Jean-Claude, Julien Eynard, M. Anwar Hasan, and Vincent Zucca. A full RNS variant of FV like somewhat
 * homomorphic encryption schemes. SAC 2016, pp. 423-442, https://eprint.iacr.org/2016/510.
 * </p>
 * <p></p>
 * The implementation is from: https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/rns.h#L22
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/17
 */
public class RnsBase {
    /**
     * size of base, number of q_i
     */
    private int size;
    /**
     * Z_{q_i}
     */
    private Modulus[] base;
    /**
     * q = Π_{i = 1}^{k} q_i. This is a base-2^64 number represented by long[].
     */
    private long[] baseProd;
    /**
     * q_i^* = q / q_i. Each q^* is a base-2^64 number represented by long[]. There are total number of k q_i^*.
     */
    private long[][] puncturedProdArray;
    /**
     * ~{q_i} = (q_i^*)^{-1} mod q_i.
     * Each q_i is at most 61-bit length, so do ~{q_i}, represented by long. There are total number of k ~{q_i}.
     */
    private MultiplyUintModOperand[] invPuncturedProdModBaseArray;

    /**
     * Creates an RNS-base.
     *
     * @param rnsBase a group of moduli represented by Modulus[].
     */
    public RnsBase(Modulus[] rnsBase) {
        assert rnsBase.length > 0;
        size = rnsBase.length;
        // co-prime check
        for (int i = 0; i < size; i++) {
            // in our implementation, a valid modulus must not be zero.
            if (rnsBase[i].isZero()) {
                throw new IllegalArgumentException("rns base is invalid, modulus can not be zero");
            }
            for (int j = 0; j < i; j++) {
                if (Numth.gcd(rnsBase[i].value(), rnsBase[j].value()) > 1) {
                    throw new IllegalArgumentException("rns base is invalid, each moduli must be co-prime.");
                }
            }
        }
        base = rnsBase;
        if (!initialize()) {
            throw new IllegalArgumentException("rns base is invalid.");
        }
    }

    /**
     * Creates an RNS-base.
     *
     * @param rnsBase a group of moduli represented by long[].
     */
    public RnsBase(long[] rnsBase) {
        assert rnsBase.length > 0;
        size = rnsBase.length;
        // co-prime check
        for (int i = 0; i < size; i++) {
            if (rnsBase[i] == 0) {
                throw new IllegalArgumentException("rns base is invalid, modulus can not be zero");
            }
            for (int j = 0; j < i; j++) {
                if (Numth.gcd(rnsBase[i], rnsBase[j]) > 1) {
                    throw new IllegalArgumentException("rns base is invalid, each moduli must be co-prime.");
                }
            }
        }
        base = Arrays.stream(rnsBase).mapToObj(Modulus::new).toArray(Modulus[]::new);
        if (!initialize()) {
            throw new IllegalArgumentException("rns base is invalid.");
        }
    }

    /**
     * Creates a copied RNS-base.
     *
     * @param copy the other RNS-base.
     */
    public RnsBase(RnsBase copy) {
        size = copy.size;
        // deep copy base
        long[] baseValues = IntStream.range(0, size).mapToLong(i -> copy.base[i].value()).toArray();
        base = Arrays.stream(baseValues).mapToObj(Modulus::new).toArray(Modulus[]::new);
        // deep copy base products
        baseProd = new long[size];
        System.arraycopy(copy.baseProd, 0, baseProd, 0, size);
        // shallow copy inverse punctured products
        invPuncturedProdModBaseArray = new MultiplyUintModOperand[size];
        System.arraycopy(copy.invPuncturedProdModBaseArray, 0, invPuncturedProdModBaseArray, 0, size);
        // deep copy punctured products
        puncturedProdArray = new long[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(copy.puncturedProdArray[i], 0, puncturedProdArray[i], 0, size);
        }
    }

    /**
     * private constructor.
     */
    private RnsBase() {
        // empty
    }

    /**
     * Initialize with the given base by mainly computing qi^* = q / qi ∈ Z, and ~{qi} = (qi^*)^{-1} mod qi \in Z_{qi}.
     *
     * @return true if initialization success; false otherwise.
     */
    private boolean initialize() {
        Common.mulSafe(size, size, false);
        baseProd = new long[size];
        puncturedProdArray = new long[size][size];
        invPuncturedProdModBaseArray = new MultiplyUintModOperand[size];
        for (int i = 0; i < size; i++) {
            invPuncturedProdModBaseArray[i] = new MultiplyUintModOperand();
        }
        if (size > 1) {
            long[] baseValues = IntStream.range(0, size).mapToLong(i -> base[i].value()).toArray();
            boolean invertible = true;
            for (int i = 0; i < size; i++) {
                // qi^* = q / qi = Π_{j ∈ [1, k], j ≠ i} qj.
                UintArithmetic.multiplyManyUint64Except(baseValues, size, i, puncturedProdArray[i]);
                // ~{q_i} = (q / qi)^{-1} mod qi, first compute q / qi mod qi, then compute the inverse
                long tmp = UintArithmeticSmallMod.moduloUint(puncturedProdArray[i], size - 1, base[i]);
                long[] tmpInv = new long[1];
                invertible = invertible && UintArithmeticSmallMod.tryInvertUintMod(tmp, base[i], tmpInv);
                invPuncturedProdModBaseArray[i].set(tmpInv[0], base[i]);
            }
            // Q = (Q / q0) * q0
            UintArithmetic.multiplyUint(puncturedProdArray[0], size - 1, base[0].value(), size, baseProd);
            return invertible;
        } else {
            // size == 1
            baseProd[0] = base[0].value();
            // q = q1, q1^* = q / q1 = 1
            puncturedProdArray[0] = new long[]{1L};
            invPuncturedProdModBaseArray[0].set(1, base[0]);
            return true;
        }
    }

    /**
     * Decomposes value in-place, i.e., computes [value mod q1, value mod q2, ..., value mod qk]. The implementation
     * here implies that the length of the value is equal to the size of RNS-base. In fact, we have no limit to the
     * length of the value. In order to achieve this, we often add 0 to the high position of the value. For example,
     * the input value is 1, but we need to decompose to RNS with 3 bases, then the input needs to be [1, 0, 0].
     *
     * @param value a base-2^64 value.
     */
    public void decompose(long[] value) {
        assert value.length == size;
        if (size > 1) {
            long[] valueCopy = new long[value.length];
            System.arraycopy(value, 0, valueCopy, 0, value.length);
            for (int i = 0; i < size; i++) {
                // x mod qi
                value[i] = UintArithmeticSmallMod.moduloUint(valueCopy, size, base[i]);
            }
        } else {
            // size == 1, q1 = q, x1 = x mod q1 = x
        }
    }

    /**
     * Decomposes n values [x1, ..., xk], i.e., computes [xi mod q1, xi mod q2, ..., xi mod qk] for i ∈ [1, k]. These
     * k values are organized in an 1D-array. For example:
     * <p>values = {1 0 0 2 0 0}, n = 2 --> we have n = 2 values {1 0 0}, {2 0 0}, each size is k = 3.</p>
     * Suppose the RNS-base is {3, 5, 7}, then result is represented in the order of qi, that is,
     * <p>[ x1 mod q1, x2 mod q1, ..., xn mod q1]</p>
     * <p>[ x1 mod q2, x2 mod q2, ..., xn mod q2]</p>
     * <p>...</p>
     * <p>[ x1 mod qk, x2 mod qk, ..., xn mod qk]</p>
     * For example, {1 0 0 2 0 0} --> {1, 2, 1, 2, 1, 2} = {1 mod 3, 2 mod 3, 1 mod 5, 2 mod 5, 1 mod 7, 2 mod 7}.
     *
     * @param values an array with length k * n, where k is the RNS-base size, n is the number of values.
     * @param count  the number of values n.
     */
    public void decomposeArray(long[] values, int count) {
        assert values.length == count * size;
        if (size > 1) {
            /*
             * [ x1 mod q1, x2 mod q1, ..., xn mod q1]
             * [ x1 mod q2, x2 mod q2, ..., xn mod q2]
             *                   ...
             * [ x1 mod qk, x2 mod qk, ..., xn mod qk]
             */
            long[] valuesCopy = new long[values.length];
            System.arraycopy(values, 0, valuesCopy, 0, values.length);
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < count; j++) {
                    values[i * count + j] = UintArithmeticSmallMod.moduloUint(valuesCopy, j * size, size, base[i]);
                }
            }
        } else {
            // size == 1, q1 = q, x1 = x mod q1 = x
        }
    }

    /**
     * Composes value in-place, i.e., computes x where [x1 = x mod q1, x2 = x mod q2, ..., xk = x mod qk].
     * The basic idea is:
     * <p>x = (Σ_{i = 1}^k xi * ~{qi} * qi^*) mod q</p>
     * However, the above formula needs multi-precision arithmetic operations. Instead, we can use the following formula:
     * <p>x = (Σ_{i = 1}^k [xi * ~{qi}]_{qi} * qi^*) mod q</p>
     * where [·]_{qi} means modulus the input in range [0, qi).
     *
     * @param value a base-2^64 value.
     */
    public void compose(long[] value) {
        assert value.length == size;
        if (size > 1) {
            long[] tempValue = new long[size];
            UintCore.setUint(value, size, tempValue);
            // clear
            UintCore.setZeroUint(size, value);
            long[] tempMpi = new long[size];
            // x = (Σ_{i = 1}^k [xi * ~{qi}]_{qi} * qi^*) mod q
            for (int i = 0; i < size; i++) {
                // tmp = xi * ~{qi} mod q_i
                long tmpProd = UintArithmeticSmallMod.multiplyUintMod(tempValue[i], invPuncturedProdModBaseArray[i], base[i]);
                // tmp = tmp * qi^* mod q
                UintArithmetic.multiplyUint(puncturedProdArray[i], size - 1, tmpProd, size, tempMpi);
                // x = x + (tmp * qi^*) mod q
                UintArithmeticMod.addUintUintMod(tempMpi, value, baseProd, size, value);
            }
        } else {
            // size == 1, q1^* = q / q1 = 1, ~{q1} = q1 = 1, then x = x1.
        }
    }

    /**
     * Composes n value in-place: i.e., computes xi where [xi1 = x mod q1, xi2 = x mod q2, ..., xik = x mod qk].
     * Note that the input is organized as:
     * <p>[ x1 mod q1, x2 mod q1, ..., xn mod q1]</p>
     * <p>[ x1 mod q2, x2 mod q2, ..., xn mod q2]</p>
     * <p>...</p>
     * <p>[ x1 mod qk, x2 mod qk, ..., xn mod qk]</p>
     * We need to do operations under the correct order.
     *
     * @param values an array with length k * n, where each column represent a value under the RNS-base.
     */
    public void composeArray(long[] values, int count) {
        assert values.length == count * size;
        if (size > 1) {
            // {1 0 0 2 0 0} --> {1 2 1 2 1 2} --> {1, 1, 1} and {2, 2, 2}
            long[][] decomposedValues = new long[count][size];
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < size; j++) {
                    decomposedValues[i][j] = values[i + j * count];
                }
            }
            // {1, 1, 1} --> {1, 0, 0} and {2, 2, 2} --> {2, 0, 0}
            Arrays.stream(decomposedValues).forEach(this::compose);
            for (int i = 0; i < count; i++) {
                System.arraycopy(decomposedValues[i], 0, values, i * size, size);
            }
        } else {
            // size == 1, q1^* = q / q1 = 1, ~{q1} = q1 = 1, then x = x1.
        }
    }

    /**
     * Extends the original RNS-base with a given RNS-base. Assume existing RNS-base is [q1, q2, ..., qk], by extending
     * it with the other RNS-base [q'1, q'2, ..., q'm], we have a new RNS-base [q1, q2, ..., qk, q'1, q'2, ..., q'm].
     *
     * @param other the other RNS-base for [q'1, q'2, ..., q'm].
     * @return a new RNS-base for [this.base, other.base] = [q1, q2, ..., qk, q'1, q'2, ..., q'm].
     */
    public RnsBase extend(RnsBase other) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < other.size; j++) {
                if (!Numth.areCoPrime(base[i].value(), other.base[j].value())) {
                    throw new IllegalArgumentException("cannot extend by given value");
                }
            }
        }

        // Copy over this base
        RnsBase newBase = new RnsBase();
        newBase.size = Common.addSafe(size, other.size, false);
        newBase.base = new Modulus[newBase.size];
        System.arraycopy(this.base, 0, newBase.base, 0, this.size);
        // Extend with other base
        System.arraycopy(other.base, 0, newBase.base, this.size, other.size);
        // Initialize CRT data
        if (!newBase.initialize()) {
            throw new IllegalArgumentException("cannot extend by given value");
        }
        return newBase;
    }

    /**
     * Extends the original RNS-base with a given modulus. Assume existing RNS-base is [q1, q2, ..., qk], by extending
     * it with the other modulus q', we have a new RNS-base [q1, q2, ..., qk, q'].
     *
     * @param value an extend modulus q'.
     * @return a new RNS-base for [q1, q2, ..., qk, q'].
     */
    public RnsBase extend(Modulus value) {
        if (Arrays.stream(base).parallel().anyMatch(m -> !Numth.areCoPrime(m.value(), value.value()))) {
            throw new IllegalArgumentException("cannot extend by given value");
        }
        // Copy over this base
        RnsBase newBase = new RnsBase();
        newBase.size = Common.addSafe(size, 1, false);
        newBase.base = new Modulus[newBase.size];
        System.arraycopy(this.base, 0, newBase.base, 0, this.size);
        // Extend with value
        newBase.base[size] = value;
        // Initialize CRT data
        if (!newBase.initialize()) {
            throw new IllegalArgumentException("cannot extend by given value");
        }
        return newBase;
    }

    /**
     * Extends the original RNS-base with a given modulus. Assume existing RNS-base is [q1, q2, ..., qk], by extending
     * it with the other modulus q', we have a new RNS-base [q1, q2, ..., qk, q'].
     *
     * @param value an extend modulus q'.
     * @return a new RNS-base for [q1, q2, ..., qk, q'].
     */
    public RnsBase extend(long value) {
        return extend(new Modulus(value));
    }

    /**
     * Drops the last moduli qk in the current RNS-base [q1, q2, ..., q_{k-1}, qk] and returns the new RNS-base.
     *
     * @return a new RNS-base for [q1, q2, ..., q_{k-1}].
     */
    public RnsBase drop() {
        if (size == 1) {
            throw new RuntimeException("cannot drop from base of size 1");
        }
        // Copy over this base
        RnsBase newBase = new RnsBase();
        newBase.size = this.size - 1;
        newBase.base = new Modulus[newBase.size];
        System.arraycopy(this.base, 0, newBase.base, 0, newBase.size);
        // Initialize CRT data
        newBase.initialize();
        return newBase;
    }

    /**
     * Drops the given moduli qj from the current RNS-base [q1, q2, ..., qj, ..., qk] and return a new RNS-base.
     *
     * @param value the dropped moduli.
     * @return a new RNS-base for [q1, q2, ..., q_{j - 1}, ..., q_{j + 1}, ..., qk].
     */
    public RnsBase drop(Modulus value) {
        if (size == 1) {
            throw new RuntimeException("cannot drop from base of size 1");
        }
        if (!contains(value)) {
            throw new IllegalArgumentException("base does not contain given value");
        }
        // Copy over this base
        RnsBase newBase = new RnsBase();
        newBase.size = this.size - 1;
        newBase.base = new Modulus[newBase.size];
        int sourceIndex = 0;
        int destIndex = 0;
        while (destIndex < this.size - 1) {
            if (!this.base[sourceIndex].equals(value)) {
                newBase.base[destIndex] = this.base[sourceIndex];
                destIndex++;
            }
            sourceIndex++;
        }
        // Initialize CRT data
        newBase.initialize();
        return newBase;
    }

    /**
     * Drops the given moduli qj from the current RNS-base [q1, q2, ..., qj, ..., qk] and return a new RNS-base.
     *
     * @param value the dropped moduli.
     * @return a new RNS-base for [q1, q2, ..., q_{j - 1}, ..., q_{j + 1}, ..., qk].
     */
    public RnsBase drop(long value) {
        return drop(new Modulus(value));
    }

    /**
     * Returns whether the RNS-base contains the given moduli.
     *
     * @param value moduli.
     * @return true if the RNS-base contains the moduli; false otherwise.
     */
    public boolean contains(Modulus value) {
        return Arrays.asList(base).contains(value);
    }

    /**
     * Returns whether the RNS-base contains the given moduli.
     *
     * @param value 64-bit value.
     * @return true if the RNS-base contains the moduli; false otherwise.
     */
    public boolean contains(long value) {
        return Arrays.stream(base).map(Modulus::value).anyMatch(v -> v == value);
    }

    /**
     * Returns whether the RNS-base is a sub-base of the given super RNS-base.
     *
     * @param superBase the super RNS-base.
     * @return true if the super RNS-base contains all the moduli of the RNS-base; false otherwise.
     */
    public boolean isSubBaseOf(RnsBase superBase) {
        // we use set to improve performance.
        Set<Modulus> superBaseSet = Arrays.stream(superBase.base).collect(Collectors.toSet());
        return Arrays.stream(base).allMatch(superBaseSet::contains);
    }

    /**
     * Returns whether the RNS-base is a super-base of the given sub RNS-base.
     *
     * @param subBase the sub RNS-base.
     * @return true if the RNS contains all the moduli of the sub RNS-base; false otherwise.
     */
    public boolean isSuperBaseOf(RnsBase subBase) {
        return subBase.isSubBaseOf(this);
    }

    /**
     * Gets q = Π_{i = 1}^k qi, represented as base-2^64 form.
     *
     * @return q = Π_{i = 1}^k qi.
     */
    public long[] getBaseProd() {
        return baseProd;
    }

    /**
     * Gets ~{qi} = (qi^*)^{-1} mod qi for all i ∈ [1, k]. Each ~{qi} is represented as a 64-bit value.
     *
     * @return ~{qi} = (qi^*)^{-1} mod qi for all i ∈ [1, k].
     */
    public MultiplyUintModOperand[] getInvPuncturedProdModBaseArray() {
        return invPuncturedProdModBaseArray;
    }

    /**
     * Gets ~{qi} = (qi^*)^{-1} mod qi, represented as a 64-bit value.
     *
     * @param index index.
     * @return ~{qi} = (qi^*)^{-1} mod qi.
     */
    public MultiplyUintModOperand getInvPuncturedProdModBaseArray(int index) {
        return invPuncturedProdModBaseArray[index];
    }

    /**
     * Gets qi^* = q / qi for all i ∈ [1, k]. Each qi^* is represented as a 64-bit value.
     *
     * @return qi^* = q / qi for all i ∈ [1, k].
     */
    public long[][] getPuncturedProdArray() {
        return puncturedProdArray;
    }

    /**
     * Gets qi^* = q / qi, represented as a 64-bit value.
     *
     * @param index index.
     * @return qi^* = q / qi.
     */
    public long[] getPuncturedProdArray(int index) {
        return puncturedProdArray[index];
    }

    /**
     * Gets qi for all i ∈ [1, k].
     *
     * @return qi for all i ∈ [1, k].
     */
    public Modulus[] getBase() {
        return base;
    }

    /**
     * Gets qi.
     *
     * @param index index.
     * @return qi.
     */
    public Modulus getBase(int index) {
        return base[index];
    }

    /**
     * Gets k, the size of base.
     *
     * @return k, the size of base.
     */
    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }
}
