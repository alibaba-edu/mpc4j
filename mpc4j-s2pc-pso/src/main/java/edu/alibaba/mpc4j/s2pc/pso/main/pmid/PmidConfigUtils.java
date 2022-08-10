package edu.alibaba.mpc4j.s2pc.pso.main.pmid;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.ra17.Ra17MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidFactory.PmidType;
import edu.alibaba.mpc4j.s2pc.pso.pmid.zcl22.Zcl22MpPmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pmid.zcl22.Zcl22SloppyPmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;

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
        String pmidTypeString = Preconditions.checkNotNull(
            properties.getProperty("pto_name"), "Please set pto_name"
        );
        PmidType pmidType = PmidType.valueOf(pmidTypeString);
        switch (pmidType) {
            case ZCL22_MP:
                return createZcl22MpPmidConfig(properties);
            case ZCL22_SLOPPY:
                return createZcl22SloppyPmidConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid PmidType: " + pmidTypeString);
        }
    }

    private static Zcl22MpPmidConfig createZcl22MpPmidConfig(Properties properties) {
        // 多点OPRF类型
        String mpOprfTypeString = Preconditions.checkNotNull(
            properties.getProperty("mp_oprf_type"), "Please set mp_oprf_type"
        );
        OprfFactory.OprfType mpOprfType = OprfFactory.OprfType.valueOf(mpOprfTypeString);
        MpOprfConfig mpOprfConfig;
        switch (mpOprfType) {
            case CM20:
                mpOprfConfig = new Cm20MpOprfConfig.Builder().build();
                break;
            case RA17:
                mpOprfConfig = new Ra17MpOprfConfig.Builder().build();
                break;
            default:
                throw new IllegalArgumentException("Invalid MpOprfType: " + mpOprfTypeString);
        }
        // σ的OKVS类型
        String sigmaOkvsTypeString = Preconditions.checkNotNull(
            properties.getProperty("sigma_okvs_type"), "Please set sigma_okvs_type"
        );
        OkvsType sigmaOkvsType = OkvsType.valueOf(sigmaOkvsTypeString);
        // PSU类型
        PsuConfig psuConfig = new Gmr21PsuConfig.Builder().build();

        return new Zcl22MpPmidConfig.Builder()
            .setMpOprfConfig(mpOprfConfig)
            .setSigmaOkvsType(sigmaOkvsType)
            .setPsuConfig(psuConfig)
            .build();
    }

    private static Zcl22SloppyPmidConfig createZcl22SloppyPmidConfig(Properties properties) {
        // Sloppy的OKVS类型
        String sloppyOkvsTypeString = Preconditions.checkNotNull(
            properties.getProperty("sloppy_okvs_type"), "Please set sloppy_okvs_type"
        );
        OkvsType sloppyOkvsType = OkvsType.valueOf(sloppyOkvsTypeString);
        // σ的OKVS类型
        String sigmaOkvsTypeString = Preconditions.checkNotNull(
            properties.getProperty("sigma_okvs_type"), "Please set sigma_okvs_type"
        );
        OkvsType sigmaOkvsType = OkvsType.valueOf(sigmaOkvsTypeString);
        // PSU类型
        PsuConfig psuConfig = new Gmr21PsuConfig.Builder().build();

        return new Zcl22SloppyPmidConfig.Builder()
            .setSloppyOkvsType(sloppyOkvsType)
            .setSigmaOkvsType(sigmaOkvsType)
            .setPsuConfig(psuConfig)
            .build();
    }
}
