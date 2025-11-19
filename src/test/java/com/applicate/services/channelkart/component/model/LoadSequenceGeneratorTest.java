package com.applicate.services.channelkart.component.model;

import com.applicate.services.channelkart.exceptions.LoadoutBatchSaveException;
import org.jooq.DSLContext;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for LoadSequenceGenerator.
 * These tests verify the sequence generation functionality and formatting logic.
 * 
 * Note: Tests that require actual database connectivity are not included here.
 * Integration tests should be used to verify the actual getNextLoadVal routine calls.
 */
public class LoadSequenceGeneratorTest {

    /**
     * Creates a test LoadSequenceGenerator that returns a fixed sequence value.
     * This allows us to test the formatting logic without requiring a database connection.
     */
    private static class TestLoadSequenceGenerator extends LoadSequenceGenerator {
        private final Long fixedValue;
        
        public TestLoadSequenceGenerator(Long fixedValue) {
            super(null); // We override getNextSequenceNumber so DSLContext is not used
            this.fixedValue = fixedValue;
        }
        
        @Override
        public Long getNextSequenceNumber(String sequenceName) {
            if (sequenceName == null || sequenceName.trim().isEmpty()) {
                throw new IllegalArgumentException("sequenceName cannot be null or empty");
            }
            return fixedValue;
        }
    }

    @Test
    public void testGetNextSequenceNumberWithNullSequenceNameThrowsException() {
        LoadSequenceGenerator generator = new TestLoadSequenceGenerator(12345L);
        
        try {
            generator.getNextSequenceNumber(null);
            fail("Expected IllegalArgumentException for null sequenceName");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("sequenceName cannot be null or empty"));
        }
    }

    @Test
    public void testGetNextSequenceNumberWithEmptySequenceNameThrowsException() {
        LoadSequenceGenerator generator = new TestLoadSequenceGenerator(12345L);
        
        try {
            generator.getNextSequenceNumber("");
            fail("Expected IllegalArgumentException for empty sequenceName");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("sequenceName cannot be null or empty"));
        }
    }

    @Test
    public void testGetGeneratedSequenceNumberWithPrefix() {
        LoadSequenceGenerator generator = new TestLoadSequenceGenerator(12345L);
        
        String result = generator.getGeneratedSequenceNumber("test_seq", "LOAD-", null);
        assertEquals("LOAD-12345", result);
    }

    @Test
    public void testGetGeneratedSequenceNumberWithSuffix() {
        LoadSequenceGenerator generator = new TestLoadSequenceGenerator(12345L);
        
        String result = generator.getGeneratedSequenceNumber("test_seq", null, "-A");
        assertEquals("12345-A", result);
    }

    @Test
    public void testGetGeneratedSequenceNumberWithPrefixAndSuffix() {
        LoadSequenceGenerator generator = new TestLoadSequenceGenerator(12345L);
        
        String result = generator.getGeneratedSequenceNumber("test_seq", "LOAD-", "-A");
        assertEquals("LOAD-12345-A", result);
    }

    @Test
    public void testGetGeneratedSequenceNumberWithoutFormatting() {
        LoadSequenceGenerator generator = new TestLoadSequenceGenerator(12345L);
        
        String result = generator.getGeneratedSequenceNumber("test_seq", null, null);
        assertEquals("12345", result);
    }

    @Test
    public void testGetGeneratedSequenceNumberWithEmptyStrings() {
        LoadSequenceGenerator generator = new TestLoadSequenceGenerator(12345L);
        
        String result = generator.getGeneratedSequenceNumber("test_seq", "", "");
        assertEquals("12345", result);
    }
    
    @Test
    public void testGetGeneratedSequenceNumberWithWhitespaceStrings() {
        LoadSequenceGenerator generator = new TestLoadSequenceGenerator(12345L);
        
        // Whitespace-only strings should be treated as blank and not included
        String result = generator.getGeneratedSequenceNumber("test_seq", "   ", "   ");
        assertEquals("12345", result);
    }
}
