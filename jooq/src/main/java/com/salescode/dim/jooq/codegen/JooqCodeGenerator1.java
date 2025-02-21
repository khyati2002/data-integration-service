//package com.salescode.dim.jooq.codegen;
//
//import org.jooq.codegen.GenerationTool;
//import org.jooq.meta.jaxb.*;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.Properties;
//
//public class JooqCodeGenerator1 {
//
//    private static final String DEFAULT_CONFIG_FILE = "src/main/resources/jooq.properties";
//
//    public static void main(String[] args) {
//        // Use provided config file path or default to "jooq.properties"
//        String configFile = args.length > 0 ? args[0] : DEFAULT_CONFIG_FILE;
//        Properties props = loadProperties(configFile);
//        try {
//            generateCode(props);
//        } catch (Exception e) {
//            System.err.println("Error during jOOQ code generation: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Loads configuration properties from the specified file.
//     *
//     * @param fileName The properties file name or path.
//     * @return a Properties object with loaded configuration.
//     */
//    private static Properties loadProperties(String fileName) {
//        Properties props = new Properties();
//        try (FileInputStream fis = new FileInputStream(fileName)) {
//            props.load(fis);
//            System.out.println("Loaded properties from " + fileName);
//        } catch (IOException e) {
//            System.err.println("Error loading properties file: " + fileName);
//            e.printStackTrace();
//        }
//        return props;
//    }
//
//    public static void generateCode(Properties props) throws Exception {
//        String jdbcDriver = props.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver");
//        String jdbcUrl = props.getProperty("jdbc.url", "jdbc:mysql://localhost:3306/your_database?useSSL=false");
//        String jdbcUser = props.getProperty("jdbc.user", "your_user");
//        String jdbcPassword = props.getProperty("jdbc.password", "your_password");
//        String jooqDialect = props.getProperty("jooq.meta.dialect", "org.jooq.meta.mysql.MySQLDatabase"); // org.jooq.meta.auroramysql.AuroraMySQLDatabase,org.jooq.meta.extensions.jpa.JPADatabase
//        String inputSchema = props.getProperty("database.inputSchema", "your_database");
//        String targetPackage = props.getProperty("target.package", "org.jooq.generated");
//        String targetDir = props.getProperty("target.directory", "target/generated-sources/jooq");
//        String includePattern = props.getProperty("schema.includePattern", ".*");
//        String excludePattern = props.getProperty("schema.excludePattern", "");
//
//
//        /*
//         * The destination package of your generated classes (within the destination directory)
//         * You can create case-insensitive regular expressions using this syntax: (?i:expr)
//         * Excludes match before includes, i.e. excludes have a higher priority
//         * */
//
//        Configuration configuration = new Configuration()
//                .withLogging(Logging.DEBUG)
//                // Configure the database connection here
//                .withJdbc(new Jdbc().withDriver(jdbcDriver)
//                                    .withUrl(jdbcUrl)
//                                    .withUser(jdbcUser)
//                                    .withPassword(jdbcPassword))
//                .withGenerator(new Generator()
//                        .withDatabase(new Database().withName(jooqDialect)
//                                                    .withIncludes(includePattern)
//                                                    .withExcludes(excludePattern)
//                                                    .withInputSchema(inputSchema))
//                        .withGenerate(new Generate())
//                        .withTarget(new Target().withPackageName(targetPackage)
//                                                .withDirectory(targetDir)));
//        GenerationTool.generate(configuration);
//    }
//
//}