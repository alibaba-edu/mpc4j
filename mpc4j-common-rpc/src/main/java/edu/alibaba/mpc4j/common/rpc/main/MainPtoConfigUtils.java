package edu.alibaba.mpc4j.common.rpc.main;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 * protocol config utilities.
 *
 * @author Weiran Liu
 * @date 2024/4/29
 */
public class MainPtoConfigUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainPtoConfigUtils.class);

    /**
     * private constructor
     */
    private MainPtoConfigUtils() {
        // empty
    }

    /**
     * Gets the input folder name.
     *
     * @return input folder name.
     */
    public static String getFileFolderName() {
        return "temp" + File.separator;
    }

    /**
     * append string key
     */
    private static final String SAVE_PATH_STRING = "save_path";
    /**
     * Gets the input folder name.
     *
     * @return input folder name.
     */
    public static String readFileFolderName(Properties properties) {
        return PropertiesUtils.readString(properties, SAVE_PATH_STRING, "temp") + File.separator;
    }

    /**
     * append string key
     */
    private static final String APPEND_STRING_KEY = "append_string";

    /**
     * Reads append string for output file. The default value is "".
     *
     * @param properties properties.
     * @return append string.
     */
    public static String readAppendString(Properties properties) {
        return PropertiesUtils.readString(properties, APPEND_STRING_KEY, "");
    }

    /**
     * protocol type key
     */
    public static final String PTO_TYPE_KEY = "pto_type";

    /**
     * Reads protocol type.
     *
     * @param properties properties.
     * @return protocol type.
     */
    public static String readPtoType(Properties properties) {
        String ptoType = PropertiesUtils.readString(properties, PTO_TYPE_KEY);
        LOGGER.info(MainPtoConfigUtils.PTO_TYPE_KEY + " = " + ptoType);
        return ptoType;
    }

    /**
     * Reads protocol name.
     *
     * @param enumClass  protocol type class.
     * @param properties properties.
     * @param enumKey protocol name key.
     * @return protocol name.
     */
    public static <T extends Enum<T>> T readEnum(Class<T> enumClass, Properties properties, String enumKey) {
        String valueString = PropertiesUtils.readString(properties, enumKey);
        try {
            return Enum.valueOf(enumClass, valueString);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid {}, must be in {}", valueString, Arrays.toString(enumClass.getEnumConstants()));
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * cuckoo hash bin type key
     */
    private static final String CUCKOO_HASH_BIN_TYPE_KEY = "cuckoo_hash_bin_type";

    /**
     * Reads the type of CuckooHash from properties.
     *
     * @param properties properties.
     * @return the type of CuckooHash.
     */
    public static CuckooHashBinType readCuckooHashBinType(Properties properties) {
        return readEnum(CuckooHashBinType.class, properties, CUCKOO_HASH_BIN_TYPE_KEY);
    }

    /**
     * GF2E DOKVS type key
     */
    private static final String GF2E_DOKVS_TYPE_KEY = "gf2e_okvs_type";

    /**
     * Reads the type of GF2E-DOKVS from properties.
     *
     * @param properties properties.
     * @return the type of GF2E-DOKVS.
     */
    public static Gf2eDokvsType readGf2eDokvsType(Properties properties) {
        return readEnum(Gf2eDokvsType.class, properties, GF2E_DOKVS_TYPE_KEY);
    }

    /**
     * GF2K DOKVS type key
     */
    private static final String GF2K_DOKVS_TYPE_KEY = "gf2k_okvs_type";

    /**
     * Reads the type of GF2K-DOKVS from properties.
     *
     * @param properties properties.
     * @return the type of GF2K-DOKVS.
     */
    public static Gf2kDokvsType readGf2kDokvsType(Properties properties) {
        return readEnum(Gf2kDokvsType.class, properties, GF2K_DOKVS_TYPE_KEY);
    }

    /**
     * filter type key
     */
    private static final String FILTER_TYPE_KEY = "filter_type";

    /**
     * Reads the type of Filter from properties.
     *
     * @param properties properties.
     * @return the type of Filter.
     */
    public static FilterType readFilterType(Properties properties) {
        return readEnum(FilterType.class, properties, FILTER_TYPE_KEY);
    }

    /**
     * security model key
     */
    private static final String SECURITY_MODEL_KEY = "security_model";

    /**
     * Reads SecurityModel (no default value).
     *
     * @param properties properties.
     * @return SecurityModel.
     */
    public static SecurityModel readSecurityModel(Properties properties) {
        return readEnum(SecurityModel.class, properties, SECURITY_MODEL_KEY);
    }

    /**
     * compress encode key
     */
    private static final String COMPRESS_ENCODE_KEY = "compress_encode";

    /**
     * Reads whether to use compress encode in ECC. The default value is true.
     *
     * @param properties properties.
     * @return whether to use compress encode in ECC.
     */
    public static boolean readCompressEncode(Properties properties) {
        return PropertiesUtils.readBoolean(properties, MainPtoConfigUtils.COMPRESS_ENCODE_KEY, true);
    }

    /**
     * silent COT key
     */
    private static final String SILENT_COT_KEY = "silent_cot";

    /**
     * Reads whether to use silent COT. The default value is false.
     *
     * @param properties properties.
     * @return whether to use silent COT.
     */
    public static boolean readSilentCot(Properties properties) {
        return PropertiesUtils.readBoolean(properties, MainPtoConfigUtils.SILENT_COT_KEY, false);
    }
}
