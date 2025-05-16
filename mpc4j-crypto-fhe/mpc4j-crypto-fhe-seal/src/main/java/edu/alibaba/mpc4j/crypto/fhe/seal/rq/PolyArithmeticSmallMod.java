package edu.alibaba.mpc4j.crypto.fhe.seal.rq;

import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.AbstractModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.MultiplyUintModOperand;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintCore;

/**
 * This class provides modular arithmetic for polynomials.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/polyarithsmallmod.h">
 * polyarithsmallmod.h
 * </a>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/20
 */
public class PolyArithmeticSmallMod {
    /**
     * private constructor.
     */
    private PolyArithmeticSmallMod() {
        // empty
    }

    /**
     * Mods the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void moduloPolyCoeff(CoeffIterator coeff, int n, AbstractModulus modulus, CoeffIterator coeffR) {

        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            coeffR.setCoeff(
                i,
                UintArithmeticSmallMod.barrettReduce64(coeff.getCoeff(i), modulus)
            );
        }
    }

    /**
     * Mods the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     */
    public static void moduloPolyCoeffRns(RnsIterator rns, int k, AbstractModulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rns.n() == rnsR.n();

        for (int j = 0; j < k; j++) {
            moduloPolyCoeff(rns.coeffIter[j], rns.n(), modulus[j], rnsR.coeffIter[j]);
        }
    }

    /**
     * Mods the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param m       the number of RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     */
    public static void moduloPolyCoeffPoly(PolyIterator poly, int m, AbstractModulus[] modulus, PolyIterator polyR) {
        assert poly.k() == polyR.k();
        assert m > 0;

        for (int r = 0; r < m; r++) {
            moduloPolyCoeffRns(poly.rnsIter[r], polyR.k(), modulus, polyR.rnsIter[r]);
        }
    }

    /**
     * Negates the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void negatePolyCoeffMod(CoeffIterator coeff, int n, AbstractModulus modulus, CoeffIterator coeffR) {

        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long nonZero;
        for (int i = 0; i < n; i++) {
            assert coeff.getCoeff(i) < modulusValue;
            nonZero = coeff.getCoeff(i) != 0 ? 1 : 0;
            coeffR.setCoeff(i, (modulusValue - coeff.getCoeff(i)) & (-nonZero));
        }
    }

    /**
     * Negates the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param k       the number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     */
    public static void negatePolyCoeffModRns(RnsIterator rns, int k, AbstractModulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rns.k() == rnsR.k();
        assert rns.n() == rnsR.n();

        for (int j = 0; j < k; j++) {
            negatePolyCoeffMod(rns.coeffIter[j], rnsR.n(), modulus[j], rnsR.coeffIter[j]);
        }
    }

    /**
     * Negates the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param m       the number of negated RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     */
    public static void negatePolyCoeffModPoly(PolyIterator poly, int m, AbstractModulus[] modulus, PolyIterator polyR) {
        assert poly.k() == polyR.k();
        assert m > 0;

        for (int r = 0; r < m; r++) {
            negatePolyCoeffModRns(poly.rnsIter[r], polyR.k(), modulus, polyR.rnsIter[r]);
        }
    }


    /**
     * Negates the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param posR    the result start position.
     * @param coeffR  the result Coeff representation.
     */
    public static void negatePolyCoeffMod(long[] coeff, int pos, int n,
                                          AbstractModulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long nonZero;
        for (int i = 0; i < n; i++) {
            assert coeff[pos + i] < modulusValue;
            nonZero = coeff[pos + i] != 0 ? 1 : 0;
            coeffR[posR + i] = (modulusValue - coeff[pos + i]) & (-nonZero);
        }
    }

