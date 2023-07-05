package edu.alibaba.mpc4j.dp.service.main;

/**
 * LDP heavy hitter metrics.
 *
 * @author Weiran Liu
 * @date 2023/2/2
 */
public class HhLdpMetrics {
    /**
     * server time (ms)
     */
    private long serverTimeMs;
    /**
     * client time (ms)
     */
    private long clientTimeMs;
    /**
     * payload bytes
     */
    private long payloadBytes;
    /**
     * context bytes
     */
    private long contextBytes;
    /**
     * memory bytes
     */
    private long memoryBytes;
    /**
     * warmup NDCG
     */
    private double warmupNdcg;
    /**
     * warmup precision
     */
    private double warmupPrecision;
    /**
     * NDCG
     */
    private double ndcg;
    /**
     * precision
     */
    private double precision;
    /**
     * absolute error
     */
    private double abe;
    /**
     * relative error
     */
    private double re;

    public long getServerTimeMs() {
        return serverTimeMs;
    }

    public void setServerTimeMs(long serverTimeMs) {
        this.serverTimeMs = serverTimeMs;
    }

    public long getClientTimeMs() {
        return clientTimeMs;
    }

    public void setClientTimeMs(long clientTimeMs) {
        this.clientTimeMs = clientTimeMs;
    }

    public long getPayloadBytes() {
        return payloadBytes;
    }

    public void setPayloadBytes(long payloadBytes) {
        this.payloadBytes = payloadBytes;
    }

    public long getContextBytes() {
        return contextBytes;
    }

    public void setContextBytes(long contextBytes) {
        this.contextBytes = contextBytes;
    }

    public long getMemoryBytes() {
        return memoryBytes;
    }

    public void setMemoryBytes(long memoryBytes) {
        this.memoryBytes = memoryBytes;
    }

    public double getWarmupNdcg() {
        return warmupNdcg;
    }

    public void setWarmupNdcg(double warmupNdcg) {
        this.warmupNdcg = warmupNdcg;
    }

    public double getWarmupPrecision() {
        return warmupPrecision;
    }

    public void setWarmupPrecision(double warmupPrecision) {
        this.warmupPrecision = warmupPrecision;
    }

    public double getNdcg() {
        return ndcg;
    }

    public void setNdcg(double ndcg) {
        this.ndcg = ndcg;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getAbe() {
        return abe;
    }

    public void setAbe(double abe) {
        this.abe = abe;
    }

    public double getRe() {
        return re;
    }

    public void setRe(double re) {
        this.re = re;
    }
}
