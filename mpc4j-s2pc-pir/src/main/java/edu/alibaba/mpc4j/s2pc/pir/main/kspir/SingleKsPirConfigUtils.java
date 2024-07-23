package edu.alibaba.mpc4j.s2pc.pir.main.kspir;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirConfig;

import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirFactory.StdKsPirType;


/**
 * Single standard Keyword PIR config utilities.
 *
 * @author Liqiang Peng
 * @date 2023/9/28
 */
public class SingleKsPirConfigUtils {
    /**
     * private constructor.
     */
    private SingleKsPirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static StdKsPirConfig createConfig(Properties properties) {
        StdKsPirType kwPirType = MainPtoConfigUtils.readEnum(StdKsPirType.class, properties, SingleKsPirMain.PTO_NAME_KEY);
        switch (kwPirType) {
            case Label_PSI:
                return new LabelpsiStdKsPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + StdKsPirType.class.getSimpleName() + ": " + kwPirType.name()
                );
        }
    }
}