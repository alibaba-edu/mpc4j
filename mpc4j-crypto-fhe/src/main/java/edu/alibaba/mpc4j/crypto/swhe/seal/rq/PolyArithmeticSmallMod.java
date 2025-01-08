package edu.alibaba.mpc4j.crypto.swhe.seal.rq;

import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.swhe.seal.zq.MultiplyUintModOperand;
import edu.alibaba.mpc4j.crypto.swhe.seal.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.swhe.seal.zq.UintArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.swhe.seal.zq.UintCore;

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
    public static void moduloPolyCoeff(long[] coeff, int n, Modulus modulus, long[] coeffR) {
        moduloPolyCoeff(coeff, 0, n, modulus, coeffR, 0);
    }

    /**
     * Mods the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void moduloPolyCoeff(CoeffIterator coeff, int n, Modulus modulus, CoeffIterator coeffR) {

        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            coeffR.setCoefficient(
                i,
                UintArithmeticSmallMod.barrettReduce64(coeff.getCoefficient(i), modulus)
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
    public static void moduloPolyCoeffRns(RnsIterator rns, int k, Modulus[] modulus, RnsIterator rnsR) {

        assert k > 0;
        assert rns.n == rnsR.n;

        for (int j = 0; j < k; j++) {
            moduloPolyCoeff(rns.coeffIterators[j], rns.n, modulus[j], rnsR.coeffIterators[j]);
        }
    }


    /**
     * Mods the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void moduloPolyCoeff(long[] coeff, int pos, int n, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            coeffR[posR + i] = UintArithmeticSmallMod.barrettReduce64(coeff[pos + i], modulus);
        }
    }

    /**
     * Mods the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param n       modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void moduloPolyCoeffRns(long[] rns, int n, int k, Modulus[] modulus, long[] rnsR, int nR, int kR) {
        moduloPolyCoeffRns(rns, 0, n, k, modulus, rnsR, 0, nR, kR);
    }

    /**
     * Mods the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param pos     the start position.
     * @param n       modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param posR    the result start position.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void moduloPolyCoeffRns(long[] rns, int pos, int n, int k,
                                          Modulus[] modulus, long[] rnsR, int posR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;

        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            moduloPolyCoeff(rns, pos + jOffset, n, modulus[j], rnsR, posR + jOffset);
        }
    }

    /**
     * Mods the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param n       modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param m       the number of RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void moduloPolyCoeffPoly(long[] poly, int n, int k,
                                           int m, Modulus[] modulus, long[] polyR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                moduloPolyCoeff(poly, jOffset, n, modulus[j], polyR, jOffset);
            }
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
    public static void moduloPolyCoeffPoly(PolyIterator poly, int m, Modulus[] modulus, PolyIterator polyR) {

        assert poly.k == polyR.k;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            moduloPolyCoeffRns(poly.rnsIterators[r], polyR.k, modulus, polyR.rnsIterators[r]);
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
    public static void negatePolyCoeffMod(long[] coeff, int n, Modulus modulus, long[] coeffR) {
        negatePolyCoeffMod(coeff, 0, n, modulus, coeffR, 0);
    }

    /**
     * Negates the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void negatePolyCoeffMod(CoeffIterator coeff, int n, Modulus modulus, CoeffIterator coeffR) {

        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long nonZero;
        for (int i = 0; i < n; i++) {
            assert coeff.getCoefficient(i) < modulusValue;
            nonZero = coeff.getCoefficient(i) != 0 ? 1 : 0;
            coeffR.setCoefficient(
                i,
                (modulusValue - coeff.getCoefficient(i)) & (-nonZero)
            );
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
    public static void negatePolyCoeffModRns(RnsIterator rns, int k, Modulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rns.k == rnsR.k;
        assert rns.n == rnsR.n;

        for (int j = 0; j < k; j++) {
            negatePolyCoeffMod(rns.coeffIterators[j], rnsR.n, modulus[j], rnsR.coeffIterators[j]);
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
    public static void negatePolyCoeffModPoly(PolyIterator poly, int m, Modulus[] modulus, PolyIterator polyR) {

        assert poly.k == polyR.k;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            negatePolyCoeffModRns(
                poly.rnsIterators[r],
                polyR.k,
                modulus,
                polyR.rnsIterators[r]
            );
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
                                          Modulus modulus, long[] coeffR, int posR) {
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
     * Negates the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void negatePolyCoeffModRns(long[] rns, int n, int k, Modulus[] modulus, long[] rnsR, int nR, int kR) {
        negatePolyCoeffModRns(rns, 0, n, k, modulus, rnsR, 0, nR, kR);
    }

    /**
     * Negates the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param posR    the result start position.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void negatePolyCoeffModRns(long[] rns, int pos, int n, int k,
                                             Modulus[] modulus, long[] rnsR, int posR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;

        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            negatePolyCoeffMod(rns, pos + jOffset, n, modulus[j], rnsR, posR + jOffset);
        }
    }

    /**
     * Negates the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param m       the number of negated RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     * @param nR      the result number of RNS bases.
     * @param kR      the result number of RNS bases.
     */
    public static void negatePolyCoeffModPoly(long[] poly, int n, int k,
                                              int m, Modulus[] modulus, long[] polyR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                negatePolyCoeffMod(poly, jOffset, n, modulus[j], polyR, jOffset);
            }
        }
    }

    /**
     * Adds two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param coeff2  the 2nd Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param result  the result Coeff representation.
     */
    public static void addPolyCoeffMod(long[] coeff1, long[] coeff2, int n, Modulus modulus, long[] result) {
        addPolyCoeffMod(coeff1, 0, coeff2, 0, n, modulus, result, 0);
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
    public static void addPolyCoeffMod(CoeffIterator coeff1, CoeffIterator coeff2,
                                       int n, Modulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long sum;

        for (int i = 0; i < n; i++) {
            assert coeff1.getCoefficient(i) < modulusValue;
            assert coeff2.getCoefficient(i) < modulusValue;

            sum = coeff1.getCoefficient(i) + coeff2.getCoefficient(i);
            coeffR.setCoefficient(
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
    public static void addPolyCoeffMod(long[] coeff1, int pos1, long[] coeff2, int pos2,
                                       int n, Modulus modulus, long[] coeffR, int posR) {
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
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param rns2    the 2nd RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void addPolyCoeffModRns(long[] rns1, int n1, int k1, long[] rns2, int n2, int k2,
                                          Modulus[] modulus, long[] rnsR, int n, int k) {
        addPolyCoeffModRns(rns1, 0, n1, k1, rns2, 0, n2, k2, modulus, rnsR, 0, n, k);
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
    public static void addPolyCoeffModRns(RnsIterator rns1, RnsIterator rns2, int k,
                                          Modulus[] modulus, RnsIterator rnsR) {
        assert rnsR.k == rns1.k && rnsR.k == rns2.k;
        assert rnsR.k == modulus.length;
        assert k > 0;
        assert rnsR.n == rns1.n && rnsR.n == rns2.n;

        for (int j = 0; j < k; j++) {
            addPolyCoeffMod(rns1.coeffIterators[j], rns2.coeffIterators[j], rnsR.n, modulus[j], rnsR.coeffIterators[j]);
        }
    }

    /**
     * Adds two RNS representations.
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
    public static void addPolyCoeffModRns(long[] rns1, int pos1, int n1, int k1, long[] rns2, int pos2, int n2, int k2,
                                          Modulus[] modulus, long[] rnsR, int posR, int n, int k) {
        assert k == k1 && k == k2 && k == modulus.length;
        assert n == n1 && n == n2;

        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            addPolyCoeffMod(rns1, pos1 + jOffset, rns2, pos2 + jOffset, n, modulus[j], rnsR, posR + jOffset);
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
    public static void addPolyCoeffModPoly(PolyIterator poly1, PolyIterator poly2, int m, Modulus[] modulus,
                                           PolyIterator polyR) {

        assert m > 0;
        assert poly1.k == polyR.k && poly2.k == polyR.k;

        for (int r = 0; r < m; r++) {
            addPolyCoeffModRns(
                poly1.rnsIterators[r],
                poly2.rnsIterators[r],
                polyR.k,
                modulus,
                polyR.rnsIterators[r]
            );
        }
    }

    /**
     * Adds the first m RNS representations in the two Poly-RNS representations.
     *
     * @param poly1   the 1st Poly-RNS representation.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param poly2   the 2nd Poly-RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param m       the number of added RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-NRS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void addPolyCoeffModPoly(long[] poly1, int n1, int k1, long[] poly2, int n2, int k2,
                                           int m, Modulus[] modulus, long[] polyR, int n, int k) {
        assert n == n1 && n == n2;
        assert k == k1 && k == k2 && k == modulus.length;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                addPolyCoeffMod(poly1, jOffset, poly2, jOffset, n, modulus[j], polyR, jOffset);
            }
        }
    }

    /**
     * Subtracts two Coeff representations.
     *
     * @param coeff1  the 1st Coeff representation.
     * @param coeff2  the 2nd Coeff representation.
     * @param n       modulus polynomial degree.
     * @param modulus modulus.
     * @param result  the result Coeff representation.
     */
    public static void subPolyCoeffMod(long[] coeff1, long[] coeff2, int n, Modulus modulus, long[] result) {
        subPolyCoeffMod(coeff1, 0, coeff2, 0, n, modulus, result, 0);
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
    public static void subPolyCoeffMod(CoeffIterator coeff1, CoeffIterator coeff2,
                                       int n, Modulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long[] tempResult = new long[1];
        long borrow;
        for (int i = 0; i < n; i++) {
            assert coeff1.getCoefficient(i) < modulusValue;
            assert coeff2.getCoefficient(i) < modulusValue;

            borrow = UintArithmetic.subUint64(coeff1.getCoefficient(i), coeff2.getCoefficient(i), tempResult);
            coeffR.setCoefficient(i, tempResult[0] + (modulusValue & (-borrow)));
        }
    }

    /**
     * Subtracts two Coeff representations.
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
    public static void subPolyCoeffMod(long[] coeff1, int pos1, long[] coeff2, int pos2,
                                       int n, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long modulusValue = modulus.value();
        long[] tempResult = new long[1];
        long borrow;
        for (int i = 0; i < n; i++) {
            assert coeff1[pos1 + i] < modulusValue;
            assert coeff2[pos2 + i] < modulusValue;
            borrow = UintArithmetic.subUint64(coeff1[pos1 + i], coeff2[pos2 + i], tempResult);
            coeffR[posR + i] = tempResult[0] + (modulusValue & (-borrow));
        }
    }

    /**
     * Subtracts two RNS representations.
     *
     * @param rns1    the 1st RNS representation.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param rns2    the 2nd RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void subPolyCoeffModRns(long[] rns1, int n1, int k1, long[] rns2, int n2, int k2,
                                          Modulus[] modulus, long[] rnsR, int n, int k) {
        subPolyCoeffModRns(rns1, 0, n1, k1, rns2, 0, n2, k2, modulus, rnsR, 0, n, k);
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
    public static void subPolyCoeffModRns(RnsIterator rns1, RnsIterator rns2, int k,
                                          Modulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rnsR.n == rns1.n && rnsR.n == rns2.n;


        for (int j = 0; j < k; j++) {
            subPolyCoeffMod(
                rns1.coeffIterators[j],
                rns2.coeffIterators[j],
                rnsR.n,
                modulus[j],
                rnsR.coeffIterators[j]
            );
        }
    }

    /**
     * Subtracts two RNS representations.
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
    public static void subPolyCoeffModRns(long[] rns1, int pos1, int n1, int k1, long[] rns2, int pos2, int n2, int k2,
                                          Modulus[] modulus, long[] rnsR, int posR, int n, int k) {
        assert n == n1 && n == n2;
        assert k == k1 && k == k2 && k == modulus.length;
        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            subPolyCoeffMod(rns1, pos1 + jOffset, rns2, pos2 + jOffset, n, modulus[j], rnsR, posR + jOffset);
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
                                           Modulus[] modulus, PolyIterator polyR) {

        assert polyR.k == poly1.k && polyR.k == poly2.k;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            subPolyCoeffModRns(
                poly1.rnsIterators[r],
                poly2.rnsIterators[r],
                polyR.k,
                modulus,
                polyR.rnsIterators[r]
            );
        }
    }

    /**
     * Subtracts first m RNS representations in the two Poly-RNS representations.
     *
     * @param poly1   the 1st Poly-RNS representation.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param poly2   the 2nd Poly-RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param m       the number of added RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-NRS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void subPolyCoeffModPoly(long[] poly1, int n1, int k1, long[] poly2, int n2, int k2,
                                           int m, Modulus[] modulus, long[] polyR, int n, int k) {
        assert n == n1 && n == n2;
        assert k == k1 && k == k2 && k == modulus.length;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                subPolyCoeffMod(poly1, jOffset, poly2, jOffset, n, modulus[j], polyR, jOffset);
            }
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
    public static void addPolyScalarCoeffMod(long[] coeff, int n, long scalar, Modulus modulus, long[] coeffR) {
        addPolyScalarCoeffMod(coeff, 0, n, scalar, modulus, coeffR, 0);
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
    public static void addPolyScalarCoeffMod(CoeffIterator coeff, int n,
                                             long scalar, Modulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();
        assert scalar < modulus.value();

        for (int i = 0; i < n; i++) {
            coeffR.setCoefficient(i, UintArithmeticSmallMod.addUintMod(coeff.getCoefficient(i), scalar, modulus));
        }
    }

    /**
     * Adds a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void addPolyScalarCoeffMod(long[] coeff, int pos, int n,
                                             long scalar, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();
        assert scalar < modulus.value();

        for (int i = 0; i < n; i++) {
            coeffR[posR + i] = UintArithmeticSmallMod.addUintMod(coeff[pos + i], scalar, modulus);
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
    public static void subPolyScalarCoeffMod(long[] coeff, int n, long scalar, Modulus modulus, long[] coeffR) {
        subPolyScalarCoeffMod(coeff, 0, n, scalar, modulus, coeffR, 0);
    }

    /**
     * Subtracts a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     */
    public static void subPolyScalarCoeffMod(CoeffIterator coeff, int n,
                                             long scalar, Modulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();
        assert scalar < modulus.value();

        for (int i = 0; i < n; i++) {
            coeffR.setCoefficient(i, UintArithmeticSmallMod.subUintMod(coeff.getCoefficient(i), scalar, modulus));
        }
    }

    /**
     * Subtracts a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param posR    the result Coeff representation.
     */
    public static void subPolyScalarCoeffMod(long[] coeff, int pos, int n,
                                             long scalar, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();
        assert scalar < modulus.value();

        for (int i = 0; i < n; i++) {
            coeffR[posR + i] = UintArithmeticSmallMod.subUintMod(coeff[pos + i], scalar, modulus);
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
    public static void multiplyPolyScalarCoeffMod(CoeffIterator coeff, int n,
                                                  MultiplyUintModOperand scalar, Modulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            coeffR.setCoefficient(i, UintArithmeticSmallMod.multiplyUintMod(coeff.getCoefficient(i), scalar, modulus));
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
    public static void multiplyPolyScalarCoeffMod(CoeffIterator coeff, int n,
                                                  long scalar, Modulus modulus, CoeffIterator coeffR) {
        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus), modulus);
        multiplyPolyScalarCoeffMod(coeff, n, tempScalar, modulus, coeffR);
    }

    /**
     * Multiplies a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void multiplyPolyScalarCoeffMod(long[] coeff, int pos, int n,
                                                  MultiplyUintModOperand scalar, Modulus modulus, long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        for (int i = 0; i < n; i++) {
            coeffR[posR + i] = UintArithmeticSmallMod.multiplyUintMod(coeff[pos + i], scalar, modulus);
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
    public static void multiplyPolyScalarCoeffMod(long[] coeff, int n,
                                                  long scalar, Modulus modulus, long[] coeffR) {
        multiplyPolyScalarCoeffMod(coeff, 0, n, scalar, modulus, coeffR, 0);
    }

    /**
     * Multiplies a scalar to the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void multiplyPolyScalarCoeffMod(long[] coeff, int pos, int n,
                                                  long scalar, Modulus modulus, long[] coeffR, int posR) {
        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus), modulus);
        multiplyPolyScalarCoeffMod(coeff, pos, n, tempScalar, modulus, coeffR, posR);
    }


    /**
     * Multiplies a scalar to the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void multiplyPolyScalarCoeffModRns(long[] rns, int n, int k,
                                                     long scalar, Modulus[] modulus, long[] rnsR, int nR, int kR) {
        multiplyPolyScalarCoeffModRns(rns, 0, n, k, scalar, modulus, rnsR, 0, nR, kR);
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
    public static void multiplyPolyScalarCoeffModRns(RnsIterator rns, int k, long scalar, Modulus[] modulus, RnsIterator rnsR) {

        assert k > 0;
        assert rns.n == rnsR.n;

        for (int j = 0; j < k; j++) {
            multiplyPolyScalarCoeffMod(rns.coeffIterators[j], rnsR.n, scalar, modulus[j], rnsR.coeffIterators[j]);
        }
    }

    /**
     * Multiplies a scalar to the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param posR    the result start position.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void multiplyPolyScalarCoeffModRns(long[] rns, int pos, int n, int k,
                                                     long scalar, Modulus[] modulus, long[] rnsR, int posR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;

        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, modulus[j]), modulus[j]);
            multiplyPolyScalarCoeffMod(rns, pos + jOffset, n, tempScalar, modulus[j], rnsR, posR + jOffset);
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
    public static void multiplyPolyScalarCoeffModPoly(PolyIterator poly, int m, long scalar, Modulus[] modulus, PolyIterator polyR) {

        assert m > 0;
        assert poly.k == polyR.k;

        for (int r = 0; r < m; r++) {
            multiplyPolyScalarCoeffModRns(poly.rnsIterators[r], polyR.k, scalar, modulus, polyR.rnsIterators[r]);
        }
    }

    /**
     * Multiplies a scalar to the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param m       the number of multiplied RNS representations.
     * @param scalar  scalar.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void multiplyPolyScalarCoeffModPoly(long[] poly, int n, int k,
                                                      int m, long scalar, Modulus[] modulus, long[] polyR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;
        assert m > 0;

        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                Modulus currentModulus = modulus[j];
                assert !currentModulus.isZero();
                int jOffset = rOffset + j * n;
                tempScalar.set(UintArithmeticSmallMod.barrettReduce64(scalar, currentModulus), currentModulus);
                multiplyPolyScalarCoeffMod(poly, jOffset, n, tempScalar, modulus[j], polyR, jOffset);
            }
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
    public static void dyadicProductCoeffMod(long[] coeff1, long[] coeff2, int n, Modulus modulus, long[] coeffR) {
        dyadicProductCoeffMod(coeff1, 0, coeff2, 0, n, modulus, coeffR, 0);
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
    public static void dyadicProductCoeffMod(CoeffIterator coeff1, CoeffIterator coeff2, int n, Modulus modulus, CoeffIterator coeffR) {
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
            UintArithmetic.multiplyUint64(coeff1.getCoefficient(i), coeff2.getCoefficient(i), z);
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
            coeffR.setCoefficient(i, tmp3 >= modulusValue ? tmp3 - modulusValue : tmp3);
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
    public static void dyadicProductCoeffMod(long[] coeff1, int pos1, long[] coeff2, int pos2,
                                             int n, Modulus modulus, long[] coeffR, int posR) {
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
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param rns2    the 2nd RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void dyadicProductCoeffModRns(long[] rns1, int n1, int k1, long[] rns2, int n2, int k2,
                                                Modulus[] modulus, long[] rnsR, int n, int k) {
        dyadicProductCoeffModRns(rns1, 0, n1, k1, rns2, 0, n2, k2, modulus, rnsR, 0, n, k);
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
    public static void dyadicProductCoeffModRns(RnsIterator rns1, RnsIterator rns2, int k, Modulus[] modulus, RnsIterator rnsR) {

        assert k > 0;
        assert rns1.n == rnsR.n && rns2.n == rnsR.n;

        for (int j = 0; j < k; j++) {
            dyadicProductCoeffMod(rns1.coeffIterators[j], rns2.coeffIterators[j], rnsR.n, modulus[j], rnsR.coeffIterators[j]);
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
                                                Modulus[] modulus, long[] rnsR, int posR, int n, int k) {
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
    public static void dyadicProductCoeffModPoly(PolyIterator poly1, PolyIterator poly2, int m, Modulus[] modulus, PolyIterator polyR) {

        assert m > 0;
        assert poly1.k == polyR.k && poly2.k == polyR.k;

        for (int r = 0; r < m; r++) {
            dyadicProductCoeffModRns(poly1.rnsIterators[r], poly2.rnsIterators[r], polyR.k, modulus, polyR.rnsIterators[r]);
        }
    }

    /**
     * Dyadic products first m RNS representations in two Poly-RNS representations.
     *
     * @param poly1   the 1st Poly-RNS representation.
     * @param n1      the 1st modulus polynomial degree.
     * @param k1      the 1st number of RNS bases.
     * @param poly2   the 2nd Poly-RNS representation.
     * @param n2      the 2nd modulus polynomial degree.
     * @param k2      the 2nd number of RNS bases.
     * @param m       the number of operated RNS representations.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     * @param n       the result modulus polynomial degree.
     * @param k       the result number of RNS bases.
     */
    public static void dyadicProductCoeffModPoly(long[] poly1, int n1, int k1, long[] poly2, int n2, int k2,
                                                 int m, Modulus[] modulus, long[] polyR, int n, int k) {
        assert k == k1 && k == k2 && k == modulus.length;
        assert n == n1 && n == n2;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                dyadicProductCoeffMod(poly1, jOffset, poly2, jOffset, n, modulus[j], polyR, jOffset);
            }
        }
    }

    /**
     * Negative cyclic shift Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param shift   shift.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     */
    public static void negacyclicShiftPolyCoeffMod(long[] coeff, int n, int shift, Modulus modulus, long[] coeffR) {
        negacyclicShiftPolyCoeffMod(coeff, 0, n, shift, modulus, coeffR, 0);
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
    public static void negacyclicShiftPolyCoeffMod(CoeffIterator coeff, int n, int shift, Modulus modulus, CoeffIterator coeffR) {
        assert coeff != coeffR;
        assert !modulus.isZero();
        assert UintCore.getPowerOfTwo(n) >= 0;

        // Nothing to do, just copy
        if (shift == 0) {
            UintCore.setUint(coeff.getCoeff(), coeff.getOffset(), n, coeffR.getCoeff(), coeffR.getOffset(), n);
            return;
        }
        long indexRaw = shift;
        long coeffCountModMask = (long) (n) - 1L;

        for (int i = 0; i < n; i++, indexRaw++) {
            long index = indexRaw & coeffCountModMask;
            if ((indexRaw & (long) n) == 0 || coeff.getCoefficient(i) == 0) {
                coeffR.setCoefficient((int) index, coeff.getCoefficient(i));
            } else {
                coeffR.setCoefficient((int) index, modulus.value() - coeff.getCoefficient(i));
            }
        }
    }

    /**
     * Negative cyclic shift the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param shift   shift.
     * @param modulus modulus.
     * @param coeffR  the result Coeff representation.
     * @param posR    the result start position.
     */
    public static void negacyclicShiftPolyCoeffMod(long[] coeff, int pos, int n, int shift, Modulus modulus, long[] coeffR, int posR) {
        assert coeff != coeffR;
        assert !modulus.isZero();
        assert UintCore.getPowerOfTwo(n) >= 0;

        // Nothing to do, just copy
        if (shift == 0) {
            UintCore.setUint(coeff, pos, n, coeffR, posR, n);
            return;
        }
        long indexRaw = shift;
        long coeffCountModMask = (long) (n) - 1L;

        for (int i = 0; i < n; i++, indexRaw++) {
            long index = indexRaw & coeffCountModMask;
            if ((indexRaw & (long) n) == 0 || coeff[pos + i] == 0) {
                coeffR[(int) index + posR] = coeff[pos + i];
            } else {
                coeffR[(int) index + posR] = modulus.value() - coeff[i + pos];
            }
        }
    }

    /**
     * Negative cyclic shift the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param shift   shift.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void negacyclicShiftPolyCoeffModRns(long[] rns, int n, int k,
                                                      int shift, Modulus[] modulus, long[] rnsR, int nR, int kR) {
        negacyclicShiftPolyCoeffModRns(rns, 0, n, k, shift, modulus, rnsR, 0, nR, kR);
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
    public static void negacyclicShiftPolyCoeffModRns(RnsIterator rns, int k, int shift, Modulus[] modulus, RnsIterator rnsR) {
        assert k > 0;
        assert rns.n == rnsR.n;


        for (int j = 0; j < k; j++) {
            negacyclicShiftPolyCoeffMod(rns.coeffIterators[j], rnsR.n, shift, modulus[j], rnsR.coeffIterators[j]);
        }
    }

    /**
     * Negative cyclic shift the RNS representation.
     *
     * @param rns     the RNS representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param shift   shift.
     * @param modulus modulus.
     * @param rnsR    the result RNS representation.
     * @param posR    the result start position.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void negacyclicShiftPolyCoeffModRns(long[] rns, int pos, int n, int k,
                                                      int shift, Modulus[] modulus,
                                                      long[] rnsR, int posR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;

        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            negacyclicShiftPolyCoeffMod(rns, pos + jOffset, n, shift, modulus[j], rnsR, posR + jOffset);
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
    public static void negacyclicShiftPolyCoeffModPoly(PolyIterator poly, int m, int shift, Modulus[] modulus, PolyIterator polyR) {

        assert m > 0;
        assert poly.k == polyR.k;

        for (int r = 0; r < m; r++) {
            negacyclicShiftPolyCoeffModRns(poly.rnsIterators[r], polyR.k, shift, modulus, polyR.rnsIterators[r]);
        }
    }

    /**
     * Negative cyclic shift the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly    the Poly-RNS representation.
     * @param n       the modulus polynomial degree.
     * @param k       the number of RNS bases.
     * @param m       the first m operated RNS representations.
     * @param shift   shift.
     * @param modulus modulus.
     * @param polyR   the result Poly-RNS representation.
     * @param nR      the result modulus polynomial degree.
     * @param kR      the result number of RNS bases.
     */
    public static void negacyclicShiftPolyCoeffModPoly(long[] poly, int n, int k,
                                                       int m, int shift, Modulus[] modulus, long[] polyR, int nR, int kR) {
        assert k == kR && k == modulus.length;
        assert n == nR;
        assert m > 0;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                negacyclicShiftPolyCoeffMod(poly, jOffset, n, shift, modulus[j], polyR, jOffset);
            }
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
    public static void negacyclicMultiplyPolyMonoCoeffMod(long[] coeff, int n,
                                                          long monoCoeff, int monoExponent, Modulus modulus, long[] coeffR) {
        negacyclicMultiplyPolyMonoCoeffMod(coeff, 0, n, monoCoeff, monoExponent, modulus, coeffR, 0);
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
    public static void negacyclicMultiplyPolyMonoCoeffMod(CoeffIterator coeff, int n, long monoCoeff, int monoExponent, Modulus modulus, CoeffIterator coeffR) {
        assert n > 0;
        assert !modulus.isZero();

        long[] tempArray = new long[n];
        CoeffIterator temp = new CoeffIterator(tempArray, 0, n);

        multiplyPolyScalarCoeffMod(coeff, n, monoCoeff, modulus, temp);
        negacyclicShiftPolyCoeffMod(temp, n, monoExponent, modulus, coeffR);
    }

    /**
     * Multiplies c * x^e to the Coeff representation.
     *
     * @param coeff        the Coeff representation.
     * @param pos          the start position.
     * @param n            the modulus polynomial degree.
     * @param monoCoeff    the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param coeffR       the result Coeff representation.
     * @param posR         the result start position.
     */
    public static void negacyclicMultiplyPolyMonoCoeffMod(long[] coeff, int pos, int n,
                                                          long monoCoeff, int monoExponent, Modulus modulus,
                                                          long[] coeffR, int posR) {
        assert n > 0;
        assert !modulus.isZero();

        long[] temp = new long[n];
        multiplyPolyScalarCoeffMod(coeff, pos, n, monoCoeff, modulus, temp, 0);
        negacyclicShiftPolyCoeffMod(temp, 0, n, monoExponent, modulus, coeffR, posR);
    }

    /**
     * Multiplies c * x^e to the first RNS representation.
     *
     * @param rns          the RNS representation.
     * @param n            the modulus polynomial degree.
     * @param k            the number of RNS bases.
     * @param monoCoeff    the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param rnsR         the result NS representation.
     * @param nR           the result modulus polynomial degree.
     * @param kR           the result number of RNS bases.
     */
    public static void negacyclicMultiplyPolyMonoCoeffModRns(long[] rns, int n, int k,
                                                             long monoCoeff, int monoExponent, Modulus[] modulus,
                                                             long[] rnsR, int nR, int kR) {
        negacyclicMultiplyPolyMonoCoeffModRns(rns, 0, n, k, monoCoeff, monoExponent, modulus, rnsR, 0, nR, kR);
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
    public static void negacyclicMultiplyPolyMonoCoeffModRns(RnsIterator rns, int k, long monoCoeff, int monoExponent, Modulus[] modulus, RnsIterator rnsR) {

        assert k > 0;
        assert rns.n == rnsR.n;

        for (int j = 0; j < k; j++) {
            negacyclicMultiplyPolyMonoCoeffMod(rns.coeffIterators[j], rnsR.n, monoCoeff, monoExponent, modulus[j], rnsR.coeffIterators[j]);
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
    public static void negacyclicMultiplyPolyMonoCoeffModRns(RnsIterator rns, int k, CoeffIterator monoCoeffs, int monoExponent, Modulus[] modulus, RnsIterator rnsR) {

        assert k > 0;
        assert monoCoeffs.getN() == k;
        assert rns.n == rnsR.n;

        for (int j = 0; j < k; j++) {
            negacyclicMultiplyPolyMonoCoeffMod(rns.coeffIterators[j], rnsR.n, monoCoeffs.getCoefficient(j), monoExponent, modulus[j], rnsR.coeffIterators[j]);
        }
    }


    /**
     * Multiplies c * x^e to the first RNS representation.
     *
     * @param rns          the RNS representation.
     * @param pos          the start position.
     * @param n            the modulus polynomial degree.
     * @param k            the number of RNS bases.
     * @param monoCoeff    the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param rnsR         the result NS representation.
     * @param nR           the result modulus polynomial degree.
     * @param kR           the result number of RNS bases.
     * @param posR         the result start position.
     */
    public static void negacyclicMultiplyPolyMonoCoeffModRns(long[] rns, int pos, int n, int k,
                                                             long monoCoeff, int monoExponent, Modulus[] modulus,
                                                             long[] rnsR, int posR, int nR, int kR) {
        assert n == nR;
        assert k == kR && k == modulus.length;

        long[] temp = new long[n];
        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        for (int j = 0; j < k; j++) {
            int jOffset = j * n;
            tempScalar.set(UintArithmeticSmallMod.barrettReduce64(monoCoeff, modulus[j]), modulus[j]);
            multiplyPolyScalarCoeffMod(rns, pos + jOffset, n, tempScalar, modulus[j], temp, 0);
            negacyclicShiftPolyCoeffMod(temp, 0, n, monoExponent, modulus[j], rnsR, posR + jOffset);
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
    public static void negacyclicMultiplyPolyMonoCoeffModPoly(PolyIterator poly, int m, long monoCoeff, int monoExponent, Modulus[] modulus,
                                                              PolyIterator polyR) {

        assert m > 0;
        assert poly.k == polyR.k;

        for (int r = 0; r < m; r++) {
            negacyclicMultiplyPolyMonoCoeffModRns(poly.rnsIterators[r], polyR.k, monoCoeff, monoExponent, modulus, polyR.rnsIterators[r]);
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
    public static void negacyclicMultiplyPolyMonoCoeffModPoly(PolyIterator poly, int m, CoeffIterator monoCoeffs, int monoExponent, Modulus[] modulus,
                                                              PolyIterator polyR) {

        assert m > 0;
        assert poly.k == polyR.k;

        for (int r = 0; r < m; r++) {
            negacyclicMultiplyPolyMonoCoeffModRns(poly.rnsIterators[r], polyR.k, monoCoeffs, monoExponent, modulus, polyR.rnsIterators[r]);
        }
    }


    /**
     * Multiplies c * x^e to the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly         the Poly-RNS representation.
     * @param n            the modulus polynomial degree.
     * @param k            the number of RNS bases.
     * @param m            the number of multiplied RNS representations.
     * @param monoCoeff    the monotonic coefficient c.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param polyR        the result Poly-RNS representation.
     * @param nR           the result modulus polynomial degree.
     * @param kR           the result number of RNS bases.
     */
    public static void negacyclicMultiplyPolyMonoCoeffModPoly(long[] poly, int n, int k, int m,
                                                              long monoCoeff, int monoExponent, Modulus[] modulus,
                                                              long[] polyR, int nR, int kR) {
        assert n == nR;
        assert k == kR && k == modulus.length;
        assert m > 0;

        long[] temp = new long[n];
        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                tempScalar.set(UintArithmeticSmallMod.barrettReduce64(monoCoeff, modulus[j]), modulus[j]);
                multiplyPolyScalarCoeffMod(poly, jOffset, n, tempScalar, modulus[j], temp, 0);
                negacyclicShiftPolyCoeffMod(temp, 0, n, monoExponent, modulus[j], polyR, jOffset);
            }
        }
    }


    /**
     * Multiplies c_j * x^e to the first m RNS representations in the Poly-RNS representation.
     *
     * @param poly         the Poly-RNS representation.
     * @param n            the modulus polynomial degree.
     * @param k            the number of RNS bases.
     * @param m            the number of multiplied RNS representations.
     * @param monoCoeffs   the monotonic coefficient c1, ..., ck.
     * @param monoExponent the monotonic exponent e.
     * @param modulus      modulus.
     * @param polyR        the result Poly-RNS representation.
     * @param nR           the result modulus polynomial degree.
     * @param kR           the result number of RNS bases.
     */
    public static void negacyclicMultiplyPolyMonoCoeffModPoly(long[] poly, int n, int k, int m,
                                                              long[] monoCoeffs, int monoExponent, Modulus[] modulus,
                                                              long[] polyR, int nR, int kR) {
        assert n == nR;
        assert k == kR && k == modulus.length;
        assert m > 0;

        long[] temp = new long[n];
        MultiplyUintModOperand tempScalar = new MultiplyUintModOperand();
        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int jOffset = rOffset + j * n;
                tempScalar.set(UintArithmeticSmallMod.barrettReduce64(monoCoeffs[j], modulus[j]), modulus[j]);
                multiplyPolyScalarCoeffMod(poly, jOffset, n, tempScalar, modulus[j], temp, 0);
                negacyclicShiftPolyCoeffMod(temp, 0, n, monoExponent, modulus[j], polyR, jOffset);
            }
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
    public static long polyInftyNormCoeffMod(long[] coeff, int n, Modulus modulus) {
        return polyInftyNormCoeffMod(coeff, 0, n, modulus);
    }

    /**
     * Computes the infinity norm of the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @return the infinity norm of the Coeff representation.
     */
    public static long polyInftyNormCoeffMod(CoeffIterator coeff, int n, Modulus modulus) {
        assert n > 0;
        assert !modulus.isZero();
        // Construct negative threshold (first negative modulus value) to compute absolute values of coeffs.
        long modulusNegThreshold = (modulus.value() + 1) >>> 1;

        // Mod out the poly coefficients and choose a symmetric representative from [-modulus, modulus).
        // Keep track of the max.
        long result = 0;
        for (int i = 0; i < n; i++) {
            long polyCoeff = UintArithmeticSmallMod.barrettReduce64(coeff.getCoefficient(i), modulus);
            polyCoeff = polyCoeff >= modulusNegThreshold ? modulus.value() - polyCoeff : polyCoeff;
            if (polyCoeff > result) {
                result = polyCoeff;
            }
        }
        return result;
    }

    /**
     * Computes the infinity norm of the Coeff representation.
     *
     * @param coeff   the Coeff representation.
     * @param pos     the start position.
     * @param n       the modulus polynomial degree.
     * @param modulus modulus.
     * @return the infinity norm of the Coeff representation.
     */
    public static long polyInftyNormCoeffMod(long[] coeff, int pos, int n, Modulus modulus) {
        assert n > 0;
        assert !modulus.isZero();
        // Construct negative threshold (first negative modulus value) to compute absolute values of coeffs.
        long modulusNegThreshold = (modulus.value() + 1) >>> 1;

        // Mod out the poly coefficients and choose a symmetric representative from [-modulus, modulus).
        // Keep track of the max.
        long result = 0;
        for (int i = 0; i < n; i++) {
            long polyCoeff = UintArithmeticSmallMod.barrettReduce64(coeff[pos + i], modulus);
            polyCoeff = polyCoeff >= modulusNegThreshold ? modulus.value() - polyCoeff : polyCoeff;
            if (polyCoeff > result) {
                result = polyCoeff;
            }
        }
        return result;
    }
}
