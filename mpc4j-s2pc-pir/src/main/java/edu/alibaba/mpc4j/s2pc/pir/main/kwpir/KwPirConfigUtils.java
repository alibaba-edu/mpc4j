package edu.alibaba.mpc4j.s2pc.pir.main.kwpir;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22.Aaag22KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;

import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory.*;

/**
 * Keyword PIR config utils.
 *
 * @author Liqiang Peng
 * @date 2023/9/28
 */
public class KwPirConfigUtils {

    private KwPirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static KwPirConfig createKwPirConfig(Properties properties) {
        // read protocol type
        String kwPirTypeString = PropertiesUtils.readString(properties, "pto_name");
        KwPirType kwPirType = KwPirType.valueOf(kwPirTypeString);
        switch (kwPirType) {
            case CMG21:
                return new Cmg21KwPirConfig.Builder().build();
            case ALPR21:
                return new Alpr21KwPirConfig.Builder().build();
            case AAAG22:
                return new Aaag22KwPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + KwPirType.class.getSimpleName() + ": " + kwPirType.name()
                );
        }
    }
}