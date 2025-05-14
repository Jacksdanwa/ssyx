package com.atguigu.ssyx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//计算完成回调
public interface CompletableFutureDemo03 {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        System.out.println("main begin");

        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getName());
            int value = 1024;
            System.out.println("value:" + value);
            return value;
        }, executorService).whenComplete((rs,exception)->{
            System.out.println("when complete:" + rs);
            System.out.println("when exception:" + exception);
        });

        System.out.println("main over");
    }
}
