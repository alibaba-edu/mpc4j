package edu.alibaba.mpc4j.s2pc.pso.main.ccpsi;

import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;

import java.util.Properties;

/**
 * CCPSI config utilities.
 *
 * @author Feng Han
 * @date 2023/10/10
 */
public class CcpsiConfigUtils {
    /**
     * private constructor.
     */
    private CcpsiConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static CcpsiConfig createConfig(Properties properties) {
        CcpsiType ccpsiType = MainPtoConfigUtils.readEnum(CcpsiType.class, properties, CcpsiMain.PTO_NAME_KEY);
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        switch (ccpsiType) {
            case CGS22:
                return new Cgs22CcpsiConfig.Builder(silent).build();
            case PSTY19:
                return new Psty19CcpsiConfig.Builder(silent).build();
            case RS21:
                return new Rs21CcpsiConfig.Builder(silent).build();
            default:
                throw new IllegalArgumentException("Invalid " + CcpsiType.class.getSimpleName() + ": " + ccpsiType.name());
        }
    }
}
