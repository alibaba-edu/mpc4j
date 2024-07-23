package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;

/**
 * GF(2^128) finite field.
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public interface Gf2k extends Gf2e {
    /**
     * Gets type.
     *
     * @return type.
     */
    Gf2kType getGf2kType();

    /**
     * Returns if the given subfield is a subfield.
     *
     * @param subfield subfield.
     * @return true if the given subfield is a subfield, false otherwise.
     */
    default boolean isSubfield(Gf2e subfield) {
        int subfieldL = subfield.getL();
        int fieldL = getL();
        // subfield l be 2^k and l <= λ
        return IntMath.isPowerOfTwo(subfieldL) && subfieldL <= fieldL;
    }

    /**
     * Extends a subfield element into the field.
     *
     * @param subfield        subfield.
     * @param subfieldElement subfield element.
     * @return extend element.
     */
    default byte[] extend(Gf2e subfield, byte[] subfieldElement) {
        assert isSubfield(subfield);
        int subByteL = subfield.getByteL();
        int byteOffset = getByteL() - subByteL;
        byte[] extendElement = createZero();
        System.arraycopy(subfieldElement, 0, extendElement, byteOffset, subByteL);
        return extendElement;
    }
}
