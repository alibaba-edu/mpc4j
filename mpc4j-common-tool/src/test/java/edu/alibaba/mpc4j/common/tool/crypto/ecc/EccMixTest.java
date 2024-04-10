package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory.EccType;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * test mix ECC usage.
 *
 * @author Weiran Liu
 * @date 2024/4/1
 */
public class EccMixTest {

    @Test
    public void testMixEcc() {
        SecureRandom secureRandom = new SecureRandom();
        // create all types of ECC and try to do some computation.
        Ecc[] eccs = Arrays.stream(EccType.values())
            .map(EccFactory::createInstance)
            .toArray(Ecc[]::new);
        ByteMulEcc[] byteMulEccs = Arrays.stream(ByteEccType.values())
            .filter(ByteEccFactory::isByteMulEcc)
            .map(ByteEccFactory::createMulInstance)
            .toArray(ByteMulEcc[]::new);
        Arrays.stream(eccs).forEach(ecc -> {
            ECPoint h = ecc.randomPoint(secureRandom);
            BigInteger r = ecc.randomZn(secureRandom);
            ecc.multiply(h, r);
        });
        Arrays.stream(byteMulEccs).forEach(byteMulEcc -> {
            byte[] h = byteMulEcc.randomPoint(secureRandom);
            byte[] r = byteMulEcc.randomScalar(secureRandom);
            byteMulEcc.mul(h, r);
        });
    }
}
