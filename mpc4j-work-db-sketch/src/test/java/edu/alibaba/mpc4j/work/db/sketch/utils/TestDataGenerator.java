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
 * Test data generator for creating synthetic and real-world data with various distributions.
 * Supports Gaussian, Uniform, AOL, Netflix, and Distinct distributions for testing sketch algorithms.
 */
public class TestDataGenerator {
    private final Map<String, BufferedReader> readerMap;
    private long mask;
    private int std;
    
    /**
     * Constructs a test data generator with specified standard deviation for Gaussian distribution
     * @param std standard deviation for Gaussian distribution
     */
    public TestDataGenerator(int std) {
        this.readerMap = new HashMap<String, BufferedReader>();
        this.std = std;
    }

    /**
     * Generates update data with specified distribution
     * @param elementBitLen bit length of each element
     * @param dataSize number of elements to generate
     * @param dataType data type (GAUSSIAN, UNIFORM, AOL, NETFLIX, DISTINCT)
     * @param random random number generator
     * @return generated data array
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
     * Generates distinct data for testing cardinality estimation
     * @param bitLen bit length of each element
     * @param m number of distinct elements to generate
     * @return array of distinct BigIntegers
     */
    private BigInteger[] generateDistinctData(int bitLen, int m) {
        if (m <= 0) {
            throw new IllegalArgumentException("Size m must be positive");
        }
        if (LongUtils.ceilLog2(m) > bitLen) {
            throw new IllegalArgumentException("Size log m must be less than " + bitLen);
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
     * Generates uniformly distributed data
     * @param elementBitLen bit length of each element
     * @param updateRowNum number of elements to generate
     * @param random random number generator
     * @return uniformly distributed data array
     */
    private BigInteger[] genUniformData(int elementBitLen, int updateRowNum, Random random) {
        MathPreconditions.checkPositiveInRangeClosed("0 < elementBitLen <= 64", elementBitLen, 64);
        return IntStream.range(0, updateRowNum).mapToObj(i ->
                BitVectorFactory.createRandom(elementBitLen, random).getBigInteger()).toArray(BigInteger[]::new);
    }

    /**
     * Generates Gaussian-distributed data
     * @param elementBitLen bit length of each element
     * @param updateRowNum number of elements to generate
     * @param random random number generator
     * @return Gaussian-distributed data array
     */
    private BigInteger[] genGaussianData(int elementBitLen, int updateRowNum, Random random) {
        BigInteger[] updateData = new BigInteger[updateRowNum];
        for (int i = 0; i < updateData.length; i++) {
            updateData[i] = BigInteger.valueOf((long) random.nextGaussian(Math.pow(2, elementBitLen - 1), Math.pow(2, std)));
            if (updateData[i].compareTo(BigInteger.valueOf(1L << elementBitLen)) >= 0) {
                updateData[i] = BigInteger.valueOf(1L << elementBitLen - 1);
            }
            if (updateData[i].compareTo(BigInteger.ZERO) <= 0) {
                updateData[i] = BigInteger.ONE;
            }
        }
        return updateData;
    }
    /**
     * Gets a buffered reader for the specified data file
     * @param file name of the data file (without extension)
     * @return buffered reader for the file
     * @throws IOException if file cannot be opened
     */
    private BufferedReader getReader(String file) throws IOException {
        if (readerMap.containsKey(file)) {
            return readerMap.get(file);
        }
        String fileName = file + ".txt";
        InputStream inputStream = DataGenerator.class.getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new RuntimeException("File not found in resources: " + fileName);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        readerMap.put(file, reader);
        return reader;
    }
    /**
     * Reads real-world data from file (AOL or Netflix)
     * @param file name of the data file
     * @param dataSize number of elements to read
     * @return array of BigIntegers from real-world data
     */
    private BigInteger[] getRealData(String file, int dataSize) {

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
        } catch (Exception e) {
            throw new RuntimeException("Error reading file: " + file + ".txt", e);
        }
        return result.toArray(new BigInteger[0]);
    }
    /**
     * Hashes a website URL to a BigInteger using CRC32
     * @param website website URL to hash
     * @return hashed BigInteger value
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
     * Normalizes a website URL for consistent hashing
     * @param website website URL to normalize
     * @return normalized URL string
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
