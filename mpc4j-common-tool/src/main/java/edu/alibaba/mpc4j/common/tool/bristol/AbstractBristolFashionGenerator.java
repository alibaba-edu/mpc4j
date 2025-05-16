package edu.alibaba.mpc4j.common.tool.bristol;

import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * abstract Bristol Fashion circuit generator.
 * <p>
 * Although the Bristol Fashion circuit supports many gate operations, most open-source library only supports XOR, AND,
 * MAND, and INV gates.
 * <p>
 * The implementation is mostly inspired by
 * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py">lowmc.py</a>.
 *
 * @author Weiran Liu
 * @date 2025/4/7
 */
abstract class AbstractBristolFashionGenerator {
    /**
     * number of used wires
     */
    private int usedWireNum;
    /**
     * input sizes. The size is the number of input values, and each element is the number of wires for that input.
     */
    private final TIntArrayList inputSizes;
    /**
     * flag indicating if the generator is setting inputs.
     */
    private boolean settingInputs;
    /**
     * output sizes. The size is the number of output values, and each element is the number of wires for that output.
     */
    private final TIntArrayList outputSizes;
    /**
     * flag indicating if the generator is setting outputs.
     */
    private boolean settingOutputs;
    /**
     * gates
     */
    private final ArrayList<String> gates;

    AbstractBristolFashionGenerator() {
        inputSizes = new TIntArrayList();
        outputSizes = new TIntArrayList();
        gates = new ArrayList<>();
        usedWireNum = 0;
        settingInputs = true;
        settingOutputs = false;
    }

    /**
     * Makes a block of wires with given width.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L16">lowmc.py</a>.
     *
     * @param width the width of the block.
     * @return block represented as [start, end) of the wire index.
     */
    private int[] makeBlock(int width) {
        assert width > 0;
        usedWireNum += width;
        return new int[]{usedWireNum - width, usedWireNum};
    }

    /**
     * Makes an input block of wires with given width.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L21">lowmc.py</a>.
     *
     * @param width the width of the input block.
     * @return input block represented as [start, end) of the wire index.
     */
    protected int[] makeInputBlock(int width) {
        assert width > 0;
        assert settingInputs : "Cannot make input block after setting internal blocks";
        assert !settingOutputs : "Cannot make input block after setting output blocks";
        inputSizes.add(width);
        return makeBlock(width);
    }

    /**
     * Makes an internal block of wires with given width.
     *
     * @param width the width of the internal block.
     * @return internal block represented as [start, end) of the wire index.
     */
    protected int[] makeInternalBlock(int width) {
        assert width > 0;
        if (settingInputs) {
            settingInputs = false;
        }
        assert !settingOutputs : "Cannot make internal block after setting output blocks";
        return makeBlock(width);
    }

    /**
     * Makes an output block of wires with given width.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L25">lowmc.py</a>.
     *
     * @param width the width of the output block.
     * @return output block represented as [start, end) of the wire index.
     */
    protected int[] makeOutputBlock(int width) {
        if (settingInputs) {
            settingInputs = false;
        }
        if (!settingOutputs) {
            settingOutputs = true;
        }
        outputSizes.add(width);
        return makeBlock(width);
    }

    /**
     * Checks if the given block is valid.
     *
     * @param block block represented as [start, end) of the wire index.
     * @return true if the block is valid, false otherwise.
     */
    private boolean validBlock(int[] block) {
        assert block.length == 2;
        return block[0] >= 0 && block[1] >= 0 && (block[1] - block[0] > 0);
    }

    /**
     * Checks if the given wire is valid.
     *
     * @param wire wire.
     * @return true if the wire is valid, false otherwise.
     */
    private boolean validWire(int wire) {
        return wire >= 0;
    }

    /**
     * Gets width of the block.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L29">lowmc.py</a>.
     *
     * @param block block represented as [start, end) of the wire index.
     * @return width.
     */
    protected int widthOf(int[] block) {
        assert validBlock(block);
        return block[1] - block[0];
    }

    /**
     * Gets the first wire (include) of the block.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L32">lowmc.py</a>.
     *
     * @param block block represented as [start, end) of the wire index.
     * @return first wire of the block.
     */
    protected int firstWire(int[] block) {
        assert validBlock(block);
        return block[0];
    }

    /**
     * Gets the block corresponding to the given wire.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L35">lowmc.py</a>.
     *
     * @param wire wire.
     * @return block corresponding to the given wire.
     */
    protected int[] oneWireBlock(int wire) {
        assert validWire(wire);
        return new int[]{wire, wire + 1};
    }

    /**
     * Creates an XOR gate with two input wires and one output wire.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L38">lowmc.py</a>.
     *
     * @param inputWire1 wire 1.
     * @param inputWire2 wire 2.
     * @param outputWire output wire.
     */
    protected void xorWires(int inputWire1, int inputWire2, int outputWire) {
        assert validWire(inputWire1);
        assert validWire(inputWire2);
        assert validWire(outputWire);
        gates.add("2 1 " + inputWire1 + " " + inputWire2 + " " + outputWire + " " + GateOperation.XOR.name());
    }

    /**
     * Creates an INV wire with one input wire and one output wire.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L45">lowmc.py</a>.
     *
     * @param inputWire  input wire.
     * @param outputWire output wire.
     */
    protected void invWires(int inputWire, int outputWire) {
        gates.add("1 1 " + inputWire + " " + outputWire + " " + GateOperation.INV.name());
    }