    /**
     * Adds two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param coeff2  the 2nd Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void addPolyCoeffMod(CoeffIterator coeff1, CoeffIterator coeff2, int n,
                                       AbstractModulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long sum;

        for (int i = 0; i < n; i++) {
            assert coeff1.getCoeff(i) < modulusValue;
            assert coeff2.getCoeff(i) < modulusValue;

            sum = coeff1.getCoeff(i) + coeff2.getCoeff(i);
            coeffR.setCoeff(
                i,
                sum >= modulusValue ? sum - modulusValue : sum
            );
        }
    }

    /**
     * Adds two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param pos1    the 1st start position.
     * @param coeff2  the 2nd Coeff representation.
     * @param pos2    the 2nd start position.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void addPolyCoeffMod(long[] coeff1, int pos1, long[] coeff2, int pos2, int n,
                                       AbstractModulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long sum;
        for (int i = 0; i < n; i++) {
            assert coeff1[pos1 + i] < modulusValue;
            assert coeff2[pos2 + i] < modulusValue;
            sum = coeff1[pos1 + i] + coeff2[pos2 + i];
            coeffR[posR + i] = sum >= modulusValue ? sum - modulusValue : sum;
        }
    }

    /**
     * Adds two RNS representations.
     *
     * @param rns1    the 1st RNS representation.
     * @param rns2    the 2nd RNS representation.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param k       the result number of RNS bases.
     */
    public static void addPolyCoeffMod(RnsIterator rns1, RnsIterator rns2, int k,
                                       AbstractModulus[] modulus, RnsIterator rnsR) {
        assert rnsR.k() == rns1.k() && rnsR.k() == rns2.k();
        assert rnsR.k() == modulus.length;
        assert k > 0;
        assert rnsR.n() == rns1.n() && rnsR.n() == rns2.n();

        for (int j = 0; j < k; j++) {
            addPolyCoeffMod(rns1.coeffIter[j], rns2.coeffIter[j], rnsR.n(), modulus[j], rnsR.coeffIter[j]);
        }
    }

    /**
     * Adds the first m RNS representations in the two Poly-RNS representations.
     *
     * @param poly1   the 1st Poly-RNS representation.
     * @param poly2   the 2nd Poly-RNS representation.
     * @param m       the number of added RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-NRS representation.
     */
    public static void addPolyCoeffModPoly(PolyIterator poly1, PolyIterator poly2, int m,
                                           AbstractModulus[] modulus, PolyIterator polyR) {
        assert m > 0;
        assert poly1.k() == polyR.k() && poly2.k() == polyR.k();

        for (int r = 0; r < m; r++) {
            addPolyCoeffMod(poly1.rnsIter[r], poly2.rnsIter[r], polyR.k(), modulus, polyR.rnsIter[r]);
        }
    }

    /**
     * Subtracts two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param coeff2  the 2nd Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void subPolyCoeffMod(CoeffIterator coeff1, CoeffIterator coeff2, int n,
                                       AbstractModulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long[] tempResult = new long[1];
        long borrow;
        for (int i = 0; i < n; i++) {
            assert coeff1.getCoeff(i) < modulusValue;
            assert coeff2.getCoeff(i) < modulusValue;

            borrow = UintArithmetic.subUint64(coeff1.getCoeff(i), coeff2.getCoeff(i), tempResult);
            coeffR.setCoeff(i, tempResult[0] + (modulusValue & (-borrow)));
        }
    }

    /**
     * Subtracts two RNS representations.
     *
     * @param rns1    the 1st RNS representation.
     * @param rns2    the 2nd RNS representation.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param k       the result number of RNS bases.
     */
    public static void subPolyCoeffMod(RnsIterator rns1, RnsIterator rns2, int k,
                                       AbstractModulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rnsR.n() == rns1.n() && rnsR.n() == rns2.n();


        for (int j = 0; j < k; j++) {
            subPolyCoeffMod(
                rns1.coeffIter[j],
                rns2.coeffIter[j],
                rnsR.n(),
                modulus[j],
                rnsR.coeffIter[j]
            );
        }
    }

    /**
     * Subtracts first m RNS representations in the two Poly-RNS representations.
     *
     * @param poly1   the 1st Poly-RNS representation.
     * @param poly2   the 2nd Poly-RNS representation.
     * @param m       the number of added RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-NRS representation.
     */
    public static void subPolyCoeffModPoly(PolyIterator poly1, PolyIterator poly2, int m,
                                           AbstractModulus[] modulus, PolyIterator polyR) {
        assert polyR.k() == poly1.k() && polyR.k() == poly2.k();
        assert m > 0;

        for (int r = 0; r < m; r++) {
            subPolyCoeffMod(poly1.rnsIter[r], poly2.rnsIter[r], polyR.k(), modulus, polyR.rnsIter[r]);
        }
    }

    /**
     * Adds a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void addPolyScalarCoeffMod(CoeffIterator coeff, int n, long scalar,
                                             AbstractModulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();
        assert scalar < modulus.value();

        for (int i = 0; i < n; i++) {
            coeffR.setCoeff(i, UintArithmeticSmallMod.addUintMod(coeff.getCoeff(i), scalar, modulus));
        }
    }

    /**
     * Subtracts a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     */
    public static void subPolyScalarCoeffMod(CoeffIterator coeff, int n, long scalar,
                                             AbstractModulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();
        assert scalar < modulus.value();

        for (int i = 0; i < n; i++) {
            coeffR.setCoeff(i, UintArithmeticSmallMod.subUintMod(coeff.getCoeff(i), scalar, modulus));
        }
    }


