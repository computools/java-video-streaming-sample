package com.computools.pubcam.core.services.ffmpeg;

import com.computools.pubcam.core.dto.CameraDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.computools.pubcam.core.services.ffmpeg.AbstractFFMPEGClientService.TEMP_DIR;

public class SimpleFFMPEGJobCreator implements FFMPEGJobCreator {

    private final Logger logger = LoggerFactory.getLogger(SimpleFFMPEGJobCreator.class);
    private final String prefix = "%Y%m%d%H%M%S.mp4";
    private final String script;

    public Process createJob(CameraDTO camera) {
        String dir = (camera.getIp() + camera.getPort() + camera.getSufix()).replaceAll("\\p{Punct}", "");
        String url = createUrlForCam(camera);

        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        builder.command(generateCommands(url, TEMP_DIR + dir + prefix));
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            logger.error("Cant start process -> " + camera, e);
        }

        return process;
    }

    private List<String> generateCommands(String url, String dir) {
        String[] arrayCommands = String.format(script, url, dir).split(" ");
        return Arrays.asList(arrayCommands);
    }


    public String createUrlForCam(CameraDTO camera) {
        StringBuilder builder = new StringBuilder();
        if (camera.getLogin() != null && camera.getPassword() != null) {
            builder.append("rtsp://")
                    .append(camera.getLogin()).append(":")
                    .append(camera.getPassword()).append("@")
                    .append(camera.getIp()).append(":")
                    .append(camera.getPort()).append(camera.getSufix())
//                    .append("?")
            ;
        } else {
            builder.append("rtsp://")
                    .append(camera.getIp()).append(":")
                    .append(camera.getPort()).append(camera.getSufix());
        }
        return builder.toString();
    }



    public SimpleFFMPEGJobCreator(String script) {
        this.script = script;
    }
}
