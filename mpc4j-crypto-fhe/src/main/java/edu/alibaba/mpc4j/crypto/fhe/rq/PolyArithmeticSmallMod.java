package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

/**
 * This class provides modular arithmetic for polynomials.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/polyarithsmallmod.h
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
