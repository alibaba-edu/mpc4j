package edu.alibaba.mpc4j.crypto.fhe.seal.rns;

import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.AbstractModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintArithmeticSmallMod;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * This class used for converting x in RNS-Base Q = [q1, q2, ..., qk] into another RNS-Base M = [m1, m2, ..., mn].
 * The scheme comes from Section 3.1, Equation (2) in the following paper:
 * <p>
 * Bajard, Jean-Claude, Julien Eynard, M. Anwar Hasan, and Vincent Zucca. A full RNS variant of FV like somewhat
 * homomorphic encryption schemes. SAC 2016, pp. 423-442.
 * <p>
 * The implementation is from <code>BaseConverter</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/rns.h#L129">rns.h</a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/19
 */
public class BaseConverter {
    /**
     * input base, size of k: q_1, q_2, ..., q_k, prod is q
     */
    private final RnsBase inBase;
    /**
     * output base, size of k': p_1, p_2, ..., p_{k'}, prod is p
     */
    private final RnsBase outBase;
    /**
     * baseChangeMatrix[i][j] = q_j^* mod p_i, q_j^* = q / q_j is a multi-precision integer, p_i is up to 61-bit,
     * so use 2D-array is enough.
     * It is an k' * k matrix, where k' is the size of outBase, k is the size of inBase, organized as follows:
     * <p>[ q_1^* mod p_1, q_2^* mod p_1, ..., q_k^* mod p_1]</p>
     * <p>[ q_1^* mod p_2, q_2^* mod p_2, ..., q_k^* mod p_2]</p>
     * <p>...</p>
     * <p>[ q_1^* mod p_{k'}, q_2^* mod p_{k'}, ...,  q_k^* mod p_{k'}]</p>
     */
    private long[][] baseChangeMatrix;

    /**
     * Creates a base converter.
     *
     * @param inBase  the input RNS-base q = [q_1, q_2, ..., q_k].
     * @param outBase the output RNS-base p = [p_1, p_2, ..., p_{k'}].
     */
    public BaseConverter(RnsBase inBase, RnsBase outBase) {
        this.inBase = inBase;
        this.outBase = outBase;
        initialize();
    }

    /**
     * initialize the base converter.
     */
    private void initialize() {
        Common.mulSafe(inBase.size(), outBase.size(), false);
        baseChangeMatrix = new long[outBase.size()][inBase.size()];
        for (int i = 0; i < outBase.size(); i++) {
            for (int j = 0; j < inBase.size(); j++) {
                // q_ij = q_j^* mod m_i
                baseChangeMatrix[i][j] = UintArithmeticSmallMod.moduloUint(
                    inBase.getPuncturedProdArray(j), inBase.size(), outBase.getBase(i)
                );
            }
        }
    }

    /**
     * Computes Fast Base Conversion, i.e., FastBcov(x, q, p).
     * <p>The input is an RNS in base q = {q_1, ..., q_k}.</p>
     * <p>The output is an RNS in base p = {p_1, ..., p_{k'}}.</p>
     * The algorithm in shown in Section 3.1, Equation (2) of the BEHZ16 paper.
     *
     * @param in  input RNS in base q = {q_1, ..., q_k}.
     * @param out output RNS in base p = {p_1, ..., p_{k'}}.
     */
    public void fastConvert(CoeffIterator in, CoeffIterator out) {
        assert in.n() == inBase.size();
        assert out.n() == outBase.size();
        // temp = x_i * ~{q_i} mod q_i
        long[] temp = IntStream.range(0, inBase.size())
            .mapToLong(i ->
                UintArithmeticSmallMod.multiplyUintMod(
                    in.getCoeff(i), inBase.getInvPuncturedProdModBaseArray(i), inBase.getBase(i)
                )
            )
            .toArray();
        // x'_i = Σ_{i = 1}^{k} (x_i * ~{q_i} mod q_i) * q_i^* mod m_j
        for (int i = 0; i < outBase.size(); i++) {
            out.setCoeff(i, UintArithmeticSmallMod.dotProductMod(temp, baseChangeMatrix[i], inBase.size(), outBase.getBase(i)));
        }
    }

    /**
     * Computes Fast Base Conversion, i.e., FastBcov(x, q, p).
     * <p>The input is an RNS in base q = {q_1, ..., q_k}.</p>
     * <p>The output is an RNS in base p = {p_1, ..., p_{k'}}.</p>
     * The algorithm in shown in Section 3.1, Equation (2) of the BEHZ16 paper.
     *
     * @param in  input RNS in base q = {q_1, ..., q_k}.
     * @param out output RNS in base p = {p_1, ..., p_{k'}}.
     */
    public void fastConvertArrayRnsIter(RnsIterator in, RnsIterator out) {
        // 不一定要完全相等，例如在 RNS 的一些工具中，某些输入是在比较大的一个 base 下，但是就对其中前几个base进行convert
        assert in.k() >= inBase.size();
        assert out.k() == outBase.size();
        assert in.n() == out.n();

        // N * k
        long[][] temp = new long[in.n()][in.k()];
        //  |x_i * \tilde{q_i}|_{q_i} i \in [0, k), the result is length-k array
        //  Now we have N x, so need N * k array store
        for (int i = 0; i < inBase.size(); i++) {
            if (inBase.getInvPuncturedProdModBaseArray(i).operand == 1) {
                for (int j = 0; j < in.n(); j++) {
                    temp[j][i] = UintArithmeticSmallMod.barrettReduce64(
                        in.coeffIter[i].getCoeff(j), inBase.getBase(i)
                    );
                }
            } else {
                for (int j = 0; j < in.n(); j++) {
                    temp[j][i] = UintArithmeticSmallMod.multiplyUintMod(
                        in.coeffIter[i].getCoeff(j), inBase.getInvPuncturedProdModBaseArray(i), inBase.getBase(i)
                    );
                }
            }
        }
        for (int i = 0; i < outBase.size(); i++) {
            for (int j = 0; j < in.n(); j++) {
                out.coeffIter[i].setCoeff(j, UintArithmeticSmallMod.dotProductMod(
                    temp[j], baseChangeMatrix[i], inBase.size(), outBase.getBase(i)
                ));
            }
        }
    }

