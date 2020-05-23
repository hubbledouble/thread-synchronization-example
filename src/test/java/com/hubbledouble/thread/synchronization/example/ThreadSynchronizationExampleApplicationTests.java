/*
 *    Copyright (c) 2020, HubbleDouble
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.hubbledouble.thread.synchronization.example;

import com.hubbledouble.thread.synchronization.RunnableCode;
import com.hubbledouble.thread.synchronization.ThreadSynchronization;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class ThreadSynchronizationExampleApplicationTests {

    @Autowired
    private ThreadSynchronization threadSynchronization;

    private static final String PROCESS_NAME = "TRANSACTION_MANAGEMENT";
    private File file = new File("src/test/resources/transaction");

    @BeforeEach
    public void setup() {
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * If {@link ThreadSynchronization#execute(String, RunnableCode)} is called from a single thread.
     * Execution will happen all the time.
     * As soon as it finish executing the {@link RunnableCode} it will allow others the execution.
     */
    @Test
    void updateTransactionsInSequenceSingleThread() {

        final boolean firstTransaction = updateTransaction(100);
        final boolean secondTransaction = updateTransaction(100);
        final boolean thirdTransaction = updateTransaction(100);

        int amount = readTransaction();
        System.out.println("----- Single Thread");
        System.out.println("---------- First transaction : " + firstTransaction);
        System.out.println("---------- Second transaction : " + secondTransaction);
        System.out.println("---------- Third transaction : " + thirdTransaction);
        System.out.println("---------- Total amount : " + amount);

        Assert.assertEquals(300, amount);

    }

    /**
     * This test is just an example of how to code will behave if it is called at the same time by
     * multiple threads.
     * In real-time {@link ThreadSynchronization} is only needed if the process is being
     * executed on multiple nodes and there is some need for coordination.
     * Therefore only 1 thread in a given node will execute a critical section of a code if two or more
     * threads are trying to process at the same time.
     *
     * @throws Exception
     */
    @Test
    void updateTransactionsInSequenceMultiThreading_MockingMultiNodeProcess() throws Exception {

        int numberOfCores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfCores);

        System.out.println("----- Multi Threading");
        AtomicInteger updatedTimes = new AtomicInteger();
        executor.submit(() -> {
            sleep(500);
            final boolean firstTransaction = updateTransaction(100);
            if (firstTransaction)
                updatedTimes.getAndIncrement();
            System.out.println("---------- First transaction : " + firstTransaction);
        });

        executor.submit(() -> {
            sleep(500);
            final boolean secondTransaction = updateTransaction(100);
            if (secondTransaction)
                updatedTimes.getAndIncrement();
            System.out.println("---------- Second transaction : " + secondTransaction);
        });

        executor.submit(() -> {
            sleep(1000);
            final boolean thirdTransaction = updateTransaction(100);
            if (thirdTransaction)
                updatedTimes.getAndIncrement();
            System.out.println("---------- Third transaction : " + thirdTransaction);
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int amount = readTransaction();
        System.out.println("---------- Total amount : " + amount);
        Assert.assertEquals(updatedTimes.get() * 100, amount);

    }

    private boolean updateTransaction(Integer amount) {

        return threadSynchronization.execute(PROCESS_NAME, () -> {

            try {
                writeTransaction(amount);
            } catch (IOException e) {
                System.out.println("Error writing to file");
            }
        });

    }

    private int readTransaction() {

        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            return Integer.parseInt(br.readLine());
        } catch (Exception e) {
            return 0;
        }

    }

    private void writeTransaction(Integer amount) throws IOException {

        int initialAmount = readTransaction();

        int total = initialAmount + amount;

        Files.write(file.toPath(), String.valueOf(total).getBytes());

    }

    private void sleep(int time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
