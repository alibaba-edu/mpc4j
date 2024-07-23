package edu.alibaba.mpc4j.s2pc.pjc.main.pmid;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuConfigUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidFactory.PmidType;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.Zcl22MpPmidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.Zcl22SloppyPmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;

import java.util.Properties;

/**
 * PMID协议配置项工具类。
 *
 * @author Weiran Liu
 * @date 2022/5/17
 */
public class PmidConfigUtils {

    private PmidConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PmidConfig createConfig(Properties properties) {
        PmidType pmidType = MainPtoConfigUtils.readEnum(PmidType.class, properties, PmidMain.PTO_NAME_KEY);
        switch (pmidType) {
            case ZCL22_MP:
                return createZcl22MpPmidConfig(properties);
            case ZCL22_SLOPPY:
                return createZcl22SloppyPmidConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PmidType.class.getSimpleName() + ": " + pmidType);
        }
    }

    private static Zcl22MpPmidConfig createZcl22MpPmidConfig(Properties properties) {
        PsuConfig psuConfig = PsuConfigUtils.createConfig(properties);
        return new Zcl22MpPmidConfig.Builder()
            .setMpOprfConfig(OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST))
            .setSigmaOkvsType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT)
            .setPsuConfig(psuConfig)
            .build();
    }

    private static Zcl22SloppyPmidConfig createZcl22SloppyPmidConfig(Properties properties) {
        PsuConfig psuConfig = PsuConfigUtils.createConfig(properties);
        return new Zcl22SloppyPmidConfig.Builder()
            .setSloppyOkvsType(Gf2eDokvsType.MEGA_BIN)
            .setSigmaOkvsType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT)
            .setPsuConfig(psuConfig)
            .build();
    }
}
