package edu.alibaba.mpc4j.dp.service.heavyhitter;

import edu.alibaba.mpc4j.dp.service.heavyhitter.config.*;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.FoHhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.FoHhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.hg.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Set;

/**
 * Heavy Hitter LDP Factory.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class HhLdpFactory {
    /**
     * the empty item prefix ⊥
     */
    public static final String BOT_PREFIX = "⊥_";
    /**
     * the default charset
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private HhLdpFactory() {
        // empty
    }

    public enum HhLdpType {
        /**
         * Frequency Oracle
         */
        FO,
        /**
         * Basic General Randomization
         */
        BGR,
        /**
         * Domain-Shrinkage Randomization
         */
        DSR,
        /**
         * Budget-Division Randomization
         */
        BDR,
        /**
         * BCold-Nomination Randomization
         */
        CNR,
    }

    /**
     * Creates an default config.
     *
     * @param type          the type.
     * @param domainSet     the domain set.
     * @param k             the k.
     * @param windowEpsilon the window epsilon.
     * @param windowSize    the window size (w).
     * @return an default config.
     */
    public static HhLdpConfig createDefaultHhLdpConfig(HhLdpType type, Set<String> domainSet,
                                                       int k, double windowEpsilon, int windowSize) {
        switch (type) {
            case FO:
                return new FoHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            case BGR:
                return new BgrHgHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            case DSR:
                return new DsrHgHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            case BDR:
                return new BdrHhgHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            case CNR:
                return new CnrHhgHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates an default config.
     *
     * @param type          the type.
     * @param domainSet     the domain set.
     * @param k             the k.
     * @param windowEpsilon the window epsilon.
     * @param windowSize    the window size (w).
     * @param w the bucket size.
     * @param lambdaH  λ_h, i.e., the cell num in each bucket.
     * @param hgRandom the randomness used in the HeavyGuardian.
     * @return an default config.
     */
    public static HgHhLdpConfig createDefaultHgHhLdpConfig(HhLdpType type, Set<String> domainSet,
                                                         int k, double windowEpsilon, int windowSize,
                                                         int w, int lambdaH, Random hgRandom) {
        switch (type) {
            case BGR:
                return new BgrHgHhLdpConfig
                    .Builder(domainSet, k, windowEpsilon, windowSize)
                    .setBucketParams(w, lambdaH)
                    .setHgRandom(hgRandom)
                    .build();
            case DSR:
                return new DsrHgHhLdpConfig
                    .Builder(domainSet, k, windowEpsilon, windowSize)
                    .setBucketParams(w, lambdaH)
                    .setHgRandom(hgRandom)
                    .build();
            case BDR:
                return new BdrHhgHhLdpConfig
                    .Builder(domainSet, k, windowEpsilon, windowSize)
                    .setBucketParams(w, lambdaH)
                    .setHgRandom(hgRandom)
                    .build();
            case CNR:
                return new CnrHhgHhLdpConfig
                    .Builder(domainSet, k, windowEpsilon, windowSize)
                    .setBucketParams(w, lambdaH)
                    .setHgRandom(hgRandom)
                    .build();
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Heavy Hitter LDP server.
     *
     * @param config the config.
     * @return an instance of Heavy Hitter LDP server.
     */
    public static HhLdpServer createServer(HhLdpConfig config) {
        HhLdpType type = config.getType();
        switch (type) {
            case FO:
                return new FoHhLdpServer((FoHhLdpConfig) config);
            case BGR:
                return new BgrHgHhLdpServer((BgrHgHhLdpConfig) config);
            case DSR:
                return new DsrHgHhLdpServer((DsrHgHhLdpConfig) config);
            case BDR:
                return new BdrHhgHhLdpServer((BdrHhgHhLdpConfig) config);
            case CNR:
                return new CnrHhgHhLdpServer((CnrHhgHhLdpConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Heavy Hitter LDP client.
     *
     * @param config the config.
     * @return an instance of Heavy Hitter LDP client.
     */
    public static HhLdpClient createClient(HhLdpConfig config) {
        HhLdpType type = config.getType();
        switch (type) {
            case FO:
                return new FoHhLdpClient((FoHhLdpConfig) config);
            case BGR:
                return new BgrHgHhLdpClient((BgrHgHhLdpConfig) config);
            case DSR:
                return new DsrHgHhLdpClient((DsrHgHhLdpConfig) config);
            case BDR:
                return new BdrHhgHhLdpClient((BdrHhgHhLdpConfig) config);
            case CNR:
                return new CnrHhgHhLdpClient((CnrHhgHhLdpConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }
}
