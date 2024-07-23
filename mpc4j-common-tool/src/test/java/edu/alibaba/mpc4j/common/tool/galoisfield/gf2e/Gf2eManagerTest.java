package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.utils.Gf2xUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * GF2E manager test.
 *
 * @author Weiran Liu
 * @date 2024/6/1
 */
public class Gf2eManagerTest {

    @Test
    public void testDefaultFiniteField() {
        for (int l : new int[] {2, 4, 8, 16, 32, 40, 64, 128}) {
            FiniteField<UnivariatePolynomialZp64> finiteField = Gf2eManager.getFiniteField(l);
            Assert.assertTrue(finiteField.isField());
            byte[] minimalPolynomial = Gf2eManager.getMinimalPolynomial(l);
            Assert.assertEquals(finiteField.getMinimalPolynomial(), Gf2xUtils.byteArrayToRings(minimalPolynomial));
        }
    }

    @Test
    public void testCreateFiniteField() {
        for (int l : new int[] {3, 5, 7, 9}) {
            FiniteField<UnivariatePolynomialZp64> finiteField = Gf2eManager.getFiniteField(l);
            Assert.assertTrue(finiteField.isField());
            byte[] minimalPolynomial = Gf2eManager.getMinimalPolynomial(l);
            Assert.assertEquals(finiteField.getMinimalPolynomial(), Gf2xUtils.byteArrayToRings(minimalPolynomial));
        }
    }
}