    /**
     * Computes Exact Fast Base Conversion, i.e., FastBcov(x, q, p).
     * Ref: Section 2.2 in An Improved RNS Variant of the BFV Homomorphic Encryption Scheme(HPS).
     * <p>The input is an RNS in base q = {q_1, ..., q_k}.</p>
     * <p>The output is an RNS in base p.</p>
     * The algorithm in shown in Section 2.2, of the HPS19 paper.
     *
     * @param in input RNS in base q = {q_1, ..., q_k}.
     * @return output RNS in a single base p.
     */
    public long exactConvert(long[] in) {
        assert in.length == inBase.size();
        // the size of out base muse be one
        if (outBase.size() != 1) {
            throw new IllegalArgumentException("out base in exact_convert_array must be one");
        }
        AbstractModulus p = outBase.getBase(0);
        if (inBase.size() > 1) {
            // v = round( \sum ([x_i * \tilde{q_i}]_{q_i} / q_i))
            // 1. [x_i * \tilde{q_i}]_{q_i}, and the fraction
            double[] fraction = new double[inBase.size()];
            long[] xiMulTildeQi = new long[inBase.size()];
            for (int i = 0; i < inBase.size(); i++) {
                xiMulTildeQi[i] = UintArithmeticSmallMod.multiplyUintMod(
                    in[i], inBase.getInvPuncturedProdModBaseArray(i), inBase.getBase(i)
                );
                fraction[i] = (double) xiMulTildeQi[i] / (double) inBase.getBase(i).value();
            }
            // compute v, and rounding
            double v = Arrays.stream(fraction).sum();
            long vRounded = Double.compare(0.5, v) == 0 ? 0 : Math.round(v);
            long qModP = UintArithmeticSmallMod.moduloUint(inBase.getBaseProd(), inBase.size(), p);
            // compute \sum ([x_i * \tilde{q_i}]_{q_i} * q_i^*)
            // matrix is 1 * k
            long sumModP = UintArithmeticSmallMod.dotProductMod(xiMulTildeQi, baseChangeMatrix[0], inBase.size(), p);
            long vMulQprodModP = UintArithmeticSmallMod.multiplyUintMod(vRounded, qModP, p);
            // [\sum ([x_i * \tilde{q_i}]_{q_i} * q_i^*) - v * q]_p
            return UintArithmeticSmallMod.subUintMod(sumModP, vMulQprodModP, p);
        } else {
            return UintArithmeticSmallMod.moduloUint(in, 1, p);
        }
    }

    /**
     * Computes Exact Fast Base Conversion, i.e., FastBcov(x, q, p).
     * Ref: Section 2.2 in An Improved RNS Variant of the BFV Homomorphic Encryption Scheme(HPS).
     * <p>The input is an RNS in base q = {q_1, ..., q_k}.</p>
     * <p>The output is an RNS in base p.</p>
     * The algorithm in shown in Section 2.2, of the HPS19 paper.
     *
     * @param in  input RNS in base q = {q_1, ..., q_k}.
     * @param out RNS in a single base p.
     */
    public void exactConvertArray(RnsIterator in, CoeffIterator out) {
        assert in.n() == out.n();
        assert in.k() == inBase.size();
        long[][] inCoeffs = RnsIterator.to2dArray(in.coeff(), in.n(), in.k());
        long[][] inColumns = new long[in.n()][in.k()];
        // transpose
        for (int i = 0; i < in.n(); i++) {
            for (int j = 0; j < in.k(); j++) {
                inColumns[i][j] = inCoeffs[j][i];
            }
        }
        // exact convert by column
        for (int i = 0; i < in.n(); i++) {
            out.setCoeff(i, exactConvert(inColumns[i]));
        }
    }

    /**
     * Gets input base size.
     *
     * @return input base size.
     */
    public int getInputBaseSize() {
        return inBase.size();
    }

    /**
     * Gets output base size.
     *
     * @return output base size.
     */
    public int getOutputBaseSize() {
        return outBase.size();
    }

    /**
     * Gets input base.
     *
     * @return input base.
     */
    public RnsBase getInputBase() {
        return inBase;
    }

    /**
     * Gets output base.
     *
     * @return output base.
     */
    public RnsBase getOutputBase() {
        return outBase;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }
}
