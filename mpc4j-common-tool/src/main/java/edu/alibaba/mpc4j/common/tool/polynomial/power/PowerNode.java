package edu.alibaba.mpc4j.common.tool.polynomial.power;

/**
 * 幂次方节点。存储计算高幂次方的路径，参考下述开源代码完成的实现。
 * <p>
 * https://github.com/microsoft/APSI/blob/main/common/apsi/powers.h
 * </p>
 *
 * @author Liqiang Peng
 * @date 2022/8/3
 */
public class PowerNode {
    /**
     * 幂次方
     */
    private final int power;
    /**
     * 深度
     */
    private final int depth;
    /**
     * 左幂次方
     */
    private int leftPower = 0;
    /**
     * 右幂次方
     */
    private int rightPower = 0;

    /**
     * 构造函数。
     *
     * @param power 幂次方。
     * @param depth 深度。
     */
    public PowerNode(int power, int depth) {
        this.power = power;
        this.depth = depth;
    }

    /**
     * 构造函数。
     *
     * @param power      幂次方。
     * @param depth      深度。
     * @param leftPower  左幂次方。
     * @param rightPower 右幂次方。
     */
    public PowerNode(int power, int depth, int leftPower, int rightPower) {
        this.power = power;
        this.depth = depth;
        this.leftPower = leftPower;
        this.rightPower = rightPower;
    }

    public int getDepth() {
        return depth;
    }

    public int getPower() {
        return power;
    }

    public int getLeftPower() {
        return leftPower;
    }

    public int getRightPower() {
        return rightPower;
    }

    public void setLeftPower(int leftPower) {
        this.leftPower = leftPower;
    }

    public void setRightPower(int rightPower) {
        this.rightPower = rightPower;
    }

    public int[] toIntArray() {
        return new int[]{leftPower, rightPower};
    }
}
