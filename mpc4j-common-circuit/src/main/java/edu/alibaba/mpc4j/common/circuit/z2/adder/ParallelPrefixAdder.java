package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixNode;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixOp;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTree;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory.PrefixTreeTypes;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Abstract Parallel Prefix Adder.
 * Parallel prefix adders are arguably the most commonly used arithmetic units in circuit design and have been extensively investigated in literature.
 * They are easy to pipeline and (part of them) enjoy lower circuit depth (compared with other adders), which are attracting to be used in MPC situation.
 * <p>
 * A taxonomy of parallel prefix adder can be found in following paper:
 * <p>
 * Harris, David. "A taxonomy of parallel prefix networks." The Thrity-Seventh Asilomar Conference on Signals, Systems & Computers, 2003. Vol. 2. IEEE, 2003.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class ParallelPrefixAdder extends AbstractAdder implements PrefixOp {
    /**
     * Prefix sum tree used for addition.
     */
    PrefixTree prefixTree;
    /**
     * the (original) propagate bits, which are used in sum-out bits generation.
     */
    private MpcZ2Vector[] ps;
    /**
     * The tuples consist of generate bits and propagate bits, which are used in prefix sum computation.
     */
    protected Tuple[] tuples;

    public ParallelPrefixAdder(MpcZ2cParty party, PrefixTreeTypes type) {
        super(party);
        this.prefixTree = PrefixTreeFactory.createPrefixSumTree(type, this);
    }

    /**
     * The tuple consists of p and g bits, which are used in prefix network computation.
     */
    protected static class Tuple implements PrefixNode {
        /**
         * the generate bit.
         */
        private final MpcZ2Vector g;
        /**
         * the propagate bit.
         */
        private final MpcZ2Vector p;

        protected Tuple(MpcZ2Vector g, MpcZ2Vector p) {
            this.g = g;
            this.p = p;
        }

        public MpcZ2Vector getG() {
            return g;
        }

        public MpcZ2Vector getP() {
            return p;
        }
    }

    @Override
    public MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, MpcZ2Vector cin)
        throws MpcAbortException {
        checkInputs(xiArray, yiArray);

        // 1. pre-computation of (g, p) tuples.
        genTuples(xiArray, yiArray);
        // 2. prefix computation using a prefix network
        prefixTree.addPrefix(l);
        // 3. carry-outs generation, where c_i = (P_i · cin) + Gi
        MpcZ2Vector[] c = genCarryOuts(cin);
        // 4. output sum bits generation, where s_i = c_i ⊕ p_{i-1}
        return genSumOuts(c, cin);
    }

    /**
     * Generates (p,g) tuples.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void genTuples(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        MpcZ2Vector[] gs = party.and(xiArray, yiArray);
        this.ps = party.xor(xiArray, yiArray);
        this.tuples = IntStream.range(0, l)
            .mapToObj(i -> new Tuple(gs[i], ps[i])).toArray(Tuple[]::new);
    }

    /**
     * Generates the carry_out bits.
     *
     * @param cin carry_in.
     * @return carry_outs.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private MpcZ2Vector[] genCarryOuts(MpcZ2Vector cin) throws MpcAbortException {
        MpcZ2Vector[] gs = Arrays.stream(tuples).map(Tuple::getG).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] ps = Arrays.stream(tuples).map(Tuple::getP).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] cins = IntStream.range(0, tuples.length).mapToObj(i -> cin).toArray(MpcZ2Vector[]::new);
        return party.xor(gs, party.and(ps, cins));
    }

    /**
     * Generates the sum output bits.
     *
     * @param c   carry_outs
     * @param cin carry_in
     * @return sum output bits
     * @throws MpcAbortException the protocol failure aborts.
     */
    private MpcZ2Vector[] genSumOuts(MpcZ2Vector[] c, MpcZ2Vector cin) throws MpcAbortException {
        MpcZ2Vector[] s = new MpcZ2Vector[l + 1];
        for (int i = l; i >= 0; i--) {
            if (i == l) {
                s[i] = party.xor(ps[i - 1], cin);
                continue;
            }
            if (i == 0) {
                s[i] = c[i];
                continue;
            }
            s[i] = party.xor(ps[i - 1], c[i]);
        }
        return s;
    }

    /**
     * Basic prefix-sum operation of parallel prefix adder, which is associative
     * and is able to be organized as parallel structure.
     *
     * @param input1 input tuple.
     * @param input2 input tuple.
     * @return output tuple.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected Tuple op(Tuple input1, Tuple input2) throws MpcAbortException {
        MpcZ2Vector gOut = party.or(input1.getG(), party.and(input1.getP(), input2.getG()));
        MpcZ2Vector pOut = party.and(input1.getP(), input2.getP());
        return new Tuple(gOut, pOut);
    }

    public static long NODE_NUM = 0;

    /**
     * Basic prefix-sum operation of parallel prefix adder in vector form, which is associative
     * and is able to be organized as parallel structure.
     *
     * @param inputs1 input tuples.
     * @param inputs2 input tuples.
     * @return output tuples.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected Tuple[] vectorOp(Tuple[] inputs1, Tuple[] inputs2) throws MpcAbortException {
        MathPreconditions.checkEqual("inputs1.num", "inputs2.num", inputs1.length, inputs2.length);

        MpcZ2Vector[] g1s = Arrays.stream(inputs1).map(Tuple::getG).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] g2s = Arrays.stream(inputs2).map(Tuple::getG).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] p1s = Arrays.stream(inputs1).map(Tuple::getP).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] p2s = Arrays.stream(inputs2).map(Tuple::getP).toArray(MpcZ2Vector[]::new);

        MpcZ2Vector[] gOuts = party.xor(g1s, party.and(p1s, g2s));
        MpcZ2Vector[] pOuts = party.and(p1s, p2s);
        NODE_NUM += (long) p1s[0].getNum() * p1s.length * 2;

        return IntStream.range(0, inputs1.length).mapToObj(i -> new Tuple(gOuts[i], pOuts[i])).toArray(Tuple[]::new);
    }

    @Override
    public Tuple[] getPrefixSumNodes() {
        return tuples;
    }

    @Override
    public void operateAndUpdate(PrefixNode[] x, PrefixNode[] y, int[] outputIndexes) throws MpcAbortException {
        Tuple[] xTuples = Arrays.stream(x).map(v -> (Tuple) v).toArray(Tuple[]::new);
        Tuple[] yTuples = Arrays.stream(y).map(v -> (Tuple) v).toArray(Tuple[]::new);
        Tuple[] result = vectorOp(xTuples, yTuples);
        // update nodes.
        IntStream.range(0, x.length).forEach(i -> tuples[outputIndexes[i]] = result[i]);
    }
}
