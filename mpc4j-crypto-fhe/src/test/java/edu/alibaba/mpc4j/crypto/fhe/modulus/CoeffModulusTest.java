package edu.alibaba.mpc4j.crypto.fhe.modulus;

import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.junit.Assert;
import org.junit.Test;

/**
 * CoeffModulus unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/modulus.cpp.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/29
 */
public class CoeffModulusTest {

    @Test
    public void testCustomException() {
        // Too small poly_modulus_degree
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(1, new int[]{2}));

        // Too large poly_modulus_degree
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(262144, new int[]{30}));

        // Invalid poly_modulus_degree
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(1023, new int[]{20}));

        // Invalid bit-size
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2048, new int[]{0}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2048, new int[]{-30}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2048, new int[]{30, -30}));

        // Too small primes requested
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2, new int[]{2}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2, new int[]{3, 3, 3}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(1024, new int[]{8}));

        // Too small poly_modulus_degree
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(1, new Modulus(2), new int[]{2}));

        // Too large poly_modulus_degree
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(262144, new Modulus(2), new int[]{30}));

        // Invalid poly_modulus_degree
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(1023, new Modulus(2), new int[]{20}));

        // Invalid bit-size
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2048, new Modulus(2), new int[]{0}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2048, new Modulus(2), new int[]{-30}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2048, new Modulus(2), new int[]{30, -30}));

        // Too large LCM(2 * poly_modulus_degree, plain_modulus)
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2048, new Modulus(1L << 53), new int[]{20}));

        // Too small primes requested
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2, new Modulus(2), new int[]{2}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(2, new Modulus(30), new int[]{6, 6}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(1024, new Modulus(257), new int[]{20}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(1024, new Modulus(255), new int[]{22, 22, 22}));
        Assert.assertThrows(IllegalArgumentException.class, () -> CoeffModulus.create(1024, new Modulus(255), new int[]{22, 22, 22}));
    }

    @Test
    public void testCustom() {
        Modulus[] cm = CoeffModulus.create(2, new int[]{});
        Assert.assertEquals(0, cm.length);

        cm = CoeffModulus.create(2, new int[]{3});
        Assert.assertEquals(1, cm.length);
        Assert.assertEquals(5, cm[0].value());

        cm = CoeffModulus.create(2, new int[]{3, 4});
        Assert.assertEquals(2, cm.length);
        Assert.assertEquals(5, cm[0].value());
        Assert.assertEquals(13, cm[1].value());

        cm = CoeffModulus.create(2, new int[]{3, 5, 4, 5});
        Assert.assertEquals(4, cm.length);
        Assert.assertEquals(5, cm[0].value());
        Assert.assertEquals(17, cm[1].value());
        Assert.assertEquals(13, cm[2].value());
        Assert.assertEquals(29, cm[3].value());

        cm = CoeffModulus.create(32, new int[]{30, 40, 30, 30, 40});
        Assert.assertEquals(5, cm.length);
        Assert.assertEquals(30, UintCore.getSignificantBitCount(cm[0].value()));
        Assert.assertEquals(40, UintCore.getSignificantBitCount(cm[1].value()));
        Assert.assertEquals(30, UintCore.getSignificantBitCount(cm[2].value()));
        Assert.assertEquals(30, UintCore.getSignificantBitCount(cm[3].value()));
        Assert.assertEquals(40, UintCore.getSignificantBitCount(cm[4].value()));
        // prime number modulo 2 * N = 1
        Assert.assertEquals(1, cm[0].value() % 64);
        Assert.assertEquals(1, cm[1].value() % 64);
        Assert.assertEquals(1, cm[2].value() % 64);
        Assert.assertEquals(1, cm[3].value() % 64);
        Assert.assertEquals(1, cm[4].value() % 64);

        // with modulus
        cm = CoeffModulus.create(2, new Modulus(4), new int[]{});
        Assert.assertEquals(0, cm.length);

        cm = CoeffModulus.create(2, new Modulus(4), new int[]{3});
        Assert.assertEquals(1, cm.length);
        Assert.assertEquals(5, cm[0].value());

        cm = CoeffModulus.create(2, new Modulus(4), new int[]{3, 4});
        Assert.assertEquals(2, cm.length);
        Assert.assertEquals(5, cm[0].value());
        Assert.assertEquals(13, cm[1].value());

        cm = CoeffModulus.create(2, new Modulus(4), new int[]{3, 5, 4, 5});
        Assert.assertEquals(4, cm.length);
        Assert.assertEquals(5, cm[0].value());
        Assert.assertEquals(17, cm[1].value());
        Assert.assertEquals(13, cm[2].value());
        Assert.assertEquals(29, cm[3].value());

        cm = CoeffModulus.create(32, new Modulus(64), new int[]{30, 40, 30, 30, 40});
        Assert.assertEquals(5, cm.length);
        Assert.assertEquals(30, UintCore.getSignificantBitCount(cm[0].value()));
        Assert.assertEquals(40, UintCore.getSignificantBitCount(cm[1].value()));
        Assert.assertEquals(30, UintCore.getSignificantBitCount(cm[2].value()));
        Assert.assertEquals(30, UintCore.getSignificantBitCount(cm[3].value()));
        Assert.assertEquals(40, UintCore.getSignificantBitCount(cm[4].value()));
        Assert.assertEquals(1, cm[0].value() % 64);
        Assert.assertEquals(1, cm[1].value() % 64);
        Assert.assertEquals(1, cm[2].value() % 64);
        Assert.assertEquals(1, cm[3].value() % 64);
        Assert.assertEquals(1, cm[4].value() % 64);

        cm = CoeffModulus.create(1024, new Modulus(255), new int[]{22, 22});
        Assert.assertEquals(2, cm.length);
        Assert.assertEquals(22, UintCore.getSignificantBitCount(cm[0].value()));
        Assert.assertEquals(22, UintCore.getSignificantBitCount(cm[1].value()));

        Assert.assertEquals(3133441L, cm[0].value());
        Assert.assertEquals(3655681L, cm[1].value());
    }
}
