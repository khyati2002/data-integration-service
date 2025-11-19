package com.applicate.services.channelkart.component.model;

import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException;
import com.salescode.dim.jooq.generated.Routines;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for generating sequence numbers using the database routine getNextLoadVal.
 * This class provides methods to generate sequence numbers with optional prefix and suffix support.
 * 
 * <p>The generator uses JOOQ to call the database stored procedure/function getNextLoadVal
 * which manages sequence generation at the database level.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Call getNextLoadVal database routine using JOOQ</li>
 *   <li>Generate sequence numbers for specified sequence names</li>
 *   <li>Support prefix and suffix formatting for generated sequences</li>
 *   <li>Handle errors during sequence generation</li>
 * </ul>
 * 
 * @see Routines#getnextloadval(org.jooq.Configuration, String)
 */
public class LoadSequenceGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LoadSequenceGenerator.class);
    
    private final DSLContext dslContext;
    
    /**
     * Constructs a new LoadSequenceGenerator with the provided DSLContext.
     * 
     * @param dslContext the JOOQ DSLContext for database operations (can be null for testing subclasses)
     */
    public LoadSequenceGenerator(DSLContext dslContext) {
        this.dslContext = dslContext;
    }
    
    /**
     * Gets the next sequence number from the database using the getNextLoadVal routine.
     * This method calls the database stored procedure/function to generate the next value
     * in the specified sequence.
     * 
     * @param sequenceName the name of the sequence to generate from
     * @return the next sequence number as a Long
     * @throws LoadoutBatchSaveException if sequence generation fails or returns null
     * @throws IllegalArgumentException if sequenceName is null or empty
     *
     */
    public Long getNextSequenceNumber(String sequenceName) {
        if (StringUtils.isBlank(sequenceName)) {
            logger.error("Attempted to generate sequence with null or empty sequenceName");
            throw new IllegalArgumentException("sequenceName cannot be null or empty");
        }
        
        if (dslContext == null) {
            logger.error("DSLContext is null, cannot call database routine");
            throw new IllegalStateException("DSLContext cannot be null for database operations");
        }
        
        logger.debug("Calling getNextLoadVal routine for sequence: {}", sequenceName);
        
        try {
            // Call the JOOQ generated routine using the DSLContext configuration
            Long nextValue = Routines.getnextloadval(dslContext.configuration(), sequenceName);
            
            if (nextValue == null) {
                logger.error("getNextLoadVal routine returned null for sequence: {}", sequenceName);
                throw new LoadoutBatchSaveException(
                    "Database routine getNextLoadVal returned null value",
                    LoadoutBatchSaveException.ErrorType.SEQUENCE_GENERATION_ERROR,
                    "sequenceName=" + sequenceName
                );
            }
            
            logger.debug("Successfully generated sequence number {} for sequence: {}", nextValue, sequenceName);
            return nextValue;
            
        } catch (LoadoutBatchSaveException e) {
            // Re-throw our custom exception
            throw e;
        } catch (Exception e) {
            throw new LoadoutBatchSaveException(
                "Failed to generate sequence number from database routine: " + e.getMessage(),
                LoadoutBatchSaveException.ErrorType.SEQUENCE_GENERATION_ERROR,
                "sequenceName=" + sequenceName,
                e
            );
        }
    }
    
    /**
     * Generates a formatted sequence number with optional prefix and suffix.
     * This method calls getNextSequenceNumber and formats the result with the provided
     * prefix and suffix strings.
     * 
     * @param sequenceName the name of the sequence to generate from
     * @param prefix optional prefix to prepend to the sequence number (can be null or empty)
     * @param suffix optional suffix to append to the sequence number (can be null or empty)
     * @return the formatted sequence number as a String
     * @throws LoadoutBatchSaveException if sequence generation fails
     * @throws IllegalArgumentException if sequenceName is null or empty
     *
     * 
     * <p>Example usage:</p>
     * <pre>
     * // Generate with prefix: "LOAD-12345"
     * String loadNumber = generator.getGeneratedSequenceNumber("loadout_seq", "LOAD-", null);
     * 
     * // Generate with suffix: "12345-A"
     * String loadNumber = generator.getGeneratedSequenceNumber("loadout_seq", null, "-A");
     * 
     * // Generate with both: "LOAD-12345-A"
     * String loadNumber = generator.getGeneratedSequenceNumber("loadout_seq", "LOAD-", "-A");
     * 
     * // Generate without formatting: "12345"
     * String loadNumber = generator.getGeneratedSequenceNumber("loadout_seq", null, null);
     * </pre>
     */
    public String getGeneratedSequenceNumber(String sequenceName, String prefix, String suffix) {
        Long nextValue = getNextSequenceNumber(sequenceName);
        
        StringBuilder result = new StringBuilder();
        
        if (StringUtils.isNotBlank(prefix)) {
            result.append(prefix);
        }
        
        result.append(nextValue);
        
        if (StringUtils.isNotBlank(suffix)) {
            result.append(suffix);
        }
        
        String formattedSequence = result.toString();
        logger.debug("Generated formatted sequence: {} for sequence: {}", formattedSequence, sequenceName);
        
        return formattedSequence;
    }
}
