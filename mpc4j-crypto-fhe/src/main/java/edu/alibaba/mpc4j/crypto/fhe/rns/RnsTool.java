package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

/**
 * This class implements the BEHZ16 RNS scheme. The scheme comes from:
 * <p>
 * A full RNS variant of FV like somewhat homomorphic encryption schemes. https://eprint.iacr.org/2016/510
 * </p>
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/rns.h#L190
 *
 * @author Anony_Trent, Liqiang Peng, Weiran Liu
 * @date 2023/8/21
 */
@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
public class RnsTool {
    /**
     * the degree of the polynomial
     */
    private int coeffCount;
    /**
     * RNS base q = (q_1, ..., q_k) for q = q_1 · q_2 ... · q_k
     */
    private RnsBase baseQ;
    /**
     * RNS base B = {p_1, ..., p_{k'}} for B = p_1 · p_2 ... · p_{k'}.
     */
    private RnsBase baseB;
    /**
     * RNS base B_sk = {B, m_sk}
     */
    private RnsBase baseBsk;
    /**
     * RNS base {B_sk, m_tilde}
     */
    private RnsBase baseBskMTilde;
    /**
     * RNS base {t, γ}, used in BFV decryption
     */
    private RnsBase baseTGamma;
    /**
     * RNS base converter: q --> B_sk
     */
    private BaseConverter baseQToBskConv;
    /**
     * RNS base converter: q --> {m_tilde}
     */
    private BaseConverter baseQToMTildeConv;
    /**
     * RNS base converter B --> q
     */
    private BaseConverter baseBToQConv;
    /**
     * RNS base converter B --> {m_sk}
     */
    private BaseConverter baseBToMskConv;
    /**
     * RNS base converter: q --> {t, γ}, used in BFV decryption
     */
    private BaseConverter baseQToTGammaConv;
    /**
     * RNS base converter: q --> {t}, used in BGV decryption
     */
    private BaseConverter baseQToTConv;
    /**
     * |prod(q)^(-1)|_{B, m_sk}
     */
    private MultiplyUintModOperand[] invProdQModBsk;
    /**
     * |-q^{-1}|_{m_tilde}, used in BFV multiplication
     */
    private MultiplyUintModOperand negInvProdQModMTilde;
    /**
     * |prod(B)^(-1)|_{m_sk}
     */
    private MultiplyUintModOperand invProdBModMsk;
    /**
     * |γ^{-1}|_t, used in BFV decryption
     */
    private MultiplyUintModOperand invGammaModT;
    /**
     * |prod(B)|_{q_1}, |prod(B)|_{q_2}, ..., |prod(B)|_{q_k}
     */
    private long[] prodBModQ;
    /**
     * |m_tilde^(-1)|_{B, m_sk}
     */
    private MultiplyUintModOperand[] invMTildeModBsk;
    /**
     * |prod(q)|_{B, m_sk}
     */
    private long[] prodQModBsk;
    /**
     * |-q^(-1)|_{t, γ}, used in BFV decryption
     */
    private MultiplyUintModOperand[] negInvQModTGamma;
    /**
     * |γt|_{q_1, ..., q_k}, used in BFV decryption
     */
    private MultiplyUintModOperand[] prodTGammaModQ;
    /**
     * |q_k^{-1}|_{q_1}, |q_k^{-1}|_{q_2}, ..., |q_k^{-1}|_{q_{k-1}}
     */
    private MultiplyUintModOperand[] invQLastModQ;
    /**
     * NTT tables for RNS base {B, m_sk}
     */
    private NttTables[] baseBskNttTables;
    /**
     * m_tilde = 2^32
     */
    private Modulus mTilde;
    /**
     * m_sk
     */
    private Modulus mSk;
    /**
     * t, i.e., the plaintext modulus
     */
    private Modulus t;
    /**
     * γ, an integer co-prime to {q_1, ..., q_k}, γ ≡ 1 (mod 2n).
     * There is a trade-off between the size of γ and the error bound. SEAL chooses γ ~ 2^61.
     */
    private Modulus gamma;
    /**
     * |(q_k)^{-1}|_t
     */
    private long invQLastModT;
    /**
     * |q_k|_t
     */
    private long qLastModT;

    /**
     * Constructs an RNS tool instance for the given parameters.
     *
     * @param polyModulusDegree the degree of the polynomial.
     * @param coeffModulus      the coefficient modulus.
     * @param plainModulus      the plaintext modulus.
     */
    public RnsTool(int polyModulusDegree, RnsBase coeffModulus, Modulus plainModulus) {
        initialize(polyModulusDegree, coeffModulus, plainModulus);
    }

