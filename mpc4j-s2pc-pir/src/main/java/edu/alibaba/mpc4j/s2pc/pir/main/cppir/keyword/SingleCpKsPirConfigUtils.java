package edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai.PaiSingleCpCksPirConfig;

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
    public static SingleCpKsPirConfig createKeywordCpPirConfig(Properties properties) {
        // read protocol type
        String singleCpPirTypeString = PropertiesUtils.readString(properties, "pto_name");
        SingleCpKsPirType singleCpKsPirType = SingleCpKsPirType.valueOf(singleCpPirTypeString);
        switch (singleCpKsPirType) {
            case ALPR21_PIANO:
                return new Alpr21SingleCpKsPirConfig.Builder()
                    .setSingleIndexCpPirConfig(new PianoSingleCpPirConfig.Builder().build())
                    .build();
            case ALPR21_SPAM:
                return new Alpr21SingleCpKsPirConfig.Builder()
                    .setSingleIndexCpPirConfig(new SpamSingleCpPirConfig.Builder().build())
                    .build();
            case ALPR21_SIMPLE:
                return new Alpr21SingleCpKsPirConfig.Builder()
                    .setSingleIndexCpPirConfig(new SimpleSingleCpPirConfig.Builder().build())
                    .build();
            case ALPR21_PAI:
                return new Alpr21SingleCpKsPirConfig.Builder()
                    .setSingleIndexCpPirConfig(new PaiSingleCpPirConfig.Builder().build())
                    .build();
            case PAI_CKS:
                return new PaiSingleCpCksPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleCpKsPirType.class.getSimpleName() + ": " + singleCpKsPirType.name()
                );
        }
    }
}