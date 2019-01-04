package com.computools.pubcam.core.clientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessHolder implements Runnable {

    private final Process process;
    private final Logger logger = LoggerFactory.getLogger(ProcessHolder.class);

    public ProcessHolder(Process process) {
        this.process = process;
    }

    @Override
    public void run() {
        try {
            process.waitFor();
            if (process.exitValue() != 0){
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    while (line != null){
                        logger.info(line);
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            process.destroyForcibly();
        }
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public void killProcess() {
        process.destroy();
    }
}
