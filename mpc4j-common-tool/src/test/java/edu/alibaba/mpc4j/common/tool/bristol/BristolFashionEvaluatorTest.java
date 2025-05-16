package edu.alibaba.mpc4j.common.tool.bristol;

import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * Bristol Fashion circuit evaluator test. All circuits stored in the resources are from
 * <a href="https://nigelsmart.github.io/MPC-Circuits/">the blog post written by Nigel Smart</a>. We remark that inputs
 * and outputs for these circuits are in little-endian format, even for the AES-128 circuit.
 *
 * @author Weiran Liu
 * @date 2025/4/7
 */
public class BristolFashionEvaluatorTest {
    /**
     * round num
     */
    private static final int ROUND = 100;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public BristolFashionEvaluatorTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testBasicAdder64() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/basic/adder64.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testAdder64(evaluator);
    }

    @Test
    public void testExtendAdder64() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/extend/adder64.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testAdder64(evaluator);
    }

    private void testAdder64(BristolFashionEvaluator evaluator) {
        // test random inputs
        for (int round = 0; round < ROUND; round++) {
            long longInput1 = secureRandom.nextLong();
            long longInput2 = secureRandom.nextLong();
            long expect = longInput1 + longInput2;
            // get input wires in little-endian format
            boolean[] input1 = BinaryUtils.longToBinary(longInput1);
            BinaryUtils.reverse(input1);
            boolean[] input2 = BinaryUtils.longToBinary(longInput2);
            BinaryUtils.reverse(input2);
            // get output wires in little-endian format
            boolean[] output = evaluator.evaluate(input1, input2);
            BinaryUtils.reverse(output);
            long actual = BinaryUtils.binaryToLong(output);
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void testBasicSub64() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/basic/sub64.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testSub64(evaluator);
    }

    @Test
    public void testExtendSub64() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/extend/sub64.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testSub64(evaluator);
    }

    private void testSub64(BristolFashionEvaluator evaluator) {
        // test random inputs
        for (int round = 0; round < ROUND; round++) {
            long longInput1 = secureRandom.nextLong();
            long longInput2 = secureRandom.nextLong();
            long expect = longInput1 - longInput2;
            // get input wires in little-endian format
            boolean[] input1 = BinaryUtils.longToBinary(longInput1);
            BinaryUtils.reverse(input1);
            boolean[] input2 = BinaryUtils.longToBinary(longInput2);
            BinaryUtils.reverse(input2);
            // get output wires in little-endian format
            boolean[] output = evaluator.evaluate(input1, input2);
            BinaryUtils.reverse(output);
            long actual = BinaryUtils.binaryToLong(output);
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void testBasicNeg64() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/basic/neg64.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testNeg64(evaluator);
    }

    @Test
    public void testExtendNeg64() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/extend/neg64.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testNeg64(evaluator);
    }

    private void testNeg64(BristolFashionEvaluator evaluator) {
        // test random inputs
        for (int round = 0; round < ROUND; round++) {
            long longInput = secureRandom.nextLong();
            long expect =  -longInput;
            // get input wires in little-endian format
            boolean[] input = BinaryUtils.longToBinary(longInput);
            BinaryUtils.reverse(input);
            // get output wires in little-endian format
            boolean[] output = evaluator.evaluate(input);
            BinaryUtils.reverse(output);
            long actual = BinaryUtils.binaryToLong(output);
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void testBasicMult64() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/basic/mult64.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testMult64(evaluator);
    }

    @Test
    public void testExtendMult64() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/extend/mult64.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testMult64(evaluator);
    }

    private void testMult64(BristolFashionEvaluator evaluator) {
        // test random inputs
        for (int round = 0; round < ROUND; round++) {
            long longInput1 = secureRandom.nextLong();
            long longInput2 = secureRandom.nextLong();
            long expect = longInput1 * longInput2;
            // get input wires in little-endian format
            boolean[] input1 = BinaryUtils.longToBinary(longInput1);
            BinaryUtils.reverse(input1);
            boolean[] input2 = BinaryUtils.longToBinary(longInput2);
            BinaryUtils.reverse(input2);
            // get output wires in little-endian format
            boolean[] output = evaluator.evaluate(input1, input2);
            BinaryUtils.reverse(output);
            long actual = BinaryUtils.binaryToLong(output);
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void testBasicAes128() {
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/extend/aes_128.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);
        testAes128(evaluator);
    }

    private void testAes128(BristolFashionEvaluator evaluator) {
        // test random message with random keys
        for (int round = 0; round < ROUND; round++) {
            Prp prp = PrpFactory.createInstance(PrpType.JDK_AES);
            byte[] plaintext = BlockUtils.randomBlock(secureRandom);
            byte[] key = BlockUtils.randomBlock(secureRandom);
            prp.setKey(key);
            byte[] expect = prp.prp(plaintext);
            // get input wires for AES-128(k,m)
            // Note for AES-128 the wire orders are in the reverse order as used in the examples given in our earlier
            // `Bristol Format', thus bit 0 becomes bit 127 etc, for key, plaintext and message.
            boolean[] inputPlaintext = BinaryUtils.byteArrayToBinary(plaintext);
            BinaryUtils.reverse(inputPlaintext);
            boolean[] inputKey = BinaryUtils.byteArrayToBinary(key);
            BinaryUtils.reverse(inputKey);
            // get output wires for AES-128(k,m)
            boolean[] output = evaluator.evaluate(inputKey, inputPlaintext);
            BinaryUtils.reverse(output);
            byte[] actual = BinaryUtils.binaryToByteArray(output);
            Assert.assertArrayEquals(expect, actual);
        }
    }

    @Test
    public void testMpSpdzExample() {
        // This is the example from MP-SPDZ.
        InputStream inputStream = Objects.requireNonNull(
            BristolFashionEvaluator.class.getClassLoader().getResourceAsStream("bristol/extend/aes_128.txt")
        );
        BristolFashionEvaluator evaluator = new BristolFashionEvaluator(inputStream);

        // key = 0x2b7e151628aed2a6abf7158809cf4f3c: https://github.com/data61/MP-SPDZ/blob/master/Compiler/circuit.py#L37
        byte[] key = Hex.decode("2b7e151628aed2a6abf7158809cf4f3c");
        // plaintext = 0x6bc1bee22e409f96e93d7e117393172a : https://github.com/data61/MP-SPDZ/blob/master/Compiler/circuit.py#L38
        byte[] plaintext = Hex.decode("6bc1bee22e409f96e93d7e117393172a");
        // result = 0x3ad77bb40d7a3660a89ecaf32466ef97 https://github.com/data61/MP-SPDZ/blob/master/Compiler/circuit.py#L45
        byte[] expect = Hex.decode("3ad77bb40d7a3660a89ecaf32466ef97");
        boolean[] inputPlaintext = BinaryUtils.byteArrayToBinary(plaintext);
        BinaryUtils.reverse(inputPlaintext);
        boolean[] inputKey = BinaryUtils.byteArrayToBinary(key);
        BinaryUtils.reverse(inputKey);
        // get output wires for AES-128(k,m)
        boolean[] output = evaluator.evaluate(inputKey, inputPlaintext);
        BinaryUtils.reverse(output);
        byte[] actual = BinaryUtils.binaryToByteArray(output);
        Assert.assertArrayEquals(expect, actual);
    }
}
