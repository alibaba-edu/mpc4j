package edu.alibaba.mpc4j.s2pc.aby.main.osn;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposerFactory.DecomposerType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.cgp20.Cgp20CstRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21FlatNetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24CstRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24FlatNetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.ms13.Ms13NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24.Prrs24OprfRosnConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;

import java.util.Properties;

/**
 * OSN config utilities.
 *
 * @author Feng Han
 * @date 2024/7/9
 */
public class RosnConfigUtils {
    /**
     * decompose length
     */
    private static final String DECOMPOSER_TYPE = "decomposer_type";
    /**
     * decompose length
     */
    private static final String DECOMPOSE_LEN = "decompose_t";
    /**
     * max nt
     */
    private static final String LOG_MAX_NT_FOR_BATCH = "log_max_nt";
    /**
     * max storage for one batch
     */
    private static final String LOG_MAX_CACHE_FOR_BATCH = "log_max_cache";
    /**
     * con32 type
     */
    private static final String CON32_TYPE = "con32_type";

    /**
     * private constructor.
     */
    private RosnConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static RosnConfig createConfig(Properties properties) {
        RosnType rosnType = MainPtoConfigUtils.readEnum(RosnType.class, properties, RosnMain.PTO_NAME_KEY);
        switch (rosnType) {
            case LLL24_NET:
                return createLll24NetOsnConfig(properties);
            case LLL24_FLAT_NET:
                return createLll24FlatNetOsnConfig(properties);
            case LLL24_CST:
                return createLll24CstOsnConfig(properties);
            case MS13_NET:
                return createMs13NetOsnConfig(properties);
            case GMR21_NET:
                return createGmr21NetOsnConfig(properties);
            case GMR21_FLAT_NET:
                return createGmrFlatNetOsnConfig(properties);
            case CGP20_CST:
                return createCgp20CstOsnConfig(properties);
            case PRRS24_OPRF:
                return createPrrs24OprfOsnConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + RosnType.class.getSimpleName() + ": " + rosnType.name());
        }
    }

    private static RosnConfig createLll24NetOsnConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        return new Lll24NetRosnConfig.Builder(silent).build();
    }

    private static RosnConfig createLll24FlatNetOsnConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        return new Lll24FlatNetRosnConfig.Builder(silent).build();
    }

    private static RosnConfig createLll24CstOsnConfig(Properties properties) {
        int t = PropertiesUtils.readInt(properties, DECOMPOSE_LEN, 32);
        Preconditions.checkArgument(IntMath.isPowerOfTwo(t), "T must be a power of 2: %s", t);
        int logMaxNt = PropertiesUtils.readInt(properties, LOG_MAX_NT_FOR_BATCH, 28);
        MathPreconditions.checkPositiveInRangeClosed("1 < logMaxNt <= 31", logMaxNt, 31);
        int logMaxCache = PropertiesUtils.readInt(properties, LOG_MAX_CACHE_FOR_BATCH, 32);
        MathPreconditions.checkPositive("logMaxCache", logMaxCache);
        String decomposerTypeStr = PropertiesUtils.readString(properties, DECOMPOSER_TYPE, "LLL24");
        DecomposerType decomposerType = DecomposerType.valueOf(decomposerTypeStr);
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        return new Lll24CstRosnConfig.Builder(t, silent).setDecomposerType(decomposerType).setMaxNt4Batch(1 << logMaxNt).setMaxCache4Batch(1L << logMaxCache).build();
    }

    private static RosnConfig createMs13NetOsnConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        return new Ms13NetRosnConfig.Builder(silent).build();
    }

    private static RosnConfig createGmr21NetOsnConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        return new Gmr21NetRosnConfig.Builder(silent).build();
    }

    private static RosnConfig createGmrFlatNetOsnConfig(Properties properties) {
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        return new Gmr21FlatNetRosnConfig.Builder(silent).build();
    }

    private static RosnConfig createCgp20CstOsnConfig(Properties properties) {
        int t = PropertiesUtils.readInt(properties, DECOMPOSE_LEN, 32);
        Preconditions.checkArgument(IntMath.isPowerOfTwo(t), "T must be a power of 2: %s", t);
        int logMaxNt = PropertiesUtils.readInt(properties, LOG_MAX_NT_FOR_BATCH, 28);
        MathPreconditions.checkPositiveInRangeClosed("1 < logMaxNt <= 31", logMaxNt, 31);
        int logMaxCache = PropertiesUtils.readInt(properties, LOG_MAX_CACHE_FOR_BATCH, 32);
        MathPreconditions.checkPositive("logMaxCache", logMaxCache);
        String decomposerTypeStr = PropertiesUtils.readString(properties, DECOMPOSER_TYPE, "LLL24");
        DecomposerType decomposerType = DecomposerType.valueOf(decomposerTypeStr);
        boolean silent = MainPtoConfigUtils.readSilentCot(properties);
        return new Cgp20CstRosnConfig.Builder(t, silent).setDecomposerType(decomposerType).setMaxNt4Batch(1 << logMaxNt).setMaxCache4Batch(1L << logMaxCache).build();
    }

    private static RosnConfig createPrrs24OprfOsnConfig(Properties properties) {
        Conv32Type conv32Type = MainPtoConfigUtils.readEnum(Conv32Type.class, properties, CON32_TYPE);
        return new Prrs24OprfRosnConfig.Builder(conv32Type).build();
    }
}
