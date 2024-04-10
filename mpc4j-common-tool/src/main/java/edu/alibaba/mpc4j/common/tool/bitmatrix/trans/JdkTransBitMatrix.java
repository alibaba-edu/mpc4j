package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import com.google.common.primitives.Longs;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import gnu.trove.map.hash.TLongByteHashMap;

/**
 * JDK Bit Matrix transpose. We use long to simulate SIMD.
 *
 * @author Weiran Liu
 * @date 2024/2/29
 */
public class JdkTransBitMatrix extends AbstractTransBitMatrix {
    /**
     * lookup table
     */
    private static final TLongByteHashMap LOOKUP_TABLE = new TLongByteHashMap(1 << Byte.SIZE);

    static {
        LOOKUP_TABLE.put(0x00_00_00_00_00_00_00_00L, (byte) 0b00000000);
        LOOKUP_TABLE.put(0x00_00_00_00_00_00_00_80L, (byte) 0b00000001);
        LOOKUP_TABLE.put(0x00_00_00_00_00_00_80_00L, (byte) 0b00000010);
        LOOKUP_TABLE.put(0x00_00_00_00_00_00_80_80L, (byte) 0b00000011);
        LOOKUP_TABLE.put(0x00_00_00_00_00_80_00_00L, (byte) 0b00000100);
        LOOKUP_TABLE.put(0x00_00_00_00_00_80_00_80L, (byte) 0b00000101);
        LOOKUP_TABLE.put(0x00_00_00_00_00_80_80_00L, (byte) 0b00000110);
        LOOKUP_TABLE.put(0x00_00_00_00_00_80_80_80L, (byte) 0b00000111);
        LOOKUP_TABLE.put(0x00_00_00_00_80_00_00_00L, (byte) 0b00001000);
        LOOKUP_TABLE.put(0x00_00_00_00_80_00_00_80L, (byte) 0b00001001);
        LOOKUP_TABLE.put(0x00_00_00_00_80_00_80_00L, (byte) 0b00001010);
        LOOKUP_TABLE.put(0x00_00_00_00_80_00_80_80L, (byte) 0b00001011);
        LOOKUP_TABLE.put(0x00_00_00_00_80_80_00_00L, (byte) 0b00001100);
        LOOKUP_TABLE.put(0x00_00_00_00_80_80_00_80L, (byte) 0b00001101);
        LOOKUP_TABLE.put(0x00_00_00_00_80_80_80_00L, (byte) 0b00001110);
        LOOKUP_TABLE.put(0x00_00_00_00_80_80_80_80L, (byte) 0b00001111);
        LOOKUP_TABLE.put(0x00_00_00_80_00_00_00_00L, (byte) 0b00010000);
        LOOKUP_TABLE.put(0x00_00_00_80_00_00_00_80L, (byte) 0b00010001);
        LOOKUP_TABLE.put(0x00_00_00_80_00_00_80_00L, (byte) 0b00010010);
        LOOKUP_TABLE.put(0x00_00_00_80_00_00_80_80L, (byte) 0b00010011);
        LOOKUP_TABLE.put(0x00_00_00_80_00_80_00_00L, (byte) 0b00010100);
        LOOKUP_TABLE.put(0x00_00_00_80_00_80_00_80L, (byte) 0b00010101);
        LOOKUP_TABLE.put(0x00_00_00_80_00_80_80_00L, (byte) 0b00010110);
        LOOKUP_TABLE.put(0x00_00_00_80_00_80_80_80L, (byte) 0b00010111);
        LOOKUP_TABLE.put(0x00_00_00_80_80_00_00_00L, (byte) 0b00011000);
        LOOKUP_TABLE.put(0x00_00_00_80_80_00_00_80L, (byte) 0b00011001);
        LOOKUP_TABLE.put(0x00_00_00_80_80_00_80_00L, (byte) 0b00011010);
        LOOKUP_TABLE.put(0x00_00_00_80_80_00_80_80L, (byte) 0b00011011);
        LOOKUP_TABLE.put(0x00_00_00_80_80_80_00_00L, (byte) 0b00011100);
        LOOKUP_TABLE.put(0x00_00_00_80_80_80_00_80L, (byte) 0b00011101);
        LOOKUP_TABLE.put(0x00_00_00_80_80_80_80_00L, (byte) 0b00011110);
        LOOKUP_TABLE.put(0x00_00_00_80_80_80_80_80L, (byte) 0b00011111);
        LOOKUP_TABLE.put(0x00_00_80_00_00_00_00_00L, (byte) 0b00100000);
        LOOKUP_TABLE.put(0x00_00_80_00_00_00_00_80L, (byte) 0b00100001);
        LOOKUP_TABLE.put(0x00_00_80_00_00_00_80_00L, (byte) 0b00100010);
        LOOKUP_TABLE.put(0x00_00_80_00_00_00_80_80L, (byte) 0b00100011);
        LOOKUP_TABLE.put(0x00_00_80_00_00_80_00_00L, (byte) 0b00100100);
        LOOKUP_TABLE.put(0x00_00_80_00_00_80_00_80L, (byte) 0b00100101);
        LOOKUP_TABLE.put(0x00_00_80_00_00_80_80_00L, (byte) 0b00100110);
        LOOKUP_TABLE.put(0x00_00_80_00_00_80_80_80L, (byte) 0b00100111);
        LOOKUP_TABLE.put(0x00_00_80_00_80_00_00_00L, (byte) 0b00101000);
        LOOKUP_TABLE.put(0x00_00_80_00_80_00_00_80L, (byte) 0b00101001);
        LOOKUP_TABLE.put(0x00_00_80_00_80_00_80_00L, (byte) 0b00101010);
        LOOKUP_TABLE.put(0x00_00_80_00_80_00_80_80L, (byte) 0b00101011);
        LOOKUP_TABLE.put(0x00_00_80_00_80_80_00_00L, (byte) 0b00101100);
        LOOKUP_TABLE.put(0x00_00_80_00_80_80_00_80L, (byte) 0b00101101);
        LOOKUP_TABLE.put(0x00_00_80_00_80_80_80_00L, (byte) 0b00101110);
        LOOKUP_TABLE.put(0x00_00_80_00_80_80_80_80L, (byte) 0b00101111);
        LOOKUP_TABLE.put(0x00_00_80_80_00_00_00_00L, (byte) 0b00110000);
        LOOKUP_TABLE.put(0x00_00_80_80_00_00_00_80L, (byte) 0b00110001);
        LOOKUP_TABLE.put(0x00_00_80_80_00_00_80_00L, (byte) 0b00110010);
        LOOKUP_TABLE.put(0x00_00_80_80_00_00_80_80L, (byte) 0b00110011);
        LOOKUP_TABLE.put(0x00_00_80_80_00_80_00_00L, (byte) 0b00110100);
        LOOKUP_TABLE.put(0x00_00_80_80_00_80_00_80L, (byte) 0b00110101);
        LOOKUP_TABLE.put(0x00_00_80_80_00_80_80_00L, (byte) 0b00110110);
        LOOKUP_TABLE.put(0x00_00_80_80_00_80_80_80L, (byte) 0b00110111);
        LOOKUP_TABLE.put(0x00_00_80_80_80_00_00_00L, (byte) 0b00111000);
        LOOKUP_TABLE.put(0x00_00_80_80_80_00_00_80L, (byte) 0b00111001);
        LOOKUP_TABLE.put(0x00_00_80_80_80_00_80_00L, (byte) 0b00111010);
        LOOKUP_TABLE.put(0x00_00_80_80_80_00_80_80L, (byte) 0b00111011);
        LOOKUP_TABLE.put(0x00_00_80_80_80_80_00_00L, (byte) 0b00111100);
        LOOKUP_TABLE.put(0x00_00_80_80_80_80_00_80L, (byte) 0b00111101);
        LOOKUP_TABLE.put(0x00_00_80_80_80_80_80_00L, (byte) 0b00111110);
        LOOKUP_TABLE.put(0x00_00_80_80_80_80_80_80L, (byte) 0b00111111);
        LOOKUP_TABLE.put(0x00_80_00_00_00_00_00_00L, (byte) 0b01000000);
        LOOKUP_TABLE.put(0x00_80_00_00_00_00_00_80L, (byte) 0b01000001);
        LOOKUP_TABLE.put(0x00_80_00_00_00_00_80_00L, (byte) 0b01000010);
        LOOKUP_TABLE.put(0x00_80_00_00_00_00_80_80L, (byte) 0b01000011);
        LOOKUP_TABLE.put(0x00_80_00_00_00_80_00_00L, (byte) 0b01000100);
        LOOKUP_TABLE.put(0x00_80_00_00_00_80_00_80L, (byte) 0b01000101);
        LOOKUP_TABLE.put(0x00_80_00_00_00_80_80_00L, (byte) 0b01000110);
        LOOKUP_TABLE.put(0x00_80_00_00_00_80_80_80L, (byte) 0b01000111);
        LOOKUP_TABLE.put(0x00_80_00_00_80_00_00_00L, (byte) 0b01001000);
        LOOKUP_TABLE.put(0x00_80_00_00_80_00_00_80L, (byte) 0b01001001);
        LOOKUP_TABLE.put(0x00_80_00_00_80_00_80_00L, (byte) 0b01001010);
        LOOKUP_TABLE.put(0x00_80_00_00_80_00_80_80L, (byte) 0b01001011);
        LOOKUP_TABLE.put(0x00_80_00_00_80_80_00_00L, (byte) 0b01001100);
        LOOKUP_TABLE.put(0x00_80_00_00_80_80_00_80L, (byte) 0b01001101);
        LOOKUP_TABLE.put(0x00_80_00_00_80_80_80_00L, (byte) 0b01001110);
        LOOKUP_TABLE.put(0x00_80_00_00_80_80_80_80L, (byte) 0b01001111);
        LOOKUP_TABLE.put(0x00_80_00_80_00_00_00_00L, (byte) 0b01010000);
        LOOKUP_TABLE.put(0x00_80_00_80_00_00_00_80L, (byte) 0b01010001);
        LOOKUP_TABLE.put(0x00_80_00_80_00_00_80_00L, (byte) 0b01010010);
        LOOKUP_TABLE.put(0x00_80_00_80_00_00_80_80L, (byte) 0b01010011);
        LOOKUP_TABLE.put(0x00_80_00_80_00_80_00_00L, (byte) 0b01010100);
        LOOKUP_TABLE.put(0x00_80_00_80_00_80_00_80L, (byte) 0b01010101);
        LOOKUP_TABLE.put(0x00_80_00_80_00_80_80_00L, (byte) 0b01010110);
        LOOKUP_TABLE.put(0x00_80_00_80_00_80_80_80L, (byte) 0b01010111);
        LOOKUP_TABLE.put(0x00_80_00_80_80_00_00_00L, (byte) 0b01011000);
        LOOKUP_TABLE.put(0x00_80_00_80_80_00_00_80L, (byte) 0b01011001);
        LOOKUP_TABLE.put(0x00_80_00_80_80_00_80_00L, (byte) 0b01011010);
        LOOKUP_TABLE.put(0x00_80_00_80_80_00_80_80L, (byte) 0b01011011);
        LOOKUP_TABLE.put(0x00_80_00_80_80_80_00_00L, (byte) 0b01011100);
        LOOKUP_TABLE.put(0x00_80_00_80_80_80_00_80L, (byte) 0b01011101);
        LOOKUP_TABLE.put(0x00_80_00_80_80_80_80_00L, (byte) 0b01011110);
        LOOKUP_TABLE.put(0x00_80_00_80_80_80_80_80L, (byte) 0b01011111);
        LOOKUP_TABLE.put(0x00_80_80_00_00_00_00_00L, (byte) 0b01100000);
        LOOKUP_TABLE.put(0x00_80_80_00_00_00_00_80L, (byte) 0b01100001);
        LOOKUP_TABLE.put(0x00_80_80_00_00_00_80_00L, (byte) 0b01100010);
        LOOKUP_TABLE.put(0x00_80_80_00_00_00_80_80L, (byte) 0b01100011);
        LOOKUP_TABLE.put(0x00_80_80_00_00_80_00_00L, (byte) 0b01100100);
        LOOKUP_TABLE.put(0x00_80_80_00_00_80_00_80L, (byte) 0b01100101);
        LOOKUP_TABLE.put(0x00_80_80_00_00_80_80_00L, (byte) 0b01100110);
        LOOKUP_TABLE.put(0x00_80_80_00_00_80_80_80L, (byte) 0b01100111);
        LOOKUP_TABLE.put(0x00_80_80_00_80_00_00_00L, (byte) 0b01101000);
        LOOKUP_TABLE.put(0x00_80_80_00_80_00_00_80L, (byte) 0b01101001);
        LOOKUP_TABLE.put(0x00_80_80_00_80_00_80_00L, (byte) 0b01101010);
        LOOKUP_TABLE.put(0x00_80_80_00_80_00_80_80L, (byte) 0b01101011);
        LOOKUP_TABLE.put(0x00_80_80_00_80_80_00_00L, (byte) 0b01101100);
        LOOKUP_TABLE.put(0x00_80_80_00_80_80_00_80L, (byte) 0b01101101);
        LOOKUP_TABLE.put(0x00_80_80_00_80_80_80_00L, (byte) 0b01101110);
        LOOKUP_TABLE.put(0x00_80_80_00_80_80_80_80L, (byte) 0b01101111);
        LOOKUP_TABLE.put(0x00_80_80_80_00_00_00_00L, (byte) 0b01110000);
        LOOKUP_TABLE.put(0x00_80_80_80_00_00_00_80L, (byte) 0b01110001);
        LOOKUP_TABLE.put(0x00_80_80_80_00_00_80_00L, (byte) 0b01110010);
        LOOKUP_TABLE.put(0x00_80_80_80_00_00_80_80L, (byte) 0b01110011);
        LOOKUP_TABLE.put(0x00_80_80_80_00_80_00_00L, (byte) 0b01110100);
        LOOKUP_TABLE.put(0x00_80_80_80_00_80_00_80L, (byte) 0b01110101);
        LOOKUP_TABLE.put(0x00_80_80_80_00_80_80_00L, (byte) 0b01110110);
        LOOKUP_TABLE.put(0x00_80_80_80_00_80_80_80L, (byte) 0b01110111);
        LOOKUP_TABLE.put(0x00_80_80_80_80_00_00_00L, (byte) 0b01111000);
        LOOKUP_TABLE.put(0x00_80_80_80_80_00_00_80L, (byte) 0b01111001);
        LOOKUP_TABLE.put(0x00_80_80_80_80_00_80_00L, (byte) 0b01111010);
        LOOKUP_TABLE.put(0x00_80_80_80_80_00_80_80L, (byte) 0b01111011);
        LOOKUP_TABLE.put(0x00_80_80_80_80_80_00_00L, (byte) 0b01111100);
        LOOKUP_TABLE.put(0x00_80_80_80_80_80_00_80L, (byte) 0b01111101);
        LOOKUP_TABLE.put(0x00_80_80_80_80_80_80_00L, (byte) 0b01111110);
        LOOKUP_TABLE.put(0x00_80_80_80_80_80_80_80L, (byte) 0b01111111);
        LOOKUP_TABLE.put(0x80_00_00_00_00_00_00_00L, (byte) 0b10000000);
        LOOKUP_TABLE.put(0x80_00_00_00_00_00_00_80L, (byte) 0b10000001);
        LOOKUP_TABLE.put(0x80_00_00_00_00_00_80_00L, (byte) 0b10000010);
        LOOKUP_TABLE.put(0x80_00_00_00_00_00_80_80L, (byte) 0b10000011);
        LOOKUP_TABLE.put(0x80_00_00_00_00_80_00_00L, (byte) 0b10000100);
        LOOKUP_TABLE.put(0x80_00_00_00_00_80_00_80L, (byte) 0b10000101);
        LOOKUP_TABLE.put(0x80_00_00_00_00_80_80_00L, (byte) 0b10000110);
        LOOKUP_TABLE.put(0x80_00_00_00_00_80_80_80L, (byte) 0b10000111);
        LOOKUP_TABLE.put(0x80_00_00_00_80_00_00_00L, (byte) 0b10001000);
        LOOKUP_TABLE.put(0x80_00_00_00_80_00_00_80L, (byte) 0b10001001);
        LOOKUP_TABLE.put(0x80_00_00_00_80_00_80_00L, (byte) 0b10001010);
        LOOKUP_TABLE.put(0x80_00_00_00_80_00_80_80L, (byte) 0b10001011);
        LOOKUP_TABLE.put(0x80_00_00_00_80_80_00_00L, (byte) 0b10001100);
        LOOKUP_TABLE.put(0x80_00_00_00_80_80_00_80L, (byte) 0b10001101);
        LOOKUP_TABLE.put(0x80_00_00_00_80_80_80_00L, (byte) 0b10001110);
        LOOKUP_TABLE.put(0x80_00_00_00_80_80_80_80L, (byte) 0b10001111);
        LOOKUP_TABLE.put(0x80_00_00_80_00_00_00_00L, (byte) 0b10010000);
        LOOKUP_TABLE.put(0x80_00_00_80_00_00_00_80L, (byte) 0b10010001);
        LOOKUP_TABLE.put(0x80_00_00_80_00_00_80_00L, (byte) 0b10010010);
        LOOKUP_TABLE.put(0x80_00_00_80_00_00_80_80L, (byte) 0b10010011);
        LOOKUP_TABLE.put(0x80_00_00_80_00_80_00_00L, (byte) 0b10010100);
        LOOKUP_TABLE.put(0x80_00_00_80_00_80_00_80L, (byte) 0b10010101);
        LOOKUP_TABLE.put(0x80_00_00_80_00_80_80_00L, (byte) 0b10010110);
        LOOKUP_TABLE.put(0x80_00_00_80_00_80_80_80L, (byte) 0b10010111);
        LOOKUP_TABLE.put(0x80_00_00_80_80_00_00_00L, (byte) 0b10011000);
        LOOKUP_TABLE.put(0x80_00_00_80_80_00_00_80L, (byte) 0b10011001);
        LOOKUP_TABLE.put(0x80_00_00_80_80_00_80_00L, (byte) 0b10011010);
        LOOKUP_TABLE.put(0x80_00_00_80_80_00_80_80L, (byte) 0b10011011);
        LOOKUP_TABLE.put(0x80_00_00_80_80_80_00_00L, (byte) 0b10011100);
        LOOKUP_TABLE.put(0x80_00_00_80_80_80_00_80L, (byte) 0b10011101);
        LOOKUP_TABLE.put(0x80_00_00_80_80_80_80_00L, (byte) 0b10011110);
        LOOKUP_TABLE.put(0x80_00_00_80_80_80_80_80L, (byte) 0b10011111);
        LOOKUP_TABLE.put(0x80_00_80_00_00_00_00_00L, (byte) 0b10100000);
        LOOKUP_TABLE.put(0x80_00_80_00_00_00_00_80L, (byte) 0b10100001);
        LOOKUP_TABLE.put(0x80_00_80_00_00_00_80_00L, (byte) 0b10100010);
        LOOKUP_TABLE.put(0x80_00_80_00_00_00_80_80L, (byte) 0b10100011);
        LOOKUP_TABLE.put(0x80_00_80_00_00_80_00_00L, (byte) 0b10100100);
        LOOKUP_TABLE.put(0x80_00_80_00_00_80_00_80L, (byte) 0b10100101);
        LOOKUP_TABLE.put(0x80_00_80_00_00_80_80_00L, (byte) 0b10100110);
        LOOKUP_TABLE.put(0x80_00_80_00_00_80_80_80L, (byte) 0b10100111);
        LOOKUP_TABLE.put(0x80_00_80_00_80_00_00_00L, (byte) 0b10101000);
        LOOKUP_TABLE.put(0x80_00_80_00_80_00_00_80L, (byte) 0b10101001);
        LOOKUP_TABLE.put(0x80_00_80_00_80_00_80_00L, (byte) 0b10101010);
        LOOKUP_TABLE.put(0x80_00_80_00_80_00_80_80L, (byte) 0b10101011);
        LOOKUP_TABLE.put(0x80_00_80_00_80_80_00_00L, (byte) 0b10101100);
        LOOKUP_TABLE.put(0x80_00_80_00_80_80_00_80L, (byte) 0b10101101);
        LOOKUP_TABLE.put(0x80_00_80_00_80_80_80_00L, (byte) 0b10101110);
        LOOKUP_TABLE.put(0x80_00_80_00_80_80_80_80L, (byte) 0b10101111);
        LOOKUP_TABLE.put(0x80_00_80_80_00_00_00_00L, (byte) 0b10110000);
        LOOKUP_TABLE.put(0x80_00_80_80_00_00_00_80L, (byte) 0b10110001);
        LOOKUP_TABLE.put(0x80_00_80_80_00_00_80_00L, (byte) 0b10110010);
        LOOKUP_TABLE.put(0x80_00_80_80_00_00_80_80L, (byte) 0b10110011);
        LOOKUP_TABLE.put(0x80_00_80_80_00_80_00_00L, (byte) 0b10110100);
        LOOKUP_TABLE.put(0x80_00_80_80_00_80_00_80L, (byte) 0b10110101);
        LOOKUP_TABLE.put(0x80_00_80_80_00_80_80_00L, (byte) 0b10110110);
        LOOKUP_TABLE.put(0x80_00_80_80_00_80_80_80L, (byte) 0b10110111);
        LOOKUP_TABLE.put(0x80_00_80_80_80_00_00_00L, (byte) 0b10111000);
        LOOKUP_TABLE.put(0x80_00_80_80_80_00_00_80L, (byte) 0b10111001);
        LOOKUP_TABLE.put(0x80_00_80_80_80_00_80_00L, (byte) 0b10111010);
        LOOKUP_TABLE.put(0x80_00_80_80_80_00_80_80L, (byte) 0b10111011);
        LOOKUP_TABLE.put(0x80_00_80_80_80_80_00_00L, (byte) 0b10111100);
        LOOKUP_TABLE.put(0x80_00_80_80_80_80_00_80L, (byte) 0b10111101);
        LOOKUP_TABLE.put(0x80_00_80_80_80_80_80_00L, (byte) 0b10111110);
        LOOKUP_TABLE.put(0x80_00_80_80_80_80_80_80L, (byte) 0b10111111);
        LOOKUP_TABLE.put(0x80_80_00_00_00_00_00_00L, (byte) 0b11000000);
        LOOKUP_TABLE.put(0x80_80_00_00_00_00_00_80L, (byte) 0b11000001);
        LOOKUP_TABLE.put(0x80_80_00_00_00_00_80_00L, (byte) 0b11000010);
        LOOKUP_TABLE.put(0x80_80_00_00_00_00_80_80L, (byte) 0b11000011);
        LOOKUP_TABLE.put(0x80_80_00_00_00_80_00_00L, (byte) 0b11000100);
        LOOKUP_TABLE.put(0x80_80_00_00_00_80_00_80L, (byte) 0b11000101);
        LOOKUP_TABLE.put(0x80_80_00_00_00_80_80_00L, (byte) 0b11000110);
        LOOKUP_TABLE.put(0x80_80_00_00_00_80_80_80L, (byte) 0b11000111);
        LOOKUP_TABLE.put(0x80_80_00_00_80_00_00_00L, (byte) 0b11001000);
        LOOKUP_TABLE.put(0x80_80_00_00_80_00_00_80L, (byte) 0b11001001);
        LOOKUP_TABLE.put(0x80_80_00_00_80_00_80_00L, (byte) 0b11001010);
        LOOKUP_TABLE.put(0x80_80_00_00_80_00_80_80L, (byte) 0b11001011);
        LOOKUP_TABLE.put(0x80_80_00_00_80_80_00_00L, (byte) 0b11001100);
        LOOKUP_TABLE.put(0x80_80_00_00_80_80_00_80L, (byte) 0b11001101);
        LOOKUP_TABLE.put(0x80_80_00_00_80_80_80_00L, (byte) 0b11001110);
        LOOKUP_TABLE.put(0x80_80_00_00_80_80_80_80L, (byte) 0b11001111);
        LOOKUP_TABLE.put(0x80_80_00_80_00_00_00_00L, (byte) 0b11010000);
        LOOKUP_TABLE.put(0x80_80_00_80_00_00_00_80L, (byte) 0b11010001);
        LOOKUP_TABLE.put(0x80_80_00_80_00_00_80_00L, (byte) 0b11010010);
        LOOKUP_TABLE.put(0x80_80_00_80_00_00_80_80L, (byte) 0b11010011);
        LOOKUP_TABLE.put(0x80_80_00_80_00_80_00_00L, (byte) 0b11010100);
        LOOKUP_TABLE.put(0x80_80_00_80_00_80_00_80L, (byte) 0b11010101);
        LOOKUP_TABLE.put(0x80_80_00_80_00_80_80_00L, (byte) 0b11010110);
        LOOKUP_TABLE.put(0x80_80_00_80_00_80_80_80L, (byte) 0b11010111);
        LOOKUP_TABLE.put(0x80_80_00_80_80_00_00_00L, (byte) 0b11011000);
        LOOKUP_TABLE.put(0x80_80_00_80_80_00_00_80L, (byte) 0b11011001);
        LOOKUP_TABLE.put(0x80_80_00_80_80_00_80_00L, (byte) 0b11011010);
        LOOKUP_TABLE.put(0x80_80_00_80_80_00_80_80L, (byte) 0b11011011);
        LOOKUP_TABLE.put(0x80_80_00_80_80_80_00_00L, (byte) 0b11011100);
        LOOKUP_TABLE.put(0x80_80_00_80_80_80_00_80L, (byte) 0b11011101);
        LOOKUP_TABLE.put(0x80_80_00_80_80_80_80_00L, (byte) 0b11011110);
        LOOKUP_TABLE.put(0x80_80_00_80_80_80_80_80L, (byte) 0b11011111);
        LOOKUP_TABLE.put(0x80_80_80_00_00_00_00_00L, (byte) 0b11100000);
        LOOKUP_TABLE.put(0x80_80_80_00_00_00_00_80L, (byte) 0b11100001);
        LOOKUP_TABLE.put(0x80_80_80_00_00_00_80_00L, (byte) 0b11100010);
        LOOKUP_TABLE.put(0x80_80_80_00_00_00_80_80L, (byte) 0b11100011);
        LOOKUP_TABLE.put(0x80_80_80_00_00_80_00_00L, (byte) 0b11100100);
        LOOKUP_TABLE.put(0x80_80_80_00_00_80_00_80L, (byte) 0b11100101);
        LOOKUP_TABLE.put(0x80_80_80_00_00_80_80_00L, (byte) 0b11100110);
        LOOKUP_TABLE.put(0x80_80_80_00_00_80_80_80L, (byte) 0b11100111);
        LOOKUP_TABLE.put(0x80_80_80_00_80_00_00_00L, (byte) 0b11101000);
        LOOKUP_TABLE.put(0x80_80_80_00_80_00_00_80L, (byte) 0b11101001);
        LOOKUP_TABLE.put(0x80_80_80_00_80_00_80_00L, (byte) 0b11101010);
        LOOKUP_TABLE.put(0x80_80_80_00_80_00_80_80L, (byte) 0b11101011);
        LOOKUP_TABLE.put(0x80_80_80_00_80_80_00_00L, (byte) 0b11101100);
        LOOKUP_TABLE.put(0x80_80_80_00_80_80_00_80L, (byte) 0b11101101);
        LOOKUP_TABLE.put(0x80_80_80_00_80_80_80_00L, (byte) 0b11101110);
        LOOKUP_TABLE.put(0x80_80_80_00_80_80_80_80L, (byte) 0b11101111);
        LOOKUP_TABLE.put(0x80_80_80_80_00_00_00_00L, (byte) 0b11110000);
        LOOKUP_TABLE.put(0x80_80_80_80_00_00_00_80L, (byte) 0b11110001);
        LOOKUP_TABLE.put(0x80_80_80_80_00_00_80_00L, (byte) 0b11110010);
        LOOKUP_TABLE.put(0x80_80_80_80_00_00_80_80L, (byte) 0b11110011);
        LOOKUP_TABLE.put(0x80_80_80_80_00_80_00_00L, (byte) 0b11110100);
        LOOKUP_TABLE.put(0x80_80_80_80_00_80_00_80L, (byte) 0b11110101);
        LOOKUP_TABLE.put(0x80_80_80_80_00_80_80_00L, (byte) 0b11110110);
        LOOKUP_TABLE.put(0x80_80_80_80_00_80_80_80L, (byte) 0b11110111);
        LOOKUP_TABLE.put(0x80_80_80_80_80_00_00_00L, (byte) 0b11111000);
        LOOKUP_TABLE.put(0x80_80_80_80_80_00_00_80L, (byte) 0b11111001);
        LOOKUP_TABLE.put(0x80_80_80_80_80_00_80_00L, (byte) 0b11111010);
        LOOKUP_TABLE.put(0x80_80_80_80_80_00_80_80L, (byte) 0b11111011);
        LOOKUP_TABLE.put(0x80_80_80_80_80_80_00_00L, (byte) 0b11111100);
        LOOKUP_TABLE.put(0x80_80_80_80_80_80_00_80L, (byte) 0b11111101);
        LOOKUP_TABLE.put(0x80_80_80_80_80_80_80_00L, (byte) 0b11111110);
        LOOKUP_TABLE.put(0x80_80_80_80_80_80_80_80L, (byte) 0b11111111);
    }

