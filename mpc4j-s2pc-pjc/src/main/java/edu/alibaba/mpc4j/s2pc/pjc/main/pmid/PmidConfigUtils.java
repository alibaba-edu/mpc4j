package edu.alibaba.mpc4j.s2pc.pjc.main.pmid;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
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
     * 创建配置项。
     *
     * @param properties 配置参数。
     * @return 配置项。
     */
    static PmidConfig createConfig(Properties properties) {
        // 读取协议类型
        String pmidTypeString = PropertiesUtils.readString(properties, "pmid_pto_name");
        PmidType pmidType = PmidType.valueOf(pmidTypeString);
        switch (pmidType) {
            case ZCL22_MP:
                return createZcl22MpPmidConfig(properties);
            case ZCL22_SLOPPY:
                return createZcl22SloppyPmidConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PmidType.class.getSimpleName() + ": " + pmidTypeString);
        }
    }

    private static Zcl22MpPmidConfig createZcl22MpPmidConfig(Properties properties) {
        // PSU类型
        PsuConfig psuConfig = PsuConfigUtils.createPsuConfig(properties);

        return new Zcl22MpPmidConfig.Builder()
            .setMpOprfConfig(OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST))
            .setSigmaOkvsType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT)
            .setPsuConfig(psuConfig)
            .build();
    }

    private static Zcl22SloppyPmidConfig createZcl22SloppyPmidConfig(Properties properties) {
        // PSU类型
        PsuConfig psuConfig = PsuConfigUtils.createPsuConfig(properties);

        return new Zcl22SloppyPmidConfig.Builder()
            .setSloppyOkvsType(Gf2eDokvsType.MEGA_BIN)
            .setSigmaOkvsType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT)
            .setPsuConfig(psuConfig)
            .build();
    }
}
