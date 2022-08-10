package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

/**
 * 抽象基础N选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
public abstract class AbstractBnotSenderOutput implements BnotSenderOutput {
    /**
     * 最大选择值
     */
    protected int n;
    /**
     * 密钥组数量
     */
    protected int num;

    protected void init(int n, int num) {
        this.n = n;
        this.num = num;
    }

    protected void assertValidInput(int index, int choice) {
        assert index >= 0 && index < num : "index must be in range [0, " + num + "): " + index;
        assert choice >= 0 && choice < n : "choice must be in range [0, " + n + "): " + choice;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getNum() {
        return num;
    }
}
