package edu.alibaba.mpc4j.crypto.swhe.seal.rns;

import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.swhe.seal.iterator.RnsIterator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * RnsBase Convert unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/rns.cpp
 * </p>
 *
 * @author Anony_Trent
 * @date 2023/8/19
 */
public class BaseConverterTest {

    @Test
    public void testInitialize() {
        // create new instance successfully
        new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{2}));
        new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{3}));
        new BaseConverter(new RnsBase(new long[]{2, 3, 5}), new RnsBase(new long[]{2}));
        new BaseConverter(new RnsBase(new long[]{2, 3, 5}), new RnsBase(new long[]{3, 5}));
        new BaseConverter(new RnsBase(new long[]{2, 3, 5}), new RnsBase(new long[]{2, 3, 5, 7, 11}));
        new BaseConverter(new RnsBase(new long[]{2, 3, 5}), new RnsBase(new long[]{7, 11}));
    }

    @Test
    public void testConvertIterator() {
        BaseConverter bct;

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{2}));
        testFastConvertIterator(bct, new long[]{0}, new long[]{0});
        testFastConvertIterator(bct, new long[]{1}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{3}));
        testFastConvertIterator(bct, new long[]{0}, new long[]{0});
        testFastConvertIterator(bct, new long[]{1}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        testFastConvertIterator(bct, new long[]{0}, new long[]{0});
        testFastConvertIterator(bct, new long[]{2}, new long[]{0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        testFastConvertIterator(bct, new long[]{0, 0}, new long[]{0});
        testFastConvertIterator(bct, new long[]{1, 1}, new long[]{1});
        testFastConvertIterator(bct, new long[]{0, 2}, new long[]{0});
        testFastConvertIterator(bct, new long[]{1, 0}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2, 3}));
        testFastConvertIterator(bct, new long[]{0, 0}, new long[]{0, 0});
        testFastConvertIterator(bct, new long[]{1, 1}, new long[]{1, 1});
        testFastConvertIterator(bct, new long[]{1, 2}, new long[]{1, 2});
        testFastConvertIterator(bct, new long[]{0, 2}, new long[]{0, 2});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{3, 4, 5}));
        testFastConvertIterator(bct, new long[]{0, 0}, new long[]{0, 0, 0});
        testFastConvertIterator(bct, new long[]{1, 1}, new long[]{1, 3, 2});
        testFastConvertIterator(bct, new long[]{1, 2}, new long[]{2, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{3, 4, 5}), new RnsBase(new long[]{2, 3}));
        testFastConvertIterator(bct, new long[]{0, 0, 0}, new long[]{0, 0});
        testFastConvertIterator(bct, new long[]{1, 1, 1}, new long[]{1, 1});
    }


    @Test
    public void testConvert() {
        BaseConverter bct;

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{2}));
        testFastConvert(bct, new long[]{0}, new long[]{0});
        testFastConvert(bct, new long[]{1}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{3}));
        testFastConvert(bct, new long[]{0}, new long[]{0});
        testFastConvert(bct, new long[]{1}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        testFastConvert(bct, new long[]{0}, new long[]{0});
        testFastConvert(bct, new long[]{2}, new long[]{0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        testFastConvert(bct, new long[]{0, 0}, new long[]{0});
        testFastConvert(bct, new long[]{1, 1}, new long[]{1});
        testFastConvert(bct, new long[]{0, 2}, new long[]{0});
        testFastConvert(bct, new long[]{1, 0}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2, 3}));
        testFastConvert(bct, new long[]{0, 0}, new long[]{0, 0});
        testFastConvert(bct, new long[]{1, 1}, new long[]{1, 1});
        testFastConvert(bct, new long[]{1, 2}, new long[]{1, 2});
        testFastConvert(bct, new long[]{0, 2}, new long[]{0, 2});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{3, 4, 5}));
        testFastConvert(bct, new long[]{0, 0}, new long[]{0, 0, 0});
        testFastConvert(bct, new long[]{1, 1}, new long[]{1, 3, 2});
        testFastConvert(bct, new long[]{1, 2}, new long[]{2, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{3, 4, 5}), new RnsBase(new long[]{2, 3}));
        testFastConvert(bct, new long[]{0, 0, 0}, new long[]{0, 0});
        testFastConvert(bct, new long[]{1, 1, 1}, new long[]{1, 1});
    }

    private void testFastConvert(BaseConverter bct, long[] in, long[] out) {
        long[] outArray = new long[out.length];
        bct.fastConvert(in, outArray);
        Assert.assertArrayEquals(outArray, out);
    }

    private void testFastConvertIterator(BaseConverter bct, long[] in, long[] out) {
        long[] outArray = new long[out.length];

        CoeffIterator inCoeffIter = new CoeffIterator(in, 0, in.length);
        CoeffIterator outCoeffIter = new CoeffIterator(out, 0, out.length);
        CoeffIterator outArrayCoeffIter = new CoeffIterator(outArray, 0, outArray.length);

        bct.fastConvert(inCoeffIter, outArrayCoeffIter);
        Assert.assertArrayEquals(outArrayCoeffIter.getCoeff(), outCoeffIter.getCoeff());
    }


    @Test
    public void tesConvertArray() {
        BaseConverter bct;
        bct = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        testFastConvertArray(bct, new long[]{0, 1, 2}, new long[]{0, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        testFastConvertArray(bct, new long[]{0, 1, 0, 0, 1, 2}, new long[]{0, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2, 3}));
        testFastConvertArray(bct, new long[]{1, 1, 0, 1, 2, 2}, new long[]{1, 1, 0, 1, 2, 2});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{3, 4, 5}));
        testFastConvertArray(bct, new long[]{0, 1, 1, 0, 1, 2}, new long[]{0, 1, 2, 0, 3, 1, 0, 2, 0});
    }

    private void testFastConvertArray(BaseConverter bct, long[] in, long[] out) {
        int inK = bct.getInputBaseSize();
        int inN = in.length / inK;
        int outK = bct.getOutputBaseSize();
        int outN = out.length / outK;
        Assert.assertEquals(inN, outN);
        long[] expectOut = Arrays.copyOf(out, out.length);
        bct.fastConvertArrayRnsIter(in, 0, inN, inK, out, 0, outN, outK);
        Assert.assertArrayEquals(expectOut, out);
    }

    @Test
    public void tesConvertArrayIterator() {
        BaseConverter bct;
        bct = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        testFastConvertArrayIterator(bct, new long[]{0, 1, 2}, new long[]{0, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        testFastConvertArrayIterator(bct, new long[]{0, 1, 0, 0, 1, 2}, new long[]{0, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2, 3}));
        testFastConvertArrayIterator(bct, new long[]{1, 1, 0, 1, 2, 2}, new long[]{1, 1, 0, 1, 2, 2});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{3, 4, 5}));
        testFastConvertArrayIterator(bct, new long[]{0, 1, 1, 0, 1, 2}, new long[]{0, 1, 2, 0, 3, 1, 0, 2, 0});
    }


    private void testFastConvertArrayIterator(BaseConverter bct, long[] in, long[] out) {
        int inK = bct.getInputBaseSize();
        int inN = in.length / inK;
        int outK = bct.getOutputBaseSize();
        int outN = out.length / outK;
        Assert.assertEquals(inN, outN);

        RnsIterator inRns = new RnsIterator(in, 0, inN, inK);
        RnsIterator outRns = new RnsIterator(out, 0, outN, outK);

        long[] expectOut = Arrays.copyOf(out, out.length);
        RnsIterator expectOutRns = new RnsIterator(expectOut, 0, outN, outK);

        bct.fastConvertArrayRnsIter(inRns, outRns);

        Assert.assertArrayEquals(expectOutRns.coeff, outRns.coeff);
    }




    @Test
    public void testExactConvert() {
        BaseConverter bct;

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{2}));
        testExactConvert(bct, new long[]{0}, 0);
        testExactConvert(bct, new long[]{1}, 1);

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{3}));
        testExactConvert(bct, new long[]{0}, 0);
        testExactConvert(bct, new long[]{1}, 1);

        bct = new BaseConverter(new RnsBase(new long[]{7}), new RnsBase(new long[]{2}));
        testExactConvert(bct, new long[]{0}, 0);
        testExactConvert(bct, new long[]{6}, 0);

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        testExactConvert(bct, new long[]{0, 0}, 0);
        testExactConvert(bct, new long[]{1, 1}, 1);
        testExactConvert(bct, new long[]{0, 2}, 0);
        testExactConvert(bct, new long[]{1, 0}, 1);
    }

    private void testExactConvert(BaseConverter bct, long[] in, long out) {
        long curOut = bct.exactConvert(in);
        Assert.assertEquals(curOut, out);
    }

    @Test
    public void testExactConvertArrayIterator() {
        BaseConverter baseConverter;
        baseConverter = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        testExactConvertArrayIterator(baseConverter, new long[]{0, 1, 2}, new long[]{0, 1, 0});

        baseConverter = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        testExactConvertArrayIterator(baseConverter, new long[]{0, 1, 0, 0, 1, 2}, new long[]{0, 1, 0});
    }

    @Test
    public void testExactConvertArray() {
        BaseConverter baseConverter;
        baseConverter = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        testExactConvertArray(baseConverter, new long[]{0, 1, 2}, new long[]{0, 1, 0});

        baseConverter = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        testExactConvertArray(baseConverter, new long[]{0, 1, 0, 0, 1, 2}, new long[]{0, 1, 0});
    }

    private void testExactConvertArrayIterator(BaseConverter bct, long[] in, long[] out) {
        Assert.assertEquals(1, bct.getOutputBaseSize());
        int inK = bct.getInputBaseSize();
        int inN = in.length / inK;
        int outK = bct.getOutputBaseSize();
        int outN = out.length / outK;

        RnsIterator inRns = new RnsIterator(in, 0, inN, inK);
        CoeffIterator outCoeff = new CoeffIterator(out, 0, outN);

        long[] outArray = RnsIterator.allocateZeroRns(outN, outK);
        CoeffIterator outArrayCoeff = new CoeffIterator(outArray, 0, outArray.length);

        bct.exactConvertArray(inRns, outArrayCoeff);
        Assert.assertArrayEquals(outCoeff.getCoeff(), outArrayCoeff.getCoeff());
    }

    private void testExactConvertArray(BaseConverter bct, long[] in, long[] out) {
        Assert.assertEquals(1, bct.getOutputBaseSize());
        int inK = bct.getInputBaseSize();
        int inN = in.length / inK;
        int outK = bct.getOutputBaseSize();
        int outN = out.length / outK;
        long[] outArray = RnsIterator.allocateZeroRns(outN, outK);
        bct.exactConvertArray(in, inN, inK, outArray);
        Assert.assertArrayEquals(out, outArray);
    }
}