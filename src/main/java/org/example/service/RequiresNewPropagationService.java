package org.example.service;

import org.example.dao.AccountDao;
import org.example.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试REQUIRES_NEW传播类型的事务行为
 * 
 * REQUIRES_NEW传播类型：
 * - 总是创建一个新事务，如果当前存在事务，则挂起当前事务
 * - 内层事务和外层事务完全独立
 * - 内层事务的提交或回滚不影响外层事务
 */
@Service
public class RequiresNewPropagationService {
    
    @Autowired
    private AccountDao accountDao;
    
    @Autowired
    @Lazy
    private RequiresNewPropagationService self; // 自注入，用于内部方法调用
    
    /**
     * 测试场景1：内层方法正常，外层方法抛出异常
     * 预期：外层数据回滚，内层数据保存（因为是完全独立的事务）
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String testRequiresNewOuterException() {
        System.out.println("=== 测试REQUIRES_NEW传播类型 - 内层正常，外层异常 ===");
        
        // 插入第一条数据（外层事务）
        Account account1 = new Account("REQ_NEW_OUTER_A", 1000.0);
        accountDao.insert(account1);
        System.out.println("方法A：插入数据 account1: " + account1);
        
        // 调用方法B（REQUIRES_NEW传播），方法B会开启独立事务
        self.methodB_RequiresNew_Normal();
        
        // 插入第二条数据（外层事务）
        Account account2 = new Account("REQ_NEW_OUTER_B", 2000.0);
        accountDao.insert(account2);
        System.out.println("方法A：插入数据 account2: " + account2);
        
        // 外层方法抛出异常
        System.out.println("方法A：抛出异常！");
        throw new RuntimeException("方法A发生异常 - 外层异常导致外层回滚，但内层已提交");
    }
    
    /**
     * 测试场景2：内层方法抛出异常，外层捕获异常
     * 预期：内层数据回滚，外层数据保存
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String testRequiresNewInnerExceptionWithCatch() {
        System.out.println("=== 测试REQUIRES_NEW传播类型 - 内层异常，外层捕获 ===");
        
        // 插入第一条数据（外层事务）
        Account account1 = new Account("REQ_NEW_CATCH_A", 1000.0);
        accountDao.insert(account1);
        System.out.println("方法A：插入数据 account1: " + account1);
        
        try {
            // 调用方法B（REQUIRES_NEW传播），方法B会抛出异常
            self.methodB_RequiresNew_Exception();
        } catch (RuntimeException e) {
            System.out.println("方法A：捕获到方法B的异常 - " + e.getMessage());
        }
        
        // 插入第二条数据（外层事务）- 可以正常执行
        Account account2 = new Account("REQ_NEW_CATCH_B", 2000.0);
        accountDao.insert(account2);
        System.out.println("方法A：插入数据 account2: " + account2);
        
        System.out.println("方法A：执行完成，外层事务应该提交");
        return "REQUIRES_NEW with catch - 外层数据保存，内层数据回滚";
    }
    
    /**
     * 测试场景3：内层方法抛出异常，外层不捕获异常
     * 预期：内层数据回滚，外层数据也回滚
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String testRequiresNewInnerExceptionWithoutCatch() {
        System.out.println("=== 测试REQUIRES_NEW传播类型 - 内层异常，外层不捕获 ===");
        
        // 插入第一条数据（外层事务）
        Account account1 = new Account("REQ_NEW_NO_CATCH_A", 1000.0);
        accountDao.insert(account1);
        System.out.println("方法A：插入数据 account1: " + account1);
        
        // 调用方法B（REQUIRES_NEW传播），方法B会抛出异常，外层不捕获
        self.methodB_RequiresNew_Exception();
        
        // 这行代码不会执行
        Account account2 = new Account("REQ_NEW_NO_CATCH_B", 2000.0);
        accountDao.insert(account2);
        System.out.println("方法A：插入数据 account2: " + account2);
        
        return "REQUIRES_NEW without catch - 应该不会到达这里";
    }
    
    /**
     * 方法B：REQUIRES_NEW传播类型
     * 正常执行，不抛出异常
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB_RequiresNew_Normal() {
        System.out.println("方法B（REQUIRES_NEW-正常）：开始执行，开启独立事务");
        
        // 插入数据
        Account accountB = new Account("REQ_NEW_B_NORMAL", 500.0);
        accountDao.insert(accountB);
        System.out.println("方法B（REQUIRES_NEW-正常）：插入数据 accountB: " + accountB);
        System.out.println("方法B（REQUIRES_NEW-正常）：正常结束，独立事务提交");
    }
    
    /**
     * 方法B：REQUIRES_NEW传播类型
     * 抛出异常
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB_RequiresNew_Exception() {
        System.out.println("方法B（REQUIRES_NEW-异常）：开始执行，开启独立事务");
        
        // 插入数据
        Account accountB = new Account("REQ_NEW_B_EXCEPTION", 500.0);
        accountDao.insert(accountB);
        System.out.println("方法B（REQUIRES_NEW-异常）：插入数据 accountB: " + accountB);
        
        // 抛出异常
        System.out.println("方法B（REQUIRES_NEW-异常）：抛出异常，独立事务回滚！");
        throw new RuntimeException("方法B发生异常 - REQUIRES_NEW传播");
    }
}
