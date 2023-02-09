package edu.alibaba.mpc4j.dp.service.heavyhitter;

/**
 * Heavy Hitter LDP server state.
 *
 * @author Weiran Liu
 * @date 2022/11/21
 */
public enum HhLdpServerState {
    /**
     * warm-up state
     */
    WARMUP,
    /**
     * statistics state
     */
    STATISTICS,
}
