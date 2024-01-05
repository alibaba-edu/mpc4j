package edu.alibaba.mpc4j.common.structure.lpn;

import org.junit.Assert;
import org.junit.Test;

/**
 * LPN参数检查器测试。各个攻击的测试数据来自于下述论文表1，但实际计算结果与论文给出的结果有非常微小的出入，这里应用本机实际计算结果。
 * Boyle, Elette, Geoffroy Couteau, Niv Gilboa, and Yuval Ishai. Compressing vector OLE. CCS 2018, pp. 896-912. 2018.
 *
 * 论文中给出的参数计算方法几乎是暴力求解，论文5.1节描述：
 * Setting λ = log_2|F| = 128, the optimal seed size is obtained by solving a 2-dimensional optimization problem over
 * the integers, with constraints 0 ≤ k ≤ n, 0 ≤ t ≤ n, and the constraints given by the requirement that the low-weight
 * parity check attack, the Gaussian elimination attack, and the ISD attack, all require at least 2^{80}. This is a
 * highly non-convex constrained optimization problem, with a very large number of local minima, making the estimation
 * of the global minimum relatively complex. We used extensive numerical analysis to compute (close to) minimal seed
 * sizes offering 80 bits of security against each of the attacks.
 *
 * 128比特安全性下LPN参数验证的测试数据来自于下述论文表2：
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public class LpnParamsCheckerTest {

    @Test
    public void testIsdCost() {
        // n = 2^10, t = 57, k = 652，论文值115，实际值111
        Assert.assertEquals(111, LpnParamsChecker.isdCost(1 << 10, 652, 57));
        // n = 2^12，t = 198，k = 1589，论文值104，实际值102
        Assert.assertEquals(102, LpnParamsChecker.isdCost(1 << 12, 1589, 98));
        // n = 2^14，t = 198，k = 3482，论文值108，实际值106
        Assert.assertEquals(106, LpnParamsChecker.isdCost(1 << 14, 3482, 198));
        // n = 2^16，t = 389，k = 7391，论文值112，实际值111
        Assert.assertEquals(111, LpnParamsChecker.isdCost(1 << 16, 7391, 389));
        // n = 2^18，t = 760，k = 15336，论文值117，实际值116
        Assert.assertEquals(116, LpnParamsChecker.isdCost(1 << 18, 15336, 760));
        // n = 2^20，t = 1419，k = 32771，论文值121，实际值120
        Assert.assertEquals(120, LpnParamsChecker.isdCost(1 << 20, 32771, 1419));
        // n = 2^22，t = 2735，k = 67440，论文值126，实际值125
        Assert.assertEquals(125, LpnParamsChecker.isdCost(1 << 22, 67440, 2735));
    }

    @Test
    public void testGaussianCost() {
        // n = 2^10, t = 57, k = 652，论文值80，实际值80
        Assert.assertEquals(80, LpnParamsChecker.gaussianCost(1 << 10, 652, 57));
        // n = 2^12，t = 198，k = 1589，论文值85，实际值85
        Assert.assertEquals(85, LpnParamsChecker.gaussianCost(1 << 12, 1589, 98));
        // n = 2^14，t = 198，k = 3482，论文值94，实际值94
        Assert.assertEquals(94, LpnParamsChecker.gaussianCost(1 << 14, 3482, 198));
        // n = 2^16，t = 389，k = 7391，论文值99，实际值99
        Assert.assertEquals(99, LpnParamsChecker.gaussianCost(1 << 16, 7391, 389));
        // n = 2^18，t = 760，k = 15336，论文值103，实际值103
        Assert.assertEquals(103, LpnParamsChecker.gaussianCost(1 << 18, 15336, 760));
        // n = 2^20，t = 1419，k = 32771，论文值106，实际值106
        Assert.assertEquals(106, LpnParamsChecker.gaussianCost(1 << 20, 32771, 1419));
        // n = 2^22，t = 2735，k = 67440，论文值108，实际值108
        Assert.assertEquals(108, LpnParamsChecker.gaussianCost(1 << 22, 67440, 2735));
    }

    @Test
    public void testParityCheckCost() {
        // n = 2^10, t = 57, k = 652，论文值93，实际值92
        Assert.assertEquals(92, LpnParamsChecker.parityCheckCost(1 << 10, 652, 57));
        // n = 2^12，t = 198，k = 1589，论文值80，实际值80
        Assert.assertEquals(80, LpnParamsChecker.parityCheckCost(1 << 12, 1589, 98));
        // n = 2^14，t = 198，k = 3482，论文值80，实际值80
        Assert.assertEquals(80, LpnParamsChecker.parityCheckCost(1 << 14, 3482, 198));
        // n = 2^16，t = 389，k = 7391，论文值80，实际值80
        Assert.assertEquals(80, LpnParamsChecker.parityCheckCost(1 << 16, 7391, 389));
        // n = 2^18，t = 760，k = 15336，论文值80，实际值80
        Assert.assertEquals(80, LpnParamsChecker.parityCheckCost(1 << 18, 15336, 760));
        // n = 2^20，t = 1419，k = 32771，论文值80，实际值80
        Assert.assertEquals(80, LpnParamsChecker.parityCheckCost(1 << 20, 32771, 1419));
        // n = 2^22，t = 2735，k = 67440，论文值80，实际值80
        Assert.assertEquals(80, LpnParamsChecker.parityCheckCost(1 << 22, 67440, 2735));
    }

    @Test
    public void testValidLpnParams() {
        // Ferret-Uni，one-time setup
        LpnParams.create(616092, 37248, 1254);
        // Ferret-Uni, Main Iteration
        LpnParams.create(10616092, 588160, 1324);
        // Ferret-Reg, one-time setup，论文值n1 = 609728，实际计算后，当n1 = 607037才可满足要求
        LpnParams.create(607037, 36288, 1269);
        // Ferret-Reg, Main Iteration，论文值n1 = 10805248，实际计算后，当n1 = 10609760才可满足要求
        LpnParams.create(10609760, 589760, 1319);
    }
}
