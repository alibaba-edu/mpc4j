package edu.alibaba.mpc4j.common.tool.bristol;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Bristol Fashion Boolean Matrix Multiplication generator. This generator is only used for testing the correctness of
 * Boolean Matrix Multiplication circuits.
 *
 * @author Weiran Liu
 * @date 2025/4/9
 */
class BristolFashionBmmGenerator extends AbstractBristolBmmGenerator {

    public BristolFashionBmmGenerator() {
        super();
    }

    /**
     * Generates the Bristol Fashion circuit for multiplying the given Boolean matrix. The circuit has one input (with
     * 128 wires) and one output (with 128 wires).
     *
     * @param matrix       the Boolean matrix to be multiplied.
     * @param outputStream output stream.
     * @throws IOException if an I/O error occurs.
     */
    public void generate(byte[][] matrix, OutputStream outputStream) throws IOException {
        BlockBmmLookupCode lookupCode = new BlockBmmLookupCode(matrix);
        // set input wires
        int[] inputBlock = makeInputBlock(BlockBmmLookupCode.BLOCK_BIT_SIZE);
        // set state, we cannot use EQW so we use XOR with zero wires instead
        int[] zeroBlock = makeInternalBlock(BlockBmmLookupCode.BLOCK_BIT_SIZE);
        int[] inputState = makeInternalBlock(BlockBmmLookupCode.BLOCK_BIT_SIZE);
        int[] outputState = makeInternalBlock(BlockBmmLookupCode.BLOCK_BIT_SIZE);
        xorGate(inputBlock, zeroBlock, inputState);
        // set lookup table wires
        int[] lut = makeInternalBlock(BlockBmmLookupCode.WINDOW_SIZE);
        fourRussiansMatrixMult(lookupCode, inputState, lut, outputState);
        // set output wires
        int[] outputBlock = makeOutputBlock(BlockBmmLookupCode.BLOCK_BIT_SIZE);
        xorGate(zeroBlock, outputState, outputBlock);
        generate(outputStream);
    }
}
