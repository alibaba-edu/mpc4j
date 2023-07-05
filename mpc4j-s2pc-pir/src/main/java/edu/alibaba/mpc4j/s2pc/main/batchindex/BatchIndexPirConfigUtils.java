package edu.alibaba.mpc4j.s2pc.main.batchindex;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;

import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory.*;

/**
 * Batch Index PIR protocol config utils.
 *
 * @author Liqiang Peng
 * @date 2023/3/20
 */
public class BatchIndexPirConfigUtils {

    private BatchIndexPirConfigUtils() {
        // empty
    }

    public static BatchIndexPirConfig createBatchIndexPirConfig(Properties properties) {
        // read protocol type
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
        return new Mr23BatchIndexPirConfig.Builder().build();
    }

    private static BatchIndexPirConfig createLpzg24BatchIndexPirConfig() {
        return new Lpzl24BatchIndexPirConfig.Builder()
            .setUpsiConfig(new Cmg21UpsiConfig.Builder().build())
            .build();
    }
}