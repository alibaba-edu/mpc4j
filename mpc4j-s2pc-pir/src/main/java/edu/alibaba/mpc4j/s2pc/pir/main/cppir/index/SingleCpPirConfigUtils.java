package edu.alibaba.mpc4j.s2pc.pir.main.cppir.index;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirConfig;

import java.util.Properties;

/**
 * client-specific preprocessing PIR config utilities.
 *
 * @author Liqiang Peng
 * @date 2023/9/26
 */
class SingleCpPirConfigUtils {
    /**
     * private constructor.
     */
    private SingleCpPirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static SingleCpPirConfig createIndexCpPirConfig(Properties properties) {
        // read protocol type
        String cpPirTypeString = PropertiesUtils.readString(properties, "pto_name");
        SingleCpPirType singleCpPirType = SingleCpPirType.valueOf(cpPirTypeString);
        switch (singleCpPirType) {
            case PIANO:
                return new PianoSingleCpPirConfig.Builder().build();
            case SPAM:
                return new SpamSingleCpPirConfig.Builder().build();
            case SIMPLE:
                return new SimpleSingleCpPirConfig.Builder().build();
            case PAI:
                return new PaiSingleCpPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleCpPirType.class.getSimpleName() + ": " + singleCpPirType.name()
                );
        }
    }
}