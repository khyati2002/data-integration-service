import com.salescode.channelkart.exceptions.ResourceNotFoundException;
import com.salescode.dataintegration.scanner.ExternalRegistryScanner;
import com.salescode.dataintegration.scanner.JarScanner;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExternalRegistryScannerTest {

    @Mock
    private DSLContext dslContext;

    @Mock
    private JarScanner jarScanner;

    @Mock
    private Map<String, Object> instanceCache;

    @InjectMocks
    private ExternalRegistryScanner externalRegistryScanner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @SneakyThrows
    @Test
    void testLoadClassesFromLob_ProfileFound_Success() {
        // Arrange
        String lob = "testLob";
        Record profileMock = mock(Record.class);
        String s3JarUrl = "file:///Users/gauravgupta/Documents/workspace/integration-bundle/target/integration-bundle-0.0.1-SNAPSHOT.jar";
        Method loadClassesMethod = ExternalRegistryScanner.class.getDeclaredMethod("fetchProfile", String.class);
        loadClassesMethod.setAccessible(true);
        ExternalRegistryScanner spyExternalRegistryScanner = spy(externalRegistryScanner);
        doReturn(profileMock).when(loadClassesMethod).invoke(spyExternalRegistryScanner, lob);
        when(profileMock.getValue("artifactURL", String.class)).thenReturn(s3JarUrl);
        externalRegistryScanner.loadClassesFromLob(lob);

        // Assert
        verify(jarScanner, times(1)).loadAndCacheClasses(eq(s3JarUrl), anyMap());
    }

    @Test
    void testLoadClassesFromLob_ProfileNotFound_ThrowsException() {
        // Arrange
        String lob = "testLob";
        when(dslContext.select().from("profile").where("lob = ? AND type = 'bundle'", lob).fetchOne()).thenReturn(null);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            externalRegistryScanner.loadClassesFromLob(lob);
        });

        assertEquals("Bundle Profile not found for LOB: testLob", exception.getMessage());
        verify(jarScanner, never()).loadAndCacheClasses(anyString(), anyMap());
    }

    @Test
    void testGetCachedInstance_ReturnsCachedClass() {
        // Arrange
        String className = "com.example.TestClass";
        Object classInstance = new Object();
        when(instanceCache.get(className)).thenReturn(classInstance);

        // Act
        Object result = externalRegistryScanner.getCachedInstance(className);

        // Assert
        assertEquals(classInstance, result);
    }

    @Test
    void testGetCachedInstance_ReturnsNullWhenNotCached() {
        // Arrange
        String className = "NonExistentClass";
        when(instanceCache.get(className)).thenReturn(null);

        // Act
        Object result = externalRegistryScanner.getCachedInstance(className);

        // Assert
        assertNull(result);
    }
}