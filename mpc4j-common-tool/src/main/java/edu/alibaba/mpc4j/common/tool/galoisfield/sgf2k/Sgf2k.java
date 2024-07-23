package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import edu.alibaba.mpc4j.common.tool.galoisfield.BytesField;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * Subfield GF2K. This API is used in Subfield-VOLE, which treats a {0,1}^κ as a combination of subfield elements.
 * Specifically, when we work in an extension field F_{{2^t}^r} of F_{2^t} where t · r = κ, we fix some monic,
 * irreducible polynomial f(X) of degree r and so F_{{2^t}^r} =∼ F_{2^t}[X]/f(X).
 *
 * @author Weiran Liu
 * @date 2024/6/1
 */
public interface Sgf2k extends BytesField {
    /**
     * Gets the subfield.
     *
     * @return subfield.
     */
    Gf2e getSubfield();

    /**
     * Gets the maximal l (in bit length) so that all elements in {0, 1}^l is a valid subfield element.
     *
     * @return the maximal l (in bit length) for the subfield.
     */
    int getSubfieldL();

    /**
     * Gets the maximal l (in byte length) so that all elements in {0, 1}^l is a valid subfield element.
     *
     * @return the maximal l (in byte length) for the subfield.
     */
    int getSubfieldByteL();

    /**
     * gets r, i.e., field L / subfield L.
     *
     * @return r.
     */
    int getR();

    /**
     * Decomposite the field element to r subfield elements.
     *
     * @param fieldElement the field element.
     * @return decomposited subfield elements.
     */
    byte[][] decomposite(byte[] fieldElement);

    /**
     * Composites r subfield elements to a field element.
     *
     * @param subfieldElements r subfield elements.
     * @return composited field element.
     */
    byte[] composite(byte[][] subfieldElements);

    /**
     * Extends the subfield element to a field element.
     *
     * @param subfieldElement the subfield element.
     * @return the field element.
     */
    byte[] extend(byte[] subfieldElement);

    /**
     * Computes p · X^h, where p is in subfield, and the result is in field.
     *
     * @param p the subfield element p.
     * @param h X^h.
     * @return p · X^h.
     */
    byte[] mixPow(byte[] p, int h);

    /**
     * Computes p · X^h, where p is in field, and the result is in field.
     *
     * @param p the field element p.
     * @param h X^h.
     * @return p · X^h.
     */
    byte[] fieldPow(byte[] p, int h);

    /**
     * Computes p · q, where p is in subfield, q is in field, and the result is in field.
     *
     * @param p the subfield element p.
     * @param q the field element q.
     * @return p · q.
     */
    default byte[] mixMul(byte[] p, byte[] q) {
        Gf2e subfield = getSubfield();
        assert subfield.validateElement(p);
        int r = getR();
        byte[][] subfieldElements = decomposite(q);
        assert subfieldElements.length == r;
        for (int i = 0; i < r; i++) {
            subfield.muli(subfieldElements[i], p);
        }
        return composite(subfieldElements);
    }

    /**
     * For a vector (x_0, ..., x_127) where x_i ∈ F_{2^t}, computes x_0 * X^{127} + ... + x_127 * X^0.
     *
     * @param xs xs.
     * @return inner product.
     */
    default byte[] mixInnerProduct(byte[][] xs) {
        int fieldL = getL();
        assert xs.length == fieldL;
        byte[] result = createZero();
        for (int i = getL() - 1; i >= 0; i--) {
            byte[] shift = createZero();
            BinaryUtils.setBoolean(shift, i, true);
            addi(result, mul(extend(xs[i]), shift));
        }
        return result;
    }

    /**
     * For a vector (x_0, ..., x_{r - 1}) where x_i ∈ F_{{2^t}}, computes x_{r - 1} * X^{r - 1} + ... + x_0 * X^0.
     *
     * @param xs xs.
     * @return inner product.
     */
    default byte[] innerProduct(byte[][] xs) {
        int r = getR();
        assert xs.length == r;
        byte[] result = createZero();
        for (int h = r - 1; h >= 0; h--) {
            byte[] mul = fieldPow(xs[h], h);
            addi(result, mul);
        }
        return result;
    }
}
