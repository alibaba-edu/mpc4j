package edu.alibaba.mpc4j.common.tool.bristol;

/**
 * This abstract class provides necessary tools for constructing Bristol Fashion Boolean Matrix multiplication generator.
 *
 * @author Weiran Liu
 * @date 2025/4/9
 */
abstract class AbstractBristolBmmGenerator extends AbstractBristolFashionGenerator {

    AbstractBristolBmmGenerator() {
        super();
    }

    /**
     * Fills out the lookup table with the given input.
     * <p>
     * The implementation is inspired by
     *
     * @param lut   lookup table block.
     * @param input input block.
     */
    private void fillOutLookupTable(int[] lut, int[] input) {
        // the input has 4 wires
        assert widthOf(input) == BlockBmmLookupCode.WINDOW_BIT_SIZE;
        // the output (lookup table) has 16 wires
        assert widthOf(lut) == BlockBmmLookupCode.WINDOW_SIZE;

        // the lookup table wire l0 is a zero wire. Here we assume the zero wire is previously assigned.
        int lut_start = firstWire(lut);
        int input_start = firstWire(input);
        // We use this for loop to great lookup table wires l1 to l15, l0 is a zero wire.
        // l_i = l_{i - 1} ⊕ w_{ctz(i)}
        for (int i = 1; i < BlockBmmLookupCode.WINDOW_SIZE; i++) {
            xorWires(lut_start + i - 1, input_start + GrayCodeGenerator.ctz(i), lut_start + i);
        }
    }

    /**
     * Creates a Boolean matrix multiplication using the Four-Russian method.
     *
     * @param lookupCode block Boolean matrix multiplication lookup code.
     * @param input      input block.
     * @param lut        lookup table block.
     * @param output     output block.
     */
    protected void fourRussiansMatrixMult(BlockBmmLookupCode lookupCode, int[] input, int[] lut, int[] output) {
        // the input is a 128-bit vector
        assert widthOf(input) == BlockBmmLookupCode.BLOCK_BIT_SIZE;
        // the output is a 128-bit vector
        assert widthOf(output) == BlockBmmLookupCode.BLOCK_BIT_SIZE;
        // the lookup table has 16 wires
        assert widthOf(lut) == BlockBmmLookupCode.WINDOW_SIZE;

        for (int i = 0; i < BlockBmmLookupCode.WINDOW_NUM; i++) {
            // create lookup table for the input, 4 wires in each loop
            fillOutLookupTable(lut, subBlock(input, BlockBmmLookupCode.WINDOW_BIT_SIZE, i));
            for (int j = 0; j < BlockBmmLookupCode.BLOCK_BIT_SIZE; j++) {
                int outputWire = firstWire(output) + j;
                int inputWire1 = (i == 0) ? firstWire(lut) : outputWire;
                xorWires(inputWire1, firstWire(lut) + lookupCode.getCode(i, j), outputWire);
            }
        }
    }
}
