package edu.alibaba.mpc4j.s2pc.pir.main.scpsi;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiFactory.ScpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22.Cgs22ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21.Rs21ScpsiConfig;

import java.util.Properties;

/**
 * SCPSI config utils.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class ScpsiConfigUtils {

    private ScpsiConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static ScpsiConfig createScpsiConfig(Properties properties) {
        String ptoTypeString = PropertiesUtils.readString(properties, "pto_name");
        ScpsiType scpsiType = ScpsiType.valueOf(ptoTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        switch (scpsiType) {
            case PSTY19:
                return new Psty19ScpsiConfig.Builder(silent).build();
            case CGS22:
                return new Cgs22ScpsiConfig.Builder(silent).build();
            case RS21:
                return new Rs21ScpsiConfig.Builder(silent).build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ScpsiType.class.getSimpleName() + ": " + scpsiType.name()
                );
        }
    }
}
