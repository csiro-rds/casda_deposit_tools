package au.csiro.casda.datadeposit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * #%L
 * CSIRO Data Access Portal
 * %%
 * Copyright (C) 2010 - 2015 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * Tester of java fork and exec.
 * 
 * Usage: <processNumber> <timeLoop> <command>. eg: 10 10 testFile.bat
 * 
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class ProcessTester
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        int numberProcess = 1;
        int timeLoop = 1;
        String command = "";

        if (args.length == 3)
        {
            try
            {
                numberProcess = Integer.parseInt(args[0]);
                timeLoop = Integer.parseInt(args[1]);
                command = args[2];
            }
            catch (NumberFormatException e)
            {
                System.err.println(
                        "Arguments are not correct <processNumber> <timeLoop> <command>. eg: 10 10 echo.bat");
                System.exit(1);
            }
        }
        else
        {
            System.err.println(
                    "Number of argument is not equal 3 <processNumber> <timeLoop> <command>. eg: 10 10 echo.bat");
            System.exit(1);
        }

        long startTime = System.currentTimeMillis();

        ProcessTester processTester = new ProcessTester();
        for (long n = 0; ((System.currentTimeMillis() - startTime) / 60000) < timeLoop; n++)
        {
            System.out.println("*********** loop count: " + n + " ************\n");

            // Creating n threads as required
            ExecutorService processExec = Executors.newFixedThreadPool(numberProcess);
            ExecutorService memoryExec = Executors.newFixedThreadPool(numberProcess);
            for (int i = 0; i < numberProcess; i++)
            {
                memoryExec.execute(processTester.new MemoryRunnable(i));
                processExec.execute(processTester.new ProcessRunnable(i, command));
            }
            processExec.shutdown();
            memoryExec.shutdown();
            processExec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            memoryExec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        System.out.println(
                "Finished all threads, total runtime: " + (System.currentTimeMillis() - startTime) / 60000 + " min");
    }

    /**
     * 
     * ProcessRunnable for command process
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public class ProcessRunnable implements Runnable
    {
        String command = "";
        int pid = 0;

        /**
         * 
         * @param pid
         * @param command
         */
        public ProcessRunnable(int pid, String command)
        {
            this.command = command;
            this.pid = pid;
        }

        @Override
        public void run()
        {
            try
            {
                List<String> commands = new ArrayList<String>();
                commands.add(command);

                ProcessBuilder pb = new ProcessBuilder(commands);
                System.out.println("Run ProcessRunnable: " + pid);
                Process process = pb.start();
                int errCode = process.waitFor();
                System.out.println(
                        "ProcessRunnable : " + pid + " executed, any errors? " + (errCode == 0 ? "No" : "Yes"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * MemoryRunnable for consume memory
     * 
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public class MemoryRunnable implements Runnable
    {
        private final int megabyte = 1024 * 1024;
        int pid = 0;

        public long bytesToMegabytes(long bytes)
        {
            return bytes / megabyte;
        }

        /**
         * 
         * @param pid
         */
        public MemoryRunnable(int pid)
        {
            this.pid = pid;
        }

        @Override
        public void run()
        {
            Vector<byte[]> vector = new Vector<byte[]>();
            byte b[] = new byte[megabyte];
            for (int i = 0; i <= megabyte; i++)
            {
                vector.add(b);
            }
            vector.clear();

            // Get the Java runtime
            Runtime runtime = Runtime.getRuntime();
            // Run the garbage collector
            // runtime.gc();
            // Calculate the used memory
            long memory = runtime.totalMemory() - runtime.freeMemory();
            System.out.println(
                    "Pid: " + pid + ", Used memory is bytes: " + memory + ". megabytes: " + bytesToMegabytes(memory));
        }
    }

}