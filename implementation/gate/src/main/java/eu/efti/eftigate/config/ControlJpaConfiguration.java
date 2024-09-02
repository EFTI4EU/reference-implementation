package eu.efti.eftigate.config;

import eu.efti.eftigate.entity.ControlEntity;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
@EnableJpaRepositories(
        basePackages = {"eu.efti.eftigate.repository"},
        entityManagerFactoryRef = "controlEntityManagerFactory",
        transactionManagerRef = "controlTransactionManager"
)
@ComponentScan("eu.efti.identifierregistry")
public class ControlJpaConfiguration {

    @Value("${spring.jpa.properties.hibernate.control_schema}")
    private String schema;

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.control")
    public DataSource controlDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.control.liquibase")
    public LiquibaseProperties controlDataSourceLiquibaseProperties() {
        return new LiquibaseProperties();
    }

    @Bean
    public SpringLiquibase controlDataSourceLiquibase() {
        return springLiquibase(controlDataSource(), controlDataSourceLiquibaseProperties());
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean controlEntityManagerFactory(
            @Qualifier("controlDataSource") final DataSource dataSource,
            final EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages(ControlEntity.class)
                .properties(jpaProperties())
                .build();
    }

    @Bean
    public PlatformTransactionManager controlTransactionManager(
            @Qualifier("controlEntityManagerFactory") final LocalContainerEntityManagerFactoryBean controlEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(controlEntityManagerFactory.getObject()));
    }

    private Map<String, Object> jpaProperties() {
        final Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.schema", schema);
        props.put("hibernate.default_schema", schema);
        return props;
    }

    private static SpringLiquibase springLiquibase(final DataSource dataSource, final LiquibaseProperties properties) {
        final SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setContexts(properties.getContexts());
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setShouldRun(properties.isEnabled());
        liquibase.setLabelFilter(properties.getLabelFilter());
        liquibase.setChangeLogParameters(properties.getParameters());
        liquibase.setRollbackFile(properties.getRollbackFile());
        return liquibase;
    }

}
