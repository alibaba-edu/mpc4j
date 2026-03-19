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

public class DataGenerator {
    private final Map<String,BufferedReader> readerMap;
    private long mask;
    public DataGenerator() {
        this.readerMap = new HashMap<String,BufferedReader>();
    }

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


    private BigInteger[] genUniformData(int elementBitLen, int updateRowNum,Random random) {
        MathPreconditions.checkPositiveInRangeClosed("0 < elementBitLen <= 64", elementBitLen, 64);
        return IntStream.range(0, updateRowNum).mapToObj(i ->
                BitVectorFactory.createRandom(elementBitLen, random).getBigInteger()).toArray(BigInteger[]::new);
    }

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
