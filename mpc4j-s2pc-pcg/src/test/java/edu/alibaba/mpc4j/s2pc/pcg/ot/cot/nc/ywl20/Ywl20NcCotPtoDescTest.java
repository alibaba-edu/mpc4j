package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * YWL20-NC-COT protocol description test.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
@RunWith(Parameterized.class)
public class Ywl20NcCotPtoDescTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCG19_REG (malicious)
        configurations.add(new Object[] {
            MspCotFactory.MspCotType.BCG19_REG.name() + " (" + SecurityModel.MALICIOUS.name() + ")",
            new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // BCG19_REG (semi-honest)
        configurations.add(new Object[] {
            MspCotFactory.MspCotType.BCG19_REG.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20_UNI (malicious)
        configurations.add(new Object[] {
            MspCotFactory.MspCotType.YWL20_UNI.name() + " (" + SecurityModel.MALICIOUS.name() + ")",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // YWL20_UNI (semi-honest)
        configurations.add(new Object[] {
            MspCotFactory.MspCotType.YWL20_UNI.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final MspCotConfig config;

    public Ywl20NcCotPtoDescTest(String name, MspCotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void testLpnParameterMap() {
        for (int logN = Ywl20NcCotPtoDesc.MIN_LOG_N; logN <= Ywl20NcCotPtoDesc.MAX_LOG_N; logN++) {
            int minN = (1 << logN);
            LpnParams iterationLpnParams = Ywl20NcCotLpnParamsFinder.findIterationLpnParams(config, minN);
            Assert.assertEquals(iterationLpnParams, Ywl20NcCotPtoDesc.getIterationLpnParams(config, minN));
            LpnParams setupLpnParams = Ywl20NcCotLpnParamsFinder.findSetupLpnParams(config, iterationLpnParams);
            Assert.assertEquals(setupLpnParams, Ywl20NcCotPtoDesc.getSetupLpnParams(config, minN));
        }
    }
}
