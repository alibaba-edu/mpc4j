package edu.alibaba.mpc4j.common.structure.fastfilter;

import java.util.Random;

/**
 * Single Table, used to efficiently store fingerprints. The implementation is inspired by
 * <a href="https://github.com/efficient/cuckoofilter/blob/master/src/singletable.h">singletable.h</a>
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
public class SingleTable {
    /**
     * tag mask
     */
    private final long tagMask;
    /**
     * number of tags in each bucket
     */
    private final int kTagsPerBucket;
    /**
     * number of buckets
     */
    private final int bucketNum;
    /**
     * buckets
     */
    private final long[][] buckets;
    /**
     * random state
     */
    private final Random random;

    public SingleTable(int bitsPerTag, int kTagsPerBucket, int bucketNum) {
        assert bitsPerTag > 0 && bitsPerTag <= Long.SIZE;
        if (bitsPerTag == Long.SIZE) {
            tagMask = 0xFFFFFFFF_FFFFFFFFL;
        } else if (bitsPerTag == Long.SIZE - 1) {
            tagMask = 0x7FFFFFFF_FFFFFFFFL;
        } else {
            tagMask = (1L << bitsPerTag) - 1;
        }
        assert kTagsPerBucket > 0;
        this.kTagsPerBucket = kTagsPerBucket;
        assert bucketNum > 0;
        this.bucketNum = bucketNum;
        buckets = new long[bucketNum][kTagsPerBucket];
        random = new Random();
    }

    /**
     * Gets number of buckets.
     *
     * @return number of buckets.
     */
    public int bucketNum() {
        return bucketNum;
    }

    /**
     * Reads the j-th tag from bucket[i]. Note that this can return 0.
     *
     * @param i bucket index.
     * @param j tag index.
     * @return tag.
     */
    public long readTag(int i, int j) {
        return buckets[i][j];
    }

    /**
     * Writes the j-th tag from bucket[i]. Note that this can write 0.
     *
     * @param i   bucket index.
     * @param j   tag index.
     * @param tag tag.
     */
    private void writeTag(int i, int j, long tag) {
        buckets[i][j] = tag;
    }

    private boolean validTag(long tag) {
        // we treat 0 as the empty tag
        return (tag != 0) && ((tag & tagMask) == tag);
    }

    /**
     * Finds tag in both bucket[i1] and bucket[i2].
     *
     * @param i1  the 1st bucket index.
     * @param i2  the 2nd bucket index.
     * @param tag tag.
     * @return true if bucket[i1] or bucket[i2] has tag; false otherwise.
     */
    public boolean findTagInBuckets(int i1, int i2, long tag) {
        assert validTag(tag);
        for (int j = 0; j < kTagsPerBucket; j++) {
            if (readTag(i1, j) == tag || readTag(i2, j) == tag) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds tag in bucket[i].
     *
     * @param i   bucket index.
     * @param tag tag.
     * @return true if bucket[i] has tag; false otherwise.
     */
    public boolean findTagInBucket(int i, long tag) {
        assert validTag(tag);
        for (int j = 0; j < kTagsPerBucket; j++) {
            if (readTag(i, j) == tag) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes tag from bucket[i].
     *
     * @param i   bucket index.
     * @param tag tag.
     * @return true if tag is successfully deleted; false if tag is not found in bucket[i].
     */
    public boolean deleteTagFromBucket(int i, long tag) {
        int j = 0;
        for ( ; j < kTagsPerBucket; j++) {
            if (readTag(i, j) == tag) {
                break;
            }
        }
        if(j == kTagsPerBucket){
            return false;
        }
        // move non-null element into the front
        int k = j + 1;
        while(k < kTagsPerBucket){
            long nextTag = readTag(i, k);
            if (nextTag != 0) {
                writeTag(i, k - 1, nextTag);
            }else{
                break;
            }
            k++;
        }
        writeTag(i, k - 1, 0);
        return true;
//        for (int j = 0; j < kTagsPerBucket; j++) {
//            if (readTag(i, j) == tag) {
//                assert findTagInBucket(i, tag);
//                writeTag(i, j, 0);
//                return true;
//            }
//        }
//        return false;
    }

    /**
     * Deletes tag from bucket[i].
     *
     * @param i bucket index.
     * @param j tag index.
     * @return the removed tag.
     */
    public long deleteIndexFromBucket(int i, int j) {
        long deleteTag = readTag(i, j);
        assert validTag(deleteTag);
        writeTag(i, j, 0);
        return deleteTag;
    }

    public boolean insertTagToBucket(int i, long tag) {
        // try to insert without kicking out
        for (int j = 0; j < kTagsPerBucket; j++) {
            if (readTag(i, j) == 0) {
                writeTag(i, j, tag);
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts tag to bucket[i], kick out a tag if bucket[i] is full.
     *
     * @param i   bucket index.
     * @param tag tag.
     * @return 0 if no old tag is kicked out; old tag if an old tag is kicked out.
     */
    public long kickInsertTagToBucket(int i, long tag) {
        // try to insert without kicking out
        for (int j = 0; j < kTagsPerBucket; j++) {
            if (readTag(i, j) == 0) {
                writeTag(i, j, tag);
                return 0;
            }
        }
        // randomly kick out a tag
        int r = random.nextInt(kTagsPerBucket);
        long oldTag = readTag(i, r);
        writeTag(i, r, tag);
        return oldTag;
    }

    /**
     * Inserts tag into bucket[i][j].
     *
     * @param i   bucket index.
     * @param j   tag index.
     * @param tag tag.
     * @return true if
     */
    public boolean insertIndexToBucket(int i, int j, long tag) {
        assert validTag(tag);
        if (readTag(i, j) == 0L) {
            writeTag(i, j, tag);
            return true;
        }
        return false;
    }

    /**
     * Gets number of tags in bucket[i].
     *
     * @param i bucket index.
     * @return number of tags in bucket[i].
     */
    public int numTagsInBucket(int i) {
        int num = 0;
        for (int j = 0; j < kTagsPerBucket; j++) {
            if (readTag(i, j) != 0) {
                num++;
            }
        }
        return num;
    }

    /**
     * Gets bucket[i].
     *
     * @param i bucket index.
     * @return bucket[i].
     */
    public long[] getBucket(int i) {
        return buckets[i];
    }
}
