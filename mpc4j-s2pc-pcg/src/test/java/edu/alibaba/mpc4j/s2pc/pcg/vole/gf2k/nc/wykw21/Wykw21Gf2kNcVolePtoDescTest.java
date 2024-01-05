package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19.Bcg19RegGf2kMspVoleConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * WYKW21-GF2K-NC-VOLE protocol description test.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
@RunWith(Parameterized.class)
public class Wykw21Gf2kNcVolePtoDescTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCG19_REG (malicious)
        configurations.add(new Object[] {
            Gf2kMspVoleFactory.Gf2kMspVoleType.BCG19_REG + " (" + SecurityModel.MALICIOUS + ")",
            new Bcg19RegGf2kMspVoleConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // BCG19_REG (semi-honest)
        configurations.add(new Object[] {
            Gf2kMspVoleFactory.Gf2kMspVoleType.BCG19_REG + " (" + SecurityModel.SEMI_HONEST + ")",
            new Bcg19RegGf2kMspVoleConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kMspVoleConfig config;

    public Wykw21Gf2kNcVolePtoDescTest(String name, Gf2kMspVoleConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void testLpnParameterMap() {
        for (int logN = Wykw21Gf2kNcVolePtoDesc.MIN_LOG_N; logN <= Wykw21Gf2kNcVolePtoDesc.MAX_LOG_N; logN++) {
            int minN = (1 << logN);
            LpnParams iterationLpnParams = Wykw21Gf2kNcVoleLpnParamsFinder.findIterationLpnParams(config, minN);
            Assert.assertEquals(iterationLpnParams, Wykw21Gf2kNcVolePtoDesc.getIterationLpnParams(config, minN));
            LpnParams setupLpnParams = Wykw21Gf2kNcVoleLpnParamsFinder.findSetupLpnParams(config, iterationLpnParams);
            Assert.assertEquals(setupLpnParams, Wykw21Gf2kNcVolePtoDesc.getSetupLpnParams(config, minN));
        }
    }
}
