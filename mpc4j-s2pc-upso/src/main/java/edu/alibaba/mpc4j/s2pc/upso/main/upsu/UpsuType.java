package edu.alibaba.mpc4j.s2pc.upso.main.upsu;

/**
 * UPSU type.
 *
 * @author Liqiang Peng
 * @date 2024/3/29
 */
public enum UpsuType {
    /**
     * TCL23 Byte Ecc DDH Permute Matrix PEQT
     */
    TCL23_BYTE_ECC_DDH,
    /**
     * TCL23 Ecc DDH Permute Matrix PEQT
     */
    TCL23_ECC_DDH,
    /**
     * TCL23 Permute + Share and OPRF Permute Matrix PEQT
     */
    TCL23_PS_OPRF_GMR21,
    /**
     * TCL23 Permute + Share and OPRF Permute Matrix PEQT
     */
    TCL23_PS_OPRF_MS13,
    /**
     * ZLP24 PKE vectorized batch PIR
     */
    ZLP24_PKE_VECTORIZED_PIR,
    /**
     * ZLP24 PEQT vectorized batch PIR + DDH Permute Matrix PEQT
     */
    ZLP24_PEQT_VECTORIZED_PIR_DDH,
    /**
     * ZLP24 PEQT vectorized batch PIR + Permute + Share and OPRF Permute Matrix PEQT
     */
    ZLP24_PEQT_VECTORIZED_PIR_PS_OPRF,
}
