package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.DefaultFixedKeyPrp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoBackupHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoDirectPrimaryHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoProgrammedPrimaryHint;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Piano hint tests.
 *
 * @author Weiran Liu
 * @date 2024/10/25
 */
public class PianoHintTest {

    @Test
    public void test1x1() {
        testPianoHint(1, 1, 0);
    }

    @Test
    public void test99x101() {
        testPianoHint(99, 101, 8888);
    }

    @Test
    public void test101x99() {
        testPianoHint(101, 99, 88);
    }

    private void testPianoHint(int chunkNum, int chunkSize, int x) {
        SecureRandom secureRandom = new SecureRandom();
        FixedKeyPrp fixedKeyPrp = new DefaultFixedKeyPrp(EnvType.STANDARD);
        // direct primary hint
        PianoDirectPrimaryHint directPrimaryHint = new PianoDirectPrimaryHint(
            fixedKeyPrp, chunkSize, chunkNum, Byte.SIZE, secureRandom
        );
        int[] separateOffsets = IntStream.range(0, chunkNum)
            .map(directPrimaryHint::expandOffset)
            .toArray();
        int[] batchOffsets = directPrimaryHint.expandOffsets();
        Assert.assertArrayEquals(separateOffsets, batchOffsets);
        int[] blockOffsets = new int[chunkNum];
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += PianoHint.PRP_BLOCK_OFFSET_NUM) {
            int[] prpBlockOffsets = directPrimaryHint.expandPrpBlockOffsets(blockChunkId);
            System.arraycopy(prpBlockOffsets, 0, blockOffsets, blockChunkId, prpBlockOffsets.length);
        }
        Assert.assertArrayEquals(separateOffsets, blockOffsets);

        // backup hint
        int puncturedChunkId = x / chunkSize;
        int puncturedOffset = Math.abs(x % chunkSize);
        PianoBackupHint backupHint = new PianoBackupHint(
            fixedKeyPrp, chunkSize, chunkNum, Byte.SIZE, puncturedChunkId, secureRandom
        );
        separateOffsets = IntStream.range(0, chunkNum)
            .map(backupHint::expandOffset)
            .toArray();
        Assert.assertEquals(-1, separateOffsets[puncturedChunkId]);
        batchOffsets = backupHint.expandOffsets();
        Assert.assertArrayEquals(separateOffsets, batchOffsets);
        blockOffsets = new int[chunkNum];
        for (int chunkId = 0; chunkId < chunkNum; chunkId += PianoHint.PRP_BLOCK_OFFSET_NUM) {
            int[] prpBlockOffsets = backupHint.expandPrpBlockOffsets(chunkId);
            System.arraycopy(prpBlockOffsets, 0, blockOffsets, chunkId, prpBlockOffsets.length);
        }
        Assert.assertArrayEquals(separateOffsets, blockOffsets);

        // programmed backup hints
        PianoProgrammedPrimaryHint programmedPrimaryHint = new PianoProgrammedPrimaryHint(backupHint, x);
        Assert.assertTrue(programmedPrimaryHint.contains(x));
        separateOffsets = IntStream.range(0, chunkNum)
            .map(programmedPrimaryHint::expandOffset)
            .toArray();
        Assert.assertEquals(puncturedOffset, separateOffsets[puncturedChunkId]);
        batchOffsets = programmedPrimaryHint.expandOffsets();
        Assert.assertArrayEquals(separateOffsets, batchOffsets);
        blockOffsets = new int[chunkNum];
        for (int chunkId = 0; chunkId < chunkNum; chunkId += PianoHint.PRP_BLOCK_OFFSET_NUM) {
            int[] prpBlockOffsets = programmedPrimaryHint.expandPrpBlockOffsets(chunkId);
            System.arraycopy(prpBlockOffsets, 0, blockOffsets, chunkId, prpBlockOffsets.length);
        }
        Assert.assertArrayEquals(separateOffsets, blockOffsets);
    }
}
