package edu.alibaba.mpc4j.s2pc.pir.main.kwpir;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.StdKwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirConfig;

import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.StdKwPirFactory.StdKwPirType;


/**
 * Standard Keyword PIR config utilities.
 *
 * @author Liqiang Peng
 * @date 2024/9/18
 */
public class StdKwPirConfigUtils {
    /**
     * private constructor.
     */
    private StdKwPirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static StdKwPirConfig createConfig(Properties properties) {
        StdKwPirType kwPirType = MainPtoConfigUtils.readEnum(StdKwPirType.class, properties, StdKwPirMain.PTO_NAME_KEY);
        return switch (kwPirType) {
            case Pantheon -> new PantheonStdKwPirConfig.Builder().build();
            case ALPR21 -> new Alpr21StdKwPirConfig.Builder().build();
        };
    }
}