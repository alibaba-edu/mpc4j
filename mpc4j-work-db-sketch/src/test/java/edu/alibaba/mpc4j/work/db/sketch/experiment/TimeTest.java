package edu.alibaba.mpc4j.work.db.sketch.experiment;

import edu.alibaba.mpc4j.work.db.sketch.utils.TestDataGenerator;
import edu.alibaba.mpc4j.work.db.sketch.utils.cms.CMS;
import edu.alibaba.mpc4j.work.db.sketch.utils.cms.CMSNaiveImpl;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

@RunWith(Parameterized.class)
public class TimeTest {

    private static final int CMSRowsNum=1;
    private static final int CMSColLogNum=14;
    private static final int GaussianSTD=13;
    private static final int bitLen=32;
    private static final int[] logDataSizes={23};
    private static final String[] sketchTypes={"CMS"};


    private final String sketchType;
    private final int logSketchSize;
    private final String dataType="GAUSSIAN";
    private final int logDataSize;
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
        fileAppender.setFile("CMSNaiveTime.log");
        fileAppender.setLayout(new PatternLayout("%d{MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"));
        fileAppender.setThreshold(Level.INFO);
        fileAppender.setAppend(true);
        fileAppender.activateOptions();

        // Configure root logger
        Logger.getRootLogger().addAppender(fileAppender);
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    public TimeTest(String sketchType, int logSketchSize,int logDataSize) {

        this.sketchType = sketchType;
        this.logSketchSize = logSketchSize;
        this.testDataGenerator = new TestDataGenerator(GaussianSTD);
        this.logDataSize = logDataSize;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{"CMS",CMSColLogNum,logDataSizes[0]});
        return configurations;
    }


    @Test
    public void testCMSNaive(){

        BigInteger[] hashParameters= testDataGenerator.genUpdateData(bitLen*2,CMSRowsNum*2,"UNIFORM",new Random());
        BigInteger[][] hashParameterPairs= new BigInteger[2][CMSRowsNum];
        System.arraycopy(hashParameters,0,hashParameterPairs[0],0,CMSRowsNum);
        System.arraycopy(hashParameters,CMSRowsNum,hashParameterPairs[1],0,CMSRowsNum);

        CMS cms= new CMSNaiveImpl(CMSRowsNum,1<<CMSColLogNum,hashParameterPairs,bitLen);

        BigInteger[] inputData= testDataGenerator.genUpdateData(bitLen,1<<logDataSize,dataType,new Random());

        long startTime = System.nanoTime();
        cms.input(inputData);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("Execution time: " + duration + " nanoseconds");
        System.out.println("Execution time: " + duration / 1_000_000 + " milliseconds");
        int[][] table=cms.getTable();
        for (int[] ints : table) {
            System.out.println(Arrays.toString(ints));
        }
    }

}
