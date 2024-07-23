package edu.alibaba.mpc4j.s2pc.pso.main.scpsi;

import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiFactory.ScpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22.Cgs22ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21.Rs21ScpsiConfig;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;

import java.util.Properties;

/**
 * SCPSI config utils.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class ScpsiConfigUtils {
    /**
     * private constructor.
     */
    private ScpsiConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static ScpsiConfig createConfig(Properties properties) {
        ScpsiType scpsiType = MainPtoConfigUtils.readEnum(ScpsiType.class, properties, ScpsiMain.PTO_NAME_KEY);
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        switch (scpsiType) {
            case PSTY19:
                return new Psty19ScpsiConfig.Builder(silent).build();
            case CGS22:
                return new Cgs22ScpsiConfig.Builder(silent).build();
            case RS21:
                return new Rs21ScpsiConfig.Builder(silent).build();
            default:
                throw new IllegalArgumentException("Invalid " + ScpsiType.class.getSimpleName() + ": " + scpsiType.name());
        }
    }
}
