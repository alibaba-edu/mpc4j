package edu.alibaba.mpc4j.dp.service.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.metrics.HeavyHitterMetrics;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory.FoLdpType;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory.HhLdpType;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.*;
import edu.alibaba.mpc4j.dp.service.heavyhitter.hg.HhgHhLdpServer;
import edu.alibaba.mpc4j.dp.service.structure.HeavyGuardian;
import edu.alibaba.mpc4j.dp.service.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Heavy Hitter LDP main class.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public class HhLdpMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(HhLdpMain.class);
    /**
     * double output format
     */
    static final DecimalFormat DOUBLE_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * time output format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * int format
     */
    private static final DecimalFormat INTEGER_DECIMAL_FORMAT = new DecimalFormat("0");
    /**
     * large data num unit, used for report current status
     */
    private static final int LARGE_DATA_NUM_UNIT = 1000000;
    /**
     * 任务类型名称
     */
    static final String TASK_TYPE_NAME = "LDP_HEAVY_HITTER";
    /**
     * server stop watch
     */
    private final StopWatch serverStopWatch;
    /**
     * client stop watch
     */
    private final StopWatch clientStopWatch;
    /**
     * report file postfix
     */
    private final String reportFilePostfix;
    /**
     * dataset name
     */
    private final String datasetName;
    /**
     * dataset path
     */
    private final String datasetPath;
    /**
     * domain set
     */
    private final Set<String> domainSet;
    /**
     * k
     */
    private final int k;
    /**
     * warmup percentage
     */
    private final double warmupPercentage;
    /**
     * warmup num
     */
    private final int warmupNum;
    /**
     * ε
     */
    private final double[] windowEpsilons;
    /**
     * α
     */
    private final double[] alphas;
    /**
     * γ_h
     */
    private final double[] gammaHs;
    /**
     * λ_l
     */
    private final int lambdaL;
    /**
     * test round
     */
    private final int testRound;
    /**
     * if run plain
     */
    private final boolean plain;
    /**
     * Frequency Oracle based type
     */
    private final List<FoLdpType> foTypeList;
    /**
     * HeavyGuardian based type
     */
    private final List<HhLdpType> hgTypeList;
    /**
     * correct heavy hitter map
     */
    private final Map<String, Integer> correctHeavyHitterMap;
    /**
     * correct heavy hitter
     */
    private final List<String> correctHeavyHitters;

    public HhLdpMain(Properties properties) throws IOException {
        serverStopWatch = new StopWatch();
        clientStopWatch = new StopWatch();
        reportFilePostfix = PropertiesUtils.readString(properties, "report_file_postfix", "");
        datasetName = PropertiesUtils.readString(properties, "dataset_name");
        // set dataset path
        datasetPath = PropertiesUtils.readString(properties, "dataset_path");
        // set domain set
        boolean containsDomainMinValue = PropertiesUtils.containsKeyword(properties, "domain_min_item");
        boolean containsDomainMaxValue = PropertiesUtils.containsKeyword(properties, "domain_max_item");
        int domainMinValue;
        int domainMaxValue;
        if (containsDomainMinValue && containsDomainMaxValue) {
            // if both values are set
            domainMinValue = PropertiesUtils.readInt(properties, "domain_min_item");
            domainMaxValue = PropertiesUtils.readInt(properties, "domain_max_item");
        } else {
            // automatically set domain
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
            domainMinValue = dataStream.mapToInt(Integer::parseInt)
                .min()
                .orElse(Integer.MIN_VALUE);
            dataStream.close();
            dataStream = StreamDataUtils.obtainItemStream(datasetPath);
            domainMaxValue = dataStream.mapToInt(Integer::parseInt)
                .max()
                .orElse(Integer.MAX_VALUE);
            dataStream.close();
        }
        MathPreconditions.checkLess("domain_min_value", domainMinValue, domainMaxValue);
        LOGGER.info("Domain Range: [{}, {}]", domainMinValue, domainMaxValue);
        domainSet = IntStream.rangeClosed(domainMinValue, domainMaxValue)
            .mapToObj(String::valueOf).collect(Collectors.toSet());
        int d = domainSet.size();
        // set heavy hitter
        k = PropertiesUtils.readInt(properties, "k");
        MathPreconditions.checkLessOrEqual("k", k, d);
        // set privacy parameters
        warmupPercentage = PropertiesUtils.readDouble(properties, "warmup_percentage");
        MathPreconditions.checkNonNegativeInRangeClosed("warmup_percentage", warmupPercentage, 1.0);
        windowEpsilons = PropertiesUtils.readDoubleArray(properties, "window_epsilon");
        lambdaL = PropertiesUtils.readIntWithDefault(properties, "lambda_l", CnrHhgHhLdpConfig.DEFAULT_LAMBDA_L);
        MathPreconditions.checkPositive("λ_l", lambdaL);
        alphas = PropertiesUtils.readDoubleArrayWithDefault(properties, "alpha");
        gammaHs = PropertiesUtils.readDoubleArrayWithDefault(properties, "gamma_h");
        Arrays.stream(gammaHs).forEach(gammaH -> MathPreconditions.checkNonNegativeInRangeClosed("γ_h", gammaH, 1.0));
        // set test round
        testRound = PropertiesUtils.readInt(properties, "test_round");
        // num and warmup num
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        int num = (int) dataStream.count();
        dataStream.close();
        warmupNum = (int) Math.round(num * warmupPercentage);
        // set if run plain
        plain = PropertiesUtils.readBoolean(properties, "plain", false);
        // set Frequency Oracle based types
        String[] foTypeStrings = PropertiesUtils.readTrimStringArrayWithDefault(properties, "fo_types");
        foTypeList = Arrays.stream(foTypeStrings)
            .map(FoLdpType::valueOf)
            .collect(Collectors.toList());
        // set HeavyGuardian based types
        String[] hgTypeStrings = PropertiesUtils.readTrimStringArrayWithDefault(properties, "hg_types");
        hgTypeList = Arrays.stream(hgTypeStrings)
            .map(HhLdpType::valueOf)
            // ignore fo types
            .filter(type -> !type.equals(HhLdpType.FO))
            .collect(Collectors.toList());
        // correct counting result
        NaiveStreamCounter streamCounter = new NaiveStreamCounter();
        dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        dataStream.forEach(streamCounter::insert);
        dataStream.close();
        Map<String, Integer> correctCountMap = domainSet.stream()
            .collect(Collectors.toMap(item -> item, streamCounter::query));
        // total item num
        long totalItemNum = correctCountMap.values().stream().mapToInt(i -> i).sum();
        LOGGER.info("Total Item Num : {}", totalItemNum);
        // correct heavy hitter
        List<Map.Entry<String, Integer>> correctOrderedList = new ArrayList<>(correctCountMap.entrySet());
        correctOrderedList.sort(Comparator.comparingInt(Map.Entry::getValue));
        Collections.reverse(correctOrderedList);
        correctHeavyHitterMap = correctOrderedList.subList(0, k).stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        correctHeavyHitters = new ArrayList<>(correctHeavyHitterMap.keySet());
        String correctHeavyHitterString = correctOrderedList.subList(0, k).stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.toList()).toString();
        LOGGER.info("Correct heavy hitters: {}", correctHeavyHitterString);
    }

    String getReportFilePostfix() {
        return reportFilePostfix;
    }

    boolean getPlain() {
        return plain;
    }

    List<FoLdpType> getFoLdpList() {
        return foTypeList;
    }

    List<HhLdpType> getHgTypeList() {
        return hgTypeList;
    }

    double[] getAlphas() {
        return alphas;
    }

    double[] getGammaHs() {
        return gammaHs;
    }

    int getLambdaL() {
        return lambdaL;
    }

    int getWarmupNum() {
        return warmupNum;
    }

    public void run() throws IOException {
        // create report file
        LOGGER.info("Create report file");
        String filePath = "".equals(reportFilePostfix)
            ? TASK_TYPE_NAME + "_" + datasetName + "_" + testRound + ".output"
            : TASK_TYPE_NAME + "_" + datasetName + "_" + testRound + "_" + reportFilePostfix + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // write warmup_percentage
        printWriter.println("warmup_percentage = " + warmupPercentage);
        // write tab
        String tab = "name\tε_w\tα\tγ_h\ts_time(s)\tc_time(s)\tcomm.(B)\tcontext(B)\tmem.(B)\t" +
            "warmup_ndcg\twarmup_precision\tndcg\tprecision\tabe\tre";
        printWriter.println(tab);
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                name", "         ε", "         α", "       γ_h",
            "           s_time(s)", "           c_time(s)",
            "            comm.(B)", "         context (B)","             mem.(B)",
            "         warmup_ndcg", "    warmup_precision",
            "                ndcg", "           precision", "                 abe", "                  re"
        );
        if (plain) {
            HhLdpAggMetrics heavyGuardianAggMetrics = runHeavyGuardian();
            printInfo(printWriter, heavyGuardianAggMetrics);
        }
        for (FoLdpType type : foTypeList) {
            for (double windowEpsilon : windowEpsilons) {
                HhLdpAggMetrics foLdpAggMetrics = runFoHeavyHitter(type, windowEpsilon);
                printInfo(printWriter, foLdpAggMetrics);
            }
        }
        if (hgTypeList.contains(HhLdpType.BGR)) {
            // consider changes of ε
            for (double windowEpsilon : windowEpsilons) {
                HhLdpAggMetrics bgrHgLdpAggMetrics = runBgrHgHeavyHitter(windowEpsilon);
                printInfo(printWriter, bgrHgLdpAggMetrics);
            }
        }
        if (hgTypeList.contains(HhLdpType.DSR)) {
            // consider changes of ε
            for (double windowEpsilon : windowEpsilons) {
                HhLdpAggMetrics dsrHgLdpAggMetrics = runDsrHgHeavyHitter(windowEpsilon);
                printInfo(printWriter, dsrHgLdpAggMetrics);
            }
        }
        if (hgTypeList.contains(HhLdpType.BDR)) {
            if (gammaHs.length > 0) {
                // manually set γ_h, do not need to run automatically setting
                for (double gammaH : gammaHs) {
                    for (double alpha : alphas) {
                        for (double windowEpsilon : windowEpsilons) {
                            HhLdpAggMetrics bdrHgLdpAggMetrics = runBdrHhgHeavyHitter(windowEpsilon, alpha, gammaH);
                            printInfo(printWriter, bdrHgLdpAggMetrics);
                        }
                    }
                }
            } else {
                // automatically set γ_h, we need warmupNum > 0
                if (warmupNum > 0) {
                    for (double alpha : alphas) {
                        for (double windowEpsilon : windowEpsilons) {
                            HhLdpAggMetrics bdrHgLdpAggMetrics = runBdrHhgHeavyHitter(windowEpsilon, alpha);
                            printInfo(printWriter, bdrHgLdpAggMetrics);
                        }
                    }
                }
            }
        }
        if (hgTypeList.contains(HhLdpType.CNR)) {
            if (gammaHs.length > 0) {
                // manually set γ_h, do not need to run automatically setting
                for (double gammaH : gammaHs) {
                    for (double alpha : alphas) {
                        for (double windowEpsilon : windowEpsilons) {
                            HhLdpAggMetrics cnrHgLdpAggMetrics = runCnrHhgHeavyHitter(windowEpsilon, alpha, gammaH);
                            printInfo(printWriter, cnrHgLdpAggMetrics);
                        }
                    }
                }
            } else {
                // automatically set γ_h, we need warmupNum > 0
                if (warmupNum > 0) {
                    for (double alpha : alphas) {
                        for (double windowEpsilon : windowEpsilons) {
                            HhLdpAggMetrics cnrHgLdpAggMetrics = runCnrHhgHeavyHitter(windowEpsilon, alpha);
                            printInfo(printWriter, cnrHgLdpAggMetrics);
                        }
                    }
                }
            }
        }
        printWriter.close();
        fileWriter.close();
    }

    private HhLdpAggMetrics runHeavyGuardian() throws IOException {
        String typeName = "PURE_HG";
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(typeName, null, null, null);
        for (int round = 0; round < testRound; round++) {
            HeavyGuardian heavyGuardian = new HeavyGuardian(1, k, 0);
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
            AtomicInteger atomicDataIndex = new AtomicInteger();
            AtomicLong payloadBytes = new AtomicLong();
            serverStopWatch.start();
            serverStopWatch.suspend();
            clientStopWatch.start();
            clientStopWatch.suspend();
            final int finalRound = round;
            dataStream.forEach(item -> {
                // report progress
                int dataIndex = atomicDataIndex.incrementAndGet();
                if (dataIndex % LARGE_DATA_NUM_UNIT == 0) {
                    LOGGER.info("round: {}, data index: {}",
                        (finalRound + 1), (dataIndex / LARGE_DATA_NUM_UNIT * LARGE_DATA_NUM_UNIT));
                }
                clientStopWatch.resume();
                byte[] itemBytes = item.getBytes(HhLdpFactory.DEFAULT_CHARSET);
                clientStopWatch.suspend();
                payloadBytes.getAndAdd(itemBytes.length);
                serverStopWatch.resume();
                String recoverItem = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
                heavyGuardian.insert(recoverItem);
                serverStopWatch.suspend();
            });
            dataStream.close();
            serverStopWatch.stop();
            long serverTimeMs = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
            serverStopWatch.reset();
            // client time
            clientStopWatch.stop();
            long clientTimeMs = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
            clientStopWatch.reset();
            long memoryBytes = GraphLayout.parseInstance(heavyGuardian).totalSize();
            // heavy hitter map
            Map<String, Double> heavyHitterMap = heavyGuardian.getRecordItemSet().stream()
                .collect(Collectors.toMap(item -> item, item -> (double) heavyGuardian.query(item)));
            // heavy hitter ordered list
            List<Map.Entry<String, Double>> heavyHitterOrderedList = new ArrayList<>(heavyHitterMap.entrySet());
            heavyHitterOrderedList.sort(Comparator.comparingDouble(Map.Entry::getValue));
            Collections.reverse(heavyHitterOrderedList);
            // heavy hitters
            List<String> heavyHitters = heavyHitterOrderedList.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            // metrics
            HhLdpMetrics metrics = new HhLdpMetrics();
            metrics.setServerTimeMs(serverTimeMs);
            metrics.setClientTimeMs(clientTimeMs);
            metrics.setPayloadBytes(payloadBytes.longValue());
            metrics.setContextBytes(0L);
            metrics.setMemoryBytes(memoryBytes);
            metrics.setNdcg(HeavyHitterMetrics.ndcg(heavyHitters, correctHeavyHitters));
            metrics.setPrecision(HeavyHitterMetrics.precision(heavyHitters, correctHeavyHitters));
            metrics.setAbe(HeavyHitterMetrics.absoluteError(heavyHitterMap, correctHeavyHitterMap));
            metrics.setRe(HeavyHitterMetrics.relativeError(heavyHitterMap, correctHeavyHitterMap));
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    private HhLdpAggMetrics runFoHeavyHitter(FoLdpType foLdpType, double windowEpsilon) throws IOException {
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(
            "FO (" + foLdpType.name() + ")", windowEpsilon, null, null
        );
        for (int round = 0; round < testRound; round++) {
            FoLdpConfig foLdpConfig = FoLdpFactory.createDefaultConfig(foLdpType, domainSet, windowEpsilon);
            HhLdpConfig hhLdpConfig = new FoHhLdpConfig
                .Builder(foLdpConfig, k)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(hhLdpConfig);
            HhLdpClient client = HhLdpFactory.createClient(hhLdpConfig);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client, round);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runBgrHgHeavyHitter(double windowEpsilon) throws IOException {
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(HhLdpType.BGR.name(), windowEpsilon, null, null);
        for (int round = 0; round < testRound; round++) {
            BgrHgHhLdpConfig config = new BgrHgHhLdpConfig
                .Builder(domainSet, k, windowEpsilon)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client, round);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runDsrHgHeavyHitter(double windowEpsilon) throws IOException {
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(HhLdpType.DSR.name(), windowEpsilon, null, null);
        for (int round = 0; round < testRound; round++) {
            DsrHgHhLdpConfig config = new DsrHgHhLdpConfig
                .Builder(domainSet, k, windowEpsilon)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client, round);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runBdrHhgHeavyHitter(double windowEpsilon, double alpha) throws IOException {
        double gammaH = 0;
        for (int round = 0; round < testRound; round++) {
            // get warmup gammaH
            BdrHhgHhLdpConfig warmupConfig = new BdrHhgHhLdpConfig
                .Builder(domainSet, k, windowEpsilon)
                .setAlpha(alpha)
                .build();
            gammaH += getWarmupHhgHeavyHitterGammaH(warmupConfig);
        }
        gammaH /= testRound;
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(
            HhLdpType.BDR.name() + " (auto γ_h)", windowEpsilon, alpha, gammaH
        );
        for (int round = 0; round < testRound; round++) {
            BdrHhgHhLdpConfig config = new BdrHhgHhLdpConfig
                .Builder(domainSet, k, windowEpsilon)
                .setAlpha(alpha)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client, round);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runBdrHhgHeavyHitter(double windowEpsilon, double alpha, double gammaH) throws IOException {
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(
            HhLdpType.BDR.name() + " (pre γ_h)", windowEpsilon, alpha, gammaH
        );
        for (int round = 0; round < testRound; round++) {
            BdrHhgHhLdpConfig config = new BdrHhgHhLdpConfig
                .Builder(domainSet, k, windowEpsilon)
                .setAlpha(alpha)
                .setGammaH(gammaH)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client, round);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runCnrHhgHeavyHitter(double windowEpsilon, double alpha) throws IOException {
        double gammaH = 0;
        for (int round = 0; round < testRound; round++) {
            // get warmup gammaH
            CnrHhgHhLdpConfig warmupConfig = new CnrHhgHhLdpConfig
                .Builder(domainSet, k, windowEpsilon)
                .setAlpha(alpha)
                .setLambdaL(lambdaL)
                .build();
            gammaH += getWarmupHhgHeavyHitterGammaH(warmupConfig);
        }
        gammaH /= testRound;
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(
            HhLdpType.CNR.name() + " (auto γ_h)", windowEpsilon, alpha, gammaH
        );
        for (int round = 0; round < testRound; round++) {
            CnrHhgHhLdpConfig config = new CnrHhgHhLdpConfig
                .Builder(domainSet, k, windowEpsilon)
                .setAlpha(alpha)
                .setLambdaL(lambdaL)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client, round);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runCnrHhgHeavyHitter(double windowEpsilon, double alpha, double gammaH) throws IOException {
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(
            HhLdpType.CNR.name() + " (pre γ_h)", windowEpsilon, alpha, gammaH
        );
        for (int round = 0; round < testRound; round++) {
            CnrHhgHhLdpConfig config = new CnrHhgHhLdpConfig
                .Builder(domainSet, k, windowEpsilon)
                .setAlpha(alpha)
                .setGammaH(gammaH)
                .setLambdaL(lambdaL)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client, round);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    private double getWarmupHhgHeavyHitterGammaH(HgHhLdpConfig warmupConfig) throws IOException {
        HhgHhLdpServer warmupServer = (HhgHhLdpServer) HhLdpFactory.createServer(warmupConfig);
        HhLdpClient warmupClient = HhLdpFactory.createClient(warmupConfig);
        // warmup
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= warmupNum)
            .map(warmupClient::warmup)
            .forEach(warmupServer::warmupInsert);
        dataStream.close();
        warmupServer.stopWarmup();
        return warmupServer.getGammaH();
    }

    HhLdpMetrics runLdpHeavyHitter(HhLdpServer server, HhLdpClient client, int round) throws IOException {
        // metrics
        HhLdpMetrics metrics = new HhLdpMetrics();
        // warmup
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= warmupNum)
            .peek(item -> {
                // report progress
                int dataIndex = warmupIndex.intValue();
                if (dataIndex % LARGE_DATA_NUM_UNIT == 0) {
                    LOGGER.info("round: {}, data index (for warmup): {}",
                        (round + 1), dataIndex / LARGE_DATA_NUM_UNIT * LARGE_DATA_NUM_UNIT);
                }
            })
            .map(client::warmup)
            .forEach(server::warmupInsert);
        dataStream.close();
        server.stopWarmup();
        // warmup information
        List<String> warmupHeavyHitters = server.orderedHeavyHitters().stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        if (warmupHeavyHitters.size() == k) {
            metrics.setWarmupNdcg(HeavyHitterMetrics.ndcg(warmupHeavyHitters, correctHeavyHitters));
            metrics.setWarmupPrecision(HeavyHitterMetrics.precision(warmupHeavyHitters, correctHeavyHitters));
        }
        // randomize
        serverStopWatch.start();
        serverStopWatch.suspend();
        clientStopWatch.start();
        clientStopWatch.suspend();
        AtomicLong payloadBytes = new AtomicLong();
        AtomicLong contextBytes = new AtomicLong();
        AtomicInteger randomizedIndex = new AtomicInteger();
        dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        dataStream.filter(item -> randomizedIndex.getAndIncrement() > warmupNum)
            .peek(item -> {
                // report progress
                int dataIndex = randomizedIndex.intValue() - warmupNum;
                if (dataIndex % LARGE_DATA_NUM_UNIT == 0) {
                    LOGGER.info("round: {}, data index (for randomized): {}",
                        (round + 1), (dataIndex / LARGE_DATA_NUM_UNIT * LARGE_DATA_NUM_UNIT));
                }
            })
            .forEach(item -> {
            clientStopWatch.resume();
            byte[] itemBytes = client.randomize(server.getServerContext(), item);
            clientStopWatch.suspend();
            payloadBytes.getAndAdd(itemBytes.length);
            contextBytes.getAndAdd(server.getServerContext().toClientInfo().length);
            serverStopWatch.resume();
            server.randomizeInsert(itemBytes);
            serverStopWatch.suspend();
        });
        dataStream.close();
        serverStopWatch.stop();
        long serverTimeMs = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        // client time
        clientStopWatch.stop();
        long clientTimeMs = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long memoryBytes = GraphLayout.parseInstance(server).totalSize();
        // final information
        Map<String, Double> heavyHitterMap = server.heavyHitters();
        Preconditions.checkArgument(heavyHitterMap.size() == k);
        // ordered heavy hitter
        List<String> heavyHitters = server.orderedHeavyHitters().stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        metrics.setServerTimeMs(serverTimeMs);
        metrics.setClientTimeMs(clientTimeMs);
        metrics.setPayloadBytes(payloadBytes.longValue());
        metrics.setContextBytes(contextBytes.longValue());
        metrics.setMemoryBytes(memoryBytes);
        metrics.setNdcg(HeavyHitterMetrics.ndcg(heavyHitters, correctHeavyHitters));
        metrics.setPrecision(HeavyHitterMetrics.precision(heavyHitters, correctHeavyHitters));
        metrics.setAbe(HeavyHitterMetrics.absoluteError(heavyHitterMap, correctHeavyHitterMap));
        metrics.setRe(HeavyHitterMetrics.relativeError(heavyHitterMap, correctHeavyHitterMap));
        return metrics;
    }

    private void printInfo(PrintWriter printWriter, HhLdpAggMetrics aggMetrics) {
        String typeString = aggMetrics.getTypeString();
        String windowEpsilonString = aggMetrics.getWindowEpsilonString();
        String alphaString = aggMetrics.getAlphaString();
        String gammaString = aggMetrics.getGammaString();
        double serverTime = aggMetrics.getServerTimeSecond();
        double clientTime = aggMetrics.getClientTimeSecond();
        long payloadBytes = aggMetrics.getPayloadBytes();
        long contextBytes = aggMetrics.getContextBytes();
        long memoryBytes = aggMetrics.getMemoryBytes();
        double warmupNdcg = aggMetrics.getWarmupNdcg();
        double warmupPrecision = aggMetrics.getWarmupPrecision();
        double ndcg = aggMetrics.getNdcg();
        double precision = aggMetrics.getPrecision();
        double abe = aggMetrics.getAbe();
        double re = aggMetrics.getRe();
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            StringUtils.leftPad(typeString, 20),
            StringUtils.leftPad(windowEpsilonString, 10),
            StringUtils.leftPad(alphaString, 10),
            StringUtils.leftPad(gammaString, 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(serverTime), 20),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(clientTime), 20),
            StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(payloadBytes), 20),
            StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(contextBytes), 20),
            StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(memoryBytes), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(warmupNdcg), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(warmupPrecision), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(ndcg), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(precision), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(abe), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(re), 20)
        );
        printWriter.println(
            typeString + "\t" + windowEpsilonString + "\t" + alphaString + "\t" + gammaString + "\t"
                + serverTime + "\t" + clientTime + "\t" + payloadBytes + "\t" + contextBytes + "\t" + memoryBytes + "\t"
                + warmupNdcg + "\t" + warmupPrecision + "\t" + ndcg + "\t" + precision + "\t" + abe + "\t" + re
        );
    }
}
