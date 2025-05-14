package com.atguigu.ssyx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//任务合并
public interface CompletableFutureDemo05 {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        //1 任务1 返回结果  1024
        CompletableFuture<Integer> futureA = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName() + "线程开始");
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            int value = 1024;
            System.out.println("task1：" + value);
            System.out.println(Thread.currentThread().getName() + "线程结束");
            return value;
        }, executorService);

        //2 任务2 返回结果 200
        CompletableFuture<Integer> futureB = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName() + "线程开始");
            int value = 200;
            System.out.println("task2：" + value);
            System.out.println(Thread.currentThread().getName() + "线程结束");
            return value;
        }, executorService);

        //3
        CompletableFuture<Void> all = CompletableFuture.allOf(futureA, futureB);
        all.join();
        System.out.println("over");


    }
}
