package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiParams;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiParamsChecker;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * CMG21 UPSI params checker.
 *
 * @author Liqiang Peng
 * @date 2022/8/9
 */
@Ignore
@RunWith(Parameterized.class)
public class Cmg21UpsiParamsCheckerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[] {
            "SERVER_2K_CLIENT_MAX_1", Cmg21UpsiParams.SERVER_2K_CLIENT_MAX_1
        });
        configurations.add(new Object[] {
            "SERVER_100K_CLIENT_MAX_1", Cmg21UpsiParams.SERVER_100K_CLIENT_MAX_1
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_1K_CMP", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_CMP
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_1K_COM", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_11041", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_11041
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_2K_CMP", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_CMP
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_2K_COM", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_COM
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_256", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_256
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_4K_CMP", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_CMP
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_4K_COM", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_COM
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_512_CMP", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_CMP
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_512_COM", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_COM
        });
        configurations.add(new Object[] {
            "SERVER_1M_CLIENT_MAX_5535", Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_5535
        });
        configurations.add(new Object[] {
            "SERVER_16M_CLIENT_MAX_1024", Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_1024
        });
        configurations.add(new Object[] {
            "SERVER_16M_CLIENT_MAX_2048", Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_2048
        });
        return configurations;
    }

    /**
     * params
     */
    private final Cmg21UpsiParams cmg21UpsiParams;

    public Cmg21UpsiParamsCheckerTest(String name, Cmg21UpsiParams cmg21UpsiParams) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.cmg21UpsiParams = cmg21UpsiParams;
    }

    @Test
    public void checkValid() {
        Assert.assertTrue(Cmg21UpsiParamsChecker.checkValid(cmg21UpsiParams));
    }
}
