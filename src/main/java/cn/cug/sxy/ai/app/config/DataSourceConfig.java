package cn.cug.sxy.ai.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 多数据源配置
 */
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean("postgresDataSource")
    public DataSource postgresDataSource(@Value("${spring.datasource.postgres.driver-class-name}") String driverClassName,
                                         @Value("${spring.datasource.postgres.url}") String url,
                                         @Value("${spring.datasource.postgres.username}") String username,
                                         @Value("${spring.datasource.postgres.password}") String password,
                                         @Value("${spring.datasource.postgres.hikari.maximum-pool-size:5}") int maximumPoolSize,
                                         @Value("${spring.datasource.postgres.hikari.minimum-idle:2}") int minimumIdle,
                                         @Value("${spring.datasource.postgres.hikari.idle-timeout:30000}") long idleTimeout,
                                         @Value("${spring.datasource.postgres.hikari.connection-timeout:30000}") long connectionTimeout,
                                         @Value("${spring.datasource.postgres.hikari.pool-name:PostgresHikariPool}") String poolName) {
        // 连接池配置
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(connectionTimeout);

        // 确保在启动时连接数据库
        dataSource.setInitializationFailTimeout(1);  // 设置为1ms，如果连接失败则快速失败
        dataSource.setConnectionTestQuery("SELECT 1"); // 简单的连接测试查询
        dataSource.setAutoCommit(true);
        dataSource.setPoolName(poolName);

        return dataSource;
    }

    @Bean("mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mysqlDataSource(@Value("${spring.datasource.mysql.driver-class-name}") String driverClassName,
                                      @Value("${spring.datasource.mysql.url}") String url,
                                      @Value("${spring.datasource.mysql.username}") String username,
                                      @Value("${spring.datasource.mysql.password}") String password,
                                      @Value("${spring.datasource.mysql.hikari.maximum-pool-size:10}") int maximumPoolSize,
                                      @Value("${spring.datasource.mysql.hikari.minimum-idle:5}") int minimumIdle,
                                      @Value("${spring.datasource.mysql.hikari.idle-timeout:30000}") long idleTimeout,
                                      @Value("${spring.datasource.mysql.hikari.connection-timeout:30000}") long connectionTimeout,
                                      @Value("${spring.datasource.mysql.hikari.max-lifetime:1800000}") long maxLifetime,
                                      @Value("${spring.datasource.mysql.hikari.pool-name:MySqlHikariPool}") String poolName) {
        // 连接池配置
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(connectionTimeout);

        dataSource.setMaxLifetime(maxLifetime);
        dataSource.setPoolName(poolName);
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setAutoCommit(true);

        return dataSource;
    }

}
