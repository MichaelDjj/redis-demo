package org.michael.redisdemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author Dijunjie
 * @date 2022/6/12-18:47
 */
@RestController
@Slf4j
public class DemoController {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/deductStock/{productId}")
    public void deductStock(@PathVariable String productId) {
        //分布式锁key，一般为前缀+业务唯一标识id
        String lockKey = "product:"+productId;
        //传统分布式锁
//        RLock lock = redissonClient.getLock(lockKey);
        //获取写锁，为了优化性能，读写锁互斥
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock lock = readWriteLock.writeLock();
        try {
            //加锁
            lock.lock();
            //取得锁成功
            log.info("加锁成功, 线程 ID：" + Thread.currentThread().getId());
            //业务操作
            consume();
        } finally {
            //释放当前锁
            lock.unlock();
            log.info("解锁成功, 线程 ID：" + Thread.currentThread().getId());
        }
    }

    @GetMapping("/stock/product/{productId}")
    public void getProductStock(@PathVariable String productId) {
        String lockKey = "product:"+productId;
        //获取读锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock rLock = readWriteLock.readLock();
        try {
            rLock.lock();
            //业务逻辑
            //...
        } finally {
            rLock.unlock();
        }
    }

    private void consume() {
        int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("product_stock"));
        if (stock > 0) {
            int realStock = stock - 1;
            stringRedisTemplate.opsForValue().set("product_stock", realStock + "");
            log.info("扣除库存成功，当前库存：{}", realStock);
        } else {
            log.info("库存已经为0！！！");
        }
    }

}
