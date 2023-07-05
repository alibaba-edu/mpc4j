package edu.alibaba.mpc4j.s2pc.pir.keyword.params;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirParamsChecker;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * CMG21 keyword PIR params checker.
 *
 * @author Liqiang Peng
 * @date 2022/8/9
 */
@Ignore
@RunWith(Parameterized.class)
public class Cmg21KwPirParamsCheckerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
            "SERVER_1M_CLIENT_MAX_1", Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1
        });
        configurations.add(new Object[]{
            "SERVER_1M_CLIENT_MAX_4096", Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096
        });

        return configurations;
    }

    /**
     * params
     */
    private final Cmg21KwPirParams cmg21KwPirParams;

    public Cmg21KwPirParamsCheckerTest(String name, Cmg21KwPirParams cmg21KwPirParams) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.cmg21KwPirParams = cmg21KwPirParams;
    }

    @Test
    public void checkValid() {
        Assert.assertTrue(Cmg21KwPirParamsChecker.checkValid(cmg21KwPirParams));
    }
}