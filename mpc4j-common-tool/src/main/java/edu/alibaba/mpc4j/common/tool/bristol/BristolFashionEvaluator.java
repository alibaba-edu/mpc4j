package edu.alibaba.mpc4j.common.tool.bristol;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.io.*;
import java.util.stream.IntStream;

/**
 * Bristol Fashion circuit evaluator. Bristol Fashion is a circuit format shown in
 * <a href="https://nigelsmart.github.io/MPC-Circuits/">the blog post written by Nigel Smart</a>. The evaluator is
 * initialized with a Bristol Fashion circuit and can evaluate the circuit with the given input. The evaluator supports
 * MAND gate.
 * <p>
 * The implementation comes from
 * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py">lowmc.py</a>.
 *
 * @author Weiran Liu
 * @date 2025/4/7
 */
public class BristolFashionEvaluator {
    /**
     * number of gates in the circuit
     */
    private final int gateNum;
    /**
     * number of wires in the circuit
     */
    private final int wireNum;
    /**
     * number of input values
     */
    private final int inputValueNum;
    /**
     * number of input wires per input value
     */
    private final int[] inputWireNums;
    /**
     * number of output values
     */
    private final int outputValueNum;
    /**
     * number of input wires per output value
     */
    private final int[] outputWireNums;
    /**
     * Number input wires (1 or 2, unless a MAND gate)
     */
    private final int[] gateInputWireNums;
    /**
     * List of input wires
     */
    private final int[][] gateInputWireLists;
    /**
     * List of output wires
     */
    private final int[][] gateOutputWireLists;
    /**
     * Gate operations
     */
    private final GateOperation[] gateOperations;

