package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric;

/**
 * 几何CDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
public class GeometricCdpFactory {
    /**
     * 私有构造函数
     */
    private GeometricCdpFactory() {
        // empty
    }

    /**
     * 构造几何CDP机制。
     *
     * @param geometricCdpConfig 配置项。
     * @return 几何CDP机制。
     */
    public static GeometricCdp createInstance(GeometricCdpConfig geometricCdpConfig) {
        if (geometricCdpConfig instanceof ApacheGeometricCdpConfig) {
            ApacheGeometricCdp apacheGeometricCdp = new ApacheGeometricCdp();
            apacheGeometricCdp.setup(geometricCdpConfig);
            return apacheGeometricCdp;
        } else if (geometricCdpConfig instanceof DiscreteGeometricCdpConfig) {
            DiscreteGeometricCdp discreteGeometricCdp = new DiscreteGeometricCdp();
            discreteGeometricCdp.setup(geometricCdpConfig);
            return discreteGeometricCdp;
        }
        throw new IllegalArgumentException("Invalid GeometricCdpConfig: " + geometricCdpConfig.getClass().getSimpleName());
    }
}