    /**
     * Multiplies a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void multiplyPolyScalarCoeffMod(CoeffIterator coeff, int n, MultiplyUintModOperand scalar,
                                                  AbstractModulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            coeffR.setCoeff(i, UintArithmeticSmallMod.multiplyUintMod(coeff.getCoeff(i), scalar, modulus));
        }
    }

    /**
     * Multiplies a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void multiplyPolyScalarCoeffMod(CoeffIterator coeff, int n, long scalar,
                                                  AbstractModulus modulus, CoeffIterator coeffR) {
        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus), modulus);
        multiplyPolyScalarCoeffMod(coeff, n, tempScalar, modulus, coeffR);
    }

    /**
     * Multiplies a scalar to the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param k       the number of RNS bases.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     */
    public static void multiplyPolyScalarCoeffMod(RnsIterator rns, int k, long scalar,
                                                  AbstractModulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rns.n() == rnsR.n();

        for (int j = 0; j < k; j++) {
            multiplyPolyScalarCoeffMod(rns.coeffIter[j], rnsR.n(), scalar, modulus[j], rnsR.coeffIter[j]);
        }
    }

    /**
     * Multiplies a scalar to the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param m       the number of multiplied RNS representations.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     */
    public static void multiplyPolyScalarCoeffMod(PolyIterator poly, int m, long scalar,
                                                  AbstractModulus[] modulus, PolyIterator polyR) {
        assert m > 0;
        assert poly.k() == polyR.k();

        for (int r = 0; r < m; r++) {
            multiplyPolyScalarCoeffMod(poly.rnsIter[r], polyR.k(), scalar, modulus, polyR.rnsIter[r]);
        }
    }

    /**
     * Dyadic products two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param coeff2  the 2nd Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void dyadicProductCoeffMod(CoeffIterator coeff1, CoeffIterator coeff2, int n,
                                             AbstractModulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long constRation0 = modulus.constRatio()[0];
        long constRation1 = modulus.constRatio()[1];
        for (int i = 0; i < n; i++) {
            long[] z = new long[2];
            long tmp3, carry;
            long[] tmp1 = new long[1];
            long[] tmp2 = new long[2];
            // Reduces z using base 2^64 Barrett reduction
            UintArithmetic.multiplyUint64(coeff1.getCoeff(i), coeff2.getCoeff(i), z);
            // Multiply input and const_ratio
            // Round 1
            carry = UintArithmetic.multiplyUint64Hw64(z[0], constRation0);
            UintArithmetic.multiplyUint64(z[0], constRation1, tmp2);
            tmp3 = tmp2[1] + UintArithmetic.addUint64(tmp2[0], carry, tmp1);
            // Round 2
            UintArithmetic.multiplyUint64(z[1], constRation0, tmp2);
            carry = tmp2[1] + UintArithmetic.addUint64(tmp1[0], tmp2[0], tmp1);
            // This is all we care about
            tmp1[0] = z[1] * constRation1 + tmp3 + carry;
            // Barrett subtraction
            tmp3 = z[0] - tmp1[0] * modulusValue;
            // Claim: One more subtraction is enough
            coeffR.setCoeff(i, tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3);
        }
    }

    /**
     * Dyadic products two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param pos1    the 1st start position.
     * @param coeff2  the 2nd Coeff representation.
     * @param pos2    the 2nd start position.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void dyadicProductCoeffMod(long[] coeff1, int pos1, long[] coeff2, int pos2, int n,
                                             AbstractModulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long constRation0 = modulus.constRatio()[0];
        long constRation1 = modulus.constRatio()[1];
        for (int i = 0; i < n; i++) {
            long[] z = new long[2];
            long tmp3, carry;
            long[] tmp1 = new long[1];
            long[] tmp2 = new long[2];
            // Reduces z using base 2^64 Barrett reduction
            UintArithmetic.multiplyUint64(coeff1[pos1 + i], coeff2[pos2 + i], z);
            // Multiply input and const_ratio
            // Round 1
            carry = UintArithmetic.multiplyUint64Hw64(z[0], constRation0);
            UintArithmetic.multiplyUint64(z[0], constRation1, tmp2);
            tmp3 = tmp2[1] + UintArithmetic.addUint64(tmp2[0], carry, tmp1);
            // Round 2
            UintArithmetic.multiplyUint64(z[1], constRation0, tmp2);
            carry = tmp2[1] + UintArithmetic.addUint64(tmp1[0], tmp2[0], tmp1);
            // This is all we care about
            tmp1[0] = z[1] * constRation1 + tmp3 + carry;
            // Barrett subtraction
            tmp3 = z[0] - tmp1[0] * modulusValue;
            // Claim: One more subtraction is enough
            coeffR[posR + i] = tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3;
        }
    }

    /**
     * Dyadic products two RNS representations.
     *
     * @param rns1    the 1st RNS representation.
     * @param rns2    the 2nd RNS representation.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param k       the result number of RNS bases.
     */
    public static void dyadicProductCoeffMod(RnsIterator rns1, RnsIterator rns2, int k,
                                             AbstractModulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rns1.n() == rnsR.n() && rns2.n() == rnsR.n();

        for (int j = 0; j < k; j++) {
            dyadicProductCoeffMod(rns1.coeffIter[j], rns2.coeffIter[j], rnsR.n(), modulus[j], rnsR.coeffIter[j]);
        }
    }

