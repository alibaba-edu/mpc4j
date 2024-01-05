package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIterator;
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
    public void testExactConvertArray() {
        BaseConverter baseConverter;
        baseConverter = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        testExactConvertArray(baseConverter, new long[]{0, 1, 2}, new long[]{0, 1, 0});

        baseConverter = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        testExactConvertArray(baseConverter, new long[]{0, 1, 0, 0, 1, 2}, new long[]{0, 1, 0});
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