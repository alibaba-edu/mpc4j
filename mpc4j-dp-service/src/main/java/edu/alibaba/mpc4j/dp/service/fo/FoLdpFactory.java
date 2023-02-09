package edu.alibaba.mpc4j.dp.service.fo;

import edu.alibaba.mpc4j.dp.service.fo.cms.AppleCmsFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.cms.AppleCmsFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.cms.AppleHcmsFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.cms.AppleHcmsFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.*;
import edu.alibaba.mpc4j.dp.service.fo.de.DeIndexFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.de.DeIndexFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.de.DeStringFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.de.DeStringFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.hadamard.*;
import edu.alibaba.mpc4j.dp.service.fo.lh.*;
import edu.alibaba.mpc4j.dp.service.fo.rappor.RapporFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.rappor.RapporFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.ue.OueFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.ue.OueFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.ue.SueFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.ue.SueFoLdpServer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * LDP frequency oracle Factory.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class FoLdpFactory {
    /**
     * the default charset
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private FoLdpFactory() {
        // empty
    }

    public enum FoLdpType {
        /**
         * direct encoding with items encoded via string.
         */
        DE_STRING,
        /**
         * direct encoding with items encoded via index.
         */
        DE_INDEX,
        /**
         * Symmetric Unary Encoding, also known as basic RAPPOR.
         */
        SUE,
        /**
         * Optimized Unary Encoding
         */
        OUE,
        /**
         * RAPPOR
         */
        RAPPOR,
        /**
         * Binary Local Hash
         */
        BLH,
        /**
         * Optimal Local Hash
         */
        OLH,
        /**
         * Fast Local Hash
         */
        FLH,
        /**
         * Hadamard Response
         */
        HR,
        /**
         * Hadamard Response with high privacy parameter ε
         */
        HR_HIGH_EPSILON,
        /**
         * Hadamard Mechanism
         */
        HM,
        /**
         * Hadamard Mechanism with low privacy parameter ε
         */
        HM_LOW_EPSILON,
        /**
         * Apple's Count Mean Sketch (CMS)
         */
        APPLE_CMS,
        /**
         * Apple's Hadamard Count Mean Sketch (HCMS)
         */
        APPLE_HCMS,
    }


    /**
     * Returns whether the mechanism obtain accurate estimation for extremely large ε.
     *
     * @param type the type.
     * @return whether the mechanism obtain accurate estimation for extremely large ε.
     */
    public static boolean isConverge(FoLdpType type) {
        switch (type) {
            case DE_INDEX:
            case DE_STRING:
            case SUE:
            case HR_HIGH_EPSILON:
                return true;
            case OUE:
            case RAPPOR:
            case BLH:
            case OLH:
            case FLH:
            case HR:
            case HM:
            case HM_LOW_EPSILON:
            case APPLE_CMS:
            case APPLE_HCMS:
                return false;
            default:
                throw new IllegalArgumentException("Invalid " + FoLdpFactory.FoLdpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets the maximal supported ε.
     *
     * @param type the type.
     * @return the maximal supported ε.
     */
    public static double getMaximalEpsilon(FoLdpType type) {
        switch (type) {
            case DE_INDEX:
            case DE_STRING:
            case SUE:
            case HR_HIGH_EPSILON:
            case OUE:
            case RAPPOR:
            case BLH:
            case HR:
            case HM:
            case HM_LOW_EPSILON:
            case APPLE_CMS:
            case APPLE_HCMS:
                // although many schemes support much larger ε, we manually restrict a maximal one.
                return 128;
            case OLH:
                return OlhFoLdpConfig.MAX_EPSILON;
            case FLH:
                return FlhFoLdpConfig.MAX_EPSILON;
            default:
                throw new IllegalArgumentException("Invalid " + FoLdpFactory.FoLdpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates an default config.
     *
     * @param type      the type.
     * @param domainSet the domain set.
     * @param epsilon   the epsilon.
     * @return an default config.
     */
    public static FoLdpConfig createDefaultConfig(FoLdpType type, Set<String> domainSet, double epsilon) {
        switch (type) {
            case DE_STRING:
            case DE_INDEX:
            case SUE:
            case OUE:
            case BLH:
            case HR:
            case HR_HIGH_EPSILON:
            case HM:
            case HM_LOW_EPSILON:
                return new BasicFoLdpConfig.Builder(type, domainSet, epsilon).build();
            case OLH:
                return new OlhFoLdpConfig.Builder(type, domainSet, epsilon).build();
            case FLH:
                return new FlhFoLdpConfig.Builder(type, domainSet, epsilon).build();
            case RAPPOR:
                return new RapporFoLdpConfig.Builder(type, domainSet, epsilon).build();
            case APPLE_CMS:
                return new AppleCmsFoLdpConfig.Builder(type, domainSet, epsilon).build();
            case APPLE_HCMS:
                return new AppleHcmsFoLdpConfig.Builder(type, domainSet, epsilon).build();
            default:
                throw new IllegalArgumentException("Invalid " + FoLdpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a Frequency Oracle LDP server.
     *
     * @param config the config.
     * @return a Frequency Oracle LDP server.
     */
    public static FoLdpServer createServer(FoLdpConfig config) {
        FoLdpType type = config.getType();
        switch (type) {
            case DE_STRING:
                return new DeStringFoLdpServer(config);
            case DE_INDEX:
                return new DeIndexFoLdpServer(config);
            case SUE:
                return new SueFoLdpServer(config);
            case OUE:
                return new OueFoLdpServer(config);
            case RAPPOR:
                return new RapporFoLdpServer(config);
            case BLH:
                return new BlhFoLdpServer(config);
            case OLH:
                return new OlhFoLdpServer(config);
            case FLH:
                return new FlhFoLdpServer(config);
            case HR:
                return new HrFoLdpServer(config);
            case HR_HIGH_EPSILON:
                return new HrHighEpsFoLdpServer(config);
            case HM:
                return new HmFoLdpServer(config);
            case HM_LOW_EPSILON:
                return new HmLowEpsFoLdpServer(config);
            case APPLE_CMS:
                return new AppleCmsFoLdpServer(config);
            case APPLE_HCMS:
                return new AppleHcmsFoLdpServer(config);
            default:
                throw new IllegalArgumentException("Invalid " + FoLdpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a Frequency Oracle LDP client.
     *
     * @param config the config.
     * @return a Frequency Oracle LDP client.
     */
    public static FoLdpClient createClient(FoLdpConfig config) {
        FoLdpType type = config.getType();
        switch (type) {
            case DE_STRING:
                return new DeStringFoLdpClient(config);
            case DE_INDEX:
                return new DeIndexFoLdpClient(config);
            case SUE:
                return new SueFoLdpClient(config);
            case OUE:
                return new OueFoLdpClient(config);
            case RAPPOR:
                return new RapporFoLdpClient(config);
            case BLH:
                return new BlhFoLdpClient(config);
            case OLH:
                return new OlhFoLdpClient(config);
            case FLH:
                return new FlhFoLdpClient(config);
            case HR:
                return new HrFoLdpClient(config);
            case HR_HIGH_EPSILON:
                return new HrHighEpsFoLdpClient(config);
            case HM:
                return new HmFoLdpClient(config);
            case HM_LOW_EPSILON:
                return new HmLowEpsFoLdpClient(config);
            case APPLE_CMS:
                return new AppleCmsFoLdpClient(config);
            case APPLE_HCMS:
                return new AppleHcmsFoLdpClient(config);
            default:
                throw new IllegalArgumentException("Invalid " + FoLdpType.class.getSimpleName() + ": " + type.name());
        }
    }
}
