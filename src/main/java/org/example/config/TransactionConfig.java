package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * 事务配置类
 * 使用DataSourceTransactionManager以完全支持NESTED传播类型和保存点机制
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    /**
     * 配置DataSourceTransactionManager
     * 这个事务管理器基于JDBC，原生支持保存点（savepoint），因此完美支持NESTED传播类型
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        // 显式启用嵌套事务支持
        transactionManager.setNestedTransactionAllowed(true);
        return transactionManager;
    }
}
