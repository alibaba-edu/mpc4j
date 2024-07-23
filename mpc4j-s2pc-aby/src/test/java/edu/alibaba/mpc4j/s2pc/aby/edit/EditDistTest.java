package edu.alibaba.mpc4j.s2pc.aby.edit;

import com.google.common.base.Preconditions;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag.S2pcDiagEditDistConfig;
import edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag.S2pcDiagEditDistPtoDesc;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Edit distance test.
 *
 * @author Feng Han, Li Peng
 * @date 2024/4/12
 */
@RunWith(Parameterized.class)
public class EditDistTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditDistTest.class);
    /**
     * resource file name.
     */
    private static final String EXAMPLE_FILE = "sim example.csv";
    /**
     * use silent ot.
     */
    private static final boolean SILENT = false;
    /**
     * need extend zl.
     */
    private static final boolean NEED_EXTEND = true;
    /**
     * the number of increment of zl length in a single extend step.
     */
    private static final int INCREMENT = 2;
    /**
     * need to prune unneeded cells.
     */
    private static final boolean NEED_PRUNE = true;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            S2pcDiagEditDistPtoDesc.getInstance().getPtoName(),
            new S2pcDiagEditDistConfig.Builder(SILENT)
                .setNeedExtend(NEED_EXTEND).setIncrement(INCREMENT).setNeedPrune(NEED_PRUNE).build()
        });

        return configurations;
    }

    private final DistCmpConfig config;

    public EditDistTest(String name, DistCmpConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSimple() {
        testEdit("wertq", "qwedq");
    }

    @Test
    public void testEmpty() {
        testEdit("", "");
    }

    @Test
    public void testEmptyLeft() {
        testEdit("", "123");
    }

    @Test
    public void testEmptyRight() {
        testEdit("wertq123", "");
    }

    @Test
    public void testLarge() {
        testEdit("chongqing xin xiangfeng import/export", "sk files");
    }

    @Test
    public void testLargeEqual() {
        testEdit("chongqing xin xiangfeng import/export", "chongqing xin xiangfeng import/export");
    }

    @Test
    public void testArray() {
        testEdit(new String[]{"abc", "aaa"}, new String[]{"123", "acd"}, false);
    }

    @Test
    public void testArray2() {
        testEdit(new String[]{
                "yangjiang luban industry&trade",
                "shenzhen dingshengtao industrial"
            },
            new String[]{
                "trade",
                "chongqing xin xiangfeng import/export"
            }, false);
    }

    @Test
    public void testDot() {
        testEdit("shenzhen honour ocean shipping", "j.p morgan");
    }

    @Test
    public void testFile() throws IOException, CsvValidationException {
        String[][] data = readCsv();
        int limit = data[0].length;
        testEdit(Arrays.copyOf(data[0], limit), Arrays.copyOf(data[1], limit), true);
    }

    /**
     * Computing edit distance in plain.
     *
     * @param word1 word from receiver.
     * @param word2 word from sender.
     * @return edit distance.
     */
    public static int plainEditDist(String word1, String word2) {
        if (word1 == null || word1.isEmpty()) {
            return word2.length();
        }
        if (word2 == null || word2.isEmpty()) {
            return word1.length();
        }

        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        int n = word1.length();
        int m = word2.length();

        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i < n + 1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j < m + 1; j++) {
            dp[0][j] = j;
        }

        int maxNum = Math.max(m, n);

        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                // pruning
                if (Math.abs(2 * (j - i) + n - m) > maxNum) {
                    dp[i][j] = maxNum + 1;
                    continue;
                }
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1])) + 1;
                }
            }
        }
        return dp[n][m];
    }

    /**
     * Read csv file.
     *
     * @return two data arrays.
     */
    public String[][] readCsv() throws IOException, CsvValidationException {
        List<String> senderRecords = new ArrayList<>();
        List<String> receiverRecords = new ArrayList<>();
        InputStream input = getClass().getClassLoader().getResourceAsStream(EXAMPLE_FILE);
        Preconditions.checkArgument(input != null, "input stream must not be null");
        CSVReader csvReader = new CSVReader(new
            InputStreamReader(input));
        String[] line;
        int count = 0;
        while ((line = csvReader.readNext()) != null) {
            // skip header
            if (count++ == 0) {
                continue;
            }
            Preconditions.checkArgument(line.length == 2, "values.length must equal to 2.");
            senderRecords.add(line[0]);
            receiverRecords.add(line[1]);
        }
        return new String[][]{senderRecords.toArray(new String[0]), receiverRecords.toArray(new String[0])};
    }

    private void testEdit(String senderStr, String receiverStr) {
        testEdit(new String[]{senderStr}, new String[]{receiverStr}, false);
    }

    private void testEdit(String[] senderStr, String[] receiverStr, boolean parallel) {
        // init z2 circuit
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, SILENT);
        Z2cParty z2cSender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), z2cConfig);
        Z2cParty z2cReceiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), z2cConfig);
        // init edit
        DistCmpSender sender = EditDistFactory.createSender(z2cSender, secondRpc.ownParty(), config);
        DistCmpReceiver receiver = EditDistFactory.createReceiver(z2cReceiver, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        receiver.setTaskId(randomTaskId);
        sender.setTaskId(randomTaskId);
        receiver.setParallel(parallel);
        sender.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", receiver.getPtoDesc().getPtoName());
            int maxLength = 0;
            for (String each : receiverStr) {
                maxLength = Math.max(maxLength, each.length());
            }
            for (String each : senderStr) {
                maxLength = Math.max(maxLength, each.length());
            }
            EditDistReceiverThread receiverThread = new EditDistReceiverThread(receiver, receiverStr, maxLength);
            EditDistSenderThread senderThread = new EditDistSenderThread(sender, senderStr, maxLength);
            // start
            STOP_WATCH.start();
            receiverThread.start();
            senderThread.start();
            // stop
            receiverThread.join();
            senderThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            printAndResetRpc(time);
            int[] compRes = senderThread.getRes();
            int[] realRes = IntStream.range(0, senderStr.length).map(i -> plainEditDist(senderStr[i], receiverStr[i])).toArray();
            LOGGER.info("compRes:{}", Arrays.toString(compRes));
            LOGGER.info("realRes:{}", Arrays.toString(realRes));
            Assert.assertArrayEquals(realRes, compRes);
            LOGGER.info("-----test {} end, time cost: {}ms-----", receiver.getPtoDesc().getPtoName(), time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

