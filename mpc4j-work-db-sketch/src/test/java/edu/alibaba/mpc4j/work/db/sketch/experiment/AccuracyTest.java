package edu.alibaba.mpc4j.work.db.sketch.experiment;

import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.work.db.sketch.utils.TestDataGenerator;
import edu.alibaba.mpc4j.work.db.sketch.utils.cms.CMS;
import edu.alibaba.mpc4j.work.db.sketch.utils.cms.CMSv1BatchImpl;
import edu.alibaba.mpc4j.work.db.sketch.utils.gk.GK;
import edu.alibaba.mpc4j.work.db.sketch.utils.gk.GKBatchImpl;
import edu.alibaba.mpc4j.work.db.sketch.utils.hll.HLL;
import edu.alibaba.mpc4j.work.db.sketch.utils.hll.HLLImpl;
import edu.alibaba.mpc4j.work.db.sketch.utils.mg.MG;
import edu.alibaba.mpc4j.work.db.sketch.utils.mg.MGBatchImpl;
import edu.alibaba.mpc4j.work.db.sketch.utils.mg.SSBatchImpl;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.*;


@RunWith(Parameterized.class)
public class AccuracyTest {
    //configurations of input data
    private static final int[] dataSizes={1000000,10000000};
    //the bit length of generated element
    private static final int bitLen=32;

    //configurations of sketches
    private static final String[] sketchTypes={"CMS","HLL","MG","GK"};
//    private static final String[] sketchTypes={"MG"};
    private static final int[] logSketchSizes={14,15,16,17};
    private static final String[] DataType={"GAUSSIAN","UNIFORM","AOL","NETFLIX"};
    private static final double[] GKEpsilon={0.0005,0.001,0.005,0.01,};
    private static final int CMSRowsNum=7;
    private static final int MGTopK=1000;
    private static final int QueryNum=100;
    private static final int GaussianSTD=13;


    private final String sketchType;
    private final int logSketchSize;
    private final String dataType;
    private final double GKEps;
    private static final Logger logger;
    private final TestDataGenerator testDataGenerator;

    static {
        configureLogger();
        logger = Logger.getLogger(AccuracyTest.class);
    }
    public static void configureLogger() {
        // Create file appender
        FileAppender fileAppender = new FileAppender();
        fileAppender.setName("FileLogger");
        fileAppender.setFile("STD"+GaussianSTD+".log");
        fileAppender.setLayout(new PatternLayout("%d{MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"));
        fileAppender.setThreshold(Level.INFO);
        fileAppender.setAppend(true);
        fileAppender.activateOptions();

        // Configure root logger
        Logger.getRootLogger().addAppender(fileAppender);
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        for (String sketchType: sketchTypes) {
            if(sketchType.equals("GK")){
                for(double epsilon: GKEpsilon){
                    configurations.add(new Object[]{sketchType,0,epsilon,"GAUSSIAN"});
                }
            }
            else {
                for (int logSketchSize : logSketchSizes) {
                    configurations.add(new Object[]{
                            sketchType, logSketchSize,0,"GAUSSIAN"});
                }
            }
        }
        return configurations;
    }

    public AccuracyTest(String sketchType, int logSketchSize,double GKEps,String dataType) {

        this.sketchType = sketchType;
        this.logSketchSize = logSketchSize;
        this.GKEps = GKEps;
        this.dataType = dataType;
        this.testDataGenerator = new TestDataGenerator(GaussianSTD);
    }

    @Test
    public void testSmallSize() {
        int dataSize=dataSizes[0];
        testAccuracy(dataSize);
    }

    @Test
    public void testLargeSize() {
        int dataSize=dataSizes[1];
        testAccuracy(dataSize);
    }

    public void testAccuracy(int dataSize) {
        BigInteger[] inputData= testDataGenerator.genUpdateData(bitLen,dataSize,dataType,new Random());
        switch(sketchType){
            case "CMS":{
                testCMSAccuracy(inputData,logSketchSize,QueryNum);
                break;
            }
            case "HLL":{
                testHLLAccuracy(inputData,logSketchSize);
                break;
            }
            case "MG":{
//                testMGAccuracy(inputData,logSketchSize,QueryNum);
                testSSAccuracy(inputData,logSketchSize,QueryNum);
                break;
            }
            case "GK":{
                testGKAccuracy(inputData,GKEps,QueryNum);
                break;
            }
            default:{
                throw new IllegalArgumentException("Invalid sketch type");
            }
        }
    }

