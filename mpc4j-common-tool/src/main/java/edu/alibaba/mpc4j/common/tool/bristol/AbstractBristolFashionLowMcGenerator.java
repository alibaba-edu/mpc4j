package edu.alibaba.mpc4j.common.tool.bristol;

import gnu.trove.list.array.TIntArrayList;

import java.util.stream.IntStream;

/**
 * abstract LowMC Bristol fashion circuit generator.
 *
 * @author Weiran Liu
 * @date 2025/4/9
 */
abstract class AbstractBristolFashionLowMcGenerator extends AbstractBristolBmmGenerator {

    AbstractBristolFashionLowMcGenerator() {
        super();
    }

    /**
     * Adds the round key to the state block.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L107">lowmc.py</a>.
     *
     * @param stateBlock    state block.
     * @param roundKeyBlock round key block.
     */
    protected void addRoundKey(int[] stateBlock, int[] roundKeyBlock) {
        assert widthOf(stateBlock) == BlockBmmLookupCode.BLOCK_BIT_SIZE;
        assert widthOf(roundKeyBlock) == BlockBmmLookupCode.BLOCK_BIT_SIZE;
        xorGate(stateBlock, roundKeyBlock, stateBlock);
    }

    /**
     * XORs the constants to the state block.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L110">lowmc.py</a>.
     *
     * @param stateBlock state block.
     * @param constant   constant.
     * @param oneWire    wire that is assigned as one, which we cannot explicitly verify.
     */
    protected void xorConstants(int[] stateBlock, boolean[] constant, int oneWire) {
        assert widthOf(stateBlock) == BlockBmmLookupCode.BLOCK_BIT_SIZE;
        assert constant.length == BlockBmmLookupCode.BLOCK_BIT_SIZE;

        for (int i = 0; i < BlockBmmLookupCode.BLOCK_BIT_SIZE; i++) {
            if (constant[i]) {
                xorWires(firstWire(stateBlock) + i, oneWire, firstWire(stateBlock) + i);
            }
        }
    }

    /**
     * Puts SBOX layer into the circuit.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L116">lowmc.py</a>.
     *
     * @param type    Bristol Fashion type.
     * @param nsboxes  number of sboxes.
     * @param input    input block.
     * @param output   output block.
     * @param zeroWire wire that is assigned as zero, which we cannot explicitly verify.
     */
    protected void putSboxLayer(BristolFashionType type, int nsboxes, int[] input, int[] output, int zeroWire) {
        assert widthOf(input) == BlockBmmLookupCode.BLOCK_BIT_SIZE;
        assert widthOf(output) == BlockBmmLookupCode.BLOCK_BIT_SIZE;
        // each sbox involves 3 wires, we need to ensure that 3 * nsboxes <= 128, so that sboxNum <= 128 / 3 = 42
        assert nsboxes > 0 && nsboxes <= BlockBmmLookupCode.BLOCK_BIT_SIZE / 3;

        int input_start = firstWire(input);
        int output_start = firstWire(output);
        TIntArrayList mand_inputs_1 = new TIntArrayList();
        TIntArrayList mand_inputs_2 = new TIntArrayList();
        TIntArrayList mand_outputs = new TIntArrayList(IntStream.range(output_start, output_start + 3 * nsboxes).toArray());

        for (int i = 0; i < nsboxes; i++) {
            int a = input_start + 3 * i;
            int b = input_start + 3 * i + 1;
            int c = input_start + 3 * i + 2;

            // a = a ⊕ (b ☉ c): BC + A
            mand_inputs_1.add(b);
            mand_inputs_2.add(c);
            // b = a ⊕ b ⊕ (a ☉ c): CA + A + B
            mand_inputs_1.add(c);
            mand_inputs_2.add(a);
            // c = a ⊕ b ⊕ c ⊕ (a ☉ b): AB + A + B + C
            mand_inputs_1.add(a);
            mand_inputs_2.add(b);
        }
        switch (type) {
            case BASIC -> andGate(mand_inputs_1, mand_inputs_2, mand_outputs);
            case EXTEND -> mandGate(mand_inputs_1, mand_inputs_2, mand_outputs);
        }

        for (int i = 0; i < nsboxes; i++) {
            int a = input_start + 3 * i;
            int b = input_start + 3 * i + 1;
            int c = input_start + 3 * i + 2;

            int d = output_start + 3 * i;
            int e = output_start + 3 * i + 1;
            int f = output_start + 3 * i + 2;

            // a = a ⊕ (b ☉ c): BC + A
            xorWires(d, a, d);
            // b = a ⊕ b ⊕ (a ☉ c): CA + A + B
            xorWires(e, a, e);
            xorWires(e, b, e);
            // c = a ⊕ b ⊕ c ⊕ (a ☉ b): AB + A + B + C
            xorWires(f, a, f);
            xorWires(f, b, f);
            xorWires(f, c, f);
        }

        for (int i = 3 * nsboxes; i < BlockBmmLookupCode.BLOCK_BIT_SIZE; i++) {
            xorWires(input_start + i, zeroWire, output_start + i);
        }
    }
}
