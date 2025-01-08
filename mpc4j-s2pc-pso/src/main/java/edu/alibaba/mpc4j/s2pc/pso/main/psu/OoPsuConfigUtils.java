package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.OoPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuConfig;

import java.util.Properties;

/**
 * offline-online PSU协议配置项工具类
 *
 * @author Feng Han
 * @date 2024/12/9
 */
public class OoPsuConfigUtils {
    /**
     * CP_INX_PIR_TYPE name
     */
    public final static String ROSN_TYPE = "rosn_type";

    /**
     * private constructor.
     */
    private OoPsuConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static OoPsuConfig createConfig(Properties properties) {
        PsuType psuType = MainPtoConfigUtils.readEnum(PsuType.class, properties, OoPsuMain.PTO_NAME_KEY);
        switch (psuType) {
            case GMR21:
                return generateGmr21PsuConfig(properties);
            case JSZ22_SFC:
                return createJsz22SfcPsuConfig(properties);
            case JSZ22_SFS:
                return createJsz22SfsPsuConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PsuType.class.getSimpleName() + ": " + psuType.name());
        }
    }

    private static Gmr21PsuConfig generateGmr21PsuConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        RosnType rosnType = MainPtoConfigUtils.readEnum(RosnType.class, properties, ROSN_TYPE);
        RosnConfig rosnConfig = RosnFactory.createRosnConfig(rosnType, silent);
        Gmr21MqRpmtConfig gmr21MqRpmtConfig = new Gmr21MqRpmtConfig.Builder(silent)
            .setOkvsType(Gf2eDokvsType.MEGA_BIN)
            .setRosnConfig(rosnConfig)
            .build();
        return new Gmr21PsuConfig.Builder(silent)
            .setGmr21MqRpmtConfig(gmr21MqRpmtConfig)
            .build();
    }

    private static Jsz22SfcPsuConfig createJsz22SfcPsuConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        RosnType rosnType = MainPtoConfigUtils.readEnum(RosnType.class, properties, ROSN_TYPE);
        RosnConfig rosnConfig = RosnFactory.createRosnConfig(rosnType, silent);
        return new Jsz22SfcPsuConfig.Builder(silent).setRosnConfig(rosnConfig).build();
    }

    private static Jsz22SfsPsuConfig createJsz22SfsPsuConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        RosnType rosnType = MainPtoConfigUtils.readEnum(RosnType.class, properties, ROSN_TYPE);
        RosnConfig rosnConfig = RosnFactory.createRosnConfig(rosnType, silent);
        return new Jsz22SfsPsuConfig.Builder(silent).setRosnConfig(rosnConfig).build();
    }
}
