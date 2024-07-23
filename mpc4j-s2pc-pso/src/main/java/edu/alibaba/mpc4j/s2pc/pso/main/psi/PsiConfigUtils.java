package edu.alibaba.mpc4j.s2pc.pso.main.psi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17.Oos17PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.psz14.Psz14PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22.Rr22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21.Rs21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13.Dcw13PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20.Cm20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22.Czz22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21.Gmr21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16.Rr16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17.Rr17DePsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17.Rr17EcPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.Hfh99ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.Hfh99EccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16.Kkrt16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19FastPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19LowPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20.Prty20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21.Rt21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17EccPsiConfig;

import java.util.Properties;

/**
 * PSI config utilities.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class PsiConfigUtils {
    /**
     * division parameter, only used in RR17 (Dual Execution)
     */
    private static final String DIV_PARAM = "div_param";

    /**
     * private constructor.
     */
    private PsiConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PsiConfig createConfig(Properties properties) {
        PsiType psiType = MainPtoConfigUtils.readEnum(PsiType.class, properties, PsiMain.PTO_NAME_KEY);
        switch (psiType) {
            case HFH99_ECC:
                return createHfh99EccPsiConfig(properties);
            case HFH99_BYTE_ECC:
                return createHfh99ByteEccPsiConfig();
            case KKRT16:
                return createKkrt16PsiConfig(properties);
            case CM20:
                return createCm20PsiConfig(properties);
            case RA17_ECC:
                return createRa17EccPsiConfig();
            case RA17_BYTE_ECC:
                return createRa17ByteEccPsiConfig();
            case PRTY20:
                return createPrty20PsiConfig(properties);
            case PRTY19_LOW:
                return createPrty19LowPsiConfig(properties);
            case PRTY19_FAST:
                return createPrty19FastPsiConfig(properties);
            case GMR21:
                return createGmr21PsiConfig(properties);
            case CZZ22:
                return createCzz22PsiConfig();
            case PSZ14:
                return createPsz14PsiConfig();
            case DCW13:
                return createDcw13PsiConfig();
            case OOS17:
                return createOos17PsiConfig();
            case RT21:
                return createRt21PsiConfig();
            case RS21:
                return createRs21PsiConfig(properties);
            case RR22:
                return createRr22PsiConfig(properties);
            case RR16:
                return createRr16PsiConfig();
            case RR17_DE:
                return createRr17DePsiConfig(properties);
            case RR17_EC:
                return createRr17EcPsiConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + psiType.name());
        }
    }

    private static PsiConfig createHfh99EccPsiConfig(Properties properties) {
        boolean compressEncode = MainPtoConfigUtils.readCompressEncode(properties);
        return new Hfh99EccPsiConfig.Builder().setCompressEncode(compressEncode).build();
    }

    private static PsiConfig createHfh99ByteEccPsiConfig() {
        return new Hfh99ByteEccPsiConfig.Builder().build();
    }

    private static PsiConfig createKkrt16PsiConfig(Properties properties) {
        CuckooHashBinType cuckooHashBinType = MainPtoConfigUtils.readCuckooHashBinType(properties);
        FilterType filterType = MainPtoConfigUtils.readFilterType(properties);
        return new Kkrt16PsiConfig.Builder().setCuckooHashBinType(cuckooHashBinType).setFilterType(filterType).build();
    }

    private static PsiConfig createCm20PsiConfig(Properties properties) {
        FilterType filterType = MainPtoConfigUtils.readFilterType(properties);
        return new Cm20PsiConfig.Builder().setFilterType(filterType).build();
    }

    private static PsiConfig createRa17EccPsiConfig() {
        return new Ra17EccPsiConfig.Builder().build();
    }

    private static PsiConfig createRa17ByteEccPsiConfig() {
        return new Ra17ByteEccPsiConfig.Builder().build();
    }

    private static PsiConfig createPrty20PsiConfig(Properties properties) {
        Gf2eDokvsType okvsType = MainPtoConfigUtils.readGf2eDokvsType(properties);
        SecurityModel securityModel = MainPtoConfigUtils.readSecurityModel(properties);
        FilterType filterType = MainPtoConfigUtils.readFilterType(properties);
        return new Prty20PsiConfig.Builder(securityModel).setPaxosType(okvsType).setFilterType(filterType).build();
    }

    private static PsiConfig createPrty19LowPsiConfig(Properties properties) {
        Gf2eDokvsType okvsType = MainPtoConfigUtils.readGf2eDokvsType(properties);
        return new Prty19LowPsiConfig.Builder().setOkvsType(okvsType).build();
    }

    private static PsiConfig createPrty19FastPsiConfig(Properties properties) {
        FilterType filterType = MainPtoConfigUtils.readFilterType(properties);
        return new Prty19FastPsiConfig.Builder().setFilterType(filterType).build();
    }

    private static PsiConfig createGmr21PsiConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        return new Gmr21PsiConfig.Builder(silent).build();
    }

    private static PsiConfig createCzz22PsiConfig() {
        return new Czz22PsiConfig.Builder().build();
    }

    private static PsiConfig createPsz14PsiConfig() {
        return new Psz14PsiConfig.Builder().build();
    }

    private static PsiConfig createDcw13PsiConfig() {
        return new Dcw13PsiConfig.Builder().build();
    }

    private static PsiConfig createOos17PsiConfig() {
        return new Oos17PsiConfig.Builder().build();
    }

    private static PsiConfig createRt21PsiConfig() {
        return new Rt21PsiConfig.Builder().build();
    }

    private static PsiConfig createRs21PsiConfig(Properties properties) {
        SecurityModel securityModel = MainPtoConfigUtils.readSecurityModel(properties);
        FilterType filterType = MainPtoConfigUtils.readFilterType(properties);
        return new Rs21PsiConfig.Builder(securityModel).setFilterType(filterType).build();
    }

    private static PsiConfig createRr22PsiConfig(Properties properties) {
        SecurityModel securityModel = MainPtoConfigUtils.readSecurityModel(properties);
        Gf2kDokvsType okvsType = MainPtoConfigUtils.readGf2kDokvsType(properties);
        FilterType filterType = MainPtoConfigUtils.readFilterType(properties);
        return new Rr22PsiConfig.Builder(securityModel, okvsType).setFilterType(filterType).build();
    }

    private static PsiConfig createRr17DePsiConfig(Properties properties) {
        int divParam = PropertiesUtils.readInt(properties, DIV_PARAM);
        FilterType filterType = MainPtoConfigUtils.readFilterType(properties);
        return new Rr17DePsiConfig.Builder().setDivParam(divParam).setFilterType(filterType).build();
    }

    private static PsiConfig createRr17EcPsiConfig(Properties properties) {
        int divParam = PropertiesUtils.readInt(properties, DIV_PARAM);
        return new Rr17EcPsiConfig.Builder().setDivParam(divParam).build();
    }

    private static PsiConfig createRr16PsiConfig() {
        return new Rr16PsiConfig.Builder().build();
    }
}
