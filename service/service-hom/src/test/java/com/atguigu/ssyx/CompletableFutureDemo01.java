package com.atguigu.ssyx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//runAsync  方法不支持返回值
public interface CompletableFutureDemo01 {

    public static void main(String[] args) {
        //创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        System.out.println("main begin");
        //创建异步对象
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getName());
            int result = 1024;
            System.out.println("result：" + result);
        }, executorService);
        System.out.println("main over");
    }
}
