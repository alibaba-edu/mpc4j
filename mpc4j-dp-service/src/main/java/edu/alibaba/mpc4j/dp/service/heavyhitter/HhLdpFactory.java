package edu.alibaba.mpc4j.dp.service.heavyhitter;

import edu.alibaba.mpc4j.dp.service.heavyhitter.config.FoHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.FoHhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.FoHhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.hg.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
         * frequency oracle
         */
        FO,
        /**
         * basic HeavyGuardian
         */
        BASIC,
        /**
         * Advanced HeavyGuardian
         */
        ADV,
        /**
         * Related HeavyGuardian
         */
        RELAX,
    }

    /**
     * Creates an default config.
     *
     * @param type the type.
     * @param domainSet the domain set.
     * @param k the k.
     * @param windowEpsilon the window epsilon.
     * @return an default config.
     */
    public static HhLdpConfig createDefaultConfig(HhLdpType type, Set<String> domainSet, int k, double windowEpsilon) {
        switch (type) {
            case FO:
                return new FoHhLdpConfig.Builder(type, domainSet, k, windowEpsilon).build();
            case BASIC:
            case ADV:
            case RELAX:
                return new HgHhLdpConfig.Builder(type, domainSet, k, windowEpsilon).build();
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
                return new FoHhLdpServer(config);
            case BASIC:
                return new BasicHgHhLdpServer(config);
            case ADV:
                return new AdvHhgHhLdpServer(config);
            case RELAX:
                return new RelaxHhgHhLdpServer(config);
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
                return new FoHhLdpClient(config);
            case BASIC:
                return new BasicHgHhLdpClient(config);
            case ADV:
                return new AdvHhgHhLdpClient(config);
            case RELAX:
                return new RelaxHhgHhLdpClient(config);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }
}
