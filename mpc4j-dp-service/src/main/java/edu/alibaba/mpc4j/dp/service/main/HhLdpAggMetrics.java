package edu.alibaba.mpc4j.dp.service.main;

/**
 * Aggregate heavy hitter metrics.
 *
 * @author Weiran Liu
 * @date 2023/2/2
 */
public class HhLdpAggMetrics {
    /**
     * the type string
     */
    private final String typeString;
    /**
     * ε_w
     */
    private final Double windowEpsilon;
    /**
     * α
     */
    private final Double alpha;
    /**
     * γ_h
     */
    private final Double gammaH;
    /**
     * round
     */
    private int round;
    /**
     * server time (ms)
     */
    private double serverTimeMs;
    /**
     * client time (ms)
     */
    private double clientTimeMs;
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

    public HhLdpAggMetrics(String typeString, Double windowEpsilon, Double alpha, Double gammaH) {
        this.typeString = typeString;
        this.windowEpsilon = windowEpsilon;
        this.alpha = alpha;
        this.gammaH = gammaH;
    }

    public void addMetrics(HhLdpMetrics metrics) {
        round++;
        serverTimeMs += metrics.getServerTimeMs();
        clientTimeMs += metrics.getClientTimeMs();
        payloadBytes += metrics.getPayloadBytes();
        contextBytes += metrics.getContextBytes();
        memoryBytes += metrics.getMemoryBytes();
        warmupNdcg += metrics.getWarmupNdcg();
        warmupPrecision += metrics.getWarmupPrecision();
        ndcg += metrics.getNdcg();
        precision += metrics.getPrecision();
        abe += metrics.getAbe();
        re += metrics.getRe();
    }

    public String getTypeString() {
        return typeString;
    }

    public String getWindowEpsilonString() {
        return windowEpsilon == null ? "-" : String.valueOf(windowEpsilon);
    }

    public String getAlphaString() {
        return alpha == null ? "-" : String.valueOf(alpha);
    }

    public String getGammaString() {
        return gammaH == null ? "-" : HhLdpMain.DOUBLE_DECIMAL_FORMAT.format(gammaH);
    }

    public double getServerTimeSecond() {
        double averageTimeMs = Math.round(serverTimeMs / round);
        return averageTimeMs / 1000;
    }

    public double getClientTimeSecond() {
        double averageTimeMs = Math.round(clientTimeMs / round);
        return averageTimeMs / 1000;
    }

    public long getPayloadBytes() {
        return payloadBytes / round;
    }

    public long getContextBytes() {
        return contextBytes / round;
    }

    public long getMemoryBytes() {
        return memoryBytes / round;
    }

    public double getWarmupNdcg() {
        return (double) Math.round(warmupNdcg / round * 10000) / 10000;
    }

    public double getWarmupPrecision() {
        return (double) Math.round(warmupPrecision / round * 10000) / 10000;
    }

    public double getNdcg() {
        return (double) Math.round(ndcg / round * 10000) / 10000;
    }

    public double getPrecision() {
        return (double) Math.round(precision / round * 10000) / 10000;
    }

    public double getAbe() {
        return (double) Math.round(abe / round * 10000) / 10000;
    }

    public double getRe() {
        return (double) Math.round(re / round * 10000) / 10000;
    }
}
