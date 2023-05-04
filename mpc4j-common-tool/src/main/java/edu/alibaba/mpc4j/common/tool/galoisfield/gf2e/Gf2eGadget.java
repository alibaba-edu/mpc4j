package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * GF2E域小工具。来自下述论文：
 * <p>
 * Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: faster malicious arithmetic secure computation with
 * oblivious transfer. CCS 2016, pp. 830-842. 2016.
 * </p>
 * Section 2，Notation部分。 由于元素采用大端表示，小工具向量（Gadget Array）的元素顺序和原论文相反。即：
 * <p>
 * gadget = (X^{127}, X^{126}, ...., X, 1)
 *
 * @author Weiran Liu
 * @date 2023/3/13
 */
public class Gf2eGadget {
    /**
     * the GF2E instance
     */
    private final Gf2e gf2e;
    /**
     * l
     */
    private final int l;
    /**
     * byte l
     */
    private final int byteL;
    /**
     * gadget array
     */
    private final byte[][] gadgetArray;

    /**
     * Creates an GF2E gadget.
     *
     * @param gf2e a GF2E instance.
     */
    public Gf2eGadget(Gf2e gf2e) {
        this.gf2e = gf2e;
        l = gf2e.getL();
        byteL = gf2e.getByteL();
        gadgetArray = IntStream.range(0, l)
            .mapToObj(i -> BigInteger.ONE.shiftLeft(l - i - 1))
            .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
            .toArray(byte[][]::new);
    }

    /**
     * Computes the inner product of the input array and the gadget array.
     *
     * @param inputArray the input array.
     * @return the inner product.
     */
    public byte[] innerProduct(byte[][] inputArray) {
        assert inputArray.length == l : "input array length must equal to " + l + ": " + inputArray.length;
        byte[] result = new byte[byteL];
        for (int i = 0; i < l; i++) {
            byte[] product = gf2e.mul(gadgetArray[i], inputArray[i]);
            gf2e.addi(result, product);
        }
        return result;
    }

    /**
     * Bit-composites a GF2E element.
     *
     * @param binary the bit representation.
     * @return the composition result.
     */
    public byte[] composition(boolean[] binary) {
        assert binary.length == l : "binary length must equal to " + l + ": " + binary.length;
        byte[] result = new byte[byteL];
        for (int i = 0; i < l; i++) {
            if (binary[i]) {
                gf2e.addi(result, gadgetArray[i]);
            }
        }
        return result;
    }

    /**
     * Bit-decomposites a GF2E element.
     *
     * @param element a GF2E element.
     * @return the decomposition result.
     */
    public boolean[] decomposition(byte[] element) {
        assert gf2e.validateRangeElement(element) : "element must be valid";
        return BinaryUtils.byteArrayToBinary(element, l);
    }
}
