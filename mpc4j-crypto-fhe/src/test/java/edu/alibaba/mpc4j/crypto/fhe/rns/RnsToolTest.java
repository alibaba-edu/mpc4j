package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;

import org.junit.Assert;
import org.junit.Test;

/**
 * RnsTool unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/rns.cpp
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/22
 */
@SuppressWarnings("PointlessArithmeticExpression")
public class RnsToolTest {

    @Test
    public void testInitialize() {
        int polyModulusDegree = 32;
        int coeffBaseCount = 4;
        int primeBitCount = 20;
        Modulus plainT = new Modulus(65537);
        // q_i mod 2N = 1
        RnsBase coeffBase = new RnsBase(Numth.getPrimes(polyModulusDegree * 2, primeBitCount, coeffBaseCount));
        // create successfully
        new RnsTool(polyModulusDegree, coeffBase, plainT);
        // throw exception
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsTool(1, coeffBase, plainT));
    }

    @Test
    public void testFastBConvMTilde() {
        Modulus plainT = new Modulus(0);
        RnsTool rnsTool;
        {
            // 1-th test
            int n = 2;
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3}), plainT);
            int inK = rnsTool.baseQ().size();
            int outK = rnsTool.baseBskMTilde().size();

            long[] in = RnsIterator.allocateZeroRns(n, inK);
            long[] out = RnsIterator.allocateZeroRns(n, outK);
            rnsTool.fastBConvMTildeRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(0, val);
            }

            in[0] = 1;
            in[1] = 2;
            rnsTool.fastBConvMTildeRnsIter(in, 0, n, inK, out, 0, n, outK);
            // These are results for fast base conversion for a length-2 array ((m_tilde), (2*m_tilde))
            // before reduction to target base.
            long temp = rnsTool.getMTilde().value() % 3;
            long temp2 = (2 * rnsTool.getMTilde().value()) % 3;
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(0).value()), out[0]);
            Assert.assertEquals(temp2 % (rnsTool.baseBskMTilde().getBase(0).value()), out[1]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(1).value()), out[2]);
            Assert.assertEquals(temp2 % (rnsTool.baseBskMTilde().getBase(1).value()), out[3]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(2).value()), out[4]);
            Assert.assertEquals(temp2 % (rnsTool.baseBskMTilde().getBase(2).value()), out[5]);
        }

        {
            int n = 2;
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3, 5}), plainT);
            int inK = rnsTool.baseQ().size();
            int outK = rnsTool.baseBskMTilde().size();

            long[] in = RnsIterator.allocateZeroRns(n, inK);
            long[] out = RnsIterator.allocateZeroRns(n, outK);
            rnsTool.fastBConvMTildeRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(0, val);
            }

            in[0] = 1;
            in[1] = 1;
            in[2] = 2;
            in[3] = 2;
            rnsTool.fastBConvMTildeRnsIter(in, 0, n, inK, out, 0, n, outK);
            long mTilde = rnsTool.getMTilde().value();
            // This is the result of fast base conversion for a length-2 array
            // ((m_tilde, 2 * m_tilde), (m_tilde, 2 * m_tilde)) before reduction to target base.
            long temp = ((2 * mTilde) % 3) * 5 + ((4 * mTilde) % 5) * 3;
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(0).value()), out[0]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(0).value()), out[1]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(1).value()), out[2]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(1).value()), out[3]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(2).value()), out[4]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(2).value()), out[5]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(3).value()), out[6]);
            Assert.assertEquals(temp % (rnsTool.baseBskMTilde().getBase(3).value()), out[7]);
        }
    }

    @Test
    public void testSmMrq() {
        // This function assumes the input is in base Bsk U {m_tilde}. If the input is
        // |[c*m_tilde]_q + qu|_m for m in Bsk U {m_tilde}, then the output is c' in Bsk
        // such that c' = c mod q. In other words, this function cancels the extra multiples
        // of q in the Bsk U {m_tilde} representation. The functions work correctly for
        // sufficiently small values of u.

        Modulus plainT = new Modulus(0);
        RnsTool rnsTool;
        {
            int n = 2;
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3}), plainT);
            int inK = rnsTool.baseBskMTilde().size();
            int outK = rnsTool.baseBsk().size();

            long[] in = RnsIterator.allocateZeroRns(n, inK);
            long[] out = RnsIterator.allocateZeroRns(n, outK);
            rnsTool.smMrqRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(0, val);
            }

            // Input base is Bsk U {m_tilde}, in this case consisting of 3 primes.
            // m_tilde is always smaller than the primes in Bsk (SEAL_INTERNAL_MOD_BIT_COUNT (61) bits).
            // Set the length-2 array to have values 1*m_tilde and 2*m_tilde.
            in[0] = rnsTool.getMTilde().value();
            in[1] = 2 * rnsTool.getMTilde().value();
            in[2] = rnsTool.getMTilde().value();
            in[3] = 2 * rnsTool.getMTilde().value();
            // modulo m_tilde
            in[4] = 0;
            in[5] = 0;
            // This should simply get rid of the m_tilde factor
            rnsTool.smMrqRnsIter(in, 0, n, inK, out, 0, n, outK);
            Assert.assertEquals(1, out[0]);
            Assert.assertEquals(2, out[1]);
            Assert.assertEquals(1, out[2]);
            Assert.assertEquals(2, out[3]);

            // Next add a multiple of q to the input and see if it is reduced properly
            in[0] = rnsTool.baseQ().getBase(0).value();
            in[1] = rnsTool.baseQ().getBase(0).value();
            in[2] = rnsTool.baseQ().getBase(0).value();
            in[3] = rnsTool.baseQ().getBase(0).value();
            in[4] = rnsTool.baseQ().getBase(0).value();
            in[5] = rnsTool.baseQ().getBase(0).value();
            rnsTool.smMrqRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(0, val);
            }
        }

        {
            int n = 2;
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3, 5}), plainT);
            int inK = rnsTool.baseBskMTilde().size();
            int outK = rnsTool.baseBsk().size();

            long[] in = RnsIterator.allocateZeroRns(n, inK);
            long[] out = RnsIterator.allocateZeroRns(n, outK);
            rnsTool.smMrqRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(0, val);
            }

            // Input base is Bsk U {m_tilde}, in this case consisting of 3 primes.
            // m_tilde is always smaller than the primes in Bsk (SEAL_INTERNAL_MOD_BIT_COUNT (61) bits).
            // Set the length-2 array to have values 1*m_tilde and 2*m_tilde.
            in[0] = rnsTool.getMTilde().value();
            in[1] = 2 * rnsTool.getMTilde().value();
            in[2] = rnsTool.getMTilde().value();
            in[3] = 2 * rnsTool.getMTilde().value();
            in[4] = rnsTool.getMTilde().value();
            in[5] = 2 * rnsTool.getMTilde().value();

            // modulo m_tilde
            in[6] = 0;
            in[7] = 0;
            // This should simply get rid of the m_tilde factor
            rnsTool.smMrqRnsIter(in, 0, n, inK, out, 0, n, outK);
            Assert.assertEquals(1, out[0]);
            Assert.assertEquals(2, out[1]);
            Assert.assertEquals(1, out[2]);
            Assert.assertEquals(2, out[3]);
            Assert.assertEquals(1, out[4]);
            Assert.assertEquals(2, out[5]);

            // Next add a multiple of q to the input and see if it is reduced properly
            in[0] = 15;
            in[1] = 30;
            in[2] = 15;
            in[3] = 30;
            in[4] = 15;
            in[5] = 30;
            in[6] = 15;
            in[7] = 30;
            rnsTool.smMrqRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(0, val);
            }

            // Now with a multiple of m_tilde + multiple of q
            in[0] = 2 * rnsTool.getMTilde().value() + 15;
            in[1] = 2 * rnsTool.getMTilde().value() + 30;
            in[2] = 2 * rnsTool.getMTilde().value() + 15;
            in[3] = 2 * rnsTool.getMTilde().value() + 30;
            in[4] = 2 * rnsTool.getMTilde().value() + 15;
            in[5] = 2 * rnsTool.getMTilde().value() + 30;
            in[6] = 2 * rnsTool.getMTilde().value() + 15;
            in[7] = 2 * rnsTool.getMTilde().value() + 30;
            rnsTool.smMrqRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(2, val);
            }
        }
    }

    @Test
    public void testFastFloor() {
        // This function assumes the input is in base q U Bsk. It outputs an approximation of
        // the value divided by q floored in base Bsk. The approximation has absolute value up
        // to k-1, where k is the number of primes in the base q.
        Modulus plainT = new Modulus(2);
        RnsTool rnsTool;
        {
            int n = 2;
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3}), plainT);
            int inK = rnsTool.baseQ().size() + rnsTool.baseBsk().size();
            int outK = rnsTool.baseBsk().size();

            long[] in = RnsIterator.allocateZeroRns(n, inK);
            long[] out = RnsIterator.allocateZeroRns(n, outK);
            rnsTool.fastFloorRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(0, val);
            }

            // The size of q U Bsk is 3. We set the input to have values 15 and 5, and divide by 3 (i.e., q).
            in[0] = 0;
            in[1] = 2;
            in[2] = 15;
            in[3] = 5;
            in[4] = 15;
            in[5] = 5;
            // We get an exact result in this case since input base only has size 1
            rnsTool.fastFloorRnsIter(in, 0, n, inK, out, 0, n, outK);
            Assert.assertEquals(5, out[0]);
            Assert.assertEquals(1, out[1]);
            Assert.assertEquals(5, out[2]);
            Assert.assertEquals(1, out[3]);

            // Now a case where the floor really shows up
            in[0] = 2;
            in[1] = 1;
            in[2] = 17;
            in[3] = 4;
            in[4] = 17;
            in[5] = 4;
            rnsTool.fastFloorRnsIter(in, 0, n, inK, out, 0, n, outK);
            Assert.assertEquals(5, out[0]);
            Assert.assertEquals(1, out[1]);
            Assert.assertEquals(5, out[2]);
            Assert.assertEquals(1, out[3]);
        }

        {
            int n = 2;
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3, 5}), plainT);
            int inK = rnsTool.baseQ().size() + rnsTool.baseBsk().size();
            int outK = rnsTool.baseBsk().size();

            long[] in = RnsIterator.allocateZeroRns(n, inK);
            long[] out = RnsIterator.allocateZeroRns(n, outK);
            rnsTool.fastFloorRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long val : out) {
                Assert.assertEquals(0, val);
            }

            // The size of q U Bsk is 3. We set the input to have values 30 and 15, and divide by 3 * 5.
            in[0] = 0;
            in[1] = 0;
            in[2] = 0;
            in[3] = 0;
            in[4] = 15;
            in[5] = 30;
            in[6] = 15;
            in[7] = 30;
            in[8] = 15;
            in[9] = 30;
            rnsTool.fastFloorRnsIter(in, 0, n, inK, out, 0, n, outK);
            Assert.assertEquals(1, out[0]);
            Assert.assertEquals(2, out[1]);
            Assert.assertEquals(1, out[2]);
            Assert.assertEquals(2, out[3]);
            Assert.assertEquals(1, out[4]);
            Assert.assertEquals(2, out[5]);

            // Now a case where the floor really shows up
            in[0] = 0;
            in[1] = 2;
            in[2] = 1;
            in[3] = 2;
            in[4] = 21;
            in[5] = 32;
            in[6] = 21;
            in[7] = 32;
            in[8] = 21;
            in[9] = 32;
            rnsTool.fastFloorRnsIter(in, 0, n, inK, out, 0, n, outK);
            Assert.assertTrue(Math.abs(1L - out[0]) <= 1);
            Assert.assertTrue(Math.abs(2L - out[1]) <= 1);
            Assert.assertTrue(Math.abs(1L - out[2]) <= 1);
            Assert.assertTrue(Math.abs(2L - out[3]) <= 1);
            Assert.assertTrue(Math.abs(1L - out[4]) <= 1);
            Assert.assertTrue(Math.abs(2L - out[5]) <= 1);
        }
    }

    @Test
    public void testFastBConvSk() {
        Modulus plainT = new Modulus(2);
        RnsTool rnsTool;
        {
            // 1-th test
            int n = 2;
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3}), plainT);
            int inK = rnsTool.baseBsk().size();
            int outK = rnsTool.baseQ().size();

            long[] in = RnsIterator.allocateZeroRns(n, inK);
            long[] out = RnsIterator.allocateZeroRns(n, outK);
            rnsTool.fastBConvSkRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long l : out) {
                Assert.assertEquals(0, l);
            }

            // The size of Bsk is 2
            in[0] = 5;
            in[1] = 6;
            in[2] = 5;
            in[3] = 6;
            rnsTool.fastBConvSkRnsIter(in, 0, n, inK, out, 0, n, outK);
            Assert.assertEquals(5 % 3, out[0]);
            Assert.assertEquals(6 % 3, out[1]);
        }

        {
            // 2-th test
            int n = 2;
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3, 5}), plainT);
            int inK = rnsTool.baseBsk().size();
            int outK = rnsTool.baseQ().size();

            long[] in = RnsIterator.allocateZeroRns(n, inK);
            long[] out = RnsIterator.allocateZeroRns(n, outK);
            rnsTool.fastBConvSkRnsIter(in, 0, n, inK, out, 0, n, outK);
            for (long l : out) {
                Assert.assertEquals(0, l);
            }

            // The size of Bsk is 3
            in[0] = 1;
            in[1] = 2;
            in[2] = 1;
            in[3] = 2;
            in[4] = 1;
            in[5] = 2;
            rnsTool.fastBConvSkRnsIter(in, 0, n, inK, out, 0, n, outK);
            Assert.assertEquals(1, out[0]);
            Assert.assertEquals(2, out[1]);
            Assert.assertEquals(1, out[2]);
            Assert.assertEquals(2, out[3]);
        }
    }

    @Test
    public void testExactScaleAndRound() {
        // This function computes [round(t/q * |input|_q)]_t exactly using the gamma-correction technique.
        int coeffCount = 2;
        Modulus plainT = new Modulus(3);
        RnsTool rnsTool = new RnsTool(coeffCount, new RnsBase(new long[]{5, 7}), plainT);
        long[] in = new long[coeffCount * rnsTool.baseBsk().size()];
        long[] out = new long[coeffCount];
        // 0 ---> scale and round must be 0
        rnsTool.decryptScaleAndRound(in, coeffCount, out);
        for (long o : out) {
            Assert.assertEquals(0, o);
        }
        // baseQSize = 2, {5, 7}, coeff count = 2
        // mod q is zero
        // inIter.coeffIter will be changed
        in[0] = 35;
        in[1] = 70;
        in[2] = 35;
        in[3] = 70;
        // Q = 35, [(t/35) * 35] mod t --> 0,  [(t/35) * 70] mod t --> 0
        // so result is zero
        rnsTool.decryptScaleAndRound(in, coeffCount, out);
        for (long o : out) {
            Assert.assertEquals(0, o);
        }
        // try non-trivial case
        // round([(29 * 7 *3 + 29 * 5 * 3) mod 35] * (3/35)) mod 3 = 2
        // round([(65 * 7 *3 + 65 * 5 * 3) mod 35] * (3/35)) mod 3 = 3 mod 3 = 0
        in[0] = 29;
        in[1] = 30 + 35;
        in[2] = 29;
        in[3] = 30 + 35;
        // Here 29 will scale and round to 2 and 30 will scale and round to 0.
        // The added 35 should not make a difference.
        rnsTool.decryptScaleAndRound(in, coeffCount, out);
        Assert.assertEquals(2, out[0]);
        Assert.assertEquals(0, out[1]);
    }

    @Test
    public void testDivideAndRoundQLastInplace() {
        RnsTool rnsTool;
        {
            int n = 2;
            Modulus plainT = new Modulus(0);
            rnsTool = new RnsTool(n, new RnsBase(new long[]{13, 7}), plainT);
            int k = rnsTool.baseQ().size();

            long[] in = PolyIterator.allocateZeroPoly(1, n, k);
            rnsTool.divideAndRoundQLastInplace(in, 0, n, k);
            Assert.assertEquals(0, in[0]);
            Assert.assertEquals(0, in[1]);

            // The size of q is 2. We set some values here and divide by the last modulus (i.e., 7).
            in[0] = 1;
            in[1] = 2;
            in[2] = 1;
            in[3] = 2;
            rnsTool.divideAndRoundQLastInplace(in, 0, n, k);
            Assert.assertEquals(0, in[0]);
            Assert.assertEquals(0, in[1]);

            // Next a case with non-trivial rounding
            in[0] = 12;
            in[1] = 11;
            in[2] = 4;
            in[3] = 3;
            rnsTool.divideAndRoundQLastInplace(in, 0, n, k);
            Assert.assertEquals(4, in[0]);
            Assert.assertEquals(3, in[1]);

            // Input array (19, 15)
            in[0] = 6;
            in[1] = 2;
            in[2] = 5;
            in[3] = 1;
            rnsTool.divideAndRoundQLastInplace(in, 0, n, k);
            Assert.assertEquals(3, in[0]);
            Assert.assertEquals(2, in[1]);
        }

        {
            int n = 2;
            Modulus plainT = new Modulus(0);
            rnsTool = new RnsTool(n, new RnsBase(new long[]{3, 5, 7, 11}), plainT);
            int k = rnsTool.baseQ().size();

            long[] in = PolyIterator.allocateZeroPoly(1, n, k);
            rnsTool.divideAndRoundQLastInplace(in, 0, n, k);
            Assert.assertEquals(0, in[0]);
            Assert.assertEquals(0, in[1]);
            Assert.assertEquals(0, in[2]);
            Assert.assertEquals(0, in[3]);
            Assert.assertEquals(0, in[4]);
            Assert.assertEquals(0, in[5]);

            // The size of q is 2. We set some values here and divide by the last modulus (i.e., 7).
            in[0] = 1;
            in[1] = 2;
            in[2] = 1;
            in[3] = 2;
            in[4] = 1;
            in[5] = 2;
            in[6] = 1;
            in[7] = 2;
            rnsTool.divideAndRoundQLastInplace(in, 0, n, k);
            Assert.assertEquals(0, in[0]);
            Assert.assertEquals(0, in[1]);
            Assert.assertEquals(0, in[2]);
            Assert.assertEquals(0, in[3]);
            Assert.assertEquals(0, in[4]);
            Assert.assertEquals(0, in[5]);

            // Next a case with non-trivial rounding; array is (60, 70)
            in[0] = 0;
            in[1] = 1;
            in[2] = 0;
            in[3] = 0;
            in[4] = 4;
            in[5] = 0;
            in[6] = 5;
            in[7] = 4;
            rnsTool.divideAndRoundQLastInplace(in, 0, n, k);
            Assert.assertTrue((3 + 2 - in[0]) % 3 <= 1);
            Assert.assertTrue((3 + 0 - in[1]) % 3 <= 1);
            Assert.assertTrue((5 + 0 - in[2]) % 5 <= 1);
            Assert.assertTrue((5 + 1 - in[3]) % 5 <= 1);
            Assert.assertTrue((7 + 5 - in[4]) % 7 <= 1);
            Assert.assertTrue((7 + 6 - in[5]) % 7 <= 1);
        }
    }

    @Test
    public void testDivideAndRoundQLastNttInplace() {
        // This function approximately divides the input values by the last prime in the base q.
        // Input is in base q; the last RNS component becomes invalid.
        // note that the last RNS component becomes invalid. So in the following test,
        // we drop the last RNS component.
        RnsTool rnsTool;

        int n = 2;
        NttTables[] nttTables = new NttTables[]{
            new NttTables(1, new Modulus(53)),
            new NttTables(1, new Modulus(13)),
        };
        Modulus plainT = new Modulus(0);
        rnsTool = new RnsTool(n, new RnsBase(new long[]{53, 13}), plainT);
        int k = rnsTool.baseQ().size();

        long[] in = PolyIterator.allocateZeroPoly(1, n, k);
        rnsTool.divideAndRoundQLastNttInplace(in, 0, n, k, nttTables);
        Assert.assertEquals(0, in[0]);
        Assert.assertEquals(0, in[1]);

        // The size of q is 2. We set some values here and divide by the last modulus (i.e., 13).
        in[0] = 1;
        in[1] = 2;
        in[2] = 1;
        in[3] = 2;
        NttTool.nttNegacyclicHarvey(in, 0, nttTables[0]);
        NttTool.nttNegacyclicHarvey(in, n, nttTables[1]);
        // We expect to get a zero output also in this case
        rnsTool.divideAndRoundQLastNttInplace(in, 0, n, k, nttTables);
        NttTool.inverseNttNegacyclicHarvey(in, 0, nttTables[0]);
        Assert.assertEquals(0, in[0]);
        Assert.assertEquals(0, in[1]);

        // Next a case with non-trivial rounding
        in[0] = 4;
        in[1] = 12;
        in[2] = 4;
        in[3] = 12;
        NttTool.nttNegacyclicHarvey(in, 0, nttTables[0]);
        NttTool.nttNegacyclicHarvey(in, n, nttTables[1]);
        // We expect to get a zero output also in this case
        rnsTool.divideAndRoundQLastNttInplace(in, 0, n, k, nttTables);
        NttTool.inverseNttNegacyclicHarvey(in, 0, nttTables[0]);
        // in[0] = 0, round(4/13) = 0
        Assert.assertTrue((53 + 1 - in[0]) % 53 <= 1);
        // in[1] = 1, round(12/13) = 1
        Assert.assertTrue((53 + 2 - in[1]) % 53 <= 1);

        // Input array (25, 35)
        in[0] = 25;
        in[1] = 35;
        in[2] = 12;
        in[3] = 9;
        NttTool.nttNegacyclicHarvey(in, 0, nttTables[0]);
        NttTool.nttNegacyclicHarvey(in, n, nttTables[1]);
        // We expect to get a zero output also in this case
        rnsTool.divideAndRoundQLastNttInplace(in, 0, n, k, nttTables);
        NttTool.inverseNttNegacyclicHarvey(in, 0, nttTables[0]);
        // round(25/13) = 2
        Assert.assertTrue((53 + 2 - in[0]) % 53 <= 1);
        // round(35/13) = 3
        Assert.assertTrue((53 + 3 - in[1]) % 53 <= 1);
    }
}