    /**
     * Creates an XOR gate with two input blocks and one output block.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L49">lowmc.py</a>.
     *
     * @param inputBlock1 input block 1.
     * @param inputBlock2 input block 2.
     * @param outputBlock output block.
     */
    protected void xorGate(int[] inputBlock1, int[] inputBlock2, int[] outputBlock) {
        int inputWire1 = firstWire(inputBlock1);
        int inputWire2 = firstWire(inputBlock2);
        int outputWire = firstWire(outputBlock);
        // two inputs and output should have the same width
        int width = widthOf(inputBlock1);
        assert width == widthOf(inputBlock2);
        assert width == widthOf(outputBlock);
        for (int i = 0; i < width; i++) {
            xorWires(inputWire1 + i, inputWire2 + i, outputWire + i);
        }
    }

    /**
     * Gets the sub-block of the given block.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L60">lowmc.py</a>.
     *
     * @param block         block.
     * @param subBlockSize  size of the sub-block.
     * @param subBlockIndex index of the sub-block.
     * @return sub-block.
     */
    protected int[] subBlock(int[] block, int subBlockSize, int subBlockIndex) {
        assert validBlock(block);
        return new int[]{
            firstWire(block) + subBlockIndex * subBlockSize, firstWire(block) + (subBlockIndex + 1) * subBlockSize
        };
    }

    /**
     * Creates an MAND gate with two groups of input wires and one group of output wires.
     * <p>
     * The implementation is inspired by
     * <a href="https://github.com/Fannxy/GigaDORAM/blob/main/circuits/lowmc.py#L70">lowmc.py</a>.
     *
     * @param inputWires1 1st group of input wires.
     * @param inputWires2 2nd group of input wires.
     * @param outputWires group of output wires.
     */
    protected void mandGate(TIntArrayList inputWires1, TIntArrayList inputWires2, TIntArrayList outputWires) {
        assert inputWires1.size() == inputWires2.size();
        assert inputWires1.size() == outputWires.size();
        int width = inputWires1.size();
        StringBuilder stringBuilder = new StringBuilder();
        // "2n n "
        stringBuilder.append(2 * width).append(" ").append(width).append(" ");
        // append 1st group of input wires
        for (int i = 0; i < width; i++) {
            stringBuilder.append(inputWires1.get(i)).append(" ");
        }
        // append 2nd group of input wires
        for (int i = 0; i < width; i++) {
            stringBuilder.append(inputWires2.get(i)).append(" ");
        }
        // append output wires
        for (int i = 0; i < width; i++) {
            stringBuilder.append(outputWires.get(i)).append(" ");
        }
        // append gate operation
        stringBuilder.append(GateOperation.MAND.name());
        gates.add(stringBuilder.toString());
    }

    /**
     * Creates an AND gate with two input wires and one output wire.
     *
     * @param inputWire1 wire 1.
     * @param inputWire2 wire 2.
     * @param outputWire output wire.
     */
    protected void andWires(int inputWire1, int inputWire2, int outputWire) {
        assert validWire(inputWire1);
        assert validWire(inputWire2);
        assert validWire(outputWire);
        gates.add("2 1 " + inputWire1 + " " + inputWire2 + " " + outputWire + " " + GateOperation.AND.name());
    }

    /**
     * Creates an AND gate with two input blocks and one output block.
     *
     * @param inputWires1 1st group of input wires.
     * @param inputWires2 2nd group of input wires.
     * @param outputWires group of output wires.
     */
    protected void andGate(TIntArrayList inputWires1, TIntArrayList inputWires2, TIntArrayList outputWires) {
        assert inputWires1.size() == inputWires2.size();
        assert inputWires1.size() == outputWires.size();
        int width = inputWires1.size();
        // append 1st group of input wires
        for (int i = 0; i < width; i++) {
            andWires(inputWires1.get(i), inputWires2.get(i), outputWires.get(i));
        }
    }

    /**
     * Resets the generator.
     */
    private void reset() {
        inputSizes.clear();
        outputSizes.clear();
        gates.clear();
        usedWireNum = 0;
        settingInputs = true;
        settingOutputs = false;
    }

    protected void generate(OutputStream outputStream) throws IOException {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        // write gateNum and wireNum
        outputStreamWriter.write(gates.size() + " " + usedWireNum);
        outputStreamWriter.write("\n");
        // write input values
        outputStreamWriter.write(inputSizes.size() + " ");
        for (int i = 0; i < inputSizes.size(); i++) {
            outputStreamWriter.write(String.valueOf(inputSizes.get(i)));
            if (i < inputSizes.size() - 1) {
                outputStreamWriter.write(" ");
            }
        }
        outputStreamWriter.write("\n");
        // write output values
        outputStreamWriter.write(outputSizes.size() + " ");
        for (int i = 0; i < outputSizes.size(); i++) {
            outputStreamWriter.write(String.valueOf(outputSizes.get(i)));
            if (i < outputSizes.size() - 1) {
                outputStreamWriter.write(" ");
            }
        }
        outputStreamWriter.write("\n");
        // skip an empty line
        outputStreamWriter.write("\n");
        // write gates
        for (String gate : gates) {
            outputStreamWriter.write(gate);
            outputStreamWriter.write("\n");
        }
        outputStreamWriter.flush();
        outputStreamWriter.close();
        // reset state
        reset();
    }
}
