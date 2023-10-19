package edu.alibaba.mpc4j.s2pc.aby.main.trun;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23.Gp23ZlTruncConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20.Rrk20ZlTruncConfig;

import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncFactory.*;

/**
 * Zl Truncation config utils.
 * 
 * @author Liqiang Peng
 * @date 2023/10/12
 */
public class ZlTruncConfigUtils {
    
    private ZlTruncConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static ZlTruncConfig createZlTruncConfig(Properties properties) {
        String zlTruncTypeString = PropertiesUtils.readString(properties, "pto_name");
        ZlTruncType zlTruncType = ZlTruncType.valueOf(zlTruncTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        switch (zlTruncType) {
            case GP23:
                return new Gp23ZlTruncConfig.Builder(silent).build();
            case RRK20:
                return new Rrk20ZlTruncConfig.Builder(silent).build();
            default:
                throw new IllegalArgumentException("Invalid " + ZlTruncType.class.getSimpleName() + ": " + zlTruncType.name());
        }
    }
}
