package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23ByteEccDdhPmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23EccDdhPmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23PsOprfPmPeqtConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Permuted Matrix PEQT test.
 *
 * @author Liqiang Peng
 * @date 2024/3/6
 */
@RunWith(Parameterized.class)
public class PmPeqtTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PmPeqtTest.class);
    /**
     * default row
     */
    private static final int DEFAULT_ROW = 30;
    /**
     * default column
     */
    private static final int DEFAULT_COLUMN = 70;
    /**
     * large row
     */
    private static final int LARGE_ROW = 1000;
    /**
     * small byte length
     */
    private static final int SMALL_BYTE_LENGTH = CommonConstants.STATS_BYTE_LENGTH;
    /**
     * default byte length
     */
    private static final int DEFAULT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * large byte length
     */
    private static final int LARGE_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH * 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // TCL23 + Permute Share and OPRF
        configurations.add(new Object[]{
            PmPeqtFactory.PmPeqtType.TZL23_PS_OPRF.name(),
            new Tcl23PsOprfPmPeqtConfig.Builder(true).build(),
        });
        // TCL23 + Byte Ecc DDH
        configurations.add(new Object[]{
            PmPeqtFactory.PmPeqtType.TCL23_BYTE_ECC_DDH.name(),
            new Tcl23ByteEccDdhPmPeqtConfig.Builder().build(),
        });
        // TCL23 + Ecc DDH
        configurations.add(new Object[]{
            PmPeqtFactory.PmPeqtType.TCL23_ECC_DDH.name(),
            new Tcl23EccDdhPmPeqtConfig.Builder().build(),
        });

        return configurations;
    }

    private final PmPeqtConfig config;

    public PmPeqtTest(String name, PmPeqtConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSpecial() {
        testPto(17, 29, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test2N() {
        testPto(2, 2, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test3N() {
        testPto(3, 3, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test4N() {
        testPto(4, 4, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test5N() {
        testPto(5, 5, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testSmallByteLength() {
        testPto(DEFAULT_ROW, DEFAULT_COLUMN, SMALL_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeByteLength() {
        testPto(DEFAULT_ROW, DEFAULT_COLUMN, LARGE_BYTE_LENGTH, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_ROW, DEFAULT_COLUMN, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_ROW, DEFAULT_COLUMN, DEFAULT_BYTE_LENGTH, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_ROW, DEFAULT_COLUMN, LARGE_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_ROW, DEFAULT_COLUMN, LARGE_BYTE_LENGTH, true);
    }

    private void testPto(int row, int column, int byteLength, boolean parallel) {
        PmPeqtSender sender = PmPeqtFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PmPeqtReceiver receiver = PmPeqtFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, row = {}, column = {}, byte_length = {}-----",
                sender.getPtoDesc().getPtoName(), row, column, byteLength);
            // generate input matrix
            Vector<byte[]> senderInput = IntStream.range(0, row * column / 2)
                .mapToObj(index -> {
                    byte[] input = new byte[byteLength];
                    SECURE_RANDOM.nextBytes(input);
                    return input;
                })
                .collect(Collectors.toCollection(Vector::new));
            Vector<byte[]> receiverInput = IntStream.range(0, row * column / 2)
                .mapToObj(index -> {
                    byte[] input = new byte[byteLength];
                    SECURE_RANDOM.nextBytes(input);
                    return input;
                })
                .collect(Collectors.toCollection(Vector::new));
            for (int i = row * column / 2; i < row * column; i++) {
                byte[] input = new byte[byteLength];
                SECURE_RANDOM.nextBytes(input);
                senderInput.add(input);
                receiverInput.add(input);
            }
            // row permutation map
            List<Integer> shufflePermutationMap = IntStream.range(0, row).boxed().collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] rowPermutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
            // column permutation map
            shufflePermutationMap = IntStream.range(0, column).boxed().collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] columnPermutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
            byte[][][] senderInputMatrix = new byte[row][column][byteLength];
            byte[][][] receiverInputMatrix = new byte[row][column][byteLength];
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
                    senderInputMatrix[i][j] = BytesUtils.clone(senderInput.get(i * column + j));
                    receiverInputMatrix[i][j] = BytesUtils.clone(receiverInput.get(i * column + j));
                }
            }
            PmPeqtSenderThread senderThread = new PmPeqtSenderThread(
                sender, senderInputMatrix, byteLength, rowPermutationMap, columnPermutationMap
            );
            PmPeqtReceiverThread receiverThread = new PmPeqtReceiverThread(
                receiver, receiverInputMatrix, byteLength, row, column
            );
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            boolean[][] receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(senderInput, receiverInput, rowPermutationMap, columnPermutationMap, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Vector<byte[]> senderInput, Vector<byte[]> receiverInput,
                              int[] rowPermutationMap, int[] columnPermutationMap, boolean[][] receiverOutput) {
        int row = rowPermutationMap.length;
        int column = columnPermutationMap.length;
        Assert.assertEquals(senderInput.size(), row * column);
        Assert.assertEquals(receiverInput.size(), row * column);
        Assert.assertEquals(receiverOutput.length, row);
        IntStream.range(0, row).forEach(i -> Assert.assertEquals(receiverOutput[i].length, column));
        byte[][][] senderPermutedInput = new byte[row][column][];
        byte[][][] receiverPermutedInput = new byte[row][column][];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                senderPermutedInput[i][j] = BytesUtils.clone(
                    senderInput.get(rowPermutationMap[i] * column + columnPermutationMap[j])
                );
                receiverPermutedInput[i][j] = BytesUtils.clone(
                    receiverInput.get(rowPermutationMap[i] * column + columnPermutationMap[j])
                );
            }
        }
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                boolean expectedValue = BytesUtils.equals(senderPermutedInput[i][j], receiverPermutedInput[i][j]);
                Assert.assertEquals(expectedValue, receiverOutput[i][j]);
            }
        }
    }
}