    /**
     * Generates the pre-computations for the given parameters.
     *
     * @param polyModulusDegree the degree of the polynomial.
     * @param q                 the coefficient modulus.
     * @param t                 the plaintext modulus.
     */
    private void initialize(int polyModulusDegree, RnsBase q, Modulus t) {
        // Return if q is out of bounds
        if (q.size() < Constants.SEAL_COEFF_MOD_COUNT_MIN || q.size() > Constants.SEAL_COEFF_MOD_COUNT_MAX) {
            throw new IllegalArgumentException("rns base is invalid.");
        }
        // Return if coeff_count is not a power of two or out of bounds
        int coeffCountPower = UintCore.getPowerOfTwo(polyModulusDegree);
        if (coeffCountPower < 0 || polyModulusDegree > Constants.SEAL_POLY_MOD_DEGREE_MAX ||
            polyModulusDegree < Constants.SEAL_POLY_MOD_DEGREE_MIN) {
            throw new IllegalArgumentException("polyModulusDegree is invalid.");
        }
        this.t = t;
        this.coeffCount = polyModulusDegree;
        // create RNS bases q = {q_1, ..., q_k}, B = {p_1, ..., p_{k'}}, B_sk = {B, m_sk}, {B_sk, m_tilde}, {t, γ}.
        int baseQSize = q.size();
        /*
         * In some cases we might need to increase the size of the base B by one, namely we require
         * K * n * t * q^2 < q * prod(B) * m_sk, where K takes into account cross terms when larger size ciphertexts
         * are used, and n is the "delta factor" for the ring. We reserve 32 bits for K * n. Here the coeff modulus
         * primes q_i are bounded to be SEAL_USER_MOD_BIT_COUNT_MAX (60) bits, and all primes in B and m_sk are
         * SEAL_INTERNAL_MOD_BIT_COUNT (61) bits.
         */
        // the bit size of q
        int totalCoeffBitCount = UintCore.getSignificantBitCountUint(q.getBaseProd(), q.size());
        int baseBSize = baseQSize;
        // ensure K * n * t * q < prod(B) * m_sk (compared in log_2()).
        // In the left, 32 is the reserved K * n.
        // In the right, prod(B) is INTERNAL_MOD_BIT_COUNT (61) * baseBSize bits, m_sk is INTERNAL_MOD_BIT_COUNT bits.
        if (32 + this.t.bitCount() + totalCoeffBitCount >=
            Constants.SEAL_INTERNAL_MOD_BIT_COUNT * baseQSize + Constants.SEAL_INTERNAL_MOD_BIT_COUNT) {
            baseBSize++;
        }
        // B_{sk} = {B, m_sk}, baseBskSize = baseBSize + 1
        int baseBskSize = Common.addSafe(baseBSize, 1, true);
        // {B_sk, m_tilde}, baseBskMTildeSize = baseBskSize + 1
        int baseBskMTildeSize = Common.addSafe(baseBskSize, 1, true);
        int baseTGammaSize = 0;
        // Sample primes for m_sk, γ, and B = (p_1, ..., p_{k'}), each is INTERNAL_MOD_BIT_COUNT (61) bits.
        Modulus[] baseConvPrimes = Numth.getPrimes(Common.mulSafe(2, coeffCount, true), Constants.SEAL_INTERNAL_MOD_BIT_COUNT, baseBskMTildeSize);
        mSk = baseConvPrimes[0];
        gamma = baseConvPrimes[1];
        Modulus[] baseBPrimes = new Modulus[baseBSize];
        System.arraycopy(baseConvPrimes, 2, baseBPrimes, 0, baseBSize);
        // fix m_tilde to be a non-prime value 2^32
        mTilde = new Modulus(1L << 32);
        // create each RNS bases
        baseQ = new RnsBase(q);
        baseB = new RnsBase(baseBPrimes);
        baseBsk = baseB.extend(mSk);
        baseBskMTilde = baseBsk.extend(mTilde);
        // Set up t-gamma base if t_ is non-zero (using BFV)
        if (!this.t.isZero()) {
            baseTGammaSize = 2;
            baseTGamma = new RnsBase(new Modulus[]{t, gamma});
        }
        // Generate the B_sk NTTTables; these are used for NTT after base extension to B_sk
        baseBskNttTables = new NttTables[baseBskSize];
        try {
            NttTables.createNttTables(coeffCountPower, baseBsk.getBase(), baseBskNttTables);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid rns bases");
        }
        if (!this.t.isZero()) {
            // Set up BaseConvTool for q --> {t}
            baseQToTConv = new BaseConverter(baseQ, new RnsBase(new Modulus[]{this.t}));
        }
        // Set up BaseConverter for q --> B_sk
        baseQToBskConv = new BaseConverter(baseQ, baseBsk);
        // Set up BaseConverter for q --> {m_tilde}
        baseQToMTildeConv = new BaseConverter(baseQ, new RnsBase(new Modulus[]{mTilde}));
        // Set up BaseConverter for B --> q
        baseBToQConv = new BaseConverter(baseB, baseQ);
        // Set up BaseConverter for B --> {m_sk}
        baseBToMskConv = new BaseConverter(baseB, new RnsBase(new Modulus[]{mSk}));
        if (baseTGamma != null) {
            // Set up BaseConverter for q --> {t, gamma}
            baseQToTGammaConv = new BaseConverter(baseQ, baseTGamma);
        }
        // Compute prod(B) mod q = [q1, q2, ..., qk]
        prodBModQ = new long[baseQSize];
        for (int i = 0; i < baseQSize; i++) {
            prodBModQ[i] = UintArithmeticSmallMod.moduloUint(baseB.getBaseProd(), baseBSize, baseQ.getBase(i));
        }
        // Compute prod(q)^(-1) mod B_sk, which has many modulus
        invProdQModBsk = new MultiplyUintModOperand[baseBskSize];
        for (int i = 0; i < baseBskSize; i++) {
            invProdQModBsk[i] = new MultiplyUintModOperand();
        }
        for (int i = 0; i < baseBskSize; i++) {
            // first reduce prod(q) to B_sk[i]
            long innerTemp = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseBsk.getBase(i));
            // then compute inverse
            long[] innerInvTemp = new long[1];
            if (!UintArithmeticSmallMod.tryInvertUintMod(innerTemp, baseBsk.getBase(i), innerInvTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            // initialize a MultiplyUintModOperand
            invProdQModBsk[i].set(innerInvTemp[0], baseBsk.getBase(i));
        }
        // used for store the result of reduce and invert
        long temp;
        long[] invTemp = new long[1];
        // Compute prod(B)^(-1) mod m_sk, m_sk is a single modulus
        temp = UintArithmeticSmallMod.moduloUint(baseB.getBaseProd(), baseBSize, mSk);
        if (!UintArithmeticSmallMod.tryInvertUintMod(temp, mSk, invTemp)) {
            throw new IllegalArgumentException("invalid rns bases");
        }
        invProdBModMsk = new MultiplyUintModOperand();
        invProdBModMsk.set(invTemp[0], mSk);
        // Compute m_tilde^(-1) mod B_sk
        invMTildeModBsk = new MultiplyUintModOperand[baseBskSize];
        for (int i = 0; i < baseBskSize; i++) {
            invMTildeModBsk[i] = new MultiplyUintModOperand();
        }
        for (int i = 0; i < baseBskSize; i++) {
            long[] innerInvTemp = new long[1];
            if (!UintArithmeticSmallMod.tryInvertUintMod(
                UintArithmeticSmallMod.barrettReduce64(mTilde.value(), baseBsk.getBase(i)),
                baseBsk.getBase(i),
                innerInvTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            invMTildeModBsk[i].set(innerInvTemp[0], baseBsk.getBase(i));
        }
        // Compute -prod(q)^(-1) mod m_tilde
        temp = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, mTilde);
        if (!UintArithmeticSmallMod.tryInvertUintMod(temp, mTilde, invTemp)) {
            throw new IllegalArgumentException("invalid rns bases");
        }
        // note that the neg
        negInvProdQModMTilde = new MultiplyUintModOperand();
        negInvProdQModMTilde.set(UintArithmeticSmallMod.negateUintMod(invTemp[0], mTilde), mTilde);
        // Compute prod(q) mod B_sk
        prodQModBsk = new long[baseBskSize];
        for (int i = 0; i < baseBskSize; i++) {
            prodQModBsk[i] = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseBsk.getBase(i));
        }
        if (baseTGamma != null) {
            // Compute γ^(-1) mod t
            if (!UintArithmeticSmallMod.tryInvertUintMod(
                UintArithmeticSmallMod.barrettReduce64(gamma.value(), this.t), this.t, invTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            invGammaModT = new MultiplyUintModOperand();
            invGammaModT.set(invTemp[0], this.t);
            // Compute prod({t, γ}) mod q
            prodTGammaModQ = new MultiplyUintModOperand[baseQSize];
            for (int i = 0; i < baseQSize; i++) {
                prodTGammaModQ[i] = new MultiplyUintModOperand();
            }
            for (int i = 0; i < baseQSize; i++) {
                prodTGammaModQ[i].set(
                    // t * γ mod q_i, SEAL use UintArithmeticSmallMod.multiplyUintMod(t, γ, baseQ.getBase(i))
                    // Here, we use UintArithmeticSmallMod.moduloUint(baseTGamma.getBaseProd(), 2, baseQ.getBase(i))
                    UintArithmeticSmallMod.moduloUint(baseTGamma.getBaseProd(), 2, baseQ.getBase(i)),
                    baseQ.getBase(i)
                );
            }
            // Compute -prod(q)^(-1) mod {t, γ}
            negInvQModTGamma = new MultiplyUintModOperand[baseTGammaSize];
            for (int i = 0; i < baseTGammaSize; i++) {
                negInvQModTGamma[i] = new MultiplyUintModOperand();
            }
            for (int i = 0; i < baseTGammaSize; i++) {
                long curTemp = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseTGamma.getBase(i));
                long[] curInvTemp = new long[1];
                if (!UintArithmeticSmallMod.tryInvertUintMod(curTemp, baseTGamma.getBase(i), curInvTemp)) {
                    throw new IllegalArgumentException("invalid rns bases");
                }
                // neg
                negInvQModTGamma[i].set(
                    UintArithmeticSmallMod.negateUintMod(curInvTemp[0], baseTGamma.getBase(i)), baseTGamma.getBase(i)
                );
            }
        }
        // Compute q[last]^(-1) mod q[i] for i = [0, last - 1], i.e. q_k mod q_1, q_k mod q_2, ..., q_k mod q_{k-1}
        // This is used by modulus switching and rescaling
        invQLastModQ = new MultiplyUintModOperand[baseQSize - 1];
        for (int i = 0; i < baseQSize - 1; i++) {
            invQLastModQ[i] = new MultiplyUintModOperand();
        }
        long qLast = baseQ.getBase(baseQSize - 1).value();
        for (int i = 0; i < baseQSize - 1; i++) {
            long[] curInvTemp = new long[1];
            if (!UintArithmeticSmallMod.tryInvertUintMod(qLast, baseQ.getBase(i), curInvTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            invQLastModQ[i].set(curInvTemp[0], baseQ.getBase(i));
        }
        // compute q_k mod t and (q_k)^{-1} mod t
        if (!t.isZero()) {
            if (!UintArithmeticSmallMod.tryInvertUintMod(qLast, this.t, invTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            invQLastModT = invTemp[0];
            qLastModT = UintArithmeticSmallMod.barrettReduce64(qLast, this.t);
        }
    }

    /**
     * Decrypts a ciphertext and stores the result in destination. The algorithm is from Algorithm 1 in [BEHZ16].
     *
     * @param input       polynomial in RNS form to decrypt.
     * @param inputN      the coefficient count of the polynomial.
     * @param destination the result to overwrite with decrypted values, the length is the coefficient count.
     */
    public void decryptScaleAndRound(long[] input, int inputN, long[] destination) {
        assert input != null;
        assert inputN == this.coeffCount;
        assert destination.length == this.coeffCount;
        int baseQSize = baseQ.size();
        // the decryption RNS modulus {t, γ}
        int baseTGammaSize = baseTGamma.size();
        // step 1-3: compute |γt · ct(s)|_{q_1, ..., q_k}, where |γt|_{q_1, ..., q_k} is pre-computed
        long[] temp = PolyCore.allocateZeroPolyArray(baseQSize, inputN, 1);
        for (int i = 0; i < baseQSize; i++) {
            // compute temp = |γt · ct(s)|_{q_1, ..., q_k}
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                input, i * inputN, inputN, prodTGammaModQ[i], baseQ.getBase(i), temp, i * inputN
            );
        }
        // compute s^{t, γ} = FastBconv(temp, {q_1, ..., q_k}, {t, γ})
        // therefore, s^{t, γ} = FastBconv(|γt · ct(s)|_{q_1, ..., q_k}, {q_1, ..., q_k}, {t, γ})
        long[] tempTGammaRns = PolyCore.allocateZeroPolyArray(baseTGammaSize, inputN, 1);
        baseQToTGammaConv.fastConvertArrayRnsIter(
            temp, 0, inputN, baseQSize, tempTGammaRns, 0, inputN, baseTGammaSize
        );
        // compute s^{t, γ} = s^{t, γ} × |-q^{-1}|_{t, γ}
        // therefore, s^{t, γ} = FastBconv(|γt · ct(s)|_{q_1, ..., q_k}, {q_1, ..., q_k}, {t, γ}) × |-q^{-1}|_{t, γ}
        for (int i = 0; i < baseTGammaSize; i++) {
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                tempTGammaRns, i * inputN, inputN,
                negInvQModTGamma[i], baseTGamma.getBase(i),
                tempTGammaRns, i * inputN
            );
        }
        // step 4: ~s^(γ) ← [s^(γ)]_{γ}, that is, subtract q/2 if the coefficient is in [q/2, q).
        // step 5(1): m^(t) ← [(s^(t) - ~s^(γ))]_t
        long gammaDiv2 = baseTGamma.getBase(1).value() >>> 1;
        for (int i = 0; i < inputN; i++) {
            if (tempTGammaRns[inputN + i] > gammaDiv2) {
                // the coefficient is in [q/2, q).
                // compute |s^(t) + (γ - |s^(γ)|_{γ})|_t instead of |s^t - (|s^(γ)|_{γ} - γ)|_t
                destination[i] = UintArithmeticSmallMod.addUintMod(
                    tempTGammaRns[i],
                    UintArithmeticSmallMod.barrettReduce64(gamma.value() - tempTGammaRns[inputN + i], t),
                    t
                );
            } else {
                // the coefficient is in [0, q/2], compute |s^t - |s^(γ)|_{γ}|_t
                destination[i] = UintArithmeticSmallMod.subUintMod(
                    tempTGammaRns[i],
                    UintArithmeticSmallMod.barrettReduce64(tempTGammaRns[inputN + i], t),
                    t
                );
            }
            // step 5(2): m^(t) ← [m^(t) × |γ^{-1}|_{t}]_t, therefore, m^(t) ← [[(s^(t) - ~s^(γ))]_t × |γ^{-1}|_{t}]_t
            if (destination[i] != 0) {
                destination[i] = UintArithmeticSmallMod.multiplyUintMod(destination[i], invGammaModT, t);
            }
        }
    }

    /**
     * Converts fast ct * m_tilde from base q = (q_1, ..., q_k) to base {B, m_sk, m_tilde}.
     * <p>The input is the ciphertext ct in base q = (q_1, ..., q_k).</p>
     * <p>The output (written in destination) is the ciphertext ct * m_tilde in base {B, m_sk, m_tilde}.</p>
     * This is the Step S0 of Algorithm 3 in the BEHZ16 paper. The reason for multiplying m_tilde is shown in Lemma 4.
     *
     * @param input          the ciphertext ct in base q = (q_1, ..., q_k).
     * @param inputPos       start index of input ciphertext.
     * @param inputN         coefficient count of input ciphertext.
     * @param inputK         coefficient modulus size of input ciphertext.
     * @param destination    the destination ciphertext ct * m_tilde in base {B, m_sk, m_tilde}.
     * @param destinationPos start index of destination ciphertext.
     * @param destinationN   coefficient count of destination ciphertext.
     * @param destinationK   coefficient modulus size of destination ciphertext.
     */
    public void fastBConvMTildeRnsIter(
        long[] input, int inputPos, int inputN, int inputK,
        long[] destination, int destinationPos, int destinationN, int destinationK
    ) {
        assert input != null && destination != null;
        assert inputN == coeffCount;
        assert destinationN == coeffCount;
        assert inputK == baseQ.size();
        assert destinationK == baseBskMTilde.size();
        /*
         * Require: Input in q.
         * Ensure: Output in {B_sk, m_tilde}.
         */
        int baseQSize = baseQ.size();
        int baseBskSize = baseBsk.size();
        // We need to multiply first the input with m_tilde mod q
        // This is to facilitate Montgomery reduction in the next step of multiplication
        // This is NOT an ideal approach: as mentioned in BEHZ16, multiplication by
        // m_tilde can be easily merge into the base conversion operation; however, then
        // we could not use the BaseConverter as below without modifications.
        long[] temp = RnsIterator.allocateZeroRns(coeffCount, baseQSize);
        // (input * m_tilde) mod q
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
            input, inputPos, inputN, baseQSize,
            mTilde.value(), baseQ.getBase(),
            temp, 0, coeffCount, baseQSize
        );
        // Now convert to B_sk, i.e., FastBcov([input * m_tilde]_q, q, {B_sk})
        long[] tempOut = RnsIterator.allocateZeroRns(coeffCount, baseBskSize);
        baseQToBskConv.fastConvertArrayRnsIter(
            temp, 0, coeffCount, baseQSize,
            tempOut, 0, coeffCount, baseBskSize
        );
        // Finally, convert to {m_tilde}, i.e., FastBcov([input * m_tilde]_q, q, {m_tilde})
        long[] tempOut2 = RnsIterator.allocateZeroRns(coeffCount, 1);
        baseQToMTildeConv.fastConvertArrayRnsIter(
            temp, 0, coeffCount, baseQSize,
            tempOut2, 0, coeffCount, 1
        );
        // output = {tempOut, tempOut2} = {[input * m_tilde]_q in {B_sk}, [input * m_tilde]_q in {m_tilde}}
        System.arraycopy(tempOut, 0, destination, destinationPos, tempOut.length);
        System.arraycopy(tempOut2, 0, destination, destinationPos + tempOut.length, tempOut2.length);
    }

    /**
     * The implementation of the function SmRq, i.e., Small Montgomery Reduction mod q.
     * <p>The input is the ciphertext c'' in base {B_sk, m_tilde}. </p>
     * <p>The output (written in destination) is the ciphertext c' in base {B_sk}. </p>
     * This is the Step S1 of Algorithm 3 in the BEHZ16 paper. The detail is shown in Algorithm 2 of the BEHZ16 paper.
     *
     * @param input          the ciphertext c'' in base {B_sk, m_tilde}.
     * @param inputPos       start index of input ciphertext.
     * @param inputN         coefficient count of input ciphertext.
     * @param inputK         coefficient modulus size of input ciphertext.
     * @param destination    the destination ciphertext c' in base {B_sk}.
     * @param destinationPos start index of destination ciphertext.
     * @param destinationN   coefficient count of destination ciphertext.
     * @param destinationK   coefficient modulus size of destination ciphertext.
     */
    public void smMrqRnsIter(
        long[] input, int inputPos, int inputN, int inputK,
        long[] destination, int destinationPos, int destinationN, int destinationK
    ) {
        assert input != null;
        assert inputN == coeffCount;
        assert destination != null;
        assert destinationN == coeffCount;
        /*
         * Require: Input in base {B_sk, m_tilde}
         * Ensure: Output in base B_sk
         */
        assert inputK == baseBskMTilde.size();
        assert destinationK == baseBsk.size();

        int baseBskSize = baseBsk.size();
        // input base is {B_sk, m_tilde}, so the size is |B_sk| + 1. The last component of the input is mod m_tilde
        int inputMTildeOffset = baseBskSize * coeffCount;
        long mTildeDiv2 = mTilde.value() >>> 1;
        // Step 1: r_{m_tilde} ← [-c''_{m_tilde} / q]_{m_tilde}.
        // Here r_{m_tilde} = |-c''_{m_tilde} / q|_{m_tilde} instead of []_{m_tilde}.
        long[] rMTilde = new long[coeffCount];
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
            input, inputPos + inputMTildeOffset, coeffCount, negInvProdQModMTilde, mTilde, rMTilde, 0
        );
        // Step 2: for m ∈ B_sk do
        for (int i = 0; i < baseBskSize; i++) {
            MultiplyUintModOperand prodQModBskElt = new MultiplyUintModOperand();
            // since the following operation for q is in |·|_m, we first reduce q to |q|_m
            prodQModBskElt.set(prodQModBsk[i], baseBsk.getBase(i));
            for (int j = 0; j < coeffCount; j++) {
                long temp = rMTilde[j];
                // convert r_{m_tilde} from |-c''_{m_tilde} / q|_{m_tilde} to [-c''_{m_tilde} / q]_{m_tilde}
                if (temp >= mTildeDiv2) {
                    temp += (baseBsk.getBase(i).value() - mTilde.value());
                }
                // c = |c''_m + q · r_{m_tilde}|_m
                long c = UintArithmeticSmallMod.multiplyAddUintMod(
                    temp, prodQModBskElt, input[inputPos + i * inputN + j], baseBsk.getBase(i)
                );
                // c'_m ← |c · {m_tilde}^(-1)|_m = |(c''_m + q · r_{m_tilde}) · {m_tilde}^(-1)|_{m}
                destination[destinationPos + i * destinationN + j]
                    = UintArithmeticSmallMod.multiplyUintMod(c, invMTildeModBsk[i], baseBsk.getBase(i));
            }
        }
    }

    /**
     * The implementation of the function fastRNSFloor, i.e., fast RNS floor.
     * <p>The input is the ciphertext a in base {q, B_sk}.</p>
     * <p>The output is the ciphertext a in base B_sk.</p>
     * This is the Step S3 of Algorithm 3 in the BEHZ16 paper. The detail is shown in Lemma 5 of the BEHZ16 paper.
     *
     * @param input          the ciphertext a in base {q, B_sk}.
     * @param inputPos       start index of input ciphertext.
     * @param inputN         coefficient count of input ciphertext.
     * @param inputK         coefficient modulus size of input ciphertext.
     * @param destination    the ciphertext a in base B_sk.
     * @param destinationPos start index of destination ciphertext.
     * @param destinationN   coefficient count of destination ciphertext.
     * @param destinationK   coefficient modulus size of destination ciphertext.
     */
    public void fastFloorRnsIter(
        long[] input, int inputPos, int inputN, int inputK,
        long[] destination, int destinationPos, int destinationN, int destinationK
    ) {
        assert input != null;
        assert inputN == coeffCount;
        assert destination != null;
        assert destinationN == coeffCount;
        /*
         * Require: Input in base {q, B_sk}
         * Ensure: Output in base B_sk
         */
        assert inputK == baseQ.size() + baseBsk.size();
        assert destinationK == baseBsk.size();
        int baseQSize = baseQ.size();
        int baseBskSize = baseBsk.size();
        // FactBCov(|a|_q, q, B_sk)
        int inputInBaseQ = 0;
        baseQToBskConv.fastConvertArrayRnsIter(
            input, inputPos + inputInBaseQ, inputN, baseQ.size(),
            destination, destinationPos, destinationN, destinationK
        );
        int inputInBsk = baseQSize * coeffCount;
        for (int i = 0; i < baseBskSize; i++) {
            for (int j = 0; j < coeffCount; j++) {
                // It is not necessary for the negation to be reduced modulo base_Bsk_elt
                // (a - FastBConv(|a|_q, q, B_sk)) * q^{-1} mod B_sk
                destination[destinationPos + i * coeffCount + j]
                    = UintArithmeticSmallMod.multiplyUintMod(
                    input[inputPos + inputInBsk + i * coeffCount + j]
                        + (baseBsk.getBase(i).value() - destination[destinationPos + i * coeffCount + j]),
                    invProdQModBsk[i],
                    baseBsk.getBase(i)
                );
            }
        }
    }

    /**
     * The implementation of the function FastBconvSK, i.e., fast base conversion from B_sk to q.
     * <p>The input is the ciphertext x in base B_sk.</p>
     * <p>The output is the ciphertext x in base q.</p>
     * This is the Step S4 of Algorithm 3 in the BEHZ16 paper. The detail is shown in Lemma 6 of the BEHZ16 paper.
     * Ref Lemma 6 the equation (13) in BEHZ16
     *
     * @param input          the ciphertext x in base B_sk.
     * @param inputPos       start index of input ciphertext.
     * @param inputN         coefficient count of input ciphertext.
     * @param inputK         coefficient modulus size of input ciphertext.
     * @param destination    the ciphertext x in base q.
     * @param destinationPos start index of destination ciphertext.
     * @param destinationN   coefficient count of destination ciphertext.
     * @param destinationK   coefficient modulus size of destination ciphertext.
     */
    public void fastBConvSkRnsIter(
        long[] input, int inputPos, int inputN, int inputK,
        long[] destination, int destinationPos, int destinationN, int destinationK
    ) {
        assert inputN == coeffCount;
        assert inputK == baseBsk.size();
        assert destinationK == baseQ.size();
        assert destinationN == coeffCount;
        /*
         * Require: Input in base B_sk
         * Ensure: Output in base q
         */
        int baseQSize = baseQ.size();
        int baseBSize = baseB.size();
        // FastBcov(x, B, q)
        int inputInBaseB = 0;
        baseBToQConv.fastConvertArrayRnsIter(
            input, inputPos + inputInBaseB, coeffCount, baseBSize,
            destination, destinationPos, destinationN, destinationK
        );
        // Compute α_{sk, x}, Fast convert B -> {m_sk}; input is in B_sk, but we only use B
        long[] temp = new long[coeffCount];
        // FastBonv(x, B, {m_sk})
        baseBToMskConv.fastConvertArrayRnsIter(
            input, inputPos + inputInBaseB, coeffCount, baseBSize,
            temp, 0, coeffCount, 1
        );
        // α_{sk, x} = [(FastBonv(x, B, {m_sk}) - x_{sk}) · B^{-1}]_{m_{sk}}, where B in inv_prod_B_mod_m_sk
        // Take the m_sk part of input, subtract from temp, and multiply by inv_prod_B_mod_m_sk_
        // Note: input_sk is allocated in input[base_B_size]
        long[] alphaSk = new long[coeffCount];
        // we first compute |(FastBonv(x, B, {m_sk}) - x_{sk}) · B^{-1}|_{m_{sk}}
        for (int i = 0; i < coeffCount; i++) {
            alphaSk[i] = UintArithmeticSmallMod.multiplyUintMod(
                temp[i] + (mSk.value() - input[inputPos + baseBSize * coeffCount + i]), invProdBModMsk, mSk
            );
        }
        // α_{sk, x} is now ready for the Shenoy-Kumaresan conversion; however, note that our
        // α_{sk, x} here is not a centered reduction, so we need to apply a correction below.
        long mSkDiv2 = mSk.value() >>> 1;
        for (int i = 0; i < baseQSize; i++) {
            // Set up the multiplication helpers
            MultiplyUintModOperand prodBModQElt = new MultiplyUintModOperand();
            prodBModQElt.set(prodBModQ[i], baseQ.getBase(i));

            MultiplyUintModOperand negProdBModQElt = new MultiplyUintModOperand();
            negProdBModQElt.set(baseQ.getBase(i).value() - prodBModQ[i], baseQ.getBase(i));

            for (int j = 0; j < coeffCount; j++) {
                // Correcting α_sk since it represents a negative value
                // then compute |FastBcov(x, B, q) - α_{sk, x} · M|_{q_i}
                if (alphaSk[j] > mSkDiv2) {
                    destination[destinationPos + i * coeffCount + j] =
                        UintArithmeticSmallMod.multiplyAddUintMod(
                            UintArithmeticSmallMod.negateUintMod(alphaSk[j], mSk), prodBModQElt,
                            destination[destinationPos + i * coeffCount + j], baseQ.getBase(i)
                        );
                } else {
                    // No correction needed
                    // directly compute |FastBcov(x, B, q) - α_{sk, x} · M|_{q_i}
                    // It is not necessary for the negation to be reduced modulo the small prime
                    destination[destinationPos + i * coeffCount + j] = UintArithmeticSmallMod.multiplyAddUintMod(
                        alphaSk[j], negProdBModQElt, destination[destinationPos + i * coeffCount + j], baseQ.getBase(i)
                    );
                }
            }
        }
    }

    /**
     * Divides and round the last q_k.
     * <p>The input is (ct mod q_1, ct mod q_2, ..., ct mod q_k).</p>
     * <p>The output is (ct mod q_1, ct mod q_2, ..., ct mod q_{k - 1}), but (ct mod q_k) is not removed.</p>
     *
     * @param poly ciphertext.
     * @param pos  start index of the ciphertext.
     * @param n    coefficient count of ciphertext.
     * @param k    coefficient modulus size of ciphertext.
     */
    public void divideAndRoundQLastInplace(long[] poly, int pos, int n, int k) {
        assert n == coeffCount;
        assert k == baseQ.size();

        int baseQSize = baseQ.size();
        // the position of (ct mod q_k)
        int lastInputIndex = pos + (baseQSize - 1) * n;

        // add (q_k - 1) / 2 to (ct mod q_k) to change from flooring to rounding
        Modulus lastModulus = baseQ.getBase(baseQSize - 1);
        long half = lastModulus.value() >>> 1;
        PolyArithmeticSmallMod.addPolyScalarCoeffMod(
            poly, lastInputIndex, coeffCount, half, lastModulus, poly, lastInputIndex
        );
        for (int i = 0; i < baseQSize - 1; i++) {
            long[] temp = new long[coeffCount];
            // (ct mod q_k) mod q_i
            PolyArithmeticSmallMod.moduloPolyCoeff(poly, lastInputIndex, coeffCount, baseQ.getBase(i), temp, 0);
            // Subtract rounding correction here; the negative sign will turn into a plus in the next subtraction
            long halfMod = UintArithmeticSmallMod.barrettReduce64(half, baseQ.getBase(i));
            PolyArithmeticSmallMod.subPolyScalarCoeffMod(temp, coeffCount, halfMod, baseQ.getBase(i), temp);
            // (ct mod q_i) - (ct mod q_k) mod q_i, where (ct mod q_i) is in [i * N, (i + 1) * N)
            PolyArithmeticSmallMod.subPolyCoeffMod(
                poly, pos + i * coeffCount, temp, 0, coeffCount, baseQ.getBase(i), poly, pos + i * coeffCount
            );
            // (ct mod q_i) = q_k^(-1) * ((ct mod q_i) - (ct mod q_k)) mod q_i,
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                poly, pos + i * coeffCount, coeffCount, invQLastModQ[i], baseQ.getBase(i), poly, pos + i * coeffCount
            );
        }
    }

    /**
     * Divides and round the last q_k, where ct is in NTT form.
     * <p>The input is (ct mod q_1, ct mod q_2, ..., ct mod q_k) in NTT form.</p>
     * <p>The output is (ct mod q_1, ct mod q_2, ..., ct mod q_{k - 1}) in NTT form, but (ct mod q_k) is not removed.</p>
     *
     * @param poly         ciphertext.
     * @param pos          start index of ciphertext.
     * @param n            coefficient count of ciphertext.
     * @param k            coefficient modulus size of ciphertext.
     * @param rnsNttTables NTT tables.
     */
    public void divideAndRoundQLastNttInplace(long[] poly, int pos, int n, int k, NttTables[] rnsNttTables) {
        assert poly != null;
        assert n == coeffCount;
        assert k == rnsNttTables.length;

        int baseQSize = baseQ.size();
        // the position of (ct mod q_k)
        int lastInputIndex = pos + (baseQSize - 1) * coeffCount;

        // convert to non-NTT form, modulus switching operation needs the ciphertext be in coefficient form
        NttTool.inverseNttNegacyclicHarvey(poly, lastInputIndex, rnsNttTables[baseQSize - 1]);

        // add (q_k - 1) / 2 to change from flooring to rounding
        Modulus lastModulus = baseQ.getBase(baseQSize - 1);
        long half = lastModulus.value() >>> 1;
        PolyArithmeticSmallMod.addPolyScalarCoeffMod(
            poly, lastInputIndex, coeffCount, half, lastModulus, poly, lastInputIndex
        );
        for (int i = 0; i < baseQSize - 1; i++) {
            long[] temp = new long[coeffCount];
            // (ct mod q_k) mod q_i
            if (baseQ.getBase(i).value() < lastModulus.value()) {
                PolyArithmeticSmallMod.moduloPolyCoeff(poly, lastInputIndex, coeffCount, baseQ.getBase(i), temp, 0);
            } else {
                System.arraycopy(poly, lastInputIndex, temp, 0, coeffCount);
            }
            // lazy subtraction here. ntt_negacyclic_harvey_lazy can take 0 < x < 4 * q_i input.
            long negHalfMod = baseQ.getBase(i).value() - UintArithmeticSmallMod.barrettReduce64(half, baseQ.getBase(i));
            for (int j = 0; j < coeffCount; j++) {
                temp[j] += negHalfMod;
            }
            long qiLazy;
            if (Constants.SEAL_USER_MOD_BIT_COUNT_MAX <= 60) {
                // Since SEAL uses at most 60-bit moduli, 8 * qi < 2^63,
                // this ntt_negacyclic_harvey_lazy results in [0, 4 * q_i).
                qiLazy = baseQ.getBase(i).value() << 2;
                NttTool.nttNegacyclicHarveyLazy(temp, rnsNttTables[i]);
            } else {
                // 2^60 < pi < 2^62, then 4 * pi < 2^64,
                // we perform one reduction from [0, 4 * q_i) to [0, 2 * q_i) after NTT.
                qiLazy = baseQ.getBase(i).value() << 1;
                NttTool.nttNegacyclicHarveyLazy(temp, rnsNttTables[i]);
                // from lazy NTT to correct result
                for (int j = 0; j < coeffCount; j++) {
                    temp[j] -= (qiLazy & (temp[j] >= qiLazy ? -1 : 0));
                }
            }
            // Lazy subtraction again, results in [0, 2 * qi_lazy),
            // The reduction [0, 2 * qi_lazy) -> [0, qi) is done implicitly in multiply_poly_scalar_coeffmod.
            for (int j = 0; j < coeffCount; j++) {
                poly[pos + i * coeffCount + j] += (qiLazy - temp[j]);
            }
            // q_k^(-1) * ct' mod q_i
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                poly, pos + i * coeffCount, coeffCount, invQLastModQ[i], baseQ.getBase(i), poly, pos + i * coeffCount
            );
        }
    }

