package org.example.dao;

import org.example.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户数据访问对象 - 使用Spring JDBC
 */
@Repository
public class AccountDao {
    
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    
    private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = new RowMapper<Account>() {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            Account account = new Account();
            account.setId(rs.getLong("id"));
            account.setName(rs.getString("name"));
            account.setBalance(rs.getDouble("balance"));
            return account;
        }
    };
    
    /**
     * 插入账户
     */
    public Account insert(Account account) {
        String sql = "INSERT INTO account (name, balance) VALUES (:name, :balance)";
        
        // 使用命名参数
        SqlParameterSource params = new MapSqlParameterSource()
            .addValue("name", account.getName())
            .addValue("balance", account.getBalance());
        
        // 使用KeyHolder获取生成的ID
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        
        namedJdbcTemplate.update(sql, params, keyHolder);
        
        // 获取生成的ID
        Long id = keyHolder.getKey().longValue();
        account.setId(id);
        
        return account;
    }
    
    /**
     * 查询所有账户
     */
    public List<Account> findAll() {
        String sql = "SELECT * FROM account";
        return namedJdbcTemplate.query(sql, new MapSqlParameterSource(), ACCOUNT_ROW_MAPPER);
    }
    
    /**
     * 清空所有账户
     */
    public void deleteAll() {
        String sql = "DELETE FROM account";
        namedJdbcTemplate.update(sql, new MapSqlParameterSource());
    }
    
    /**
     * 根据名称查询
     */
    public List<Account> findByName(String name) {
        String sql = "SELECT * FROM account WHERE name = :name";
        SqlParameterSource params = new MapSqlParameterSource("name", name);
        return namedJdbcTemplate.query(sql, params, ACCOUNT_ROW_MAPPER);
    }
}
