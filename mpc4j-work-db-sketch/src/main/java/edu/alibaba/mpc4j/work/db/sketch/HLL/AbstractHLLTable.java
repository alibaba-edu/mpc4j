package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTableType;

import java.util.LinkedList;
import java.util.List;

/**
 * Abstract base class for HyperLogLog (HLL) sketch table in the S³ Framework.
 * 
 * This class represents the HLL sketch data structure used for cardinality estimation.
 * It maintains the sketch table (array of counters) and a buffer for batching updates.
 * The merge protocol is triggered when the buffer is full to efficiently update the sketch.
 * 
 * Data Structure:
 * - sketchTable: Array of s counters, where s = 2^logSketchSize
 * - buffer: Temporary storage for batched updates to reduce communication overhead
 * - encKey: Encryption key used for secure hash computation
 * 
 * The HLL sketch implements the algorithm from the paper:
 * - Update: SHLL[h1(k)] = max(SHLL[h1(k)], LeadingOnes(h2(k)))
 * - Merge: Hash buffered keys → Sort → Segmented prefix-max → Compact
 */
public abstract class AbstractHLLTable implements SketchTable {

    /**
     * The HLL sketch table containing the s counters.
     * Each counter stores the maximum number of leading ones seen for keys mapped to that position.
     * In the paper notation, this corresponds to SHLL array.
     */
    private MpcVector[] sketchTable;
    
    /**
     * Buffer for batched updates.
     * Stores updated keys in row form before merging into the sketch table.
     * Batching reduces communication overhead by triggering merge only when buffer is full.
     */
    private List<MpcVector> buffer;
    
    /**
     * Logarithm of the sketch table size.
     * The actual table size is s = 2^logSketchSize.
     * This parameter determines the number of counters in the HLL sketch.
     */
    protected final int logSketchSize;
    
    /**
     * Bit length of input elements.
     * Specifies the size of each element in bits for hash computation.
     */
    private final int elementBitLen;
    
    /**
     * Encryption key for hash computation.
     * Used as the key parameter for the LowMC block cipher to compute h1 and h2 hash functions.
     */
    private final MpcZ2Vector encKey;

    /**
     * Constructs an AbstractHLLTable with existing sketch table and buffer.
     * 
     * @param sketchTable the existing sketch table array
     * @param buffer the existing buffer containing batched updates
     * @param elementBitLen bit length of input elements
     * @param logSketchSize logarithm of sketch table size (table size = 2^logSketchSize)
     * @param encKey encryption key for hash computation
     */
    public AbstractHLLTable(MpcVector[] sketchTable, List<MpcVector> buffer, int elementBitLen, int logSketchSize, MpcZ2Vector encKey) {
        this.sketchTable = sketchTable;
        this.buffer = buffer;
        this.elementBitLen = elementBitLen;
        this.logSketchSize = logSketchSize;
        this.encKey = encKey;
    }

    /**
     * Constructs an AbstractHLLTable with existing sketch table and empty buffer.
     * 
     * @param sketchTable the existing sketch table array
     * @param elementBitLen bit length of input elements
     * @param logSketchSize logarithm of sketch table size (table size = 2^logSketchSize)
     * @param encKey encryption key for hash computation
     */
    public AbstractHLLTable(MpcVector[] sketchTable, int elementBitLen, int logSketchSize, MpcZ2Vector encKey) {
        this.sketchTable = sketchTable;
        this.buffer = new LinkedList<>();
        this.elementBitLen = elementBitLen;
        this.logSketchSize = logSketchSize;
        this.encKey = encKey;
    }

    /**
     * Gets the logarithm of the sketch table size.
     * 
     * @return logSketchSize, where table size = 2^logSketchSize
     */
    public int getLogSketchSize() {
        return logSketchSize;
    }

    /**
     * Gets the bit length of input elements.
     * 
     * @return the element bit length
     */
    public int getElementBitLen() {
        return elementBitLen;
    }

    /**
     * Gets the encryption key for hash computation.
     * 
     * @return the encryption key used for LowMC cipher
     */
    public MpcZ2Vector getEncKey() {
        return encKey;
    }

    @Override
    public SketchTableType getSketchTableType() {
        return SketchTableType.HLL;
    }

    /**
     * Gets the current buffer index (number of elements in buffer).
     * 
     * @return the size of the buffer
     */
    @Override
    public int getBufferIndex() {
        return buffer.size();
    }

    /**
     * Gets the sketch table array.
     * 
     * @return the array of HLL counters
     */
    @Override
    public MpcVector[] getSketchTable() {
        return sketchTable;
    }

    /**
     * Gets the actual sketch table size.
     * 
     * @return the table size = 2^logSketchSize
     */
    @Override
    public int getTableSize() {
        return 1 << logSketchSize;
    }

    /**
     * Gets the buffer table containing batched updates.
     * 
     * @return the list of buffered elements
     */
    @Override
    public List<MpcVector> getBufferTable() {
        return buffer;
    }

    /**
     * Clears the buffer table.
     * Called after merge operation to reset the buffer for new updates.
     */
    @Override
    public void clearBufferTable() {
        buffer.clear();
    }

    /**
     * Sets the table size (not supported for HLL).
     * 
     * @param size the desired table size
     * @throws MpcAbortException always, as HLL table size is fixed at construction
     */
    @Override
    public void setTableSize(int size) throws MpcAbortException {
        throw new MpcAbortException("Cannot set table size");
    }

    /**
     * Updates the sketch table with new counter values.
     * Called after merge operation to apply compacted results.
     * 
     * @param sketchTable the new sketch table array
     */
    @Override
    public void updateSketchTable(MpcVector[] sketchTable) {
        this.sketchTable = sketchTable;
    }

    /**
     * Updates the buffer table with new buffered elements.
     * 
     * @param bufferTable the new buffer table
     */
    @Override
    public void updateBufferTable(List<MpcVector> bufferTable) {
        this.buffer = bufferTable;
    }
}
