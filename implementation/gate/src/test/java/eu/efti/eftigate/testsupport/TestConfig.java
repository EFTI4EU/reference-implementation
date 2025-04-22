package eu.efti.eftigate.testsupport;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@TestConfiguration
public class TestConfig {
    @Slf4j
    public static class DbCleaner {
        private final SQLRunner sqlRunner;

        private final List<String> tablesToClear;

        private static final List<Predicate<String>> EXCLUSIONS = List.of(
                tableName -> tableName.endsWith("databasechangelog"),
                tableName -> tableName.endsWith("databasechangeloglock"),
                tableName -> tableName.endsWith("shedlock")
        );

        private DbCleaner(SQLRunner sqlRunner) {
            this.sqlRunner = sqlRunner;
            this.tablesToClear = sqlRunner.getDatabaseTableNames()
                    .stream()
                    .filter(tableName -> EXCLUSIONS.stream().noneMatch(exclusion -> exclusion.test(tableName)))
                    .toList();
        }

        public void clearTables() {
            sqlRunner.executeStatements(List.of("TRUNCATE TABLE " + String.join(", ", tablesToClear) + ";"));
        }
    }

    @Slf4j
    @AllArgsConstructor
    private static class SQLRunner {
        private JdbcTemplate template;

        private DatabaseMetaData metaData;

        @Transactional
        void executeStatements(List<String> statements) {
            log.trace("Executing statements: {}", statements);
            statements.forEach(template::execute);
        }

        @Transactional
        List<String> getDatabaseTableNames() {
            try {
                try (var rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                    var result = new ArrayList<String>();
                    while (rs.next()) {
                        var tableName = rs.getString("TABLE_NAME");
                        var schemaName = rs.getString("TABLE_SCHEM");
                        result.add(schemaName + "." + tableName);
                    }
                    return result;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Bean
    public DbCleaner getDbCleaner(DataSource controlDataSource, DataSource identifiersDataSource) {
        var url1 = getUrl(controlDataSource);
        var url2 = getUrl(identifiersDataSource);
        if (!url1.equals(url2)) {
            throw new IllegalStateException(String.format(
                    "There are two data sources with different urls (\"%s\" and \"%s\" but this implementation only knows how to clean one of them", url1, url2));
        }
        return new DbCleaner(createSqlRunner(identifiersDataSource));
    }

    private SQLRunner createSqlRunner(DataSource dataSource) {
        var template = new JdbcTemplate(dataSource);
        var metaData = getMeta(dataSource);
        return new SQLRunner(template, metaData);
    }

    private static String getUrl(DataSource dataSource) {
        try {
            return dataSource.getConnection().getMetaData().getURL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static DatabaseMetaData getMeta(DataSource dataSource) {
        try {
            return dataSource.getConnection().getMetaData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
