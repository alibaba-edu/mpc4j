package edu.alibaba.mpc4j.s2pc.pir.main.ccpsi;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiConfig;

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
        CcpsiType ccpsiType = CcpsiType.valueOf(ccpsiTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        switch (ccpsiType) {
            case PSTY19:
                return new Psty19CcpsiConfig.Builder(silent).build();
            case CGS22:
                return new Cgs22CcpsiConfig.Builder(silent).build();
            case RS21:
                return new Rs21CcpsiConfig.Builder(silent).build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + CcpsiType.class.getSimpleName() + ": " + ccpsiType.name()
                );
        }
    }
}
