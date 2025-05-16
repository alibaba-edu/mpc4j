package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;

/**
 * Binary GF2K gadget. The scheme comes from:
 * <p>
 * Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: faster malicious arithmetic secure computation with
 * oblivious transfer. CCS 2016, pp. 830-842. 2016.
 * </p>
 * <p></p>
 * Since all elements are represented in big-endian form, the sequence of Gadget Array is in reversed order, that is,
 * <p>
 * gadget = (X^{127}, X^{126}, ...., X, 1)
 * </p>
 * This is different compared with Section 2, Notation.
 *
 * @author Weiran Liu
 * @date 2023/3/13
 */
public class Gf2kGadget {
    /**
     * the GF2K instance
     */
    private final Gf2k gf2k;
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
     * Creates an GF2K gadget.
     */
    public Gf2kGadget(Gf2k gf2k) {
        this.gf2k = gf2k;
        l = gf2k.getL();
        byteL = gf2k.getByteL();
        gadgetArray = new byte[l][byteL];
        byte[] fieldOne = gf2k.createOne();
        for (int i = l - 1; i >= 0; i--) {
            gadgetArray[i] = BlockUtils.clone(fieldOne);
            BlockUtils.shiftLefti(fieldOne, 1);
        }
    }

    /**
     * Computes the inner product of the field elements and the gadget array.
     *
     * @param fieldElements field elements.
     * @return the inner product.
     */
    public byte[] innerProduct(byte[][] fieldElements) {
        MathPreconditions.checkEqual("l", "field_elements.length", l, fieldElements.length);
        byte[] result = new byte[byteL];
        for (int i = 0; i < l; i++) {
            byte[] product = gf2k.mul(gadgetArray[i], fieldElements[i]);
            gf2k.addi(result, product);
        }
        return result;
    }

    /**
     * Bit-composites a GF2K element.
     *
     * @param binary the bit representation.
     * @return the composition result.
     */
    public byte[] composition(boolean[] binary) {
        MathPreconditions.checkEqual("l", "binary.length", l, binary.length);
        return BinaryUtils.binaryToByteArray(binary);
    }

    /**
     * Bit-decomposites a GF2K element.
     *
     * @param element a GF2K element.
     * @return the decomposition result.
     */
    public boolean[] decomposition(byte[] element) {
        Preconditions.checkArgument(gf2k.validateRangeElement(element));
        return BinaryUtils.byteArrayToBinary(element, l);
    }
}
