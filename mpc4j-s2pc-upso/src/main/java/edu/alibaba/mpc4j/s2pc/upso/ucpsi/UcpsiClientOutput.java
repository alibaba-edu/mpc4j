package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.ArrayList;

/**
 * Unbalanced Circuit PSI client output.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class UcpsiClientOutput<T> {
    /**
     * the table
     */
    private final ArrayList<T> table;
    /**
     * z1
     */
    private final SquareZ2Vector z1;

    public UcpsiClientOutput(ArrayList<T> table, SquareZ2Vector z1) {
        MathPreconditions.checkPositive("β", table.size());
        this.table = table;
        MathPreconditions.checkEqual("share bit length", "β", z1.getNum(), table.size());
        this.z1 = z1;
    }

    public int getBeta() {
        return table.size();
    }

    public ArrayList<T> getTable() {
        return table;
    }

    public SquareZ2Vector getZ1() {
        return z1;
    }
}
