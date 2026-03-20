package edu.alibaba.mpc4j.work.db.sketch.utils;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.zip.CRC32;

/**
 * Data Generator for streaming data sketches.
 * 
 * This class generates test data for sketch protocols with various distributions:
 * - GAUSSIAN: Gaussian distribution
 * - UNIFORM: Uniform distribution
 * - AOL: Real-world data from AOL search logs
 * - NETFLIX: Real-world data from Netflix movie ratings
 * - DISTINCT: Distinct values from 1 to m
 */
public class DataGenerator {
    /**
     * Map of file readers for real-world datasets
     */
    private final Map<String,BufferedReader> readerMap;
    /**
     * Bit mask for element value range
     */
    private long mask;
    
    /**
     * Creates a new DataGenerator instance
     */
    public DataGenerator() {
        this.readerMap = new HashMap<String,BufferedReader>();
    }

    /**
     * Generates update data for sketch protocols
     * 
     * @param elementBitLen bit length of each element
     * @param dataSize number of elements to generate
     * @param dataType type of data distribution (GAUSSIAN, UNIFORM, AOL, NETFLIX, DISTINCT)
     * @param random random number generator
     * @return array of BigInteger values
     */
    /**
     * Generates update data with specified distribution and bit length
     *
     * @param elementBitLen bit length for each element
     * @param dataSize number of data elements to generate
     * @param dataType type of distribution: GAUSSIAN, UNIFORM, AOL, NETFLIX, or DISTINCT
     * @param random random number generator
     * @return array of BigInteger values representing the generated data
     */
    public BigInteger[] genUpdateData(int elementBitLen, int dataSize, String dataType, Random random) {
        this.mask=(1L<<elementBitLen)-1;
        return switch (dataType) {
            case "GAUSSIAN" -> genGaussianData(elementBitLen, dataSize,random);
            case "UNIFORM" -> genUniformData(elementBitLen, dataSize,random);
            case "AOL" -> getRealData("AOL",dataSize);
            case "NETFLIX" -> getRealData("NETFLIX",dataSize);
            case "DISTINCT" -> generateDistinctData(elementBitLen,dataSize);
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Generates m distinct values from 1 to m in random order
     * 
     * @param bitLen bit length of values
     * @param m number of distinct values to generate
     * @return array of distinct BigInteger values
     */
    /**
     * Generate m distinct data values
     *
     * @param bitLen bit length for each value
     * @param m number of distinct values to generate
     * @return array of distinct BigIntegers
     */
    /**
     * Generates m distinct values from 1 to m, then shuffles them
     *
     * @param bitLen bit length constraint for values
     * @param m number of distinct values to generate
     * @return shuffled array of distinct BigInteger values
     */
    private BigInteger[] generateDistinctData(int bitLen, int m) {
        if (m <= 0) {
            throw new IllegalArgumentException("Size m must be positive");
        }
        if(LongUtils.ceilLog2(m)>bitLen){
            throw new IllegalArgumentException("Size log m must be less than "+bitLen);
        }
        List<BigInteger> list = new ArrayList<>();
        Random random = new Random();
        // Generate m distinct BigIntegers
        for (int i = 1; i <= m; i++) {
            BigInteger bigInt = BigInteger.valueOf(i);
            list.add(bigInt);
        }

        // Shuffle the list
        Collections.shuffle(list, random);
        // Convert List to BigInteger[]
        return list.toArray(new BigInteger[0]);
    }


    /**
     * Generate uniformly distributed random data
     *
     * @param elementBitLen bit length of each element
     * @param updateRowNum number of elements to generate
     * @param random random number generator
     * @return array of uniformly distributed BigIntegers
     */
    /**
     * Generates uniformly random data with specified bit length
     *
     * @param elementBitLen bit length for each element (must be between 1 and 64)
     * @param updateRowNum number of data elements to generate
     * @param random random number generator
     * @return array of uniformly random BigInteger values
     */
    private BigInteger[] genUniformData(int elementBitLen, int updateRowNum,Random random) {
        MathPreconditions.checkPositiveInRangeClosed("0 < elementBitLen <= 64", elementBitLen, 64);
        return IntStream.range(0, updateRowNum).mapToObj(i ->
                BitVectorFactory.createRandom(elementBitLen, random).getBigInteger()).toArray(BigInteger[]::new);
    }

    /**
     * Generate Gaussian distributed random data
     *
     * @param elementBitLen bit length of each element
     * @param updateRowNum number of elements to generate
     * @param random random number generator
     * @return array of Gaussian distributed BigIntegers
     */
    /**
     * Generates Gaussian distributed data with specified bit length
     * Uses mean = 2^(elementBitLen-1) and standard deviation = 2^(elementBitLen-16)
     * Values are clamped to positive range [1, 2^elementBitLen)
     *
     * @param elementBitLen bit length for each element
     * @param updateRowNum number of data elements to generate
     * @param random random number generator
     * @return array of Gaussian distributed BigInteger values
     */
    private BigInteger[] genGaussianData(int elementBitLen, int updateRowNum,Random random) {
        BigInteger[] updateData = new BigInteger[updateRowNum];
        for (int i = 0; i < updateData.length; i++) {
            updateData[i]=BigInteger.valueOf((long)random.nextGaussian(Math.pow(2,elementBitLen-1),Math.pow(2,elementBitLen-16)));
            if(updateData[i].compareTo(BigInteger.valueOf(1L <<elementBitLen))>=0){
                updateData[i]=BigInteger.valueOf(1L <<elementBitLen-1);
            }
            if(updateData[i].compareTo(BigInteger.ZERO)<=0){
                updateData[i]=BigInteger.ONE;
            }
        }
        return updateData;
    }
    /**
     * Gets or creates a buffered reader for the specified file
     * Readers are cached to avoid reopening files
     *
     * @param file name of the file (without .txt extension)
     * @return buffered reader for the file
     * @throws IOException if file cannot be found or opened
     */
    private BufferedReader getReader(String file) throws IOException {
        if(readerMap.containsKey(file)){
            return readerMap.get(file);
        }
        String fileName= file+".txt";
        InputStream inputStream = DataGenerator.class.getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new RuntimeException("File not found in resources: " + fileName);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        readerMap.put(file, reader);
        return reader;
    }
    /**
     * Reads real-world data from resource files
     * Supports AOL (web URLs) and Netflix (movie IDs) datasets
     *
     * @param file name of the dataset file (without .txt extension)
     * @param dataSize number of data elements to read
     * @return array of BigInteger values extracted from the dataset
     */
    private BigInteger[] getRealData(String file,int dataSize) {

        List<BigInteger> result = new ArrayList<>();
        BufferedReader reader = null;

        try  {
            reader=getReader(file);
            switch (file) {
                case "AOL": {
                    Pattern urlPattern = Pattern.compile("https?://[^\\s]+");
                    String line;
                    int lineNum = 0;
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = urlPattern.matcher(line);
                        if (matcher.find()) {
                            lineNum++;
                            String website = matcher.group();
                            BigInteger hash = hashWebsiteToBigInteger(website);
                            result.add(hash);
                        }
                        if(lineNum>=dataSize){
                            break;
                        }
                    }
                    break;
                }
                case "NETFLIX": {
                    Pattern pattern = Pattern.compile("\\b(\\d+)\\b");
                    String line;
                    int lineNum = 0;
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            lineNum++;
                            long firstNum = Long.parseLong(matcher.group(1));
                            result.add(BigInteger.valueOf(firstNum&mask));
                        }
                        if (lineNum >= dataSize) {
                            break;
                        }
                    }
                    break;
                }
            }
        }catch (Exception e) {
            throw new RuntimeException("Error reading file: " + file+".txt", e);
        }
        return result.toArray(new BigInteger[0]);
    }
    /**
     * Hashes a website URL to a BigInteger using CRC32
     * The URL is normalized before hashing to ensure consistency
     *
     * @param website the website URL to hash
     * @return BigInteger hash value of the website
     */
    private BigInteger hashWebsiteToBigInteger(String website) {
        try {
            // Normalize the website URL
            String normalizedUrl = normalizeWebsite(website);
//            // Create SHA-256 hash
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hashBytes = digest.digest(normalizedUrl.getBytes("UTF-8"));

            CRC32 crc32 = new CRC32();
            crc32.update(normalizedUrl.getBytes(StandardCharsets.UTF_8));
            long hashValue = crc32.getValue();
            return BigInteger.valueOf(hashValue&mask);

        } catch (Exception e) {
            throw new RuntimeException("Error hashing website: " + website, e);
        }
    }

    /**
     * Normalizes a website URL by removing protocol, www prefix, and trailing slash
     * This ensures consistent hashing of equivalent URLs
     *
     * @param website the website URL to normalize
     * @return normalized website URL
     */
    private String normalizeWebsite(String website) {
        // Remove protocol and www for consistency
        String normalized = website.toLowerCase()
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "");

        // Remove trailing slashes
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

}
