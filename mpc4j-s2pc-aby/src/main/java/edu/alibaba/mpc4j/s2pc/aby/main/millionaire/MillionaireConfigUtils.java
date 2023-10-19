package edu.alibaba.mpc4j.s2pc.aby.main.millionaire;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrFactory.ZlCorrType;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.rrk20.Rrk20MillionaireConfig;

import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory.*;

/**
 * Millionaire config utils.
 * 
 * @author Liqiang Peng
 * @date 2023/10/12
 */
public class MillionaireConfigUtils {
    
    private MillionaireConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static MillionaireConfig createMillionaireConfig(Properties properties) {
        String millionaireTypeString = PropertiesUtils.readString(properties, "pto_name");
        MillionaireType millionaireType = MillionaireType.valueOf(millionaireTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        if (millionaireType == MillionaireType.RRK20) {
            return new Rrk20MillionaireConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        } else {
            throw new IllegalArgumentException(
                "Invalid " + MillionaireType.class.getSimpleName() + ": " + millionaireType.name()
            );
        }
    }
}