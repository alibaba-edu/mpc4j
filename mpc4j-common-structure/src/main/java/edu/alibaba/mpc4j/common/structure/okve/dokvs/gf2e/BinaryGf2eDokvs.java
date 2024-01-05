package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

/**
 * Binary DOKVS. Decode algorithm in binary DOKVS can be simply written as y = &lt;v(x), D&gt;, where v(x) is the binary
 * position, D is the DOKVS.
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
public interface BinaryGf2eDokvs<T> extends Gf2eDokvs<T> {
    /**
     * Gets the binary positions for the given key. All positions are in range [0, m). The positions is distinct.
     *
     * @param key the key.
     * @return the binary positions.
     */
    int[] positions(T key);

    /**
     * Gets the maximal position num.
     *
     * @return the maximal position num.
     */
    int maxPositionNum();
}
