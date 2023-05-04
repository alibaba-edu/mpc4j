package edu.alibaba.mpc4j.s2pc.main.ucpsi;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22.Cgs22UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir.PirUrbopprfConfig;

import java.util.Properties;

/**
 * UCPSI config utils.
 * 
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class UcpsiConfigUtils {
    
    private UcpsiConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static UcpsiConfig createUcpsiConfig(Properties properties) {
        // 读取协议类型
        String ucpsiTypeString = PropertiesUtils.readString(properties, "pto_name");
        UcpsiType ucpsiType = UcpsiType.valueOf(ucpsiTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        switch (ucpsiType) {
            case CGS22_OKVS:
                return createCgs22UcpsiOkvsConfig(silent);
            case CGS22_PIR:
                return createCgs22UcpsiPirConfig(silent);
            case PSTY19_OKVS:
                return createPsty19UcpsiOkvsConfig(silent);
            case PSTY19_PIR:
                return createPsty19UcpsiPirConfig(silent);
            default:
                throw new IllegalArgumentException("Invalid " + UcpsiType.class.getSimpleName() + ": " + ucpsiType.name());
        }
    }

    private static UcpsiConfig createPsty19UcpsiOkvsConfig(boolean silent) {
        return new Psty19UcpsiConfig.Builder(silent).build();
    }

    private static UcpsiConfig createPsty19UcpsiPirConfig(boolean silent) {
        return new Psty19UcpsiConfig.Builder(silent)
            .setUbopprfConfig(new PirUbopprfConfig.Builder().build())
            .build();
    }

    private static UcpsiConfig createCgs22UcpsiOkvsConfig(boolean silent) {
        return new Cgs22UcpsiConfig.Builder(silent).build();
    }

    private static UcpsiConfig createCgs22UcpsiPirConfig(boolean silent) {
        return new Cgs22UcpsiConfig.Builder(silent)
            .setUrbopprfConfig(new PirUrbopprfConfig.Builder().build())
            .build();
    }
}
