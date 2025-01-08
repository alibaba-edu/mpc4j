package edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory.CpKsPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet.ChalametCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimplePgmCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirConfig;
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
        return switch (cpKsPirType) {
            case ALPR21 -> {
                CpIdxPirType cpIdxPirType = MainPtoConfigUtils.readEnum(
                    CpIdxPirType.class, properties, CpIdxPirMain.PTO_NAME_KEY
                );
                yield switch (cpIdxPirType) {
                    case PAI -> new Alpr21CpKsPirConfig.Builder()
                        .setCpIdxPirConfig(new PaiCpIdxPirConfig.Builder().build())
                        .build();
                    case MIR -> new Alpr21CpKsPirConfig.Builder()
                        .setCpIdxPirConfig(new MirCpIdxPirConfig.Builder().build())
                        .build();
                    case PIANO -> new Alpr21CpKsPirConfig.Builder()
                        .setCpIdxPirConfig(new PianoCpIdxPirConfig.Builder().build())
                        .build();
                    case SIMPLE -> new Alpr21CpKsPirConfig.Builder()
                        .setCpIdxPirConfig(new SimpleCpIdxPirConfig.Builder().build())
                        .build();
                    default -> throw new IllegalArgumentException(
                        "Invalid " + CpIdxPirType.class.getSimpleName() + ": " + cpIdxPirType.name()
                    );
                };
            }
            case PAI_CKS -> new PaiCpCksPirConfig.Builder().build();
            case SIMPLE_NAIVE -> new SimpleNaiveCpKsPirConfig.Builder().build();
            case PGM_INDEX -> new SimplePgmCpKsPirConfig.Builder().build();
            case SIMPLE_BIN -> new SimpleBinCpKsPirConfig.Builder().build();
            case CHALAMET -> new ChalametCpKsPirConfig.Builder().build();
        };
    }
}