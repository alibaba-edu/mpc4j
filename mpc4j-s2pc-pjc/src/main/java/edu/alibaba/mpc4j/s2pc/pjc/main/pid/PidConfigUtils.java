package edu.alibaba.mpc4j.s2pc.pjc.main.pid;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory.PidType;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20ByteEccPidConfig;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuConfigUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20EccPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21MpPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21SloppyPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.czz24.Czz24PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;

import java.util.Properties;

/**
 * PID协议配置项工具类。
 *
 * @author Weiran Liu
 * @date 2022/5/16
 */
public class PidConfigUtils {
    /**
     * private constructor.
     */
    private PidConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PidConfig createConfig(Properties properties) {
        PidType pidType = MainPtoConfigUtils.readEnum(PidType.class, properties, PidMain.PTO_NAME_KEY);
        switch (pidType) {
            case BKMS20_BYTE_ECC:
                return createBkms20EccPidConfig();
            case BKMS20_ECC:
                return createBkms20PidConfig(properties);
            case GMR21_MP:
                return createGmr21MpPidConfig(properties);
            case GMR21_SLOPPY:
                return createGmr21SloppyPidConfig(properties);
            case CZZ24:
                return createCzz24PidConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PidFactory.PidType.class.getSimpleName() + ":" + pidType);
        }
    }

    private static Bkms20ByteEccPidConfig createBkms20EccPidConfig() {
        return new Bkms20ByteEccPidConfig.Builder().build();
    }

    private static Bkms20EccPidConfig createBkms20PidConfig(Properties properties) {
        boolean compressEncode = MainPtoConfigUtils.readCompressEncode(properties);
        return new Bkms20EccPidConfig.Builder()
            .setCompressEncode(compressEncode)
            .build();
    }

    private static Gmr21MpPidConfig createGmr21MpPidConfig(Properties properties) {
        PsuConfig psuConfig = PsuConfigUtils.createConfig(properties);
        return new Gmr21MpPidConfig.Builder()
            .setMpOprfConfig(OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST))
            .setPsuConfig(psuConfig)
            .build();
    }

    private static Gmr21SloppyPidConfig createGmr21SloppyPidConfig(Properties properties) {
        PsuConfig psuConfig = PsuConfigUtils.createConfig(properties);
        return new Gmr21SloppyPidConfig.Builder()
            .setSloppyOkvsType(Gf2eDokvsType.MEGA_BIN)
            .setPsuConfig(psuConfig)
            .build();
    }

    private static Czz24PidConfig createCzz24PidConfig(Properties properties) {
        PsuConfig psuConfig = PsuConfigUtils.createConfig(properties);
        return new Czz24PidConfig.Builder()
            .setSloppyOkvsType(Gf2eDokvsType.MEGA_BIN)
            .setPsuConfig(psuConfig)
            .build();
    }
}