    /**
     * data is represented using an 2D-array
     */
    private final byte[][] data;
    /**
     * row in byte
     */
    private final int rowBytes;
    /**
     * row offset
     */
    private final int rowOffset;
    /**
     * row rounded to divide Byte.SIZE
     */
    private final int roundByteRows;
    /**
     * column offset
     */
    private final int columnOffset;
    /**
     * column rounded to divide Byte.SIZE
     */
    private final int roundByteColumns;

    public JdkTransBitMatrix(final int rows, final int columns) {
        super(rows, columns);
        rowBytes = CommonUtils.getByteLength(rows);
        roundByteRows = rowBytes * Byte.SIZE;
        rowOffset = roundByteRows - rows;
        int columnBytes = CommonUtils.getByteLength(columns);
        roundByteColumns = columnBytes * Byte.SIZE;
        columnOffset = roundByteColumns - columns;
        data = new byte[roundByteColumns][rowBytes];
    }


    @Override
    public boolean get(int x, int y) {
        assert (x >= 0 && x < rows);
        assert (y >= 0 && y < columns);
        // do not forget to add offset in the column index
        return BinaryUtils.getBoolean(data[y + columnOffset], x + rowOffset);
    }

    @Override
    public byte[] getColumn(int y) {
        assert (y >= 0 && y < columns);
        return data[y + columnOffset];
    }

