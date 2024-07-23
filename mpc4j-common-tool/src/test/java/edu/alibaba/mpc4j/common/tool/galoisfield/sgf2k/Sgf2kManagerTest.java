package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eManager;
import org.junit.Assert;
import org.junit.Test;

/**
 * Subfield GF2K manager test.
 *
 * @author Weiran Liu
 * @date 2024/6/2
 */
public class Sgf2kManagerTest {
    /**
     * field L
     */
    private static final int FIELD_L = CommonConstants.BLOCK_BIT_LENGTH;

    @Test
    public void testParameters() {
        for (int subfieldL : new int[]{2, 4, 8, 16, 32, 64}) {
            FiniteField<UnivariatePolynomialZp64> subfieldFiniteField = Gf2eManager.getFiniteField(subfieldL);
            int r = FIELD_L / subfieldL;
            FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> fieldFiniteField
                = Sgf2kManager.getFieldFiniteField(subfieldL);
            Assert.assertTrue(fieldFiniteField.isField());
            UnivariatePolynomial<UnivariatePolynomialZp64> fieldRingsMinimalPolynomial = fieldFiniteField.getMinimalPolynomial();
            Assert.assertEquals(r, fieldRingsMinimalPolynomial.degree());
            byte[][] fieldMinimalPolynomial = Sgf2kManager.getFieldMinimalPolynomial(subfieldL);
            Assert.assertTrue(subfieldFiniteField.isOne(fieldRingsMinimalPolynomial.get(fieldMinimalPolynomial.length - 1)));
        }
    }
}
