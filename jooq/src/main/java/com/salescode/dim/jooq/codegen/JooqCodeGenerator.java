//package com.salescode.dim.jooq.codegen;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.salescode.dim.ConfigValidator;
//import org.jooq.codegen.GenerationTool;
//import org.jooq.meta.jaxb.*;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//
//public class JooqCodeGenerator {
//
//    public static Map<String, Properties> loadPropertiesFromJson(String filePath) {
//        Map<String, Properties> propertiesMap = new HashMap<>();
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        try {
//            // Read JSON file into a list of nodes
//            List<JsonNode> jsonNodes = objectMapper.readValue(new FileInputStream(filePath), new TypeReference<List<JsonNode>>() {
//            });
//
//            for (JsonNode node : jsonNodes) {
//                String groupId = node.get("PropertyGroupId").asText();
//                JsonNode propertyMapNode = node.get("PropertyMap");
//
//                Properties properties = new Properties();
//                propertyMapNode.fields()
//                               .forEachRemaining(entry -> properties.setProperty(entry.getKey(), entry.getValue()
//                                                                                                      .asText()));
//
//                propertiesMap.put(groupId, properties);
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to read properties from JSON file", e);
//        }
//
//        return propertiesMap;
//    }
//
//    public static void main(String[] args) throws Exception {
//        Map<String, Properties> stringPropertiesMap = loadPropertiesFromJson("src/main/resources/flink-application-properties-dev.json");
//
//        Properties properties = stringPropertiesMap.get("Common");
//
//        validateConfig(properties);
//
//        // Start the JOOQ code generation process
//        generateCode(properties);
//    }
//
//    /**
//     * Generates JOOQ code based on the provided database properties.
//     *
//     * @param props Database configuration properties.
//     * @throws Exception if the code generation fails.
//     */
//    public static void generateCode(Properties props) throws Exception {
//        // Retrieve and validate required database configurations
//        String jdbcDriver = props.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver");
//        String jdbcUrl = props.getProperty("jdbc.url");
//        String jdbcUser = props.getProperty("jdbc.user");
//        String jdbcPassword = props.getProperty("jdbc.password");
//        String jooqDialect = props.getProperty("jooq.meta.dialect", "org.jooq.meta.mysql.MySQLDatabase"); // org.jooq.meta.auroramysql.AuroraMySQLDatabase,org.jooq.meta.extensions.jpa.JPADatabase
//        String inputSchema = props.getProperty("database.inputSchema", jdbcUrl.substring(jdbcUrl.lastIndexOf("/") + 1)
//                                                                              .split("\\?")[0]); // get from jdbc.url :3306/db_name
//        String targetPackage = props.getProperty("target.package", "com.salescode.dim.jooq.generated");
//        String targetDir = props.getProperty("target.directory", "target/generated-sources/jooq");
//        String includePattern = props.getProperty("schema.includePattern", ".*");
//        String excludePattern = props.getProperty("schema.excludePattern", "");
//
//        /*
//         * Configure JOOQ code generation with validated properties.
//         * Excludes match before includes, i.e., excludes have a higher priority.
//         */
//        Configuration configuration = new Configuration()
//                .withLogging(Logging.DEBUG)
//                .withJdbc(new Jdbc()
//                        .withDriver(jdbcDriver)
//                        .withUrl(jdbcUrl)
//                        .withUser(jdbcUser)
//                        .withPassword(jdbcPassword))
//                .withGenerator(new Generator()
//                        .withDatabase(new Database()
//                                        .withName(jooqDialect)
//                                        .withIncludes(includePattern)
//                                        .withExcludes(excludePattern)
//                                        .withInputSchema(inputSchema)
////                                .withForcedTypes(new ForcedType[]{
////                                        new ForcedType()
////                                })
//                        )
//                        .withGenerate(new Generate()
//                                .withPojos(true)
//                                .withRecords(true)
//                        )
//                        .withTarget(new Target()
//                                .withPackageName(targetPackage)
//                                .withDirectory(targetDir))
//                        .withStrategy(new Strategy())
//                );
//
//        // Start JOOQ code generation
//        GenerationTool.generate(configuration);
//    }
//
//    private static void validateConfig(Properties props) {
//        ConfigValidator.validate(props, "jdbc.url", "jdbc.user", "jdbc.password");
//    }
//}