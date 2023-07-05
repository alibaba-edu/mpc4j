package edu.alibaba.mpc4j.dp.service.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;

/**
 * abstract Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2023/3/18
 */
public abstract class AbstractHhLdpServer implements HhLdpServer {
    /**
     * the type
     */
    private final HhLdpFactory.HhLdpType type;
    /**
     * the state
     */
    protected HhLdpServerState hhLdpServerState;
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

    public AbstractHhLdpServer(HhLdpConfig hhLdpConfig) {
        type = hhLdpConfig.getType();
        d = hhLdpConfig.getD();
        k = hhLdpConfig.getK();
        windowEpsilon = hhLdpConfig.getWindowEpsilon();
        hhLdpServerState = HhLdpServerState.WARMUP;
    }

    protected void checkState(HhLdpServerState expect) {
        Preconditions.checkArgument(hhLdpServerState.equals(expect), "The state must be %s: %s", expect, hhLdpServerState);
    }

    @Override
    public HhLdpFactory.HhLdpType getType() {
        return type;
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public int getK() {
        return k;
    }
}
