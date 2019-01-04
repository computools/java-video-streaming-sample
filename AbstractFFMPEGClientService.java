package com.computools.pubcam.core.services.ffmpeg;

import com.computools.pubcam.core.clientService.FrameGrabber;
import com.computools.pubcam.core.clientService.ProcessHolder;
import com.computools.pubcam.core.dto.CameraDTO;
import com.computools.pubcam.core.services.context.EventStreamContext;
import com.computools.pubcam.core.services.context.StreamContext;
import com.computools.pubcam.core.services.stream.Streamer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public abstract class AbstractFFMPEGClientService implements FFMPEGClientService {

    private final FFMPEGJobCreator jobCreator;
    private final Streamer streamer;
    protected final StreamContext streamContext;
    private final long timeOut;

    protected final ExecutorService cameraHolderExecutor = Executors.newCachedThreadPool();

    public static final String TEMP_DIR = "/tmp/pubcam/";


    public FrameGrabber startCameraStream(CameraDTO camera) {
        FrameGrabber frameGrabber = streamContext.getCurrentCameras().values().stream()
                .filter(client -> compareCameras(client.getCameraDto(), camera))
                .findFirst().orElse(null);

        if (frameGrabber != null) {
            streamContext.getCurrentCameras().remove(frameGrabber.getToken());
            camera.setToken(frameGrabber.getToken());
            frameGrabber.killProcess();
        }


        Process ffmpegJob = jobCreator.createJob(camera);
        return createStreamingClient(ffmpegJob, camera);
    }

    private boolean compareCameras(CameraDTO client, CameraDTO camera) {
        return client.equals(camera);
    }


    private FrameGrabber createStreamingClient(Process job, CameraDTO cameraDTO) {
        ProcessHolder holder = new ProcessHolder(job);
        FrameGrabber frameGrabber = new FrameGrabber(holder, cameraDTO, streamer, timeOut);
        cameraHolderExecutor.submit(holder);
        cameraHolderExecutor.submit(frameGrabber);
        return frameGrabber;
    }

    protected void cleanUp() {
        try(Stream<Path> pathStream = Files.walk(Paths.get(TEMP_DIR))) {
            pathStream.forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public AbstractFFMPEGClientService(FFMPEGJobCreator jobCreator,
                                       Streamer streamer,
                                       StreamContext streamContext,
                                       long  timeOut) {
        this.jobCreator = jobCreator;
        this.streamer = streamer;
        this.streamContext = streamContext;
        this.timeOut = timeOut;
    }
}
