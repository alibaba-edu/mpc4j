package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

/**
 * CST Random OSN protocol step.
 *
 * @author Weiran Liu
 * @date 2024/5/9
 */
enum CstRosnPtoStep {
    /**
     * receiver sends (δ^(1), ..., δ^(d - 1))
     */
    RECEIVER_SEND_BST_LITTLE_DELTA,
    /**
     * receiver sends mask input m = x + a^(1)
     */
    RECEIVER_SEND_BST_MASK_INPUT,
    /**
     * receiver sends mask output w
     */
    RECEIVER_SEND_BST_MASK_OUTPUT,
}
