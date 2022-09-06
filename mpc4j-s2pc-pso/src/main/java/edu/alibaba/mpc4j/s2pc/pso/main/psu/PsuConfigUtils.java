package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc.EccOvdmFactory.EccOvdmType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e.Gf2eOvdmFactory.Gf2eOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.kkrt16.Kkrt16OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc.LowMcOprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19OptPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19OriPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.Zcl22PkePsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.Zcl22SkePsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;

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
            case KRTW19_ORI:
                return createKrtw19OriPsuConfig();
            case KRTW19_OPT:
                return createKrtw19OptPsuConfig();
            case GMR21:
                return generateGmr21PsuConfig(properties);
            case ZCL22_PKE:
                return createZcl22PkePsuConfig(properties);
            case ZCL22_SKE:
                return createZcl22SkePsuConfig(properties);
            case JSZ22_SFC:
                return createJsz22SfcPsuConfig(properties);
            case JSZ22_SFS:
                return createJsz22SfsPsuConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PsuType.class.getSimpleName() + ": " + psuType.name());
        }
    }

    private static PsuConfig createKrtw19OriPsuConfig() {
        return new Krtw19OriPsuConfig.Builder()
            .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .setOkvsType(OkvsType.POLYNOMIAL)
            .build();
    }

    private static Krtw19OptPsuConfig createKrtw19OptPsuConfig() {
        return new Krtw19OptPsuConfig.Builder()
            .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .build();
    }

    private static Gmr21PsuConfig generateGmr21PsuConfig(Properties properties) {
        // 是否使用安静OT
        boolean silentCot = PropertiesUtils.readBoolean(properties, "silent_cot", false);
        OsnConfig osnConfig = silentCot
            ? new Gmr21OsnConfig.Builder()
                .setCotConfig(new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build()
            : new Gmr21OsnConfig.Builder()
                .setCotConfig(new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build();
        CoreCotConfig coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);

        return new Gmr21PsuConfig.Builder()
            .setCoreCotConfig(coreCotConfig)
            .setOsnConfig(osnConfig)
            .setOkvsType(OkvsType.MEGA_BIN)
            .build();
    }

    private static Zcl22SkePsuConfig createZcl22SkePsuConfig(Properties properties) {
        boolean offlineZ2Mtg = PropertiesUtils.readBoolean(properties, "offline_z2_mtg", true);
        if (offlineZ2Mtg) {
            Z2MtgConfig offlineZ2MtgConfig = new OfflineZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build();
            BcConfig offlineBcConfig = new Bea91BcConfig.Builder()
                .setZ2MtgConfig(offlineZ2MtgConfig)
                .build();
            OprpConfig offlineOprpConfig = new LowMcOprpConfig.Builder()
                .setBcConfig(offlineBcConfig)
                .build();
            return new Zcl22SkePsuConfig.Builder()
                .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setOprpConfig(offlineOprpConfig)
                .setBcConfig(offlineBcConfig)
                .setGf2eOvdmType(Gf2eOvdmType.H3_SINGLETON_GCT)
                .build();
        } else {
            return new Zcl22SkePsuConfig.Builder()
                .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setOprpConfig(OprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setBcConfig(BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setGf2eOvdmType(Gf2eOvdmType.H3_SINGLETON_GCT)
                .build();
        }
    }

    private static Zcl22PkePsuConfig createZcl22PkePsuConfig(Properties properties) {
        // 是否使用压缩编码
        boolean compressEncode = PropertiesUtils.readBoolean(properties, "compress_encode", true);

        return new Zcl22PkePsuConfig.Builder()
            .setCoreCotConfig(CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .setCompressEncode(compressEncode)
            .setEccOvdmType(EccOvdmType.H3_SINGLETON_GCT)
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
        OsnConfig osnConfig = silentCot
            ? new Gmr21OsnConfig.Builder()
                .setCotConfig(new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build()
            : new Gmr21OsnConfig.Builder()
                .setCotConfig(new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build();
        CoreCotConfig coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);

        return new Jsz22SfcPsuConfig.Builder()
            .setCoreCotConfig(coreCotConfig)
            .setOsnConfig(osnConfig)
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
        OsnConfig osnConfig = silentCot
            ? new Gmr21OsnConfig.Builder()
            .setCotConfig(new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
            .build()
            : new Gmr21OsnConfig.Builder()
                .setCotConfig(new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build();

        return new Jsz22SfsPsuConfig.Builder()
            .setOsnConfig(osnConfig)
            .setOprfConfig(oprfConfig)
            .setCuckooHashBinType(cuckooHashBinType)
            .build();
    }
}
