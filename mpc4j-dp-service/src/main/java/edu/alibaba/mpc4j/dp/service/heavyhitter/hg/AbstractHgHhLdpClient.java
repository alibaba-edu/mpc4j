package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.BucketDomain;

/**
 * Abstract HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public abstract class AbstractHgHhLdpClient implements HgHhLdpClient {
    /**
     * the type
     */
    private final HhLdpFactory.HhLdpType type;
    /**
     * the bucket domain
     */
    protected final BucketDomain bucketDomain;
    /**
     * d = |Ω|
     */
    protected final int d;
    /**
     * the number of heavy hitters k
     */
    protected final int k;
    /**
     * budget num
     */
    protected final int w;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    protected final int lambdaH;
    /**
     * the private parameter ε / w
     */
    protected final double windowEpsilon;

    AbstractHgHhLdpClient(HhLdpConfig config) {
        HgHhLdpConfig hgHhLdpConfig = (HgHhLdpConfig) config;
        type = hgHhLdpConfig.getType();
        d = hgHhLdpConfig.getD();
        k = hgHhLdpConfig.getK();
        windowEpsilon = config.getWindowEpsilon();
        w = hgHhLdpConfig.getW();
        lambdaH = hgHhLdpConfig.getLambdaH();
        // init bucket domain
        bucketDomain = new BucketDomain(hgHhLdpConfig.getDomainSet(), w, lambdaH);
    }

    protected void checkItemInDomain(String item) {
        Preconditions.checkArgument(bucketDomain.contains(item), "%s is not in the domain", item);
    }

    @Override
    public byte[] warmup(String item) {
        checkItemInDomain(item);
        return item.getBytes(HhLdpFactory.DEFAULT_CHARSET);
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

    @Override
    public int getW() {
        return w;
    }

    @Override
    public int getLambdaH() {
        return lambdaH;
    }
}
