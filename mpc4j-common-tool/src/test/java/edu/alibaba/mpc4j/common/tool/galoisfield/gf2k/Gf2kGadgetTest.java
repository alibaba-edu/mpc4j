package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Binary GF2K gadget test.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class Gf2kGadgetTest {
    /**
     * the random test round
     */
    private static final int RANDOM_ROUND = 40;
    /**
     * field
     */
    private final Gf2k field;
    /**
     * binary GF2K gadget
     */
    private final Gf2kGadget gadget;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Gf2kGadgetTest() {
        field = Gf2kFactory.createInstance(EnvType.STANDARD);
        gadget = new Gf2kGadget(field);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testBitComposition() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] element = field.createRangeRandom(secureRandom);
            boolean[] decomposition = gadget.decomposition(element);
            byte[] compositeElement = gadget.composition(decomposition);
            Assert.assertArrayEquals(element, compositeElement);
        }
    }

    @Test
    public void testInnerProduct() {
        int l = field.getL();
        int byteL = field.getByteL();
        // inner product of 0 is 0
        byte[] allZeroFieldElement = field.createZero();
        byte[][] zeroElementVector = new byte[l][byteL];
        byte[] zeroInnerProduct = gadget.innerProduct(zeroElementVector);
        Assert.assertArrayEquals(allZeroFieldElement, zeroInnerProduct);
        // inner product of 1 is -1
        boolean[] allOneBinary = new boolean[l];
        Arrays.fill(allOneBinary, true);
        byte[] allOneFieldElement = gadget.composition(allOneBinary);
        byte[][] oneElementVector = IntStream.range(0, l)
            .mapToObj(index -> field.createOne())
            .toArray(byte[][]::new);
        byte[] oneInnerProduct = gadget.innerProduct(oneElementVector);
        Assert.assertArrayEquals(allOneFieldElement, oneInnerProduct);
    }
}
