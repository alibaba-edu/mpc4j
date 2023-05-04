package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * GF2E gadget tests.
 *
 * @author Weiran Liu
 * @date 2023/3/13
 */
@RunWith(Parameterized.class)
public class Gf2eGadgetTest {
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * the random test round
     */
    private static final int RANDOM_ROUND = 40;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 128, 256};
        // add each l
        for (int l : ls) {
            configurations.add(new Object[]{"l = " + l + ")", l});
        }

        return configurations;
    }

    /**
     * the GF2E instance
     */
    private final Gf2e gf2e;
    /**
     * the GF2E gadget
     */
    private final Gf2eGadget gf2eGadget;

    public Gf2eGadgetTest(String name, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, l);
        gf2eGadget = new Gf2eGadget(gf2e);
    }

    @Test
    public void testBitComposition() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] element = gf2e.createRangeRandom(SECURE_RANDOM);
            boolean[] decomposition = gf2eGadget.decomposition(element);
            byte[] compositeElement = gf2eGadget.composition(decomposition);
            Assert.assertArrayEquals(element, compositeElement);
        }
    }

    @Test
    public void testInnerProduct() {
        int l = gf2e.getL();
        int byteL = gf2e.getByteL();
        // inner product of 0 is 0
        byte[] zero = gf2e.createZero();
        byte[][] zeroElementVector = new byte[l][byteL];
        byte[] zeroInnerProduct = gf2eGadget.innerProduct(zeroElementVector);
        Assert.assertArrayEquals(zero, zeroInnerProduct);
        // inner product of 1 is -1
        byte[] negOne = gf2e.createZero();
        BytesUtils.noti(negOne, l);
        byte[][] oneElementVector = IntStream.range(0, l)
            .mapToObj(index -> gf2e.createOne())
            .toArray(byte[][]::new);
        byte[] oneInnerProduct = gf2eGadget.innerProduct(oneElementVector);
        Assert.assertArrayEquals(negOne, oneInnerProduct);
    }
}
