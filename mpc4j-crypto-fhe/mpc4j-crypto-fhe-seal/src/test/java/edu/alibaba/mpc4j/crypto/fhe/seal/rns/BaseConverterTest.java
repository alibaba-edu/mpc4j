package edu.alibaba.mpc4j.crypto.fhe.seal.rns;

import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.RnsIterator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * RnsBase Convert unit tests.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/rns.cpp">rns.cpp</a>.
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
        bct_test(bct, new long[]{0}, new long[]{0});
        bct_test(bct, new long[]{1}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{3}));
        bct_test(bct, new long[]{0}, new long[]{0});
        bct_test(bct, new long[]{1}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        bct_test(bct, new long[]{0}, new long[]{0});
        bct_test(bct, new long[]{2}, new long[]{0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        bct_test(bct, new long[]{0, 0}, new long[]{0});
        bct_test(bct, new long[]{1, 1}, new long[]{1});
        bct_test(bct, new long[]{0, 2}, new long[]{0});
        bct_test(bct, new long[]{1, 0}, new long[]{1});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2, 3}));
        bct_test(bct, new long[]{0, 0}, new long[]{0, 0});
        bct_test(bct, new long[]{1, 1}, new long[]{1, 1});
        bct_test(bct, new long[]{1, 2}, new long[]{1, 2});
        bct_test(bct, new long[]{0, 2}, new long[]{0, 2});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{3, 4, 5}));
        bct_test(bct, new long[]{0, 0}, new long[]{0, 0, 0});
        bct_test(bct, new long[]{1, 1}, new long[]{1, 3, 2});
        bct_test(bct, new long[]{1, 2}, new long[]{2, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{3, 4, 5}), new RnsBase(new long[]{2, 3}));
        bct_test(bct, new long[]{0, 0, 0}, new long[]{0, 0});
        bct_test(bct, new long[]{1, 1, 1}, new long[]{1, 1});
    }

    private void bct_test(BaseConverter bct, long[] in, long[] out) {
        // auto bct_test = [&](const BaseConverter &bct, const vector<uint64_t> &in, const vector<uint64_t> &out) {
        //     uint64_t in_array[3], out_array[3];
        //     copy(in.cbegin(), in.cend(), in_array);
        //     bct.fast_convert(in.data(), out_array, pool);
        //     for (size_t i = 0; i < out.size(); i++)
        //     {
        //         ASSERT_EQ(out[i], out_array[i]);
        //     }
        // };
        long[] in_array = new long[in.length];
        long[] out_array = new long[out.length];
        System.arraycopy(in, 0, in_array, 0, in.length);

        bct.fastConvert(CoeffIterator.wrap(in_array, in_array.length), CoeffIterator.wrap(out_array, out_array.length));
        Assert.assertArrayEquals(out, out_array);
    }

    @Test
    public void testExactConvert() {
        BaseConverter bct;

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{2}));
        bct_exact_test(bct, new long[]{0}, 0);
        bct_exact_test(bct, new long[]{1}, 1);

        bct = new BaseConverter(new RnsBase(new long[]{2}), new RnsBase(new long[]{3}));
        bct_exact_test(bct, new long[]{0}, 0);
        bct_exact_test(bct, new long[]{1}, 1);

        bct = new BaseConverter(new RnsBase(new long[]{7}), new RnsBase(new long[]{2}));
        bct_exact_test(bct, new long[]{0}, 0);
        bct_exact_test(bct, new long[]{6}, 0);

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        bct_exact_test(bct, new long[]{0, 0}, 0);
        bct_exact_test(bct, new long[]{1, 1}, 1);
        bct_exact_test(bct, new long[]{0, 2}, 0);
        bct_exact_test(bct, new long[]{1, 0}, 1);
    }

    private void bct_exact_test(BaseConverter bct, long[] in, long out) {
        long out_single = bct.exactConvert(in);
        Assert.assertEquals(out_single, out);
    }

    @Test
    public void testConvertArray() {
        BaseConverter bct;
        bct = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        bct_test_array(bct, new long[]{0, 1, 2}, new long[]{0, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        bct_test_array(bct, new long[]{0, 1, 0, 0, 1, 2}, new long[]{0, 1, 0});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2, 3}));
        bct_test_array(bct, new long[]{1, 1, 0, 1, 2, 2}, new long[]{1, 1, 0, 1, 2, 2});

        bct = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{3, 4, 5}));
        bct_test_array(bct, new long[]{0, 1, 1, 0, 1, 2}, new long[]{0, 1, 2, 0, 3, 1, 0, 2, 0});
    }


    private void bct_test_array(BaseConverter bct, long[] in, long[] out) {
        // auto bct_test = [&](const BaseConverter &bct, const vector<uint64_t> &in, const vector<uint64_t> &out) {
        //     uint64_t in_array[3 * 3], out_array[3 * 3];
        //     copy(in.cbegin(), in.cend(), in_array);
        //     bct.fast_convert_array(ConstRNSIter(in.data(), 3), RNSIter(out_array, 3), pool);
        //     for (size_t i = 0; i < out.size(); i++)
        //     {
        //         ASSERT_EQ(out[i], out_array[i]);
        //     }
        // };
        int inK = bct.getInputBaseSize();
        int inN = in.length / inK;
        int outK = bct.getOutputBaseSize();
        int outN = out.length / outK;
        Assert.assertEquals(inN, outN);

        RnsIterator inRns = RnsIterator.wrap(in, inN, inK);
        RnsIterator outRns = RnsIterator.wrap(out, outN, outK);

        long[] expectOut = Arrays.copyOf(out, out.length);
        RnsIterator expectOutRns = RnsIterator.wrap(expectOut, outN, outK);

        bct.fastConvertArrayRnsIter(inRns, outRns);

        Assert.assertArrayEquals(expectOutRns.coeff(), outRns.coeff());
    }

    @Test
    public void testExactConvertArray() {
        BaseConverter baseConverter;
        baseConverter = new BaseConverter(new RnsBase(new long[]{3}), new RnsBase(new long[]{2}));
        bct_exact_test_array(baseConverter, new long[]{0, 1, 2}, new long[]{0, 1, 0});

        baseConverter = new BaseConverter(new RnsBase(new long[]{2, 3}), new RnsBase(new long[]{2}));
        bct_exact_test_array(baseConverter, new long[]{0, 1, 0, 0, 1, 2}, new long[]{0, 1, 0});
    }

    private void bct_exact_test_array(BaseConverter bct, long[] in, long[] out) {
        Assert.assertEquals(1, bct.getOutputBaseSize());
        int inK = bct.getInputBaseSize();
        int inN = in.length / inK;
        int outK = bct.getOutputBaseSize();
        int outN = out.length / outK;

        RnsIterator inRns = RnsIterator.wrap(in, inN, inK);
        long[] out_array = RnsIterator.allocateArray(outN, outK);

        bct.exactConvertArray(inRns, CoeffIterator.wrap(out_array, out_array.length));
        Assert.assertArrayEquals(out, out_array);
    }
}