package org.example.service;

import org.example.dao.AccountDao;
import org.example.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试REQUIRED传播类型的事务行为
 * 
 * REQUIRED是Spring的默认传播类型：
 * - 如果当前存在事务，则加入该事务
 * - 如果当前没有事务，则创建一个新事务
 */
@Service
public class RequiredPropagationService {
    
    @Autowired
    private AccountDao accountDao;
    
    @Autowired
    @Lazy
    private RequiredPropagationService self; // 自注入，用于内部方法调用
    
    /**
     * 方法A：REQUIRED传播类型
     * 场景1：catch子方法异常 - 由于REQUIRED在同一事务中，即使捕获异常，事务也会被标记为rollback-only
     * 所以这个方法会回滚所有数据
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String testRequiredWithCatch() {
        System.out.println("\n=== 测试REQUIRED传播类型 - 方法A捕获异常 ===");
        
        // 插入第一条数据
        Account account1 = new Account("REQUIRED_CATCH_A", 1000.0);
        accountDao.insert(account1);
        System.out.println("方法A：插入数据 account1: " + account1);
        
        try {
            // 调用方法B（REQUIRED传播）
            self.methodB_Required();
        } catch (RuntimeException e) {
            System.out.println("方法A：捕获到方法B的异常 - " + e.getMessage());
            System.out.println("方法A：注意！在REQUIRED传播中，即使捕获了异常，事务也已被标记为rollback-only");
            System.out.println("方法A：继续执行会导致 'Transaction silently rolled back' 错误");
            // 这里不能继续执行，因为事务已经被标记为rollback-only
            throw new IllegalStateException("REQUIRED传播中，子方法异常后无法继续执行，事务已标记为回滚", e);
        }
        
        return "这段代码不会执行";
    }
    
    /**
     * 方法A：REQUIRED传播类型
     * 场景2：不catch子方法异常 - A会回滚
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String testRequiredWithoutCatch() {
        System.out.println("=== 测试REQUIRED传播类型 - 方法A不捕获异常 ===");
        
        // 插入第一条数据
        Account account1 = new Account("REQUIRED_NO_CATCH_A", 1000.0);
        accountDao.insert(account1);
        System.out.println("方法A：插入数据 account1: " + account1);
        
        // 调用方法B（REQUIRED传播），不捕获异常
        self.methodB_Required();
        
        // 这行代码不会执行
        Account account2 = new Account("REQUIRED_NO_CATCH_B", 2000.0);
        accountDao.insert(account2);
        System.out.println("方法A：插入数据 account2: " + account2);
        
        return "REQUIRED without catch - 应该不会到达这里";
    }
    
    /**
     * 方法B：REQUIRED传播类型
     * 会抛出异常
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void methodB_Required() {
        System.out.println("方法B（REQUIRED）：开始执行");
        
        // 插入数据
        Account accountB = new Account("REQUIRED_METHOD_B", 500.0);
        accountDao.insert(accountB);
        System.out.println("方法B（REQUIRED）：插入数据 accountB: " + accountB);
        
        // 抛出异常
        System.out.println("方法B（REQUIRED）：抛出异常！");
        throw new RuntimeException("方法B发生异常 - REQUIRED传播");
    }
}
