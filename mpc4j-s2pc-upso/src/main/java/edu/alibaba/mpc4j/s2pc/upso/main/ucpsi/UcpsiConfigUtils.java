package edu.alibaba.mpc4j.s2pc.upso.main.ucpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.pir.PirOkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiConfig;

import java.util.Properties;

/**
 * UCPSI config utils.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class UcpsiConfigUtils {

    private UcpsiConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static UcpsiConfig createUcpsiConfig(Properties properties) {
        String ucpsiTypeString = PropertiesUtils.readString(properties, "pto_name");
        UcpsiType ucpsiType = UcpsiType.valueOf(ucpsiTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        switch (ucpsiType) {
            case PSTY19_OKVS:
                return createPsty19UcpsiOkvsConfig(silent);
            case PSTY19_SIMPLE_PIR:
                return createPsty19UcpsiSimplePirConfig(silent);
            case SJ23_PEQT:
                return createSj23UcpsiPeqtConfig(silent);
            case SJ23_PSM:
                return createSj23UcpsiPmtConfig(silent);
            case PSTY19_VECTORIZED_BATCH_PIR_2_HASH:
                return createPsty192HashUcpsiVectorizedBatchPirConfig(silent);
            case PSTY19_VECTORIZED_BATCH_PIR_3_HASH:
                return createPsty193HashUcpsiVectorizedBatchPirConfig(silent);
            default:
                throw new IllegalArgumentException("Invalid " + UcpsiType.class.getSimpleName() + ": " + ucpsiType.name());
        }
    }

    private static UcpsiConfig createPsty19UcpsiOkvsConfig(boolean silent) {
        return new Psty19UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent)
            .setOkvrConfig(new PirOkvrConfig.Builder()
                .setSparseOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT)
                .build())
            .build();
    }

    private static UcpsiConfig createPsty19UcpsiSimplePirConfig(boolean silent) {
        return new Psty19UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent)
            .setOkvrConfig(new PirOkvrConfig.Builder()
                .setBatchIndexPirConfig(new NaiveBatchIndexPirConfig.Builder().build())
                .setSparseOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT)
                .build())
            .build();
    }

    private static UcpsiConfig createPsty192HashUcpsiVectorizedBatchPirConfig(boolean silent) {
        return new Psty19UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent)
            .setOkvrConfig(new PirOkvrConfig.Builder()
                .setSparseOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT)
                .build())
            .build();
    }

    private static UcpsiConfig createPsty193HashUcpsiVectorizedBatchPirConfig(boolean silent) {
        return new Psty19UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent)
            .setOkvrConfig(new PirOkvrConfig.Builder()
                .setSparseOkvsType(Gf2eDokvsFactory.Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT)
                .build())
            .build();
    }

    private static UcpsiConfig createSj23UcpsiPeqtConfig(boolean silent) {
        return new Sj23PeqtUcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
    }

    private static UcpsiConfig createSj23UcpsiPmtConfig(boolean silent) {
        return new Sj23PdsmUcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
    }
}
