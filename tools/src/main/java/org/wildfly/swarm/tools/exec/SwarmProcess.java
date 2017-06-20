/**
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.tools.exec;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Bob McWhirter
 */
public class SwarmProcess {

    public SwarmProcess(Process process, File processFile, OutputStream stdout, Path stdoutFile, OutputStream stderr, Path stderrFile) throws IOException {
        this.process = process;
        this.latch = new CountDownLatch(1);
        this.stdout = new IOBridge(this.latch, process.getInputStream(), stdout, stdoutFile);
        this.stderr = new IOBridge(this.latch, process.getErrorStream(), stderr, stderrFile);
        this.processFile = processFile;

        if (processFile != null) {
            System.out.println("Process file in Swarm process:" + processFile);
        }

        new Thread(this.stdout).start();
        new Thread(this.stderr).start();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    if (!this.process.isAlive()) {
                        this.latch.countDown();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }

        }).start();
    }

    public Exception getError() {
        if (this.stdout.getError() != null) {
            return this.stdout.getError();
        }
        if (this.stderr.getError() != null) {
            return this.stderr.getError();
        }
        return null;
    }

    public OutputStream getOutputStream() {
        return process.getOutputStream();
    }

    public void destroy() {
        process.destroy();
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public Process destroyForcibly() {
        return process.destroyForcibly();
    }

    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return process.waitFor(timeout, unit);
    }

    public int exitValue() {
        return process.exitValue();
    }

    public void awaitReadiness(long timeout, TimeUnit timeUnit) throws InterruptedException {
        this.latch.await(timeout, timeUnit);
    }

    public int stop() throws InterruptedException {
        return stop(10, TimeUnit.SECONDS);
    }

    public int stop(long timeout, TimeUnit timeUnit) throws InterruptedException {

        if (this.processFile != null) {
            System.out.println("----DELETING FILE IN SWARM PROCESS:" + processFile.getAbsolutePath());
            this.processFile.delete();
            System.out.println("----FILE DELETED IN SWARM PROCESS");
            while (true) {
                if (!process.isAlive()) {
                    System.out.println("BREAK SWARM PROCESS");
                    break;
                }
            }
        } else {
            this.process.destroy();
        }

        System.out.println("Process status:" + this.process.exitValue());

        try {
            System.out.println("Closing stdout");
            this.stdout.close();
            System.out.println("stdout closed");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Closing sterr");
            this.stderr.close();
            System.out.println("stderr closed");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!this.process.waitFor(timeout, timeUnit)) {
            System.out.println("Destroying forcibly");
            process.destroyForcibly();
        }

        if (!process.isAlive()) {
            return process.exitValue();
        }

        return -1;
    }

    private final Process process;

    private final IOBridge stdout;

    private final IOBridge stderr;

    private final CountDownLatch latch;

    private File processFile;
}
