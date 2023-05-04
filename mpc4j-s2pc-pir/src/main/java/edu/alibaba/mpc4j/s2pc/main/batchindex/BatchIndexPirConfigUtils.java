package edu.alibaba.mpc4j.s2pc.main.batchindex;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirConfig;

import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirFactory.*;

/**
 * Batch Index PIR 协议配置项工具类。
 *
 * @author Liqiang Peng
 * @date 2023/3/20
 */
public class BatchIndexPirConfigUtils {

    private BatchIndexPirConfigUtils() {
        // empty
    }

    public static BatchIndexPirConfig createBatchIndexPirConfig(Properties properties) {
        // 读取协议类型
        String batchIndexPirTypeString = PropertiesUtils.readString(properties, "batch_pir_pto_name");
        BatchIndexPirType pirType = BatchIndexPirType.valueOf(batchIndexPirTypeString);
        switch (pirType) {
            case PSI_PIR:
                return createLpzg24BatchIndexPirConfig();
            case VECTORIZED_BATCH_PIR:
                return createMr23BatchIndexPirConfig();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + BatchIndexPirType.class.getSimpleName() + ": " + pirType.name()
                );
        }
    }

    private static BatchIndexPirConfig createMr23BatchIndexPirConfig() {
        return new Mr23BatchIndexPirConfig.Builder()
            .setCompressEncode(true)
            .build();
    }

    private static BatchIndexPirConfig createLpzg24BatchIndexPirConfig() {
        return new Lpzg24BatchIndexPirConfig.Builder()
            .setCompressEncode(true)
            .build();
    }
}
