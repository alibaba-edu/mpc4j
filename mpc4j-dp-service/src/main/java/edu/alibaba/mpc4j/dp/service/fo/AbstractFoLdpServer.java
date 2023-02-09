package edu.alibaba.mpc4j.dp.service.fo;

import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.Domain;

/**
 * Abstract Frequency Oracle LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/14
 */
public abstract class AbstractFoLdpServer implements FoLdpServer {
    /**
     * the type
     */
    private final FoLdpFactory.FoLdpType type;
    /**
     * the domain
     */
    protected final Domain domain;
    /**
     * d = |Ω|
     */
    protected final int d;
    /**
     * the private parameter ε
     */
    protected final double epsilon;
    /**
     * the number of inserted items
     */
    protected int num;

    public AbstractFoLdpServer(FoLdpConfig config) {
        type = config.getType();
        domain = config.getDomain();
        d = domain.getD();
        epsilon = config.getEpsilon();
        num = 0;
    }

    @Override
    public FoLdpFactory.FoLdpType getType() {
        return type;
    }

    @Override
    public double getEpsilon() {
        return epsilon;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public int getNum() {
        return num;
    }
}
