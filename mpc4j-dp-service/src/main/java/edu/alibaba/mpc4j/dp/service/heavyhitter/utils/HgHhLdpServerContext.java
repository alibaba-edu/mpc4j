package edu.alibaba.mpc4j.dp.service.heavyhitter.utils;

import edu.alibaba.mpc4j.dp.service.tool.BucketDoubleComparator;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * The server context with Local Differential Privacy used in the HeavyGuardian-based solutions.
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
public class HgHhLdpServerContext implements HhLdpServerContext {
    /**
     * bucket comparator
     */
    private final BucketDoubleComparator bucketComparator;
    /**
     * the bucket
     */
    private final ArrayList<Map<String, Double>> buckets;

    /**
     * Creates the context based on buckets.
     *
     * @param buckets buckets.
     * @return the context.
     */
    public static HgHhLdpServerContext fromBuckets(ArrayList<Map<String, Double>> buckets) {
        return new HgHhLdpServerContext(buckets);
    }

    private HgHhLdpServerContext(ArrayList<Map<String, Double>> buckets) {
        bucketComparator = new BucketDoubleComparator();
        this.buckets = buckets;
    }

    public Map<String, Double> getBucket(int bucketIndex) {
        return buckets.get(bucketIndex);
    }

    @Override
    public byte[] toClientInfo() {
        try {
            // w bucket, each bucket has k element, each element contains an item and its corresponding (double) count
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            // write w
            int w = buckets.size();
            dataOutputStream.writeInt(w);
            // write each budget
            for (Map<String, Double> bucket : buckets) {
                // write size
                int bucketSize = bucket.size();
                dataOutputStream.writeInt(bucketSize);
                // write each element
                for (Map.Entry<String, Double> entry : bucket.entrySet()) {
                    dataOutputStream.writeUTF(entry.getKey());
                }
                // see if the last element count
                Map.Entry<String, Double> weakestCell = Collections.min(bucket.entrySet(), bucketComparator);
                double weakestCount = weakestCell.getValue();
                dataOutputStream.writeBoolean(weakestCount <= 1);
            }
            dataOutputStream.flush();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            dataOutputStream.close();
            byteArrayOutputStream.close();
            return byteArray;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(buckets).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HgHhLdpServerContext)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        HgHhLdpServerContext that = (HgHhLdpServerContext) obj;
        return new EqualsBuilder().append(this.buckets, that.buckets).isEquals();
    }
}
