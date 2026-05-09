package org.example.service;

import org.example.dao.AccountDao;
import org.example.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试NESTED传播类型的事务行为
 * 
 * NESTED传播类型：
 * - 如果当前存在事务，则在嵌套事务中执行（保存点机制）
 * - 如果当前没有事务，则创建一个新事务（类似REQUIRED）
 * - 嵌套事务可以独立回滚，而不影响外层事务
 * 
 * 注意：NESTED只支持JDBC事务管理器，需要数据库支持保存点
 */
@Service
public class NestedPropagationService {
    
    @Autowired
    private AccountDao accountDao;
    
    @Autowired
    @Lazy
    private NestedPropagationService self; // 自注入，用于内部方法调用
    
    /**
     * 方法A：REQUIRED传播类型
     * 场景1：catch子方法异常 - A不会回滚，B会回滚
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String testNestedWithCatch() {
        System.out.println("=== 测试NESTED传播类型 - 方法A捕获异常 ===");
        
        // 插入第一条数据
        Account account1 = new Account("NESTED_CATCH_A", 1000.0);
        accountDao.insert(account1);
        System.out.println("方法A：插入数据 account1: " + account1);
        
        try {
            // 调用方法B（NESTED传播）
            self.methodB_Nested();
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("方法A：捕获到方法B的异常 - " + e.getMessage());
        }
        
        // 插入第二条数据
        Account account2 = new Account("NESTED_CATCH_B", 2000.0);
        accountDao.insert(account2);
        System.out.println("方法A：插入数据 account2: " + account2);
        
        System.out.println("方法A：执行完成，事务应该提交");
        return "NESTED with catch - 只有account1和account2应该保存，methodB的数据应该回滚";
    }
    
    /**
     * 方法A：REQUIRED传播类型
     * 场景2：不catch子方法异常 - A会回滚
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String testNestedWithoutCatch() {
        System.out.println("=== 测试NESTED传播类型 - 方法A不捕获异常 ===");
        
        // 插入第一条数据
        Account account1 = new Account("NESTED_NO_CATCH_A", 1000.0);
        accountDao.insert(account1);
        System.out.println("方法A：插入数据 account1: " + account1);
        
        // 调用方法B（NESTED传播），不捕获异常
        self.methodB_Nested();
        
        // 这行代码不会执行
        Account account2 = new Account("NESTED_NO_CATCH_B", 2000.0);
        accountDao.insert(account2);
        System.out.println("方法A：插入数据 account2: " + account2);
        
        return "NESTED without catch - 应该不会到达这里";
    }
    
    /**
     * 方法B：NESTED传播类型
     * 会抛出异常
     */
    @Transactional(propagation = Propagation.NESTED)
    public void methodB_Nested() {
        System.out.println("方法B（NESTED）：开始执行");
        
        // 插入数据
        Account accountB = new Account("NESTED_METHOD_B", 500.0);
        accountDao.insert(accountB);
        System.out.println("方法B（NESTED）：插入数据 accountB: " + accountB);
        
        // 抛出异常
        System.out.println("方法B（NESTED）：抛出异常！");
        throw new RuntimeException("方法B发生异常 - NESTED传播");
    }
    
    /**
     * 方法B：NESTED传播类型
     * 正常执行，不抛出异常
     */
    @Transactional(propagation = Propagation.NESTED)
    public void methodB_Nested_Normal() {
        System.out.println("方法B（NESTED-正常）：开始执行");
        
        // 插入数据
        Account accountB = new Account("NESTED_B_NORMAL", 500.0);
        accountDao.insert(accountB);
        System.out.println("方法B（NESTED-正常）：插入数据 accountB: " + accountB);
        System.out.println("方法B（NESTED-正常）：正常结束");
    }
    
    /**
     * 测试场景3：内层方法正常，外层方法抛出异常
     * 预期：所有数据都回滚（包括内层方法的数据）
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String testNestedOuterException() {
        System.out.println("=== 测试NESTED传播类型 - 内层正常，外层异常 ===");
        
        // 插入第一条数据
        Account account1 = new Account("NESTED_OUTER_A", 1000.0);
        accountDao.insert(account1);
        System.out.println("方法A：插入数据 account1: " + account1);
        
        // 调用方法B（NESTED传播），方法B正常执行
        self.methodB_Nested_Normal();
        
        // 插入第二条数据
        Account account2 = new Account("NESTED_OUTER_B", 2000.0);
        accountDao.insert(account2);
        System.out.println("方法A：插入数据 account2: " + account2);
        
        // 外层方法抛出异常
        System.out.println("方法A：抛出异常！");
        throw new RuntimeException("方法A发生异常 - 外层异常导致全部回滚");
    }
}
