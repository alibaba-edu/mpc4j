package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.Kkrt16OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23PkePsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23SkePsuConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;

import java.util.Properties;

/**
 * PSU协议配置项工具类。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class PsuConfigUtils {

    private PsuConfigUtils() {
        // empty
    }

    /**
     * 创建配置项。
     *
     * @param properties 配置参数。
     * @return 配置项。
     */
    public static PsuConfig createPsuConfig(Properties properties) {
        // 读取协议类型
        String psuTypeString = PropertiesUtils.readString(properties, "psu_pto_name");
        PsuType psuType = PsuType.valueOf(psuTypeString);
        switch (psuType) {
            case KRTW19:
                return createKrtw19PsuConfig();
            case GMR21:
                return generateGmr21PsuConfig(properties);
            case ZCL23_PKE:
                return createZcl23PkePsuConfig(properties);
            case ZCL23_SKE:
                return createZcl23SkePsuConfig(properties);
            case JSZ22_SFC:
                return createJsz22SfcPsuConfig(properties);
            case JSZ22_SFS:
                return createJsz22SfsPsuConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PsuType.class.getSimpleName() + ": " + psuType.name());
        }
    }

    private static Krtw19PsuConfig createKrtw19PsuConfig() {
        return new Krtw19PsuConfig.Builder()
            .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .build();
    }

    private static Gmr21PsuConfig generateGmr21PsuConfig(Properties properties) {
        boolean silentCot = PropertiesUtils.readBoolean(properties, "silent_cot", false);
        return new Gmr21PsuConfig.Builder(silentCot)
            .setOkvsType(Gf2eDokvsType.MEGA_BIN)
            .build();
    }

    private static Zcl23SkePsuConfig createZcl23SkePsuConfig(Properties properties) {
        boolean offlineZ2Mtg = PropertiesUtils.readBoolean(properties, "offline_z2_mtg", true);
        if (offlineZ2Mtg) {
            Z2MtgConfig offlineZ2MtgConfig = new OfflineZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build();
            Z2cConfig offlineZ2cConfig = new Bea91Z2cConfig.Builder(SecurityModel.SEMI_HONEST)
                .setZ2MtgConfig(offlineZ2MtgConfig)
                .build();
            OprpConfig offlineOprpConfig = new LowMcOprpConfig.Builder(SecurityModel.SEMI_HONEST)
                .setZ2cConfig(offlineZ2cConfig)
                .build();
            return new Zcl23SkePsuConfig.Builder(SecurityModel.SEMI_HONEST)
                .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setOprpConfig(offlineOprpConfig)
                .setBcConfig(offlineZ2cConfig)
                .setGf2eDokvsType(Gf2eDokvsType.H3_SINGLETON_GCT)
                .build();
        } else {
            return new Zcl23SkePsuConfig.Builder(SecurityModel.SEMI_HONEST)
                .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setOprpConfig(OprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true))
                .setBcConfig(Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true))
                .setGf2eDokvsType(Gf2eDokvsType.H3_SINGLETON_GCT)
                .build();
        }
    }

    private static Zcl23PkePsuConfig createZcl23PkePsuConfig(Properties properties) {
        // 是否使用压缩编码
        boolean compressEncode = PropertiesUtils.readBoolean(properties, "compress_encode", true);

        return new Zcl23PkePsuConfig.Builder()
            .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .setCompressEncode(compressEncode)
            .setEccDokvsType(EccDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT)
            .build();
    }

    private static Jsz22SfcPsuConfig createJsz22SfcPsuConfig(Properties properties) {
        // OPRF类型
        String oprfTypeString = PropertiesUtils.readString(properties, "oprf_type",
            OprfFactory.OprfType.CM20.toString());
        OprfFactory.OprfType oprfType = OprfFactory.OprfType.valueOf(oprfTypeString);
        OprfConfig oprfConfig;
        switch (oprfType) {
            case KKRT16_OPT:
                oprfConfig = new Kkrt16OptOprfConfig.Builder().build();
                break;
            case CM20:
                oprfConfig = new Cm20MpOprfConfig.Builder().build();
                break;
            default:
                throw new IllegalArgumentException(PsuType.JSZ22_SFC.name()
                    + " does not support " + OprfFactory.OprfType.class.getSimpleName()
                    + ": " + oprfType);
        }
        // 布谷鸟哈希类型
        String cuckooHashTypeString = PropertiesUtils.readString(properties, "cuckoo_hash_bin_type",
            CuckooHashBinType.NO_STASH_PSZ18_4_HASH.toString());
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        // 是否使用安静OT
        boolean silentCot = PropertiesUtils.readBoolean(properties, "silent_cot", false);

        return new Jsz22SfcPsuConfig.Builder(silentCot)
            .setOprfConfig(oprfConfig)
            .setCuckooHashBinType(cuckooHashBinType)
            .build();
    }

    private static Jsz22SfsPsuConfig createJsz22SfsPsuConfig(Properties properties) {
        // OPRF类型
        String oprfTypeString = PropertiesUtils.readString(properties, "oprf_type",
            OprfFactory.OprfType.CM20.toString());
        OprfFactory.OprfType oprfType = OprfFactory.OprfType.valueOf(oprfTypeString);
        OprfConfig oprfConfig;
        switch (oprfType) {
            case KKRT16_OPT:
                oprfConfig = new Kkrt16OptOprfConfig.Builder().build();
                break;
            case CM20:
                oprfConfig = new Cm20MpOprfConfig.Builder().build();
                break;
            default:
                throw new IllegalArgumentException(PsuType.JSZ22_SFS.name()
                    + " does not support " + OprfFactory.OprfType.class.getSimpleName()
                    + ": " + oprfType);
        }
        // 布谷鸟哈希类型
        String cuckooHashTypeString = PropertiesUtils.readString(properties, "cuckoo_hash_bin_type",
            CuckooHashBinType.NO_STASH_PSZ18_4_HASH.toString());
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        // 是否使用安静OT
        boolean silentCot = PropertiesUtils.readBoolean(properties, "silent_cot", false);

        return new Jsz22SfsPsuConfig.Builder(silentCot)
            .setOprfConfig(oprfConfig)
            .setCuckooHashBinType(cuckooHashBinType)
            .build();
    }
}
