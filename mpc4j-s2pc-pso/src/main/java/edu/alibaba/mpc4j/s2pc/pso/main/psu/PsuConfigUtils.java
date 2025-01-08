package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import edu.alibaba.mpc4j.s2pc.pso.psu.czz24.Czz24CwOprfPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23PkePsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23SkePsuConfig;

import java.util.Properties;

/**
 * PSU协议配置项工具类。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class PsuConfigUtils {
    /**
     * CP_INX_PIR_TYPE name
     */
    public final static String ROSN_TYPE = "rosn_type";
    /**
     * private constructor.
     */
    private PsuConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PsuConfig createConfig(Properties properties) {
        PsuType psuType = MainPtoConfigUtils.readEnum(PsuType.class, properties, PsuMain.PTO_NAME_KEY);
        switch (psuType) {
            case KRTW19:
                return createKrtw19PsuConfig();
            case GMR21:
                return generateGmr21PsuConfig(properties);
            case ZCL23_PKE:
                return createZcl23PkePsuConfig(properties);
            case ZCL23_SKE:
                return createZcl23SkePsuConfig();
            case JSZ22_SFC:
                return createJsz22SfcPsuConfig(properties);
            case JSZ22_SFS:
                return createJsz22SfsPsuConfig(properties);
            case CZZ24_CW_OPRF:
                return createCzz24CwOprfPsuConfig();
            default:
                throw new IllegalArgumentException("Invalid " + PsuType.class.getSimpleName() + ": " + psuType.name());
        }
    }

    private static Krtw19PsuConfig createKrtw19PsuConfig() {
        return new Krtw19PsuConfig.Builder().build();
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

    private static Zcl23SkePsuConfig createZcl23SkePsuConfig() {
        return new Zcl23SkePsuConfig.Builder(SecurityModel.SEMI_HONEST, true).build();
    }

    private static Zcl23PkePsuConfig createZcl23PkePsuConfig(Properties properties) {
        boolean compressEncode = MainPtoConfigUtils.readCompressEncode(properties);
        Zcl23PkeMqRpmtConfig zcl23PkeMqRpmtConfig = new Zcl23PkeMqRpmtConfig.Builder()
            .setCompressEncode(compressEncode)
            .build();
        return new Zcl23PkePsuConfig.Builder()
            .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .setZcl23PkeMqRpmtConfig(zcl23PkeMqRpmtConfig)
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

    private static Czz24CwOprfPsuConfig createCzz24CwOprfPsuConfig() {
        return new Czz24CwOprfPsuConfig.Builder().build();
    }
}
