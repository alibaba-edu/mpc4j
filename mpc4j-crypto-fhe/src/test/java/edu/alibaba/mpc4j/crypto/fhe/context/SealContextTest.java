package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;

import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * SEALContext unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/context.cpp.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/12
 */
public class SealContextTest {

    @Test
    public void testBfvContextConstructor() {
        // Nothing set
        SchemeType scheme = SchemeType.BFV;
        EncryptionParameters parms = new EncryptionParameters(scheme);
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(qualifiers.parameterError, ErrorType.INVALID_COEFF_MODULUS_SIZE);
            Assert.assertFalse(qualifiers.usingFft);
            Assert.assertFalse(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Not relatively prime coeff moduli
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{2, 30});
        parms.setPlainModulus(2);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.FAILED_CREATING_RNS_BASE, qualifiers.parameterError);
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertFalse(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Plain modulus not relatively prime to coeff moduli
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(34);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_PLAIN_MODULUS_CO_PRIMALITY, qualifiers.parameterError);
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Plain modulus not smaller than product of coeff moduli
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17});
        parms.setPlainModulus(41);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            Assert.assertEquals(17, context.firstContextData().totalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_PLAIN_MODULUS_TOO_LARGE, qualifiers.parameterError);
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // FFT poly but not NTT modulus, 3 mod 2 * 4 != 1
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{3});
        parms.setPlainModulus(2);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            Assert.assertEquals(3, context.firstContextData().totalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_COEFF_MODULUS_NO_NTT, qualifiers.parameterError);
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertFalse(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Parameters OK; no fast plain lift
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(18);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            Assert.assertEquals(697L, context.firstContextData().totalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Parameters OK; fast plain lift, plain modulus less than all the coeff moduli
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(16);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            EncryptionParameterQualifiers keyQualifiers = context.keyContextData().qualifiers();
            Assert.assertEquals(17L, context.firstContextData().totalCoeffModulus()[0]);
            Assert.assertEquals(697L, context.keyContextData().totalCoeffModulus()[0]);
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertFalse(keyQualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertTrue(context.usingKeySwitching());
        }

        // Parameters OK; no batching due to non-prime plain modulus
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(49);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            Assert.assertEquals(697L, context.keyContextData().totalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Parameters OK; batching enabled
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            Assert.assertEquals(697L, context.keyContextData().totalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertTrue(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Parameters OK; batching and fast plain lift enabled
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{137, 193});
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            Assert.assertEquals(137L, context.firstContextData().totalCoeffModulus()[0]);
            Assert.assertEquals(26441L, context.keyContextData().totalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            EncryptionParameterQualifiers keyQualifiers = context.keyContextData().qualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertTrue(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertFalse(keyQualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertTrue(context.usingKeySwitching());
        }

        // Parameters OK; batching and fast plain lift enabled; nullptr RNG
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{137, 193});
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(null);
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            Assert.assertEquals(137L, context.firstContextData().totalCoeffModulus()[0]);
            Assert.assertEquals(26441L, context.keyContextData().totalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            EncryptionParameterQualifiers keyQualifiers = context.keyContextData().qualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertTrue(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertFalse(keyQualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertTrue(context.usingKeySwitching());
        }

        // Parameters not OK due to too small poly_modulus_degree and enforce_hes
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{137, 193});
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(null);
        {
            SealContext context = new SealContext(parms, false, SecLevelType.TC128);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_PARAMETERS_INSECURE, qualifiers.parameterError);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Parameters not OK due to too large coeff_modulus and enforce_hes
        parms.setPolyModulusDegree(2048);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(4096, SecLevelType.TC128));
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(null);
        {
            SealContext context = new SealContext(parms, false, SecLevelType.TC128);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_PARAMETERS_INSECURE, qualifiers.parameterError);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }

        // Parameters OK; descending modulus chain
        parms.setPolyModulusDegree(4096);
        parms.setCoeffModulus(new long[]{0xffffee001L, 0xffffc4001L});
        parms.setPlainModulus(73);
        {
            SealContext context = new SealContext(parms, false, SecLevelType.TC128);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertTrue(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.TC128, qualifiers.securityLevel);
            Assert.assertTrue(context.usingKeySwitching());
        }

        // Parameters OK; no standard security
        parms.setPolyModulusDegree(4096);
        parms.setCoeffModulus(new long[]{0x1ffffe0001L, 0xffffee001L, 0xffffc4001L});
        parms.setPlainModulus(73);
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            EncryptionParameterQualifiers keyQualifiers = context.keyContextData().qualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertTrue(keyQualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertTrue(context.usingKeySwitching());
        }

        // Parameters OK; using batching; no keyswitching
        parms.setPolyModulusDegree(2048);
        parms.setCoeffModulus(CoeffModulus.create(2048, new int[]{40}));
        parms.setPlainModulus(65537);
        {
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertTrue(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertTrue(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.usingKeySwitching());
        }
    }

    @Test
    public void testModulusChainExpansion() {
        {
            SchemeType scheme = SchemeType.BFV;
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(4);
            parms.setCoeffModulus(new long[]{41, 137, 193, 65537});
            parms.setPlainModulus(73);

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ContextData contextData = context.keyContextData();
            Assert.assertEquals(2, contextData.chainIndex());
            Assert.assertEquals(71047416497L, contextData.totalCoeffModulus()[0]);
            Assert.assertNull(contextData.prevContextData());
            Assert.assertEquals(contextData.parmsId(), context.keyParmsId());

            ContextData prevContextData = contextData;
            contextData = contextData.nextContextData();
            Assert.assertEquals(1, contextData.chainIndex());
            Assert.assertEquals(1084081L, contextData.totalCoeffModulus()[0]);
            Assert.assertEquals(contextData.prevContextData().parmsId(), prevContextData.parmsId());

            prevContextData = contextData;
            contextData = contextData.nextContextData();
            Assert.assertEquals(0, contextData.chainIndex());
            Assert.assertEquals(5617L, contextData.totalCoeffModulus()[0]);
            Assert.assertEquals(contextData.prevContextData().parmsId(), prevContextData.parmsId());

            Assert.assertNull(contextData.nextContextData());
            Assert.assertEquals(contextData.parmsId(), context.lastParmsId());

            context = new SealContext(parms, false, SecLevelType.NONE);
            Assert.assertEquals(1, context.keyContextData().chainIndex());
            Assert.assertEquals(0, context.firstContextData().chainIndex());
            Assert.assertEquals(71047416497L, context.keyContextData().totalCoeffModulus()[0]);
            Assert.assertEquals(1084081L, context.firstContextData().totalCoeffModulus()[0]);

            Assert.assertNull(context.firstContextData().nextContextData());
            Assert.assertNotNull(context.firstContextData().prevContextData());
        }

        // TODO: test BGV

        // TODO: test CKKS
    }

    @Test
    public void testBfvParameterError() {
        SchemeType scheme = SchemeType.BFV;
        EncryptionParameters parms = new EncryptionParameters(scheme);
        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();

        qualifiers.parameterError = ErrorType.NONE;
        Assert.assertEquals(qualifiers.parameterErrorName(), "none");
        Assert.assertEquals(qualifiers.parameterErrorMessage(), "constructed but not yet validated");

        qualifiers.parameterError = ErrorType.SUCCESS;
        Assert.assertEquals(qualifiers.parameterErrorName(), "success");
        Assert.assertEquals(qualifiers.parameterErrorMessage(), "valid");

        qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_BIT_COUNT;
        Assert.assertEquals(qualifiers.parameterErrorName(), "invalid coeff modulus bit count");
        Assert.assertEquals(qualifiers.parameterErrorMessage(),  "coeffModulus's primes' bit counts are not bounded by USER_MOD_BIT_COUNT_MIN(MAX)");

        parms.setPolyModulusDegree(127);
        parms.setCoeffModulus(new long[] {17, 73});
        parms.setPlainModulus(41);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());

        context = new SealContext(parms, false, SecLevelType.NONE);
        Assert.assertFalse(context.isParametersSet());
        Assert.assertEquals(context.parametersErrorName(), "invalid poly modulus degree non power of two");
        Assert.assertEquals(context.parametersErrorMessage(), "polyModulusDegree is not a power of two");
    }

    // TODO: testBGVContextConstructor

    // TODO: testGBCParameterError
}
