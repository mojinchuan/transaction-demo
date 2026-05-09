package org.example.controller;

import org.example.dao.AccountDao;
import org.example.service.NestedPropagationService;
import org.example.service.RequiredPropagationService;
import org.example.service.RequiresNewPropagationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 事务传播类型测试控制器
 */
@RestController
@RequestMapping("/api/transaction")
public class TransactionTestController {
    
    @Autowired
    private RequiredPropagationService requiredPropagationService;
    
    @Autowired
    private NestedPropagationService nestedPropagationService;
    
    @Autowired
    private RequiresNewPropagationService requiresNewPropagationService;
    
    @Autowired
    private AccountDao accountDao;
    
    /**
     * 测试REQUIRED传播类型 - 捕获异常
     */
    @GetMapping("/required/with-catch")
    public Map<String, Object> testRequiredWithCatch() {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = requiredPropagationService.testRequiredWithCatch();
            result.put("success", true);
            result.put("message", message);
            result.put("data", accountDao.findAll());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("data", accountDao.findAll());
            result.put("explanation", 
                "REQUIRED传播 + catch异常：\n" +
                "- 方法A和方法B在同一个事务中\n" +
                "- 方法B抛出异常后，整个事务被标记为rollback-only\n" +
                "- 即使方法A捕获了异常，也无法继续执行（会导致 'Transaction silently rolled back' 错误）\n" +
                "- 所有数据都不会被保存\n" +
                "- 这是REQUIRED和NESTED的关键区别！");
        }
        return result;
    }
    
    /**
     * 测试REQUIRED传播类型 - 不捕获异常
     */
    @GetMapping("/required/without-catch")
    public Map<String, Object> testRequiredWithoutCatch() {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = requiredPropagationService.testRequiredWithoutCatch();
            result.put("success", true);
            result.put("message", message);
            result.put("data", accountDao.findAll());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("data", accountDao.findAll());
            result.put("explanation", 
                "REQUIRED传播 + 不catch异常：\n" +
                "- 方法A和方法B在同一个事务中\n" +
                "- 方法B抛出异常，导致整个事务回滚\n" +
                "- 所有数据都不会被保存");
        }
        return result;
    }
    
    /**
     * 测试NESTED传播类型 - 捕获异常
     */
    @GetMapping("/nested/with-catch")
    public Map<String, Object> testNestedWithCatch() {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = nestedPropagationService.testNestedWithCatch();
            result.put("success", true);
            result.put("message", message);
            result.put("data", accountDao.findAll());
            result.put("explanation", 
                "NESTED传播 + catch异常：\n" +
                "- 方法B在嵌套事务中执行（使用保存点）\n" +
                "- 方法B抛出异常后，只回滚到保存点（方法B的数据不保存）\n" +
                "- 方法A捕获了异常，继续执行，外层事务正常提交\n" +
                "- 只有account1和account2会被保存，accountB不会保存");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    /**
     * 测试NESTED传播类型 - 不捕获异常
     */
    @GetMapping("/nested/without-catch")
    public Map<String, Object> testNestedWithoutCatch() {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = nestedPropagationService.testNestedWithoutCatch();
            result.put("success", true);
            result.put("message", message);
            result.put("data", accountDao.findAll());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("data", accountDao.findAll());
            result.put("explanation", 
                "NESTED传播 + 不catch异常：\n" +
                "- 方法B在嵌套事务中执行\n" +
                "- 方法B抛出异常，虽然可以回滚到保存点\n" +
                "- 但异常继续传播到方法A，导致外层事务也回滚\n" +
                "- 所有数据都不会被保存");
        }
        return result;
    }
    
    /**
     * 测试NESTED传播类型 - 内层正常，外层异常
     */
    @GetMapping("/nested/outer-exception")
    public Map<String, Object> testNestedOuterException() {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = nestedPropagationService.testNestedOuterException();
            result.put("success", true);
            result.put("message", message);
            result.put("data", accountDao.findAll());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("data", accountDao.findAll());
            result.put("explanation", 
                "NESTED传播 + 外层异常：\n" +
                "- 方法B（NESTED）正常执行，没有异常\n" +
                "- 方法A在调用方法B后抛出异常\n" +
                "- 由于方法A是外层事务，它的异常导致整个事务回滚\n" +
                "- 包括方法B的数据在内的所有数据都会被回滚\n" +
                "- 这说明：NESTED只能隔离内层异常，不能隔离外层异常");
        }
        return result;
    }
    
    /**
     * 测试REQUIRES_NEW传播类型 - 内层正常，外层异常
     */
    @GetMapping("/requires-new/outer-exception")
    public Map<String, Object> testRequiresNewOuterException() {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = requiresNewPropagationService.testRequiresNewOuterException();
            result.put("success", true);
            result.put("message", message);
            result.put("data", accountDao.findAll());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("data", accountDao.findAll());
            result.put("explanation", 
                "REQUIRES_NEW传播 + 外层异常：\n" +
                "- 方法B（REQUIRES_NEW）在独立事务中执行并正常提交\n" +
                "- 方法A在外层事务中抛出异常，外层事务回滚\n" +
                "- 方法B的数据已提交，不受外层回滚影响\n" +
                "- 结果：只有方法B的数据保存，方法A的数据回滚\n" +
                "- 这说明：REQUIRES_NEW创建完全独立的事务");
        }
        return result;
    }
    
    /**
     * 测试REQUIRES_NEW传播类型 - 内层异常，外层捕获
     */
    @GetMapping("/requires-new/inner-exception-catch")
    public Map<String, Object> testRequiresNewInnerExceptionWithCatch() {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = requiresNewPropagationService.testRequiresNewInnerExceptionWithCatch();
            result.put("success", true);
            result.put("message", message);
            result.put("data", accountDao.findAll());
            result.put("explanation", 
                "REQUIRES_NEW传播 + 内层异常 + catch：\n" +
                "- 方法B（REQUIRES_NEW）在独立事务中执行，抛出异常后回滚\n" +
                "- 方法A捕获了异常，继续执行\n" +
                "- 方法A的外层事务正常提交\n" +
                "- 结果：方法A的数据保存，方法B的数据回滚");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("data", accountDao.findAll());
        }
        return result;
    }
    
    /**
     * 测试REQUIRES_NEW传播类型 - 内层异常，外层不捕获
     */
    @GetMapping("/requires-new/inner-exception-no-catch")
    public Map<String, Object> testRequiresNewInnerExceptionWithoutCatch() {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = requiresNewPropagationService.testRequiresNewInnerExceptionWithoutCatch();
            result.put("success", true);
            result.put("message", message);
            result.put("data", accountDao.findAll());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("data", accountDao.findAll());
            result.put("explanation", 
                "REQUIRES_NEW传播 + 内层异常 + 不catch：\n" +
                "- 方法B（REQUIRES_NEW）在独立事务中执行，抛出异常后回滚\n" +
                "- 异常传播到方法A，导致外层事务也回滚\n" +
                "- 结果：所有数据都回滚（方法B已回滚，方法A也回滚）");
        }
        return result;
    }
    
    /**
     * 查询所有账户数据
     */
    @GetMapping("/accounts")
    public Map<String, Object> getAllAccounts() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", accountDao.findAll());
        result.put("count", accountDao.findAll().size());
        return result;
    }
    
    /**
     * 清空所有数据
     */
    @DeleteMapping("/clear")
    public Map<String, Object> clearAllData() {
        Map<String, Object> result = new HashMap<>();
        accountDao.deleteAll();
        result.put("success", true);
        result.put("message", "所有数据已清空");
        return result;
    }
}
