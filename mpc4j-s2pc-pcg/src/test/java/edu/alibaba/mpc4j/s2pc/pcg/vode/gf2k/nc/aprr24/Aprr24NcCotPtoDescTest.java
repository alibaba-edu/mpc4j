package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeFactory.Gf2kMspVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19.Bcg19RegGf2kMspVodeConfig;
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
public class Aprr24NcCotPtoDescTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCG19_REG (semi-honest)
        configurations.add(new Object[] {
            Gf2kMspVodeType.BCG19_REG.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Bcg19RegGf2kMspVodeConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kMspVodeConfig config;

    public Aprr24NcCotPtoDescTest(String name, Gf2kMspVodeConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void testLpnParameterMap() {
        for (int logN = Aprr24Gf2kNcVodePtoDesc.MIN_LOG_N; logN <= Aprr24Gf2kNcVodePtoDesc.MAX_LOG_N; logN++) {
            int minN = (1 << logN);
            LpnParams iterationLpnParams = Aprr24Gf2kNcVodeLpnParamsFinder.findIterationLpnParams(config, minN);
            Assert.assertEquals(iterationLpnParams, Aprr24Gf2kNcVodePtoDesc.getIterationLpnParams(config, minN));
            LpnParams setupLpnParams = Aprr24Gf2kNcVodeLpnParamsFinder.findSetupLpnParams(config, iterationLpnParams);
            Assert.assertEquals(setupLpnParams, Aprr24Gf2kNcVodePtoDesc.getSetupLpnParams(config, minN));
        }
    }
}
