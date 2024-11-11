import com.salescode.dataintegration.scanner.ExternalRegistryScanner;
import com.salescode.dataintegration.scanner.JarScanner;
import com.salescode.dis.FlinkApplication;
import com.salescode.jooq.generated.tables.CkMetadata;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = FlinkApplication.class)
public class ExternalRegistryScannerTest1 {

    private final String LOB = "mondelezckinduat";

    @Autowired
    private DSLContext dslContext; // Mock Jooq's DSLContext

    @Mock
    private JarScanner jarScanner; // Mock JarScanner

    @Autowired
    private ExternalRegistryScanner externalRegistryScanner; // Test target

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    public void testLoadClassesFromLob_success() {
        // Mock a profile record from Jooq
        Class<? extends CkMetadata> entityClass = CkMetadata.class;
        List<Table<?>> tables = dslContext.meta()
//                .filterSchemas(s -> s.getName().equals("ckroot"))
                .getTables();
        System.out.println(tables);
        org.jooq.Table<?> table = tables.stream().filter(t -> t.getName().equalsIgnoreCase(entityClass.getSimpleName().toUpperCase())).findFirst().orElseThrow(() -> new IllegalArgumentException("Table not found for POJO class: " + entityClass));
        String tableName = dslContext.render(table);


        Record profile = dslContext.select().from("profile").where("lob = ? AND type = 'bundle'", LOB).fetchOne();

//        assert profile != null;
//        String decrypt = profile.getValue("profile", String.class);
//        JsonNode parse = JSONUtils.parse(decrypt);
//        String artifactURL = parse.get("artifactURL").asText();

        JarScanner jarScanner1 = new JarScanner();
        Map<String, Object> instanceCache = new HashMap<>();
        String artifactURL1 = "https://sellinabundle.s3.ap-south-1.amazonaws.com/channelkart/server/mondelezckinduat/latest/channelkart-bundle.jar?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20241023T125722Z&X-Amz-SignedHeaders=host&X-Amz-Expires=604799&X-Amz-Credential=AKIAQDZHVLDKJNQTXEYP%2F20241023%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Signature=73c6aad7eaf97e45752b4ead4564bab13639f7738a1c51c2f6b7763cbdef3c2b";
        String artifactURL2 = "file:///Users/gauravgupta/Documents/workspace/integration-bundle/target/integration-bundle-0.0.1-SNAPSHOT.jar";
        jarScanner1.loadAndCacheClasses(artifactURL2, instanceCache);

//        instanceCache.get()
//        externalRegistryScanner.loadClassesFromLob(LOB);

        // Verify that the jarScanner was called to load and cache proxies
//        verify(jarScanner).loadAndCacheClasses(eq(url), anyMap());
    }

}