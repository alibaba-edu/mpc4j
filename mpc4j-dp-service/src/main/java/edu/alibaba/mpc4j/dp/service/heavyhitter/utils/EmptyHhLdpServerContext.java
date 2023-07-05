package edu.alibaba.mpc4j.dp.service.heavyhitter.utils;

/**
 * The empty server context for Heavy Hitter with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class EmptyHhLdpServerContext implements HhLdpServerContext {

    @Override
    public byte[] toClientInfo() {
        return new byte[0];
    }
}