    /**
     * Compute decryption mod t.
     *
     * @param phase       ciphertext.
     * @param n           coefficient count of ciphertext.
     * @param k           coefficient modulus size of ciphertext.
     * @param destination destination.
     */
    public void decryptModT(long[] phase, int n, int k, long[] destination) {
        // Use exact base conversion rather than convert the base through the compose API
        baseQToTConv.exactConvertArray(phase, n, k, destination);
    }

    /**
     * Gets |(q_k)^{-1}|_t.
     *
     * @return |(q_k)^{-1}|_t.
     */
    public long invQLastModT() {
        return invQLastModT;
    }

    /**
     * Gets NTT tables for RNS base {B, m_sk}.
     *
     * @return NTT tables for RNS base {B, m_sk}.
     */
    public NttTables[] baseBskNttTables() {
        return baseBskNttTables;
    }

    /**
     * Gets RNS base q = (q_1, ..., q_k) for q = q_1 · q_2 ... · q_k.
     *
     * @return RNS base q = (q_1, ..., q_k).
     */
    public RnsBase baseQ() {
        return baseQ;
    }

    /**
     * Gets RNS base B = {p_1, ..., p_{k'}} for B = p_1 · p_2 ... · p_{k'}.
     *
     * @return RNS base B = {p_1, ..., p_{k'}}.
     */
    public RnsBase getBaseB() {
        return baseB;
    }

