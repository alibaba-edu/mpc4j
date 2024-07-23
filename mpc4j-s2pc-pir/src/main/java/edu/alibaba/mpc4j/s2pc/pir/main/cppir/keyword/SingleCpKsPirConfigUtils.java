package edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory.CpKsPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.index.CpIdxPirMain;

import java.util.Properties;

/**
 * client-specific preprocessing KSPIR config utilities.
 *
 * @author Liqiang Peng
 * @date 2023/9/27
 */
class SingleCpKsPirConfigUtils {
    /**
     * private constructor.
     */
    private SingleCpKsPirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static CpKsPirConfig createConfig(Properties properties) {
        CpKsPirType cpKsPirType = MainPtoConfigUtils.readEnum(
            CpKsPirType.class, properties, SingleCpKsPirMain.PTO_NAME_KEY
        );
        switch (cpKsPirType) {
            case ALPR21:
                CpIdxPirType cpIdxPirType = MainPtoConfigUtils.readEnum(
                    CpIdxPirType.class, properties, CpIdxPirMain.PTO_NAME_KEY
                );
                switch (cpIdxPirType) {
                    case PAI:
                        return new Alpr21CpKsPirConfig.Builder()
                            .setCpIdxPirConfig(new PaiCpIdxPirConfig.Builder().build())
                            .build();
                    case SPAM:
                        return new Alpr21CpKsPirConfig.Builder()
                            .setCpIdxPirConfig(new SpamCpIdxPirConfig.Builder().build())
                            .build();
                    case PIANO:
                        return new Alpr21CpKsPirConfig.Builder()
                            .setCpIdxPirConfig(new PianoCpIdxPirConfig.Builder().build())
                            .build();
                    case SIMPLE:
                        return new Alpr21CpKsPirConfig.Builder()
                            .setCpIdxPirConfig(new SimpleCpIdxPirConfig.Builder().build())
                            .build();
                    default:
                        throw new IllegalArgumentException(
                            "Invalid " + CpIdxPirType.class.getSimpleName() + ": " + cpIdxPirType.name()
                        );
                }
            case PAI_CKS:
                return new PaiCpCksPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + CpKsPirType.class.getSimpleName() + ": " + cpKsPirType.name()
                );
        }
    }
}