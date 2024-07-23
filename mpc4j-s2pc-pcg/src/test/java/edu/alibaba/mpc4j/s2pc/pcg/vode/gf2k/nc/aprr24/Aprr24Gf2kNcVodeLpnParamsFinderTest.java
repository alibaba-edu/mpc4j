package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeFactory.Gf2kMspVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19.Bcg19RegGf2kMspVodeConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * APRR24 LPN parameter finder tests.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
@RunWith(Parameterized.class)
public class Aprr24Gf2kNcVodeLpnParamsFinderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aprr24Gf2kNcVodeLpnParamsFinderTest.class);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCG19_REG (semi-honest)
        configurations.add(new Object[] {
            Gf2kMspVodeType.BCG19_REG + " (" + SecurityModel.SEMI_HONEST + ")",
            new Bcg19RegGf2kMspVodeConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * GF2K-MSP-VOLE config
     */
    private final Gf2kMspVodeConfig config;

    public Aprr24Gf2kNcVodeLpnParamsFinderTest(String name, Gf2kMspVodeConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void test2To12() {
        testLpnParamsFinder(1 << 12);
    }

    @Test
    public void test2To13() {
        testLpnParamsFinder(1 << 13);
    }

    @Test
    public void test2To14() {
        testLpnParamsFinder(1 << 14);
    }

    @Test
    public void test2To15() {
        testLpnParamsFinder(1 << 15);
    }

    @Test
    public void test2To16() {
        testLpnParamsFinder(1 << 16);
    }

    @Test
    public void test2To17() {
        testLpnParamsFinder(1 << 17);
    }

    @Test
    public void test2To18() {
        testLpnParamsFinder(1 << 18);
    }

    @Test
    public void test2To19() {
        testLpnParamsFinder(1 << 19);
    }

    @Test
    public void test2To20() {
        testLpnParamsFinder(1 << 20);
    }

    @Test
    public void test2To21() {
        testLpnParamsFinder(1 << 21);
    }

    @Test
    public void test2To22() {
        testLpnParamsFinder(1 << 22);
    }

    private void testLpnParamsFinder(int minN) {
        LOGGER.info("-----find LPN Params for n = {}-----", minN);
        LpnParams iterationLpnParams = Aprr24Gf2kNcVodeLpnParamsFinder.findIterationLpnParams(config, minN);
        LpnParams setupLpnParams = Aprr24Gf2kNcVodeLpnParamsFinder.findSetupLpnParams(config, iterationLpnParams);
        LOGGER.info("Setup    : {}", setupLpnParams);
        LOGGER.info("Iteration: {}", iterationLpnParams);
    }

    @Test
    public void testIterationOutputSize() {
        LOGGER.info("-----get {} output size-----", config.getPtoType());
        LpnParams wolverineRegLpnParams = LpnParams.uncheckCreate(10805248, 589760, 1319);
        int wolverineRegOutputSize = Aprr24Gf2kNcVodeLpnParamsFinder.getIterationOutputSize(config, wolverineRegLpnParams);
        LOGGER.info("Wolverine Reg {}: output size = {}", wolverineRegLpnParams, wolverineRegOutputSize);
    }
}
