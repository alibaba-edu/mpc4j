package edu.alibaba.mpc4j.crypto.swhe.seal.ntt;

import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.zq.MultiplyUintModOperand;

/**
 * NTT tool class, used to perform NTT operations on polynomials. Note that there is no such class in SEAL.
 * Here, the NTT related methods in SEAL under the util namespace are encapsulated into this class.
 * <p>
 * Negative cyclic means NTT in Z[x] / (x^n + 1) rather than Z[x] / (x^n + 1). the latter is named positive cyclic NTT.
 * <p>
 * In RLWE-based cryptography, all operations are done in Z[x] / (x^n + 1).
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/ntt.h#L69">
 * inline methods in NTTTables located at ntt.h
 * </a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/27
 */
public class NttTool {
    /**
     * private constructor.
     */
    private NttTool() {
        // empty
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly with lazy modulo operation.
     *
     * @param coeff  A vector a = (a[0], a[1], ..., a[n − 1]) ∈ Z_n^q in standard ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyLazy(long[] coeff, NttTables tables) {
        nttNegacyclicHarveyLazy(coeff, 0, tables);
    }


    /**
     * Negative cyclic NTT using Harvey's butterfly with lazy modulo operation.
     *
     * @param coeff  A vector a = (a[pos + 0], a[pos + 1], ..., a[pos + n − 1]) ∈ Z_n^q in standard ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyLazy(CoeffIterator coeff, NttTables tables) {
        tables.nttHandler.transformToRev(coeff.getCoeff(), coeff.getOffset(), tables.getCoeffCountPower(), tables.getRootPowers(), null);
    }


    /**
     * Negative cyclic NTT using Harvey's butterfly with lazy modulo operation.
     *
     * @param coeff  A vector a = (a[pos + 0], a[pos + 1], ..., a[pos + n − 1]) ∈ Z_n^q in standard ordering.
     * @param pos    the start position.
     * @param tables the pre-computed NTT tables.
     */
    private static void nttNegacyclicHarveyLazy(long[] coeff, int pos, NttTables tables) {
        tables.nttHandler.transformToRev(coeff, pos, tables.getCoeffCountPower(), tables.getRootPowers(), null);
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly with lazy modulo operation for whole small modulo in the RNS
     * representation.
     *
     * @param rns    an RNS representation.
     * @param k      coefficient modulus size k.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyLazyRns(RnsIterator rns, int k, NttTables[] tables) {
        assert k == tables.length;
        assert rns.n == tables[0].getCoeffCount();

        for (int j = 0; j < k; j++) {
            nttNegacyclicHarveyLazy(rns.coeffIterators[j], tables[j]);
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly with lazy modulo operation for the j-th small modulo in the RNS
     * representation.
     *
     * @param rns    an RNS representation.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param j      the i-th RNS representation.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyLazyRns(long[] rns, int n, int k, int j, NttTables[] tables) {
        assert k == tables.length;
        assert j >= 0 && j < k;
        assert n == tables[j].getCoeffCount();

        // the j-th small polynomial is at j * n
        nttNegacyclicHarveyLazy(rns, j * n, tables[j]);
    }


    /**
     * Negative cyclic NTT using Harvey's butterfly with lazy modulo operation for the r-th RNS representation in the
     * Poly-RNS representation.
     *
     * @param poly   a Poly-RNS representation.
     * @param m      number of RNS representations.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyLazyPoly(PolyIterator poly, int m, NttTables[] tables) {
        assert m > 0;
        for (int r = 0; r < m; r++) {
            nttNegacyclicHarveyLazyRns(poly.rnsIterators[r], poly.k, tables);
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly with lazy modulo operation for the r-th RNS representation in the
     * Poly-RNS representation.
     *
     * @param poly   a Poly-RNS representation.
     * @param m      number of RNS representations.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param r      the r-th RNS representation.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyLazyPoly(long[] poly, int m, int n, int k, int r, NttTables[] tables) {
        assert k == tables.length;
        assert r >= 0 && r < m;
        // the r-th RNS representation is at rOffset = r * n * k
        int rOffset = r * n * k;
        for (int j = 0; j < k; j++) {
            assert n == tables[j].getCoeffCount();
            // the j-th small modulo is at rOffset + j * n
            int pos = rOffset + j * n;
            nttNegacyclicHarveyLazy(poly, pos, tables[j]);
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly with lazy modulo operation for the r-th RNS representation, j-th
     * small modulo in the Poly-RNS representation.
     *
     * @param poly   a Poly-RNS representation.
     * @param m      number of RNS representations.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param r      the r-th RNS representation.
     * @param j      the j-th small module.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyLazyPoly(long[] poly, int m, int n, int k, int r, int j, NttTables[] tables) {
        assert k == tables.length;
        assert r >= 0 && r < m;
        assert j >= 0 && j < k;
        assert n == tables[j].getCoeffCount();
        // the r-th RNS representation is at rOffset = r * n * k, the j-th small module is at offset + j * n
        int rOffset = r * n * k;
        int pos = rOffset + j * n;
        nttNegacyclicHarveyLazy(poly, pos, tables[j]);
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly.
     *
     * @param coeff  A vector a = (a[0], ..., a[n − 1]) ∈ Z_n^q in standard ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarvey(long[] coeff, NttTables tables) {
        nttNegacyclicHarvey(coeff, 0, tables);
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly.
     *
     * @param coeff  A vector a = (a[0 + pos], ..., a[n + pos − 1]) ∈ Z_n^q in standard ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarvey(CoeffIterator coeff, NttTables tables) {
        nttNegacyclicHarveyLazy(coeff, tables);
        // Finally, maybe we need to reduce every coefficient modulo q, but we know that they are in the range [0, 4q).
        long modulus = tables.getModulus().value();
        long twoTimesModulus = modulus * 2;
        int n = 1 << tables.getCoeffCountPower();
        for (int i = 0; i < n; i++) {
            // Note: I must be passed to the lambda by reference.
            if (coeff.getCoefficient(i) >= twoTimesModulus) {
                coeff.setCoefficient(i, coeff.getCoefficient(i) - twoTimesModulus);
            }
            if (coeff.getCoefficient(i) >= modulus) {
                coeff.setCoefficient(i, coeff.getCoefficient(i) - modulus);
            }
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly.
     *
     * @param coeff  A vector a = (a[0 + pos], ..., a[n + pos − 1]) ∈ Z_n^q in standard ordering.
     * @param pos    the start position.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarvey(long[] coeff, int pos, NttTables tables) {
        nttNegacyclicHarveyLazy(coeff, pos, tables);
        // Finally, maybe we need to reduce every coefficient modulo q, but we know that they are in the range [0, 4q).
        long modulus = tables.getModulus().value();
        long twoTimesModulus = modulus * 2;
        int n = 1 << tables.getCoeffCountPower();
        for (int i = 0; i < n; i++) {
            // Note: I must be passed to the lambda by reference.
            if (coeff[pos + i] >= twoTimesModulus) {
                coeff[pos + i] -= twoTimesModulus;
            }
            if (coeff[pos + i] >= modulus) {
                coeff[pos + i] -= modulus;
            }
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly for all polynomials in the RNS polynomial representation.
     *
     * @param rns    an RNS representation.
     * @param k      coefficient modulus size k.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyRns(RnsIterator rns, int k, NttTables[] tables) {
        assert k == tables.length;
        for (int j = 0; j < k; j++) {
            nttNegacyclicHarvey(rns.coeffIterators[j], tables[j]);
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly for all polynomials in the RNS polynomial representation.
     *
     * @param rns    an RNS representation.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyRns(long[] rns, int n, int k, NttTables[] tables) {
        assert k == tables.length;
        for (int j = 0; j < k; j++) {
            assert n == tables[j].getCoeffCount();
            nttNegacyclicHarvey(rns, j * n, tables[j]);
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly for j-th small modulo in the RNS polynomial representation.
     *
     * @param rns    an RNS representation.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param j      the j-th small module.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyRns(long[] rns, int n, int k, int j, NttTables[] tables) {
        assert k == tables.length;
        assert j >= 0 && j < k;
        assert n == tables[j].getCoeffCount();
        nttNegacyclicHarvey(rns, j * n, tables[j]);
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly for all polynomials in the Poly-RNS polynomial representation.
     *
     * @param poly   a poly-RNS representation.
     * @param m      number of RNS representations.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyPoly(PolyIterator poly, int m, NttTables[] tables) {
        for (int r = 0; r < m; r++) {
            nttNegacyclicHarveyRns(poly.rnsIterators[r], poly.k, tables);
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly for all polynomials in the Poly-RNS polynomial representation.
     *
     * @param poly   a poly-RNS representation.
     * @param m      number of RNS representations.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyPoly(long[] poly, int m, int n, int k, NttTables[] tables) {
        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int pos = rOffset + j * n;
                nttNegacyclicHarvey(poly, pos, tables[j]);
            }
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly for the r-th RNS representation in the Poly-RNS polynomial
     * representation.
     *
     * @param poly   a poly-RNS representation.
     * @param m      number of RNS representations.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param r      the r-th RNS representation.
     * @param tables the pre-computed NTT tables.
     */
    public static void nttNegacyclicHarveyPoly(long[] poly, int m, int n, int k, int r, NttTables[] tables) {
        assert k == tables.length;
        assert r >= 0 && r < m;
        int rOffset = r * n * k;
        for (int j = 0; j < k; j++) {
            assert n == tables[j].getCoeffCount();
            int pos = rOffset + j * n;
            nttNegacyclicHarvey(poly, pos, tables[j]);
        }
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly with lazy modulo operation.
     *
     * @param coeff  A vector a = (a[0], a[1], ..., a[n − 1]) ∈ Z_n^q in bit-reversed ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyLazy(long[] coeff, NttTables tables) {
        inverseNttNegacyclicHarveyLazy(coeff, 0, tables);
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly with lazy modulo operation.
     *
     * @param coeff  A vector a = (a[pos + 0], a[pos + 1], ..., a[pos + n − 1]) ∈ Z_n^q in bit-reversed ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyLazy(CoeffIterator coeff, NttTables tables) {
        // Final adjustments; compute a[j] = a[j] * n^{-1} mod q. We incorporated the final adjustment in the butterfly.
        MultiplyUintModOperand invN = tables.getInvDegreeModulo();
        tables.nttHandler.transformFromRev(coeff.getCoeff(), coeff.getOffset(), tables.getCoeffCountPower(), tables.getInvRootPowers(), invN);
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly with lazy modulo operation.
     *
     * @param rns    A vector a = (a[pos + 0], a[pos + 1], ..., a[pos + n − 1]) ∈ Z_n^q in bit-reversed ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyLazyRns(RnsIterator rns, int k, NttTables[] tables) {

        for (int j = 0; j < k; j++) {
            inverseNttNegacyclicHarveyLazy(rns.coeffIterators[j], tables[j]);
        }
    }


    /**
     * Negative cyclic INTT using Harvey's butterfly with lazy modulo operation.
     *
     * @param coeff  A vector a = (a[pos + 0], a[pos + 1], ..., a[pos + n − 1]) ∈ Z_n^q in bit-reversed ordering.
     * @param pos    the start position.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyLazy(long[] coeff, int pos, NttTables tables) {
        // Final adjustments; compute a[j] = a[j] * n^{-1} mod q. We incorporated the final adjustment in the butterfly.
        MultiplyUintModOperand invN = tables.getInvDegreeModulo();
        tables.nttHandler.transformFromRev(coeff, pos, tables.getCoeffCountPower(), tables.getInvRootPowers(), invN);
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly with lazy modulo operation for all polynomials in the Poly-RNS
     * representation.
     *
     * @param poly   a Poly-RNS representation.
     * @param m      number of RNS representations.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyLazyPoly(PolyIterator poly, int m, NttTables[] tables) {
        for (int r = 0; r < m; r++) {
            inverseNttNegacyclicHarveyLazyRns(poly.rnsIterators[r], poly.k, tables);
        }
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly with lazy modulo operation for all polynomials in the Poly-RNS
     * representation.
     *
     * @param poly   a Poly-RNS representation.
     * @param m      number of RNS representations.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyLazyPoly(long[] poly, int m, int n, int k, NttTables[] tables) {
        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int pos = rOffset + j * n;
                inverseNttNegacyclicHarveyLazy(poly, pos, tables[j]);
            }
        }
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly.
     *
     * @param coeff  A vector a = (a[0], a[1], ..., a[n − 1]) ∈ Z_n^q in bit-reversed ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarvey(long[] coeff, NttTables tables) {
        inverseNttNegacyclicHarvey(coeff, 0, tables);
    }


    /**
     * Negative cyclic INTT using Harvey's butterfly.
     *
     * @param coeff  A vector a = (a[pos + 0], a[pos + 1], ..., a[pos + n − 1]) ∈ Z_n^q in bit-reversed ordering.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarvey(CoeffIterator coeff, NttTables tables) {
        inverseNttNegacyclicHarveyLazy(coeff, tables);
        // We incorporated the final adjustment in the butterfly. Only need to reduce here.
        long modulus = tables.getModulus().value();
        int n = 1 << tables.getCoeffCountPower();
        for (int i = 0; i < n; i++) {
            if (coeff.getCoefficient(i) >= modulus) {
                coeff.setCoefficient(i, coeff.getCoefficient(i) - modulus);
            }
        }
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly.
     *
     * @param coeff  A vector a = (a[pos + 0], a[pos + 1], ..., a[pos + n − 1]) ∈ Z_n^q in bit-reversed ordering.
     * @param pos    the start position.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarvey(long[] coeff, int pos, NttTables tables) {
        inverseNttNegacyclicHarveyLazy(coeff, pos, tables);
        // We incorporated the final adjustment in the butterfly. Only need to reduce here.
        long modulus = tables.getModulus().value();
        int n = 1 << tables.getCoeffCountPower();
        for (int i = 0; i < n; i++) {
            if (coeff[pos + i] >= modulus) {
                coeff[pos + i] -= modulus;
            }
        }
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly for all polynomials in the RNS polynomial representation.
     *
     * @param rns    an RNS representation.
     * @param k      coefficient modulus size k.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyRns(RnsIterator rns, int k, NttTables[] tables) {
        assert k == tables.length;

        for (int j = 0; j < k; j++) {
            inverseNttNegacyclicHarvey(rns.coeffIterators[j], tables[j]);
        }
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly for all polynomials in the RNS polynomial representation.
     *
     * @param rns    an RNS representation.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyRns(long[] rns, int n, int k, NttTables[] tables) {
        assert k == tables.length;

        for (int j = 0; j < k; j++) {
            assert n == tables[0].getCoeffCount();
            inverseNttNegacyclicHarvey(rns, j * n, tables[j]);
        }
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly for the j-th small modulo in the RNS polynomial representation.
     *
     * @param rns    an RNS representation.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param j      the j-th small modulo.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyRns(long[] rns, int n, int k, int j, NttTables[] tables) {
        assert k == tables.length;
        assert n == tables[j].getCoeffCount();

        inverseNttNegacyclicHarvey(rns, j * n, tables[j]);
    }


    /**
     * Negative cyclic INTT using Harvey's butterfly for all polynomials in the Poly-RNS polynomial representation.
     *
     * @param poly   a Poly-RNS representation.
     * @param m      number of RNS representations.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyPoly(PolyIterator poly, int m, NttTables[] tables) {
        assert m > 0;

        for (int r = 0; r < m; r++) {
            inverseNttNegacyclicHarveyRns(poly.rnsIterators[r], poly.k, tables);
        }
    }

    /**
     * Negative cyclic INTT using Harvey's butterfly for all polynomials in the Poly-RNS polynomial representation.
     *
     * @param poly   a Poly-RNS representation.
     * @param m      number of RNS representations.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyPoly(long[] poly, int m, int n, int k, NttTables[] tables) {
        assert k == tables.length;

        for (int r = 0; r < m; r++) {
            int rOffset = r * n * k;
            for (int j = 0; j < k; j++) {
                int pos = rOffset + j * n;
                inverseNttNegacyclicHarvey(poly, pos, tables[j]);
            }
        }
    }

    /**
     * Negative cyclic NTT using Harvey's butterfly for the r-th RNS representation in the Poly-RNS polynomial
     * representation.
     *
     * @param poly   a Poly-RNS representation.
     * @param m      number of RNS representations.
     * @param n      modulus degree N.
     * @param k      coefficient modulus size k.
     * @param r      the r-th RNS representation.
     * @param tables the pre-computed NTT tables.
     */
    public static void inverseNttNegacyclicHarveyPoly(long[] poly, int m, int n, int k, int r, NttTables[] tables) {
        assert k == tables.length;
        assert r >= 0 && r < m;

        int rOffset = r * n * k;
        for (int j = 0; j < k; j++) {
            int pos = rOffset + j * n;
            inverseNttNegacyclicHarvey(poly, pos, tables[j]);
        }
    }
}
