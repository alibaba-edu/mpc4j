package edu.alibaba.mpc4j.dp.service.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;

import java.util.Set;

/**
 * abstract Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/3/18
 */
public abstract class AbstractHhLdpClient implements HhLdpClient {
    /**
     * the type
     */
    private final HhLdpFactory.HhLdpType type;
    /**
     * d = |Ω|
     */
    protected final int d;
    /**
     * the number of heavy hitters k
     */
    protected final int k;
    /**
     * the private parameter ε / w
     */
    protected final double windowEpsilon;
    /**
     * the domain set
     */
    protected final Set<String> domainSet;

    public AbstractHhLdpClient(HhLdpConfig hhLdpConfig) {
        type = hhLdpConfig.getType();
        d = hhLdpConfig.getD();
        k = hhLdpConfig.getK();
        windowEpsilon = hhLdpConfig.getWindowEpsilon();
        domainSet = hhLdpConfig.getDomainSet();
    }

    @Override
    public byte[] warmup(String item) {
        checkItemInDomain(item);
        return item.getBytes(HhLdpFactory.DEFAULT_CHARSET);
    }

    protected void checkItemInDomain(String item) {
        Preconditions.checkArgument(domainSet.contains(item), "%s is not in the domain", item);
    }

    @Override
    public HhLdpFactory.HhLdpType getType() {
        return type;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public int getK() {
        return k;
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
    }
}
