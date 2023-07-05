package edu.alibaba.mpc4j.dp.service.heavyhitter.utils;

/**
 * The server context for Heavy Hitter with Local Differential Privacy.
 * <p>
 * <li>The server context is generated from the server.</li>
 * <li>The server context is used in the client.</li>
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
public interface HhLdpServerContext {
    /**
     * Serialize parts of the context to byte array.
     *
     * @return parts of the context to byte array.
     */
    byte[] toClientInfo();
}
