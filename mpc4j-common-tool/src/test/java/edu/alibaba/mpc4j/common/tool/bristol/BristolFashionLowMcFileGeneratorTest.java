package edu.alibaba.mpc4j.common.tool.bristol;

import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.security.SecureRandom;

/**
 * Bristol Fashion LowMC circuit file generator test.
 *
 * @author Weiran Liu
 * @date 2025/4/9
 */
public class BristolFashionLowMcFileGeneratorTest {
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public BristolFashionLowMcFileGeneratorTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testBasicConstant() throws IOException {
        int[] rounds = new int[]{20, 21, 23, 32, 192, 208, 287};
        for (int round : rounds) {
            testConstant(BristolFashionType.BASIC, round, PrpType.valueOf("JDK_BYTES_LOW_MC_" + round));
        }
    }

    @Test
    public void testExtendConstant() throws IOException {
        int[] rounds = new int[]{20, 21, 23, 32, 192, 208, 287};
        for (int round : rounds) {
            testConstant(BristolFashionType.EXTEND, round, PrpType.valueOf("JDK_BYTES_LOW_MC_" + round));
        }
    }

    private void testConstant(BristolFashionType type, int round, PrpType prpType) throws IOException {
        byte[] key = BlockUtils.zeroBlock();
        byte[] plaintext = BlockUtils.zeroBlock();
        test(type, round, prpType, key, plaintext);
    }

    @Test
    public void testBasicRandom() throws IOException {
        int[] rounds = new int[]{20, 21, 23, 32, 192, 208, 287};
        for (int round : rounds) {
            testRandom(BristolFashionType.BASIC, round, PrpType.valueOf("JDK_BYTES_LOW_MC_" + round));
        }
    }

    @Test
    public void testExtendRandom() throws IOException {
        int[] rounds = new int[]{20, 21, 23, 32, 192, 208, 287};
        for (int round : rounds) {
            testRandom(BristolFashionType.EXTEND, round, PrpType.valueOf("JDK_BYTES_LOW_MC_" + round));
        }
    }

    private void testRandom(BristolFashionType type, int round, PrpType prpType) throws IOException {
        byte[] key = BlockUtils.randomBlock(secureRandom);
        byte[] plaintext = BlockUtils.randomBlock(secureRandom);
        test(type, round, prpType, key, plaintext);
    }

    private void test(BristolFashionType type, int round, PrpType prpType, byte[] key, byte[] plaintext) throws IOException {
        // expect
        Prp prp = PrpFactory.createInstance(prpType);
        prp.setKey(key);
        byte[] expect = prp.prp(plaintext);

        // generate and evaluate circuit
        BristolFashionLowMcFileGenerator generator = new BristolFashionLowMcFileGenerator(round);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generate(type, outputStream);
        byte[] circuit = outputStream.toByteArray();
        outputStream.close();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(circuit);
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        inputStream.close();
        boolean[] binaryKey = BinaryUtils.byteArrayToBinary(key);
        boolean[] binaryPlaintext = BinaryUtils.byteArrayToBinary(plaintext);
        boolean[] binaryCiphertext = evaluator.evaluate(binaryKey, binaryPlaintext);
        byte[] actual = BinaryUtils.binaryToByteArray(binaryCiphertext);
        Assert.assertArrayEquals(expect, actual);
    }

    @Test
    public void testMpSpdz() throws IOException {
        // example from MP-SPDZ, provided by Li Peng
        byte[] key = BlockUtils.zeroBlock();
        byte[] plaintext = BlockUtils.zeroBlock();
        byte[] expect = Hex.decode("84215ad4aa1b6f5c24852f9d8799415c");

        // generate and evaluate circuit
        BristolFashionLowMcFileGenerator generator = new BristolFashionLowMcFileGenerator(20);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generate(BristolFashionType.BASIC, outputStream);
        byte[] circuit = outputStream.toByteArray();
        outputStream.close();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(circuit);
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        inputStream.close();
        boolean[] binaryKey = BinaryUtils.byteArrayToBinary(key);
        BinaryUtils.reverse(binaryKey);
        boolean[] binaryPlaintext = BinaryUtils.byteArrayToBinary(plaintext);
        BinaryUtils.reverse(binaryPlaintext);
        boolean[] binaryCiphertext = evaluator.evaluate(binaryKey, binaryPlaintext);
        BinaryUtils.reverse(binaryCiphertext);
        byte[] actual = BinaryUtils.binaryToByteArray(binaryCiphertext);
        Assert.assertArrayEquals(expect, actual);
    }
}