    /**
     * Dyadic products two RNS representations.
     *
     * @param rns1    the 1st RNS representation.
     * @param pos1    the 1st start position.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param rns2    the 2nd RNS representation.
     * @param pos2    the 2nd start position.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param posR    the result start position.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void dyadicProductCoeffModRns(long[] rns1, int pos1, int n1, int k1, long[] rns2, int pos2, int n2, int k2,
                                                AbstractModulus[] modulus, long[] rnsR, int posR, int n, int k) {
        assert k == k1 && k == k2 && k == modulus.length;
        assert n == n1 && n == n2;

        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            dyadicProductCoeffMod(rns1, pos1 + jOffset, rns2, pos2 + jOffset, n, modulus[j], rnsR, posR + jOffset);
        }
    }


    /**
     * Dyadic products first m RNS representations in two Poly-RNS representations.
     *
     * @param poly1   the 1st Poly-RNS representation.
     * @param poly2   the 2nd Poly-RNS representation.
     * @param m       the number of operated RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     */
    public static void dyadicProductCoeffModPoly(PolyIterator poly1, PolyIterator poly2, int m,
                                                 AbstractModulus[] modulus, PolyIterator polyR) {
        assert m > 0;
        assert poly1.k() == polyR.k() && poly2.k() == polyR.k();

        for (int r = 0; r < m; r++) {
            dyadicProductCoeffMod(poly1.rnsIter[r], poly2.rnsIter[r], polyR.k(), modulus, polyR.rnsIter[r]);
        }
    }

    /**
     * Negative cyclic shift the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param shift   shift.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void negacyclicShiftPolyCoeffMod(CoeffIterator coeff, int n, int shift,
                                                   AbstractModulus modulus, CoeffIterator coeffR) {
        assert coeff != coeffR;
        assert !modulus.isZero();
        assert UintCore.getPowerOfTwo(n) >= 0;

        // Nothing to do, just copy
        if (shift == 0) {
            UintCore.setUint(coeff.coeff(), coeff.ptr(), n, coeffR.coeff(), coeffR.ptr(), n);
            return;
        }
        long indexRaw = shift;
        long coeffCountModMask = (long) (n) - 1L;

        for (int i = 0; i < n; i++, indexRaw++) {
            long index = indexRaw & coeffCountModMask;
            if ((indexRaw & (long) n) == 0 || coeff.getCoeff(i) == 0) {
                coeffR.setCoeff((int) index, coeff.getCoeff(i));
            } else {
                coeffR.setCoeff((int) index, modulus.value() - coeff.getCoeff(i));
            }
        }
    }

    /**
     * Negative cyclic shift the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param k       the number of RNS bases.
     * @param shift   shift.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     */
    public static void negacyclicShiftPolyCoeffModRns(RnsIterator rns, int k, int shift,
                                                      AbstractModulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rns.n() == rnsR.n();

        for (int j = 0; j < k; j++) {
            negacyclicShiftPolyCoeffMod(rns.coeffIter[j], rnsR.n(), shift, modulus[j], rnsR.coeffIter[j]);
        }
    }

    /**
     * Negative cyclic shift the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param m       the first m operated RNS representations.
     * @param shift   shift.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     */
    public static void negacyclicShiftPolyCoeffModPoly(PolyIterator poly, int m, int shift,
                                                       AbstractModulus[] modulus, PolyIterator polyR) {
        assert m > 0;
        assert poly.k() == polyR.k();

        for (int r = 0; r < m; r++) {
            negacyclicShiftPolyCoeffModRns(poly.rnsIter[r], polyR.k(), shift, modulus, polyR.rnsIter[r]);
        }
    }

