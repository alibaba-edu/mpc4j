package edu.alibaba.mpc4j.common.tool.bristol;

import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.crypto.prp.JdkBytesLowMcPrp;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import org.bouncycastle.util.encoders.Hex;

import java.io.*;
import java.util.Objects;

/**
 * Bristol Fashion LowMC circuit generator based on files (identical with LowMC in package <code>prp</code>.
 *
 * @author Weiran Liu
 * @date 2025/4/9
 */
public class BristolFashionLowMcFileGenerator extends AbstractBristolFashionLowMcGenerator {
    /**
     * LowMC configuration file path
     */
    private static final String LOW_MC_RESOURCE_FILE_PATH = "low_mc/";
    /**
     * LowMC configuration file prefix
     */
    private static final String LOW_MC_FILE_PREFIX = "lowmc_128_128_";
    /**
     * LowMC configuration file suffix
     */
    private static final String LOW_MC_FILE_SUFFIX = ".txt";
    /**
     * linear transformation matrix prefix
     */
    private static final String LINEAR_MATRIX_PREFIX = "L_";
    /**
     * key expand matrix prefix
     */
    private static final String KEY_MATRIX_PREFIX = "K_";
    /**
     * constant prefix
     */
    private static final String CONSTANT_PREFIX = "C_";
    /**
     * size = 128
     */
    private static final int SIZE = BlockBmmLookupCode.BLOCK_BIT_SIZE;
    /**
     * byte size
     */
    private static final int BYTE_SIZE = BlockBmmLookupCode.BLOCK_BYTE_SIZE;
    /**
     * number of round
     */
    private final int round;
    /**
     * key matrix, (r + 1) number of 128 × 128 boolean matrix
     */
    private final ByteDenseBitMatrix[] keyMatrices;
    /**
     * linear transformation matrix, (r) number of 128 × 128 boolean matrix
     */
    private final ByteDenseBitMatrix[] linearMatrices;
    /**
     * constants, (r) number of 128-bit element
     */
    private final boolean[][] constants;

    /**
     * Creates a Bristol Fashion LowMC circuit generator.
     *
     * @param round round, must be in 20, 21, 23, 32, 192, 208, or 287.
     */
    public BristolFashionLowMcFileGenerator(int round) {
        assert round == 20 || round == 21 || round == 23 || round == 32 || round == 192 || round == 208 || round == 287;
        this.round = round;
        // open the configuration file
        String lowMcFileName = LOW_MC_RESOURCE_FILE_PATH + LOW_MC_FILE_PREFIX + round + LOW_MC_FILE_SUFFIX;
        try {
            InputStream lowMcInputStream = Objects.requireNonNull(
                JdkBytesLowMcPrp.class.getClassLoader().getResourceAsStream(lowMcFileName)
            );
            InputStreamReader lowMcInputStreamReader = new InputStreamReader(lowMcInputStream);
            BufferedReader lowMcBufferedReader = new BufferedReader(lowMcInputStreamReader);
            // load (r) number of linear transformation matrix
            linearMatrices = new ByteDenseBitMatrix[round];
            for (int roundIndex = 0; roundIndex < round; roundIndex++) {
                // flag line
                String label = lowMcBufferedReader.readLine();
                assert label.equals(LINEAR_MATRIX_PREFIX + roundIndex);
                // followed by BLOCK_BIT_LENGTH number of data
                byte[][] squareMatrix = new byte[SIZE][];
                for (int bitIndex = 0; bitIndex < SIZE; bitIndex++) {
                    String line = lowMcBufferedReader.readLine();
                    squareMatrix[bitIndex] = Hex.decode(line);
                    assert squareMatrix[bitIndex].length == BYTE_SIZE;
                }
                linearMatrices[roundIndex] = ByteDenseBitMatrix.createFromDense(SIZE, squareMatrix);
            }
            // load (r + 1) number of key expand matrix
            keyMatrices = new ByteDenseBitMatrix[round + 1];
            for (int roundIndex = 0; roundIndex < round + 1; roundIndex++) {
                // flag line
                String label = lowMcBufferedReader.readLine();
                assert label.equals(KEY_MATRIX_PREFIX + roundIndex);
                // followed by BLOCK_BIT_LENGTH number of data
                byte[][] squareMatrix = new byte[SIZE][];
                for (int bitIndex = 0; bitIndex < SIZE; bitIndex++) {
                    String line = lowMcBufferedReader.readLine();
                    squareMatrix[bitIndex] = Hex.decode(line);
                    assert squareMatrix[bitIndex].length == BYTE_SIZE;
                }
                keyMatrices[roundIndex] = ByteDenseBitMatrix.createFromDense(SIZE, squareMatrix);
            }
            // load (r) number of constant
            constants = new boolean[round][];
            for (int roundIndex = 0; roundIndex < round; roundIndex++) {
                // flag line
                String label = lowMcBufferedReader.readLine();
                assert label.equals(CONSTANT_PREFIX + roundIndex);
                // followed by a constant
                String line = lowMcBufferedReader.readLine();
                constants[roundIndex] = BinaryUtils.byteArrayToBinary(Hex.decode(line));
                assert constants[roundIndex].length == SIZE;
            }
            lowMcBufferedReader.close();
            lowMcInputStreamReader.close();
            lowMcInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to read LowMC configuration file" + lowMcFileName);
        }
    }

    /**
     * Generates the Bristol Fashion LowMC circuit.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L78">lowmc.py</a>.
     *
     * @param type Bristol Fashion type.
     * @param outputStream output stream.
     */
    public void generate(BristolFashionType type, OutputStream outputStream) throws IOException {
        int[] keyBlock = makeInputBlock(SIZE);
        int[] inputBlock = makeInputBlock(SIZE);
        int[] zeroBlock = makeInternalBlock(SIZE);
        int zeroWire = firstWire(makeInternalBlock(1));
        int oneWire = firstWire(makeInternalBlock(1));
        invWires(zeroWire, oneWire);
        int[] state = makeInternalBlock(SIZE);
        int[] tmpState = makeInternalBlock(SIZE);
        int[] lut = makeInternalBlock(BlockBmmLookupCode.WINDOW_SIZE);
        int[] expandedKeyBlock = makeInternalBlock(SIZE);

        // LowMC procedure
        xorGate(inputBlock, zeroBlock, state);
        BlockBmmLookupCode initKeyLookupTableCode = new BlockBmmLookupCode(keyMatrices[0].getByteArrayData());
        fourRussiansMatrixMult(initKeyLookupTableCode, keyBlock, lut, expandedKeyBlock);
        addRoundKey(state, expandedKeyBlock);

        for (int i = 0; i < round; i++) {
            putSboxLayer(type, 10, state, tmpState, zeroWire);
            BlockBmmLookupCode linearLookupTableCode = new BlockBmmLookupCode(linearMatrices[i].getByteArrayData());
            fourRussiansMatrixMult(linearLookupTableCode, tmpState, lut, state);
            xorConstants(state, constants[i], oneWire);
            BlockBmmLookupCode roundKeyLookupTableCode = new BlockBmmLookupCode(keyMatrices[i + 1].getByteArrayData());
            fourRussiansMatrixMult(roundKeyLookupTableCode, keyBlock, lut, expandedKeyBlock);
            addRoundKey(state, expandedKeyBlock);
        }

        int[] outputBlock = makeOutputBlock(SIZE);
        xorGate(zeroBlock, state, outputBlock);
        generate(outputStream);
    }
}
