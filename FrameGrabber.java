package com.computools.pubcam.core.clientService;

import com.computools.pubcam.core.dto.CameraDTO;
import com.computools.pubcam.core.services.stream.Streamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.computools.pubcam.core.services.ffmpeg.AbstractFFMPEGClientService.TEMP_DIR;


/**
 * author Manzar Victor
 *
 * @mail - iimpacttt@gmail.com
 */
public class FrameGrabber implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(FrameGrabber.class);

    private final String searchString;
    private Path currentFramePath;
    private final CameraDTO cameraDTO;
    private final ProcessHolder processHolder;
    private final Streamer streamer;
    private long lastGrabbedFrame = System.currentTimeMillis();
    private final long timeOut;

    public FrameGrabber(ProcessHolder processHolder,
                        CameraDTO cameraDTO,
                        Streamer streamer, long timeOut) {
        this.cameraDTO = cameraDTO;
        this.streamer = streamer;
        this.processHolder = processHolder;
        this.timeOut = timeOut;
        String temp = cameraDTO.getIp() + cameraDTO.getPort() + cameraDTO.getSufix();
        searchString = temp.replaceAll("\\p{Punct}", "");
        logger.info(String.format("Starting new connection -> %s", cameraDTO.toString()));
    }

    @Override
    public void run() {
        while (isAlive()){
            try {
                Thread.sleep(100);
                grabNewFrame();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if ((System.currentTimeMillis() - lastGrabbedFrame) > timeOut) {
                cleanUp();
                logger.error("Frame did not changed over " + (timeOut / 1000) + " seconds for " + cameraDTO.toString());
            }
        }
        cleanUp();
        logger.info("Job finished for -> " + cameraDTO.toString());
    }


    private void grabNewFrame() {
        List<String> files = getListOfFiles();

        Collections.reverse(files);

        for (int i = 0; i < files.size(); i++){
            if (i == 1){
                String pathToFrame = TEMP_DIR + files.get(i);
                if (currentFramePath == null || !currentFramePath.equals(Paths.get(pathToFrame))) {
                    setCurrentFrame(Paths.get(pathToFrame));
                }
            }
            if (i > 1){
                String pathToFrame = TEMP_DIR + files.get(i);
                deleteFile(pathToFrame);
            }
        }
    }

    private void setCurrentFrame(Path currentFramePath) {
        if (this.currentFramePath != null && this.currentFramePath.equals(currentFramePath)) return;
        try {
            this.currentFramePath = currentFramePath;
            streamer.stream(cameraDTO.getToken(), Files.readAllBytes(currentFramePath));
            lastGrabbedFrame = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanUp() {
        if (processHolder.isAlive()) {
            processHolder.killProcess();
        }
        getListOfFiles().forEach(fileName -> deleteFile(TEMP_DIR + fileName));
    }

    private List<String> getListOfFiles() {
        File file = new File(TEMP_DIR);

        return Arrays.stream(file.list()).filter(item -> item.contains(searchString))
                .sorted()
                .collect(Collectors.toList());

    }

    private void deleteFile(String path){
        try {
            Files.delete(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getToken() {
        return cameraDTO.getToken();
    }

    public boolean isAlive() {
        return processHolder.isAlive();
    }

    public synchronized void killProcess(){
        processHolder.killProcess();
    }

    public CameraDTO getCameraDto() {
        return this.cameraDTO;
    }
}