    /**
     * Multiplies c * x^e to the Coeff representation.
     *
     * @param coeff        the Coeff representation.
     * @param n            the modulus polynomial degree.
     * @param monoCoeff    the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param coeffR       the result Coeff representation.
     */
    public static void negacyclicMultiplyPolyMonoCoeffMod(CoeffIterator coeff, int n, long monoCoeff, int monoExponent,
                                                          AbstractModulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        CoeffIterator temp = CoeffIterator.allocate(n);

        multiplyPolyScalarCoeffMod(coeff, n, monoCoeff, modulus, temp);
        negacyclicShiftPolyCoeffMod(temp, n, monoExponent, modulus, coeffR);
    }

    /**
     * Multiplies c * x^e to the first RNS representation.
     *
     * @param rns          the RNS representation.
     * @param k            the number of RNS bases.
     * @param monoCoeff    the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param rnsR         the result NS representation.
     */
    public static void negacyclicMultiplyPolyMonoCoeffModRns(RnsIterator rns, int k, long monoCoeff, int monoExponent,
                                                             AbstractModulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rns.n() == rnsR.n();

        for (int j = 0; j < k; j++) {
            negacyclicMultiplyPolyMonoCoeffMod(
                rns.coeffIter[j], rnsR.n(), monoCoeff, monoExponent, modulus[j], rnsR.coeffIter[j]
            );
        }
    }

    /**
     * Multiplies c * x^e to the first RNS representation.
     *
     * @param rns          the RNS representation.
     * @param k            the number of RNS bases.
     * @param monoCoeffs   the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param rnsR         the result NS representation.
     */
    public static void negacyclicMultiplyPolyMonoCoeffModRns(RnsIterator rns, int k, CoeffIterator monoCoeffs, int monoExponent,
                                                             AbstractModulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert monoCoeffs.n() == k;
        assert rns.n() == rnsR.n();

        for (int j = 0; j < k; j++) {
            negacyclicMultiplyPolyMonoCoeffMod(
                rns.coeffIter[j], rnsR.n(), monoCoeffs.getCoeff(j), monoExponent, modulus[j], rnsR.coeffIter[j]
            );
        }
    }

    /**
     * Multiplies c * x^e to the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly         the Poly-RNS representation.
     * @param m            the number of multiplied RNS representations.
     * @param monoCoeff    the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param polyR        the result Poly-RNS representation.
     */
    public static void negacyclicMultiplyPolyMonoCoeffModPoly(PolyIterator poly, int m, long monoCoeff, int monoExponent,
                                                              AbstractModulus[] modulus, PolyIterator polyR) {
        assert m > 0;
        assert poly.k() == polyR.k();

        for (int r = 0; r < m; r++) {
            negacyclicMultiplyPolyMonoCoeffModRns(poly.rnsIter[r], polyR.k(), monoCoeff, monoExponent, modulus, polyR.rnsIter[r]);
        }
    }

    /**
     * Multiplies c * x^e to the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly         the Poly-RNS representation.
     * @param m            the number of multiplied RNS representations.
     * @param monoCoeffs   the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param polyR        the result Poly-RNS representation.
     */
    public static void negacyclicMultiplyPolyMonoCoeffModPoly(PolyIterator poly, int m, CoeffIterator monoCoeffs, int monoExponent,
                                                              AbstractModulus[] modulus, PolyIterator polyR) {
        assert m > 0;
        assert poly.k() == polyR.k();

        for (int r = 0; r < m; r++) {
            negacyclicMultiplyPolyMonoCoeffModRns(poly.rnsIter[r], polyR.k(), monoCoeffs, monoExponent, modulus, polyR.rnsIter[r]);
        }
    }

    /**
     * Computes the infinity norm of the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @return the infinity norm of the Coeff representation.
     */
    public static long polyInftyNormCoeffMod(CoeffIterator coeff, int n, AbstractModulus modulus) {
        assert n > 0;
        assert !modulus.isZero();
        // Construct negative threshold (first negative modulus value) to compute absolute values of coeffs.
        long modulusNegThreshold = (modulus.value() + 1) >>> 1;

        // Mod out the poly coefficients and choose a symmetric representative from [-modulus, modulus).
        // Keep track of the max.
        long result = 0;
        for (int i = 0; i < n; i++) {
            long polyCoeff = UintArithmeticSmallMod.barrettReduce64(coeff.getCoeff(i), modulus);
            polyCoeff = polyCoeff >= modulusNegThreshold ? modulus.value() - polyCoeff : polyCoeff;
            if (polyCoeff > result) {
                result = polyCoeff;
            }
        }
        return result;
    }
}
