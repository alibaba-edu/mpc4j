package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTableType;

import java.util.LinkedList;
import java.util.List;

/**
 * Abstract base class for Count-Min Sketch (CMS) table structures in the S³ framework.
 * 
 * <p>This class implements the CMS data structure, which maintains:
 * - A sketch table: a log(1/δ)×s array storing frequency counts for hashed keys
 * - A buffer: temporary storage for incoming updates before batch merging
 * - Hash parameters: parameters for the hash functions mapping keys to table indices</p>
 * 
 * <p>The CMS structure follows the paper's algorithm:
 * - Update: CMS[i][h_i(k)] += v for each hash function h_i
 * - Query: returns min{CMS[i][h_i(k)]} across all hash rows
 * - Merge: processes buffered updates through sorting, prefix-sum, and compaction</p>
 */
public class AbstractCMSTable implements SketchTable {
    /**
     * The sketch table storing frequency counts.
     * 
     * <p>This is the core CMS data structure: a log(1/δ)×s array where each row
     * corresponds to a different hash function. The table stores only the payload
     * (frequency counts) in secret-shared form for secure MPC computation.</p>
     */
    private MpcVector[] sketchTable;
    /**
     * The buffer for storing incoming updates before merging.
     * 
     * <p>This buffer accumulates new data items (keys) until it reaches capacity.
     * Data is stored in row form, where each entry represents a key to be processed.
     * When the buffer is full, the Merge protocol is triggered to batch-process
     * all buffered updates into the sketch table.</p>
     */
    private List<MpcVector> buffer;
    /**
     * The base-2 logarithm of the sketch table size (log s).
     * 
     * <p>This determines the number of hash functions used in CMS (log(1/δ)),
     * where s = 2^logSketchSize is the width of each row in the sketch table.</p>
     */
    protected final int logSketchSize;
    /**
     * The bit length of input elements (keys).
     * 
     * <p>This specifies the size of the domain from which keys are drawn,
     * affecting the hash function parameters and representation.</p>
     */
    private final int elementBitLen;
    /**
     * Parameters for hash computation in CMS.
     * 
     * <p>These parameters include the hash function coefficients (a, b) for arithmetic hashing
     * and the encryption key for secure oblivious permutation (SOPRP) in binary hashing.</p>
     */
    private final HashParameters hashParameters;

    /**
     * Constructs an abstract CMS table with the specified parameters.
     * 
     * @param sketchTable     the initial sketch table array (secret-shared frequency counts)
     * @param elementBitLen   the bit length of input elements (keys)
     * @param logSketchSize   the base-2 logarithm of the sketch table size
     * @param hashParameters  the hash parameters for computing key hashes
     */
    public AbstractCMSTable(MpcVector[] sketchTable, int elementBitLen, int logSketchSize, HashParameters hashParameters) {
        this.sketchTable = sketchTable;
        this.buffer = new LinkedList<>();
        this.elementBitLen = elementBitLen;
        this.logSketchSize = logSketchSize;
        this.hashParameters = hashParameters;
    }

    /**
     * Gets the base-2 logarithm of the sketch table size.
     * 
     * @return logSketchSize, where the table size is 2^logSketchSize
     */
    public int getLogSketchSize() {
        return logSketchSize;
    }

    /**
     * Gets the bit length of input elements.
     * 
     * @return the number of bits used to represent each key
     */
    public int getElementBitLen() {
        return elementBitLen;
    }

    /**
     * Gets the hash parameters for this CMS table.
     * 
     * @return the hash parameters including coefficients and encryption key
     */
    public HashParameters getHashParameters() {
        return hashParameters;
    }

    @Override
    public SketchTableType getSketchTableType() {
        return SketchTableType.CMS;
    }

    /**
     * Gets the current number of items in the buffer.
     * 
     * @return the buffer size (number of pending updates)
     */
    @Override
    public int getBufferIndex() {
        return buffer.size();
    }

    /**
     * Gets the sketch table array.
     * 
     * @return the secret-shared frequency counts in the CMS structure
     */
    @Override
    public MpcVector[] getSketchTable() {
        return sketchTable;
    }

    /**
     * Gets the actual size of the sketch table.
     * 
     * @return the table size, which is 2^logSketchSize
     */
    @Override
    public int getTableSize() {
        return 1 << logSketchSize;
    }

    /**
     * Gets the buffer containing pending updates.
     * 
     * @return the list of buffered keys waiting to be merged
     */
    @Override
    public List<MpcVector> getBufferTable() {
        return buffer;
    }

    /**
     * Clears the buffer, removing all pending updates.
     */
    @Override
    public void clearBufferTable() {
        buffer.clear();
    }

    /**
     * Sets the table size (not supported for CMS).
     * 
     * @param size the desired size (ignored)
     * @throws MpcAbortException always, as CMS table size is fixed
     */
    @Override
    public void setTableSize(int size) throws MpcAbortException {
        throw new MpcAbortException("Cannot set table size");
    }

    /**
     * Updates the sketch table with new frequency counts.
     * 
     * <p>This is called after the Merge protocol completes to update the
     * sketch table with the merged results from the buffer.</p>
     * 
     * @param sketchTable the new sketch table array with updated counts
     */
    @Override
    public void updateSketchTable(MpcVector[] sketchTable) {
        this.sketchTable = sketchTable;
    }

    /**
     * Updates the buffer with new data.
     * 
     * @param bufferTable the new buffer contents
     */
    @Override
    public void updateBufferTable(List<MpcVector> bufferTable) {
        this.buffer = bufferTable;
    }

}
