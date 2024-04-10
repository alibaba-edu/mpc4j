package edu.alibaba.mpc4j.s2pc.upso.main.okvr;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi.Cmg21BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.okvs.OkvsOkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.pir.PirOkvrConfig;

import java.util.Properties;

/**
 * OKVR config utils.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class OkvrConfigUtils {

    private OkvrConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static OkvrConfig createOkvrConfig(Properties properties) {
        String okvrTypeString = PropertiesUtils.readString(properties, "pto_name");
        OkvrType okvrType = OkvrType.valueOf(okvrTypeString);
        switch (okvrType) {
            case DIRECT_2_HASH:
                return new OkvsOkvrConfig.Builder()
                    .setOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT)
                    .build();
            case DIRECT_3_HASH:
                return new OkvsOkvrConfig.Builder()
                    .setOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT)
                    .build();
            case LABEL_PSI_2_HASH:
                return new PirOkvrConfig.Builder()
                    .setSparseOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT)
                    .setBatchIndexPirConfig(new Cmg21BatchIndexPirConfig.Builder().build())
                    .build();
            case LABEL_PSI_3_HASH:
                return new PirOkvrConfig.Builder()
                    .setSparseOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT)
                    .setBatchIndexPirConfig(new Cmg21BatchIndexPirConfig.Builder().build())
                    .build();
            case BATCH_PIR_2_HASH:
                return new PirOkvrConfig.Builder()
                    .setSparseOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT)
                    .setBatchIndexPirConfig(new Mr23BatchIndexPirConfig.Builder().build())
                    .build();
            case BATCH_PIR_3_HASH:
                return new PirOkvrConfig.Builder()
                    .setSparseOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT)
                    .setBatchIndexPirConfig(new Mr23BatchIndexPirConfig.Builder().build())
                    .build();
            default:
                throw new IllegalArgumentException("Invalid " + OkvrType.class.getSimpleName() + ": " + okvrType.name());
        }
    }
}