package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;

import org.junit.Assert;
import org.junit.Test;

/**
 * Galois unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/galois.cpp
 * </p>
 *
 * @author Anony_Trent, Liqiang Peng
 * @date 2023/9/11
 */
public class GaloisToolTest {

    @Test
    public void testCreate() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new GaloisTool(0));
        Assert.assertThrows(IllegalArgumentException.class, () -> new GaloisTool(18));
        new GaloisTool(1);
        new GaloisTool(13);
    }

    @Test
    public void testEltFromStep() {
        GaloisTool galoisTool = new GaloisTool(3);
        Assert.assertEquals(15, galoisTool.getEltFromStep(0));
        Assert.assertEquals(3, galoisTool.getEltFromStep(1));
        Assert.assertEquals(3, galoisTool.getEltFromStep(-3));
        Assert.assertEquals(9, galoisTool.getEltFromStep(2));
        Assert.assertEquals(9, galoisTool.getEltFromStep(-2));
        Assert.assertEquals(11, galoisTool.getEltFromStep(3));
        Assert.assertEquals(11, galoisTool.getEltFromStep(-1));
    }

    @Test
    public void testEltFromSteps() {
        GaloisTool galoisTool = new GaloisTool(3);
        int[] elts = galoisTool.getEltsFromSteps(new int[] {0, 1, -3, 2, -2, 3, -1});
        int[] eltsTrue = new int[] {15, 3, 3, 9, 9, 11, 11};
        for (int i = 0; i < elts.length; i++) {
            Assert.assertEquals(eltsTrue[i], elts[i]);
        }
    }

    @Test
    public void testEltsAll() {
        GaloisTool galoisTool = new GaloisTool(3);
        int[] elts = galoisTool.getEltsAll();
        int[] eltsTrue = new int[] {15, 3, 11, 9, 9};
        for (int i = 0; i < elts.length; i++) {
            Assert.assertEquals(eltsTrue[i], elts[i]);
        }
    }

    @Test
    public void testIndexFromElt() {
        Assert.assertEquals(7, GaloisTool.getIndexFromElt(15));
        Assert.assertEquals(1, GaloisTool.getIndexFromElt(3));
        Assert.assertEquals(4, GaloisTool.getIndexFromElt(9));
        Assert.assertEquals(5, GaloisTool.getIndexFromElt(11));
    }

    @Test
    public void testApplyGalois() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        parms.setPolyModulusDegree(8);
        parms.setCoeffModulus(new long[]{17});
        parms.setPlainModulus(3);
        SealContext context = new SealContext(parms, false, CoeffModulus.SecLevelType.NONE);
        SealContext.ContextData context_data = context.keyContextData();
        GaloisTool galoisTool = context_data.galoisTool();
        long[] in = new long[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        long[] out = new long[8];
        long[] outTrue = new long[] { 0, 14, 6, 1, 13, 7, 2, 12 };
        galoisTool.applyGaloisRnsIter(in, 0, 8, 1, 3, parms.coeffModulus(), out, 0, 8, 1);
        for (int i = 0; i < 8; i++) {
            Assert.assertEquals(outTrue[i], out[i]);
        }
    }

    @Test
    public void testApplyGaloisNTT() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        parms.setPolyModulusDegree(8);
        parms.setCoeffModulus(new long[]{17});
        parms.setPlainModulus(3);
        SealContext context = new SealContext(parms, false, CoeffModulus.SecLevelType.NONE);
        SealContext.ContextData context_data = context.keyContextData();
        GaloisTool galoisTool = context_data.galoisTool();
        long[] in = new long[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        long[] out = new long[8];
        long[] outTrue = new long[] { 4, 5, 7, 6, 1, 0, 2, 3 };
        galoisTool.applyGaloisNttRnsIter(in, 8, 1, 3, out, 8, 1);
        for (int i = 0; i < 8; i++) {
            Assert.assertEquals(outTrue[i], out[i]);
        }
    }
}