    @Override
    public void setColumn(int y, byte[] byteArray) {
        assert (y >= 0 && y < columns);
        assert BytesUtils.isFixedReduceByteArray(byteArray, rowBytes, rows);
        data[y + columnOffset] = byteArray;
    }

    @Override
    public TransBitMatrix transpose() {
        JdkTransBitMatrix b = new JdkTransBitMatrix(columns, rows);
        for (int cc = 0; cc < roundByteColumns; cc += Byte.SIZE) {
            int ccByte = cc / Byte.SIZE;
            for (int rr = 0; rr < roundByteRows; rr += Byte.SIZE) {
                int rrByte = rr / Byte.SIZE;
                // there is no way to assign long using 8 bytes in SIMD so that this is the bottleneck.
                long vec = Longs.fromBytes(
                    data[cc][rrByte], data[cc + 1][rrByte], data[cc + 2][rrByte], data[cc + 3][rrByte],
                    data[cc + 4][rrByte], data[cc + 5][rrByte], data[cc + 6][rrByte], data[cc + 7][rrByte]
                );
                // we cannot find a batch way to assign long, maybe there will be a better way in the future
                for (int i = 0; i < Byte.SIZE; i++) {
                    // _mm_movemask_epi8(vec)
                    long movemask = vec & 0x8080808080808080L;
                    b.data[rr + i][ccByte] = LOOKUP_TABLE.get(movemask);
                    // _mm_slli_epi64(vec, 1)
                    vec <<= 1;
                }
            }
        }
        return b;
    }

    @Override
    public TransBitMatrixType getTransBitMatrixType() {
        return TransBitMatrixType.JDK;
    }
}
