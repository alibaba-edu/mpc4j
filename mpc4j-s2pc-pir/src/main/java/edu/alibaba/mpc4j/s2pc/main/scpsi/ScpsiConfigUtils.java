package edu.alibaba.mpc4j.s2pc.main.scpsi;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22.Cgs22ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

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
        // 读取协议类型
        String ptoTypeString = PropertiesUtils.readString(properties, "pto_name");
        ScpsiFactory.ScpsiType scpsiType = ScpsiFactory.ScpsiType.valueOf(ptoTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        switch (scpsiType) {
            case PSTY19:
                return createPsty19ScpsiConfig(silent);
            case CGS22:
                return createCgs22ScpsiConfig(silent);
            default:
                throw new IllegalArgumentException("Invalid " + PsuFactory.PsuType.class.getSimpleName() + ": " + scpsiType.name());
        }
    }

    private static ScpsiConfig createPsty19ScpsiConfig(boolean silent) {
        return new Psty19ScpsiConfig.Builder(silent).build();
    }

    private static ScpsiConfig createCgs22ScpsiConfig(boolean silent) {
        return new Cgs22ScpsiConfig.Builder(silent).build();
    }
}