    public BristolFashionEvaluator(InputStream inputStream) {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            // A line defining the number of gates and then the number of wires in the circuit.
            String gateWireNumLine = bufferedReader.readLine();
            String[] splitGateWireNumLines = gateWireNumLine.split(" ");
            MathPreconditions.checkEqual(
                "split_gate_wire_num_lines_num", "2",
                splitGateWireNumLines.length, 2
            );
            gateNum = Integer.parseInt(splitGateWireNumLines[0]);
            wireNum = Integer.parseInt(splitGateWireNumLines[1]);
            // The number of input values niv, Then niv numbers defining the number of input wires per input value.
            String inputLine = bufferedReader.readLine();
            String[] splitInputLines = inputLine.split(" ");
            inputValueNum = Integer.parseInt(splitInputLines[0]);
            MathPreconditions.checkEqual(
                "split_input_line_num", String.valueOf(inputValueNum + 1),
                splitInputLines.length, inputValueNum + 1
            );
            inputWireNums = IntStream.range(1, inputValueNum + 1)
                .mapToObj(i -> Integer.parseInt(splitInputLines[i]))
                .mapToInt(Integer::intValue)
                .toArray();
            // The number of output values nov, Then nov numbers defining the number of input wires per output value.
            String outputLine = bufferedReader.readLine();
            String[] splitOutputLines = outputLine.split(" ");
            outputValueNum = Integer.parseInt(splitOutputLines[0]);
            MathPreconditions.checkEqual(
                "split_output_line_num", String.valueOf(outputValueNum + 1),
                splitOutputLines.length, outputValueNum + 1
            );
            outputWireNums = IntStream.range(1, outputValueNum + 1)
                .mapToObj(i -> Integer.parseInt(splitOutputLines[i]))
                .mapToInt(Integer::intValue)
                .toArray();
            // Skip an empty line.
            String emptyLine = bufferedReader.readLine();
            Preconditions.checkArgument(emptyLine.isEmpty());
            // gates
            gateInputWireNums = new int[gateNum];
            gateInputWireLists = new int[gateNum][];
            gateOutputWireLists = new int[gateNum][];
            gateOperations = new GateOperation[gateNum];
            int gateIndex = 0;
            while (true) {
                String gateLine = bufferedReader.readLine();
                if (gateLine == null) {
                    // we read the end of line
                    MathPreconditions.checkEqual("gate_index", "gate_num", gateIndex, gateNum);
                    break;
                }
                if (gateLine.isEmpty()) {
                    // we read an empty line
                    MathPreconditions.checkEqual("gate_index", "gate_num", gateIndex, gateNum);
                    break;
                }
                // read gate
                String[] splitGateLines = gateLine.split(" ");
                gateInputWireNums[gateIndex] = Integer.parseInt(splitGateLines[0]);
                int outputWireNum = Integer.parseInt(splitGateLines[1]);
                MathPreconditions.checkEqual(
                    "split_gate_line_num", String.valueOf(gateInputWireNums[gateIndex] + outputWireNum + 3),
                    splitGateLines.length, gateInputWireNums[gateIndex] + outputWireNum + 3
                );
                gateOperations[gateIndex] = GateOperation.valueOf(splitGateLines[splitGateLines.length - 1]);
                switch (gateOperations[gateIndex]) {
                    case INV, NOT, EQ, EQW -> {
                        // note that for EQ, we sightly abuse input wires since it is not a wire but a value.
                        MathPreconditions.checkEqual("split_gate_line_num", "5", splitGateLines.length, 5);
                        MathPreconditions.checkEqual("input_wire_num", "1", gateInputWireNums[gateIndex], 1);
                        MathPreconditions.checkEqual("output_wire_num", "1", outputWireNum, 1);
                        gateInputWireLists[gateIndex] = new int[]{Integer.parseInt(splitGateLines[2])};
                        gateOutputWireLists[gateIndex] = new int[]{Integer.parseInt(splitGateLines[3])};
                    }
                    case AND, XOR -> {
                        MathPreconditions.checkEqual("split_gate_line_num", "6", splitGateLines.length, 6);
                        MathPreconditions.checkEqual("input_wire_num", "2", gateInputWireNums[gateIndex], 2);
                        MathPreconditions.checkEqual("output_wire_num", "1", outputWireNum, 1);
                        gateInputWireLists[gateIndex] = new int[]{Integer.parseInt(splitGateLines[2]), Integer.parseInt(splitGateLines[3])};
                        gateOutputWireLists[gateIndex] = new int[]{Integer.parseInt(splitGateLines[4])};
                    }
                    case MAND -> {
                        gateInputWireLists[gateIndex] = IntStream.range(2, 2 + gateInputWireNums[gateIndex])
                            .mapToObj(i -> Integer.parseInt(splitGateLines[i]))
                            .mapToInt(Integer::intValue)
                            .toArray();
                        gateOutputWireLists[gateIndex] = IntStream.range(2 + gateInputWireNums[gateIndex], 2 + gateInputWireNums[gateIndex] + outputWireNum)
                            .mapToObj(i -> Integer.parseInt(splitGateLines[i]))
                            .mapToInt(Integer::intValue)
                            .toArray();
                    }
                }
                gateIndex++;
            }
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to read Bristol Fashion input stream.");
        }
    }

    /**
     * Evaluate the circuit with the given inputs. We provide this API since some circuits have 1 input and 1 output.
     *
     * @param input input.
     * @return output.
     */
    public boolean[] evaluate(boolean[] input) {
        return evaluate(new boolean[][]{input})[0];
    }

    /**
     * Evaluate the circuit with the given inputs. We provide this API since most circuits have 2 inputs and 1 output.
     *
     * @param input1 1st input.
     * @param input2 2nd input.
     * @return output.
     */
    public boolean[] evaluate(boolean[] input1, boolean[] input2) {
        return evaluate(new boolean[][]{input1, input2})[0];
    }

    /**
     * Evaluate the circuit with the given inputs.
     *
     * @param inputs inputs.
     * @return outputs.
     */
    public boolean[][] evaluate(boolean[][] inputs) {
        // verify inputs
        MathPreconditions.checkEqual("inputs_num", String.valueOf(inputValueNum), inputs.length, inputValueNum);
        for (int i = 0; i < inputValueNum; i++) {
            MathPreconditions.checkEqual("input_wire_num", String.valueOf(inputWireNums[i]), inputs[i].length, inputWireNums[i]);
        }
        boolean[] wires = new boolean[wireNum];
        // set input wires
        int inputOffset = 0;
        for (int i = 0; i < inputValueNum; i++) {
            System.arraycopy(inputs[i], 0, wires, inputOffset, inputWireNums[i]);
            inputOffset += inputWireNums[i];
        }
        // evaluate gates
        for (int gateIndex = 0; gateIndex < gateNum; gateIndex++) {
            GateOperation gateOperation = gateOperations[gateIndex];
            switch (gateOperation) {
                case INV, NOT, EQ, EQW -> {
                    boolean input = wires[gateInputWireLists[gateIndex][0]];
                    switch (gateOperation) {
                        case INV, NOT -> wires[gateOutputWireLists[gateIndex][0]] = !input;
                        case EQW -> wires[gateOutputWireLists[gateIndex][0]] = input;
                        case EQ -> {
                            // EQ is a little bit different, the input is not a wire, but a value.
                            boolean value = (gateInputWireLists[gateIndex][0] == 1);
                            wires[gateOutputWireLists[gateIndex][0]] = value;
                        }
                    }
                }
                case AND, XOR -> {
                    boolean input1 = wires[gateInputWireLists[gateIndex][0]];
                    boolean input2 = wires[gateInputWireLists[gateIndex][1]];
                    switch (gateOperation) {
                        case AND -> wires[gateOutputWireLists[gateIndex][0]] = input1 & input2;
                        case XOR -> wires[gateOutputWireLists[gateIndex][0]] = input1 ^ input2;
                    }
                }
                case MAND -> {
                    int num = gateInputWireNums[gateIndex];
                    for (int j = 0; j < num / 2; j++) {
                        boolean input1 = wires[gateInputWireLists[gateIndex][j]];
                        boolean input2 = wires[gateInputWireLists[gateIndex][j + num / 2]];
                        wires[gateOutputWireLists[gateIndex][j]] = input1 & input2;
                    }
                }
            }
        }
        // set output wires
        boolean[][] outputs = new boolean[outputValueNum][];
        int outputOffset = wireNum;
        for (int i = 0; i < outputValueNum; i++) {
            outputOffset -= outputWireNums[i];
        }
        for (int i = 0; i < outputValueNum; i++) {
            outputs[i] = new boolean[outputWireNums[i]];
            System.arraycopy(wires, outputOffset, outputs[i], 0, outputWireNums[i]);
            outputOffset += outputWireNums[i];
        }
        return outputs;
    }
}
