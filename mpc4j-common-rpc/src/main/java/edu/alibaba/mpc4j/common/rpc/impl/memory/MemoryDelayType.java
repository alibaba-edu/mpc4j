package edu.alibaba.mpc4j.common.rpc.impl.memory;

import com.google.common.base.Preconditions;

/**
 * 内存通信延迟类型。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public enum MemoryDelayType {
    /**
     * 无延迟，无穷大带宽，无RTT时间
     */
    NO_DELAY(0.0, 0.0),
    /**
     * 局域网延迟，10Gbps带宽，RTT = 0.2ms
     */
    BANDWIDTH_10G_RTT_200_US(10.0 * (1 << 10), 0.2),
    /**
     * 1Gbps带宽，RTT = 40ms
     */
    BANDWIDTH_1G_RTT_40_MS(1 << 10, 40.0),
    /**
     * 150Mbps带宽，RTT = 40ms
     */
    BANDWIDTH_150M_RTT_40_MS(150, 40.0),
    /**
     * 100Mbps带宽，RTT = 40ms
     */
    BANDWIDTH_100M_RTT_40_MS(100, 40.0),
    /**
     * 50Mbps带宽，RTT = 40ms
     */
    BANDWIDTH_50M_RTT_40_MS(50, 40.0),
    /**
     * 10Mbps带宽，RTT = 40ms
     */
    BANDWIDTH_10M_RTT_40_MS(10, 40.0),
    /**
     * 1Mbps带宽，RTT = 40ms
     */
    BANDWIDTH_1M_RTT_40_MS(1, 40.0),
    /**
     * 广域网延迟，1Mbps带宽，RTT = 80ms
     */
    WAN(1.0, 80.0);
    /**
     * 带宽，单位为Mbps，0表示带宽无穷大
     */
    private final double bandWidth;
    /**
     * 往返时间（Round Trip Time），单位为ms，0表示无往返时间
     */
    private final double rtt;

    MemoryDelayType(double bandWidth, double rtt) {
        Preconditions.checkArgument(bandWidth >= 0);
        Preconditions.checkArgument(rtt >= 0);
        this.bandWidth = bandWidth;
        this.rtt = rtt;
    }

    /**
     * 返回带宽，单位为Mbps，0表示带宽无穷大。
     *
     * @return 带宽。
     */
    public double getBandWidth() {
        return this.bandWidth;
    }

    /**
     * 返回往返时间，单位为ms，0表示无往返时间。
     *
     * @return 往返时间。
     */
    public double getRtt() {
        return this.rtt;
    }

}