    private void testCMSAccuracy(BigInteger[] inputData,int logSketchSize,int queryNum){
        Map<BigInteger, BigInteger> actualCount=new HashMap<>();
        for(BigInteger key: inputData){
            actualCount.put(key, actualCount.getOrDefault(key, BigInteger.ZERO).add(BigInteger.ONE));
        }

        int t=1<<logSketchSize;
        BigInteger[] hashParameters= testDataGenerator.genUpdateData(bitLen*2,CMSRowsNum*2,"UNIFORM",new Random());
        BigInteger[][] hashParameterPairs= new BigInteger[2][CMSRowsNum];
        System.arraycopy(hashParameters,0,hashParameterPairs[0],0,CMSRowsNum);
        System.arraycopy(hashParameters,CMSRowsNum,hashParameterPairs[1],0,CMSRowsNum);

        CMS cms=new CMSv1BatchImpl(CMSRowsNum,t,hashParameterPairs,bitLen);
        cms.input(inputData);

        BigInteger[] queryKeys= genQueryKeys(inputData,queryNum);

        double[] relativeErrors=new double[queryNum];
        BigInteger[] accurateResults=new BigInteger[queryNum];
        BigInteger[] absoluteErrors=new BigInteger[queryNum];
        BigInteger count=BigInteger.ZERO;
        for(int i=0;i<queryNum;i++){
            BigInteger accurateRes=actualCount.getOrDefault(queryKeys[i], BigInteger.ZERO);
            accurateResults[i]=accurateRes;
            count=count.add(accurateRes);
            BigInteger sketchRes=BigInteger.valueOf(cms.query(queryKeys[i]));
            absoluteErrors[i]=sketchRes.subtract(accurateRes);
            relativeErrors[i]= absoluteErrors[i].doubleValue()/accurateRes.doubleValue();
        }
        double absoluteError=Arrays.stream(absoluteErrors).reduce(BigInteger.ZERO,BigInteger::add).doubleValue();
        double relativeError=absoluteError/count.doubleValue();
        absoluteError=absoluteError/queryNum;

        logger.info("CMS\t"+"DataSize: "+inputData.length+"\tL0 value: "+actualCount.size()+"\tSketchSize: "+logSketchSize+"\tDataType: "+dataType);
        logger.info("absoluteError: "+absoluteError+"\trelativeError: "+relativeError);
    }
    private void testHLLAccuracy(BigInteger[] inputData,int logSketchSize){
        int times=12;
        double[] relativeErrors=new double[times];
        double[] absoluteErrors=new double[times];
        Map<BigInteger, BigInteger> actualCount = new HashMap<>();
        for (BigInteger key : inputData) {
            actualCount.put(key, BigInteger.ZERO);
        }
        int accurateL0 = actualCount.size();
        for(int i=0;i<times;i++) {
            PlainZ2Vector key = PlainZ2Vector.createRandom(CommonConstants.BLOCK_BIT_LENGTH, new Random());
            HLL hll = new HLLImpl(1 << logSketchSize, key, 30);
            hll.input(inputData);
            double sketchL0 = hll.query();

            double absoluteError = Math.abs(accurateL0 - sketchL0);
            double relativeError = absoluteError / accurateL0;
            relativeErrors[i]=relativeError;
            absoluteErrors[i]=absoluteError;
        }
        double max=Double.NEGATIVE_INFINITY;
        double min=Double.MAX_VALUE;
        for(int i=0;i<times;i++){
            if(absoluteErrors[i]>max){
                max = absoluteErrors[i];
            }
            if(absoluteErrors[i]<min){
                min = absoluteErrors[i];
            }
        }
        double average= (Arrays.stream(absoluteErrors).sum()-max-min)/(times-2);
        double relativeError=average/accurateL0;
        logger.info("HLL\t"+"DataSize: "+inputData.length+"\tL0 value:"+actualCount.size()+"\tSketchSize: "+logSketchSize+"\tDataType: "+dataType);
        logger.info("\taverage absoluteError: "+average+"\trelativeError: "+relativeError);
    }
    private void testMGAccuracy(BigInteger[] inputData,int logSketchSize,int queryNum){
        TreeMap<BigInteger, BigInteger> histogram=new TreeMap<>();
        for(BigInteger key: inputData){
            histogram.put(key, histogram.getOrDefault(key, BigInteger.ZERO).add(BigInteger.ONE));
        }
        MG mg=new MGBatchImpl(1L <<logSketchSize);
        mg.input(inputData);

        BigInteger[] queryKeys= genQueryKeys(inputData,queryNum);

        double[] relativeErrors=new double[queryNum];
        BigInteger[] absoluteErrors=new BigInteger[queryNum];
        BigInteger[] accurateReses=new BigInteger[queryNum];
        BigInteger[] sketchReses=new BigInteger[queryNum];
        for(int i=0;i<queryNum;i++){
            BigInteger accurateRes=histogram.getOrDefault(queryKeys[i], BigInteger.ZERO);
            BigInteger sketchRes=mg.query(queryKeys[i]);

            absoluteErrors[i]=accurateRes.subtract(sketchRes);
            relativeErrors[i]= absoluteErrors[i].doubleValue()/accurateRes.doubleValue();

            accurateReses[i]=accurateRes;
            sketchReses[i]=sketchRes;
        }
        double absoluteError= Arrays.stream(absoluteErrors).reduce(BigInteger.ZERO,BigInteger::add).doubleValue();
        double relativeError= absoluteError/ Arrays.stream(accurateReses).reduce(BigInteger.ZERO,BigInteger::add).doubleValue();

        absoluteError=absoluteError/queryNum;
        Map.Entry[] descMap=Arrays.stream(histogram.entrySet().toArray())
                .sorted((e1, e2)->
                        ((Map.Entry<BigInteger,BigInteger>)e2).getValue().compareTo(((Map.Entry<BigInteger,BigInteger>)e1)
                                .getValue())).toArray(Map.Entry[]::new);
        Map.Entry<BigInteger,BigInteger>[] realTopK=new Map.Entry[MGTopK];
        System.arraycopy(descMap,0,realTopK,0,MGTopK);

        Map<BigInteger, BigInteger> topK=mg.query(MGTopK);

        int count=0;
        for(Map.Entry<BigInteger, BigInteger> entry: realTopK){
            if(topK.containsKey(entry.getKey())){
                count++;
            }
        }
        double recallRate= (double) count /MGTopK;
        logger.info("MG\t"+"DataSize: "+inputData.length+"\tSketchSize: "+logSketchSize+"\tDataType: "+dataType);
        logger.info("absoluteError: "+absoluteError+"\trelativeError: "+relativeError+"\trecallRate: "+recallRate);
    }
    private void testSSAccuracy(BigInteger[] inputData,int logSketchSize,int queryNum) {
        TreeMap<BigInteger, BigInteger> histogram = new TreeMap<>();
        for (BigInteger key : inputData) {
            histogram.put(key, histogram.getOrDefault(key, BigInteger.ZERO).add(BigInteger.ONE));
        }
        MG ss = new SSBatchImpl(1L << logSketchSize);
        ss.input(inputData);

        BigInteger[] queryKeys = genQueryKeys(inputData, queryNum);

        double[] relativeErrors = new double[queryNum];
        BigInteger[] absoluteErrors = new BigInteger[queryNum];
        BigInteger[] accurateReses = new BigInteger[queryNum];
        BigInteger[] sketchReses = new BigInteger[queryNum];
        for (int i = 0; i < queryNum; i++) {
            BigInteger accurateRes = histogram.getOrDefault(queryKeys[i], BigInteger.ZERO);
            BigInteger sketchRes = ss.query(queryKeys[i]);

            absoluteErrors[i] = accurateRes.subtract(sketchRes);
            relativeErrors[i] = absoluteErrors[i].doubleValue() / accurateRes.doubleValue();

            accurateReses[i] = accurateRes;
            sketchReses[i] = sketchRes;
        }
        double absoluteError = Arrays.stream(absoluteErrors).reduce(BigInteger.ZERO, BigInteger::add).doubleValue();
        double relativeError = absoluteError / Arrays.stream(accurateReses).reduce(BigInteger.ZERO, BigInteger::add).doubleValue();

        absoluteError = absoluteError / queryNum;

        Map.Entry[] descMap=Arrays.stream(histogram.entrySet().toArray())
                .sorted((e1, e2)->
                        ((Map.Entry<BigInteger,BigInteger>)e2).getValue().compareTo(((Map.Entry<BigInteger,BigInteger>)e1)
                                .getValue())).toArray(Map.Entry[]::new);
        Map.Entry<BigInteger,BigInteger>[] realTopK=new Map.Entry[MGTopK];
        System.arraycopy(descMap,0,realTopK,0,MGTopK);

        Map<BigInteger, BigInteger> topK=ss.query(MGTopK);

        int count=0;
        for(Map.Entry<BigInteger, BigInteger> entry: realTopK){
            if(topK.containsKey(entry.getKey())){
                count++;
            }
        }
        double recallRate= (double) count /MGTopK;
        logger.info("SS\t"+"DataSize: "+inputData.length+"\tSketchSize: "+logSketchSize+"\tDataType: "+dataType);
        logger.info("absoluteError: "+absoluteError+"\trelativeError: "+relativeError+"\trecallRate: "+recallRate);
    }
        private void testGKAccuracy(BigInteger[] inputData,double epsilon,int queryNum){
        TreeMap<BigInteger, BigInteger> histogram=new TreeMap<>();
        for(BigInteger key: inputData){
            histogram.put(key, histogram.getOrDefault(key, BigInteger.ZERO).add(BigInteger.ONE));
        }

        GK gk=new GKBatchImpl((float) epsilon);
        gk.input(inputData);

        BigInteger[] queryKeys= genQueryKeys(inputData,queryNum);

        double[] relativeErrors=new double[queryNum];
        BigInteger[] absoluteErrors=new BigInteger[queryNum];
        for(int i=0;i<queryNum;i++){
            BigInteger accurateRes=getTrueRank(queryKeys[i],histogram);
            BigInteger sketchRes=gk.query(queryKeys[i]);
            absoluteErrors[i]=sketchRes.subtract(accurateRes).abs();
            relativeErrors[i]= absoluteErrors[i].doubleValue()/accurateRes.doubleValue();
        }
        BigInteger absoluteError=BigInteger.ZERO;
        double relativeError=0;
        for(int i=0;i<queryNum;i++){
            absoluteError=absoluteError.add(absoluteErrors[i]);
            relativeError+=relativeErrors[i];
        }
        absoluteError=absoluteError.divide(BigInteger.valueOf(queryNum));
        relativeError=relativeError/queryNum;
        logger.info("GK\t"+"DataSize: "+inputData.length+"\tepsilon: "+epsilon+"\tDataType: "+dataType+"\tabsoluteError: ");
        logger.info(absoluteError+"\trelativeError: "+relativeError);
    }
    private BigInteger getTrueRank(BigInteger key,TreeMap<BigInteger, BigInteger> histogram){
        BigInteger accurateRes=histogram.headMap(key)
                .values().stream().reduce(BigInteger.ZERO, BigInteger::add)
                .add(histogram.get(key).shiftRight(1));
        return accurateRes;
    }

    private BigInteger[] genQueryKeys(BigInteger[] pool, int queryNum){
        return selectRandomIndices(pool, queryNum).toArray(new BigInteger[0]);
    }
    public static <T> List<T> selectRandomIndices(T[] array, int m) {
        if (m > array.length) {
            throw new IllegalArgumentException("m cannot be larger than array length");
        }
        if (m < 0) {
            throw new IllegalArgumentException("m cannot be negative");
        }

        if (m == 0) {
            return new ArrayList<>();
        }

        Random random = new Random();
        Set<Integer> selectedIndices = new HashSet<>();
        List<T> result = new ArrayList<>(m);

        while (selectedIndices.size() < m) {
            int randomIndex = random.nextInt(array.length);
            if (selectedIndices.add(randomIndex)) {
                result.add(array[randomIndex]);
            }
        }

        return result;
    }
}
