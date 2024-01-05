package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Silver Code Creator factory.
 *
 * @author Hanwen Feng
 * @date 2022/3/18
 */
public class SilverCodeCreatorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SilverCodeCreatorFactory.class);
    /**
     * private constructor
     */
    private SilverCodeCreatorFactory() {
        //empty
    }

    /**
     * Creates a Silver Code Creator.
     *
     * @param silverCodeType Silver Code type.
     * @param ceilLogN target log code size (log(n)).
     * @return a Silver Coder Creator.
     */
    public static SilverCodeCreator createInstance(SilverCodeType silverCodeType, int ceilLogN) {
        if (ceilLogN < SilverCodeCreatorUtils.MIN_LOG_N || ceilLogN > SilverCodeCreatorUtils.MAX_LOG_N) {
            return new FullSilverCodeCreator(silverCodeType, ceilLogN);
        }
        try {
            // try to find an online creator.
            return new OnlineSilverCodeCreator(silverCodeType, ceilLogN);
        } catch (IllegalStateException e) {
            LOGGER.info("Silver File: " + silverCodeType.name() + "_" + ceilLogN + " is NOT ready. Starting Full LDPC Creator...");
            return new FullSilverCodeCreator(silverCodeType, ceilLogN);
        }
    }
}
