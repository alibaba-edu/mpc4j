package edu.alibaba.mpc4j.s2pc.upso.main.upsu;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.ms13.Ms13OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23ByteEccDdhPmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23EccDdhPmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23PsOprfPmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23.Tcl23UpsuConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PeqtUpsuConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PkeUpsuConfig;

import java.util.Properties;

/**
 * UPSU config utils.
 *
 * @author Liqiang Peng
 * @date 2024/3/29
 */
public class UpsuConfigUtils {

    private UpsuConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static UpsuConfig createUpsuConfig(Properties properties) {
        String upsuTypeString = PropertiesUtils.readString(properties, "pto_name");
        UpsuType upsuType = UpsuType.valueOf(upsuTypeString);
        switch (upsuType) {
            case TCL23_BYTE_ECC_DDH:
                return new Tcl23UpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23ByteEccDdhPmPeqtConfig.Builder().build())
                    .build();
            case TCL23_ECC_DDH:
                return new Tcl23UpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23EccDdhPmPeqtConfig.Builder().setCompressEncode(false).build())
                    .build();
            case TCL23_PS_OPRF_MS13:
                return new Tcl23UpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23PsOprfPmPeqtConfig.Builder(false)
                        .setOsnConfig(new Ms13OsnConfig.Builder(false).build())
                        .build())
                    .build();
            case TCL23_PS_OPRF_GMR21:
                return new Tcl23UpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23PsOprfPmPeqtConfig.Builder(false)
                        .setOsnConfig(new Gmr21OsnConfig.Builder(false).build())
                        .build())
                    .build();
            case ZLP24_PKE_VECTORIZED_PIR:
                return new Zlp24PkeUpsuConfig.Builder()
                    .setBatchIndexPirConfig(new Mr23BatchIndexPirConfig.Builder().build())
                    .build();
            case ZLP24_PEQT_VECTORIZED_PIR_DDH:
                return new Zlp24PeqtUpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23ByteEccDdhPmPeqtConfig.Builder().build())
                    .setBatchIndexPirConfig(new Mr23BatchIndexPirConfig.Builder().build())
                    .build();
            case ZLP24_PEQT_VECTORIZED_PIR_PS_OPRF:
                return new Zlp24PeqtUpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23PsOprfPmPeqtConfig.Builder(false)
                        .setOsnConfig(new Gmr21OsnConfig.Builder(false).build())
                        .build())
                    .setBatchIndexPirConfig(new Mr23BatchIndexPirConfig.Builder().build())
                    .build();
            default:
                throw new IllegalArgumentException("Invalid " + UpsuType.class.getSimpleName() + ": " + upsuType.name());
        }
    }
}
