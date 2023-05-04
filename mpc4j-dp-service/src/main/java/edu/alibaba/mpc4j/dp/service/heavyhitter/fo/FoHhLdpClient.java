package edu.alibaba.mpc4j.dp.service.heavyhitter.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.AbstractHhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.FoHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.EmptyHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;

import java.util.Random;

/**
 * Abstract Heavy Hitter LDP client based on Frequency Oracle.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class FoHhLdpClient extends AbstractHhLdpClient {
    /**
     * Frequency Oracle LDP client
     */
    private final FoLdpClient foLdpClient;

    public FoHhLdpClient(FoHhLdpConfig config) {
        super(config);
        FoLdpConfig foLdpConfig = config.getFoLdpConfig();
        foLdpClient = FoLdpFactory.createClient(foLdpConfig);
    }

    @Override
    public byte[] randomize(HhLdpServerContext serverContext, String item, Random random) {
        Preconditions.checkArgument(serverContext instanceof EmptyHhLdpServerContext);
        return foLdpClient.randomize(item, random);
    }
}
