package edu.alibaba.mpc4j.s2pc.upso.main.upsu;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.ms13.Ms13NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23ByteEccDdhPmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23EccDdhPmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23PsOprfPmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirConfig;
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
    /**
     * private constructor.
     */
    private UpsuConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static UpsuConfig createConfig(Properties properties) {
        UpsuMainType upsuMainType = MainPtoConfigUtils.readEnum(UpsuMainType.class, properties, UpsuMain.PTO_NAME_KEY);
        switch (upsuMainType) {
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
                    .setPmPeqtConfig(new Tcl23PsOprfPmPeqtConfig.Builder()
                        .setOsnConfig(new Lll24DosnConfig.Builder(new Ms13NetRosnConfig.Builder(false).build()).build())
                        .build())
                    .build();
            case TCL23_PS_OPRF_GMR21:
                return new Tcl23UpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23PsOprfPmPeqtConfig.Builder()
                        .setOsnConfig(new Lll24DosnConfig.Builder(new Gmr21NetRosnConfig.Builder(false).build()).build())
                        .build())
                    .build();
            case ZLP24_PKE_VECTORIZED_PIR:
                return new Zlp24PkeUpsuConfig.Builder()
                    .setStdIdxPirConfig(new VectorizedStdIdxPirConfig.Builder().build())
                    .build();
            case ZLP24_PEQT_VECTORIZED_PIR_DDH:
                return new Zlp24PeqtUpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23ByteEccDdhPmPeqtConfig.Builder().build())
                    .setStdIdxPirConfig(new VectorizedStdIdxPirConfig.Builder().build())
                    .build();
            case ZLP24_PEQT_VECTORIZED_PIR_PS_OPRF:
                return new Zlp24PeqtUpsuConfig.Builder()
                    .setPmPeqtConfig(new Tcl23PsOprfPmPeqtConfig.Builder()
                        .setOsnConfig(new Lll24DosnConfig.Builder(new Gmr21NetRosnConfig.Builder(false).build()).build())
                        .build())
                    .setStdIdxPirConfig(new VectorizedStdIdxPirConfig.Builder().build())
                    .build();
            default:
                throw new IllegalArgumentException("Invalid " + UpsuMainType.class.getSimpleName() + ": " + upsuMainType.name());
        }
    }
}
