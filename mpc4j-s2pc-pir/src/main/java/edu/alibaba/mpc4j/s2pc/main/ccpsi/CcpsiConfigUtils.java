package edu.alibaba.mpc4j.s2pc.main.ccpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

import java.util.Properties;

/**
 * CCPSI config utils.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class CcpsiConfigUtils {

    private CcpsiConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static CcpsiConfig createCcpsiConfig(Properties properties) {
        // read protocol type
        String ccpsiTypeString = PropertiesUtils.readString(properties, "pto_name");
        CcpsiFactory.CcpsiType ccpsiType = CcpsiFactory.CcpsiType.valueOf(ccpsiTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        switch (ccpsiType) {
            case PSTY19:
                return createPsty19CcpsiConfig(silent);
            case CGS22:
                return createCgs22CcpsiConfig(silent);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + PsuFactory.PsuType.class.getSimpleName() + ": " + ccpsiType.name()
                );
        }
    }

    private static CcpsiConfig createPsty19CcpsiConfig(boolean silent) {
        return new Psty19CcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
    }

    private static CcpsiConfig createCgs22CcpsiConfig(boolean silent) {
        return new Cgs22CcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
    }
}
