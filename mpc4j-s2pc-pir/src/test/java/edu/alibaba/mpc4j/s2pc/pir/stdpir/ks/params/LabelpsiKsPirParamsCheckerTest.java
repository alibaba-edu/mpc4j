package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.params;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirParams;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirParamsChecker;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * label PSI KSPIR params checker.
 *
 * @author Liqiang Peng
 * @date 2022/8/9
 */
@Ignore
@RunWith(Parameterized.class)
public class LabelpsiKsPirParamsCheckerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
            "SERVER_1M_CLIENT_MAX_1", LabelpsiStdKsPirParams.SERVER_1M_CLIENT_MAX_1
        });
        configurations.add(new Object[]{
            "SERVER_1M_CLIENT_MAX_4096", LabelpsiStdKsPirParams.SERVER_1M_CLIENT_MAX_4096
        });

        return configurations;
    }

    /**
     * params
     */
    private final LabelpsiStdKsPirParams params;

    public LabelpsiKsPirParamsCheckerTest(String name, LabelpsiStdKsPirParams params) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.params = params;
    }

    @Test
    public void checkValid() {
        Assert.assertTrue(LabelpsiStdKsPirParamsChecker.checkValid(params));
    }
}