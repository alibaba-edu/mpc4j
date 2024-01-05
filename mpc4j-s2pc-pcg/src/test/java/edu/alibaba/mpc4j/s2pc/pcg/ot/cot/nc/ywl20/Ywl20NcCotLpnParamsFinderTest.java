package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * YWL20-NC-COT LPN parameter finder tests.
 *
 * @author Weiran Liu
 * @date 2022/01/27
 */
@RunWith(Parameterized.class)
public class Ywl20NcCotLpnParamsFinderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ywl20NcCotLpnParamsFinderTest.class);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCG19_REG (malicious)
        configurations.add(new Object[] {
            MspCotFactory.MspCotType.BCG19_REG + " (" + SecurityModel.MALICIOUS + ")",
            new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // BCG19_REG (semi-honest)
        configurations.add(new Object[] {
            MspCotFactory.MspCotType.BCG19_REG + " (" + SecurityModel.SEMI_HONEST + ")",
            new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        // YWL20_UNI (malicious)
        configurations.add(new Object[] {
            MspCotFactory.MspCotType.YWL20_UNI + " (" + SecurityModel.MALICIOUS + ")",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // YWL20_UNI (semi-honest)
        configurations.add(new Object[] {
            MspCotFactory.MspCotType.YWL20_UNI + " (" + SecurityModel.SEMI_HONEST + ")",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * MSP-COT config
     */
    private final MspCotConfig config;

    public Ywl20NcCotLpnParamsFinderTest(String name, MspCotConfig config) {
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

    @Test
    public void test2To23() {
        testLpnParamsFinder(1 << 23);
    }

    @Test
    public void test2To24() {
        testLpnParamsFinder(1 << 24);
    }

    @Test
    public void test10Million() {
        testLpnParamsFinder(10000000);
    }

    private void testLpnParamsFinder(int minN) {
        LOGGER.info("-----find LPN Params for n = {}-----", minN);
        LpnParams iterationLpnParams = Ywl20NcCotLpnParamsFinder.findIterationLpnParams(config, minN);
        LpnParams setupLpnParams = Ywl20NcCotLpnParamsFinder.findSetupLpnParams(config, iterationLpnParams);
        LOGGER.info("Setup    : {}", setupLpnParams);
        LOGGER.info("Iteration: {}", iterationLpnParams);
    }

    @Test
    public void testIterationOutputSize() {
        LOGGER.info("-----get {} output size-----", config.getPtoType());
        LpnParams ferretUniLpnParams = LpnParams.uncheckCreate(10616092, 588160, 1324);
        int ferretUniOutputSize = Ywl20NcCotLpnParamsFinder.getIterationOutputSize(config, ferretUniLpnParams);
        LOGGER.info("Ferret Uni {}: output size = {}", ferretUniLpnParams, ferretUniOutputSize);

        LpnParams ferretRegLpnParams = LpnParams.uncheckCreate(10805248, 589760, 1319);
        int ferretRegOutputSize = Ywl20NcCotLpnParamsFinder.getIterationOutputSize(config, ferretRegLpnParams);
        LOGGER.info("Ferret Reg {}: output size = {}", ferretRegLpnParams, ferretRegOutputSize);
    }
}