    /**
     * Gets RNS base B_sk = {B, m_sk}.
     *
     * @return RNS base B_sk.
     */
    public RnsBase baseBsk() {
        return baseBsk;
    }

    /**
     * Gets RNS base {B_sk, m_tilde}.
     *
     * @return RNS base {B_sk, m_tilde}.
     */
    public RnsBase baseBskMTilde() {
        return baseBskMTilde;
    }

    /**
     * Gets RNS base {t, γ}.
     *
     * @return RNS base {t, γ}.
     */
    public RnsBase getBaseTGamma() {
        return baseTGamma;
    }

    /**
     * Gets m_tilde.
     *
     * @return m_tilde.
     */
    public Modulus getMTilde() {
        return mTilde;
    }

    /**
     * Gets m_sk.
     *
     * @return m_sk.
     */
    public Modulus getMsk() {
        return mSk;
    }

    /**
     * Gets plaintext modulus t.
     *
     * @return plaintext modulus t.
     */
    public Modulus getT() {
        return t;
    }

    /**
     * Gets γ, an integer co-prime to {q_1, ..., q_k}, γ ≡ 1 (mod 2n).
     *
     * @return γ.
     */
    public Modulus getGamma() {
        return gamma;
    }

    /**
     * Gets |q_k^{-1}|_{q_1}, |q_k^{-1}|_{q_2}, ..., |q_k^{-1}|_{q_{k-1}}.
     *
     * @return |q_k^{-1}|_{q_1}, |q_k^{-1}|_{q_2}, ..., |q_k^{-1}|_{q_{k-1}}.
     */
    public MultiplyUintModOperand[] getInvQLastModQ() {
        return invQLastModQ;
    }

    /**
     * Gets |q_k|_t.
     *
     * @return |q_k|_t.
     */
    public long getqLastModT() {
        return qLastModT;
    }
}
