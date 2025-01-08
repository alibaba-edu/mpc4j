package edu.alibaba.mpc4j.s2pc.pir.main.cppir.index;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo.FrodoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.MirPlinkoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.PianoPlinkoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.DoubleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirConfig;

import java.util.Properties;

/**
 * client-specific preprocessing PIR config utilities.
 *
 * @author Liqiang Peng
 * @date 2023/9/26
 */
class CpIdxPirConfigUtils {
    /**
     * private constructor.
     */
    private CpIdxPirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static CpIdxPirConfig createConfig(Properties properties) {
        CpIdxPirType cpIdxPirType = MainPtoConfigUtils.readEnum(CpIdxPirType.class, properties, CpIdxPirMain.PTO_NAME_KEY);
        switch (cpIdxPirType) {
            case PIANO -> {
                return new PianoCpIdxPirConfig.Builder().build();
            }
            case MIR -> {
                return new MirCpIdxPirConfig.Builder().build();
            }
            case SIMPLE -> {
                return new SimpleCpIdxPirConfig.Builder().build();
            }
            case PAI -> {
                return new PaiCpIdxPirConfig.Builder().build();
            }
            case DOUBLE -> {
                return new DoubleCpIdxPirConfig.Builder().build();
            }
            case FRODO -> {
                return new FrodoCpIdxPirConfig.Builder().build();
            }
            case PIANO_PLINKO -> {
                return new PianoPlinkoCpIdxPirConfig.Builder().build();
            }
            case MIR_PLINKO -> {
                return new MirPlinkoCpIdxPirConfig.Builder().build();
            }
            default -> throw new IllegalArgumentException("Invalid " + CpIdxPirType.class.getSimpleName() + ": " + cpIdxPirType.name());
        }
    }
}