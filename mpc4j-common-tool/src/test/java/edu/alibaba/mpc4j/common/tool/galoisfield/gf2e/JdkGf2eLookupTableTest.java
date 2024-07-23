package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.PolynomialMethods;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.Gf2xUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * GF(2^8) test.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class JdkGf2eLookupTableTest {

    @Test
    public void testJdkGf008LookupTable() {
        testPowLookupTable(8, JdkGf008.X_POW_MOD_LOOKUP_TABLE);
    }

    @Test
    public void testJdkGf016LookupTable() {
        testPowLookupTable(16, JdkGf016.X_POW_MOD_LOOKUP_TABLE);
    }

    @Test
    public void testJdkGf032LookupTable() {
        testPowLookupTable(32, JdkGf032.X_POW_MOD_LOOKUP_TABLE);
    }

    private void testPowLookupTable(int l, long[] lookupTable) {
        int byteL = CommonUtils.getByteLength(l);
        FiniteField<UnivariatePolynomialZp64> finiteField = Gf2eManager.getFiniteField(l);
        for (int i = 0; i < 2 * l; i++) {
            byte[] xBytes = new byte[byteL * 2];
            BinaryUtils.setBoolean(xBytes, byteL * 2 * Byte.SIZE - 1 - i, true);
            UnivariatePolynomialZp64 x = Gf2xUtils.byteArrayToRings(xBytes);
            UnivariatePolynomialZp64 remainder = PolynomialMethods.remainder(x, finiteField.getMinimalPolynomial());
            long remainderLong = new BigInteger(1, Gf2xUtils.ringsToByteArray(remainder, byteL)).longValue();
            Assert.assertEquals(remainderLong, lookupTable[i]);
        }
    }
}
