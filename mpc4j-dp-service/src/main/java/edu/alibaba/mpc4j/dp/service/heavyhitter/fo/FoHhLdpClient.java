package edu.alibaba.mpc4j.dp.service.heavyhitter.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.FoHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.EmptyHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.tool.Domain;

import java.util.Random;

/**
 * Abstract Heavy Hitter LDP client based on Frequency Oracle.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class FoHhLdpClient implements HhLdpClient {
    /**
     * the type
     */
    private final HhLdpFactory.HhLdpType type;
    /**
     * the domain
     */
    protected final Domain domain;
    /**
     * the number of heavy hitters k
     */
    protected final int k;
    /**
     * Frequency Oracle LDP client
     */
    private final FoLdpClient foLdpClient;

    public FoHhLdpClient(HhLdpConfig config) {
        FoHhLdpConfig foHhLdpConfig = (FoHhLdpConfig) config;
        type = foHhLdpConfig.getType();
        k = foHhLdpConfig.getK();
        FoLdpConfig foLdpConfig = foHhLdpConfig.getFoLdpConfig();
        domain = foLdpConfig.getDomain();
        foLdpClient = FoLdpFactory.createClient(foLdpConfig);
    }

    @Override
    public byte[] warmup(String item) {
        Preconditions.checkArgument(domain.contains(item), "%s is not in the domain", item);
        return item.getBytes(HhLdpFactory.DEFAULT_CHARSET);
    }

    @Override
    public byte[] randomize(HhLdpServerContext serverContext, String item, Random random) {
        Preconditions.checkArgument(serverContext instanceof EmptyHhLdpServerContext);
        return foLdpClient.randomize(item, random);
    }

    @Override
    public HhLdpFactory.HhLdpType getType() {
        return type;
    }

    @Override
    public int getD() {
        return foLdpClient.getD();
    }

    @Override
    public int getK() {
        return k;
    }

    @Override
    public double getWindowEpsilon() {
        return foLdpClient.getEpsilon();
    }
}
