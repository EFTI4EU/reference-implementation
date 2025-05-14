package eu.efti.identifiersregistry.config;

import eu.efti.identifiersregistry.entity.Consignment;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
@EnableJpaRepositories(
        basePackages = {"eu.efti.identifiersregistry.repository"},
        entityManagerFactoryRef = "identifiersEntityManagerFactory",
        transactionManagerRef = "identifiersTransactionManager"
)
public class IdentifiersJpaConfiguration {

    @Value("${spring.jpa.properties.hibernate.identifiers_schema}")
    private String schema;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.identifiers")
    public DataSource identifiersDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.identifiers.liquibase")
    public LiquibaseProperties identifiersLiquibaseProperties() {
        return new LiquibaseProperties();
    }

    @Bean
    public SpringLiquibase identifiersDataSourceLiquibase() {
        return springLiquibase(identifiersDataSource(), identifiersLiquibaseProperties());
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean identifiersEntityManagerFactory(
            @Qualifier("identifiersDataSource") final DataSource dataSource,
            final EntityManagerFactoryBuilder builder) {
        return builder.dataSource(dataSource)
                .packages(Consignment.class)
                .properties(jpaProperties())
                .build();
    }

    private Map<String, Object> jpaProperties() {
        final Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.schema", schema);
        props.put("hibernate.default_schema", schema);
        return props;
    }

    @Bean
    public PlatformTransactionManager identifiersTransactionManager(
            @Qualifier("identifiersEntityManagerFactory") final LocalContainerEntityManagerFactoryBean identifierEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(identifierEntityManagerFactory.getObject()));
    }

    private static SpringLiquibase springLiquibase(final DataSource dataSource, final LiquibaseProperties properties) {
        final SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setShouldRun(true);
        liquibase.setChangeLogParameters(properties.getParameters());
        liquibase.setRollbackFile(properties.getRollbackFile());
        return liquibase;
    }
}
