package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.DefaultFixedKeyPrp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint.MirBackupHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint.MirDirectPrimaryHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint.MirHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint.MirProgrammedPrimaryHint;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * MIR hint test.
 *
 * @author Weiran Liu
 * @date 2024/10/26
 */
public class MirHintTest {

    @Test
    public void test2x1() {
        // chunkNum must be even
        testMirHint(2, 1, 0);
    }

    @Test
    public void test98x101() {
        // chunkNum must be even
        testMirHint(98, 101, 8888);
    }

    @Test
    public void test100x99() {
        // chunkNum must be even
        testMirHint(100, 99, 88);
    }

    @Test
    public void test4096x4096() {
        // chunkNum must be even
        testMirHint(4096, 4096, 88);
    }

    private void testMirHint(int chunkNum, int chunkSize, int x) {
        FixedKeyPrp fixedKeyPrp = new DefaultFixedKeyPrp(EnvType.STANDARD);
        SecureRandom secureRandom = new SecureRandom();
        // direct primary hint
        MirDirectPrimaryHint directPrimaryHint = new MirDirectPrimaryHint(
            fixedKeyPrp, chunkSize, chunkNum, Byte.SIZE, secureRandom
        );
        // offsets
        int[] separateOffsets = IntStream.range(0, chunkNum)
            .map(directPrimaryHint::expandOffset)
            .toArray();
        int[] batchOffsets = directPrimaryHint.expandOffsets();
        Assert.assertArrayEquals(separateOffsets, batchOffsets);
        int[] blockOffsets = new int[chunkNum];
        for (int chunkId = 0; chunkId < chunkNum; chunkId += MirHint.PRP_BLOCK_OFFSET_NUM) {
            int[] prpBlockOffsets = directPrimaryHint.expandPrpBlockOffsets(chunkId);
            System.arraycopy(prpBlockOffsets, 0, blockOffsets, chunkId, prpBlockOffsets.length);
        }
        Assert.assertArrayEquals(separateOffsets, blockOffsets);
        // contains
        BitVector separateContains = BitVectorFactory.createZeros(chunkNum);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (directPrimaryHint.containsChunkId(chunkId)) {
                separateContains.set(chunkId, true);
            }
        }
        BitVector batchContains = directPrimaryHint.containsChunks();
        Assert.assertEquals(separateContains, batchContains);
        BitVector blockContains = BitVectorFactory.createZeros(chunkNum);
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += MirHint.PRP_BLOCK_OFFSET_NUM) {
            boolean[] prpBlockContains = directPrimaryHint.containsChunks(blockChunkId);
            for (int j = 0; j < prpBlockContains.length; j++) {
                if (prpBlockContains[j]) {
                    blockContains.set(blockChunkId + j, true);
                }
            }
        }
        Assert.assertEquals(separateContains, blockContains);

        // backup hint
        MirBackupHint backupHint = new MirBackupHint(fixedKeyPrp, chunkSize, chunkNum, Byte.SIZE, secureRandom);
        // offsets
        separateOffsets = IntStream.range(0, chunkNum)
            .map(backupHint::expandOffset)
            .toArray();
        batchOffsets = backupHint.expandOffsets();
        Assert.assertArrayEquals(separateOffsets, batchOffsets);
        blockOffsets = new int[chunkNum];
        for (int chunkId = 0; chunkId < chunkNum; chunkId += MirHint.PRP_BLOCK_OFFSET_NUM) {
            int[] prpBlockOffsets = backupHint.expandPrpBlockOffsets(chunkId);
            System.arraycopy(prpBlockOffsets, 0, blockOffsets, chunkId, prpBlockOffsets.length);
        }
        Assert.assertArrayEquals(separateOffsets, blockOffsets);
        // contains
        separateContains = BitVectorFactory.createZeros(chunkNum);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (backupHint.containsChunkId(chunkId)) {
                separateContains.set(chunkId, backupHint.containsChunkId(chunkId));
            }
        }
        batchContains = backupHint.containsChunks();
        Assert.assertEquals(separateContains, batchContains);
        blockContains = BitVectorFactory.createZeros(chunkNum);
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += MirHint.PRP_BLOCK_OFFSET_NUM) {
            boolean[] prpBlockContains = backupHint.containsChunks(blockChunkId);
            for (int j = 0; j < prpBlockContains.length; j++) {
                if (prpBlockContains[j]) {
                    blockContains.set(blockChunkId + j, true);
                }
            }
        }
        Assert.assertEquals(separateContains, blockContains);

        // programmed backup hints
        int puncturedChunkId = x / chunkSize;
        int puncturedOffset = Math.abs(x % chunkSize);
        MirProgrammedPrimaryHint programmedPrimaryHint = new MirProgrammedPrimaryHint(backupHint, x);
        // offsets
        Assert.assertTrue(programmedPrimaryHint.contains(x));
        separateOffsets = IntStream.range(0, chunkNum)
            .map(programmedPrimaryHint::expandOffset)
            .toArray();
        Assert.assertEquals(puncturedOffset, separateOffsets[puncturedChunkId]);
        batchOffsets = programmedPrimaryHint.expandOffsets();
        Assert.assertArrayEquals(separateOffsets, batchOffsets);
        blockOffsets = new int[chunkNum];
        for (int chunkId = 0; chunkId < chunkNum; chunkId += MirHint.PRP_BLOCK_OFFSET_NUM) {
            int[] prpBlockOffsets = programmedPrimaryHint.expandPrpBlockOffsets(chunkId);
            System.arraycopy(prpBlockOffsets, 0, blockOffsets, chunkId, prpBlockOffsets.length);
        }
        Assert.assertArrayEquals(separateOffsets, blockOffsets);
        // contains
        separateContains = BitVectorFactory.createZeros(chunkNum);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (programmedPrimaryHint.containsChunkId(chunkId)) {
                separateContains.set(chunkId, true);
            }
        }
        batchContains = programmedPrimaryHint.containsChunks();
        Assert.assertEquals(separateContains, batchContains);
        blockContains = BitVectorFactory.createZeros(chunkNum);
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += MirHint.PRP_BLOCK_OFFSET_NUM) {
            boolean[] prpBlockContains = programmedPrimaryHint.containsChunks(blockChunkId);
            for (int j = 0; j < prpBlockContains.length; j++) {
                if (prpBlockContains[j]) {
                    blockContains.set(blockChunkId + j, true);
                }
            }
        }
        Assert.assertEquals(separateContains, blockContains);
    }
}
