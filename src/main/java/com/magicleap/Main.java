package com.magicleap;

import com.magicleap.config.InjectionConfig;
import dagger.Component;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import javax.inject.Singleton;

import static spark.Spark.*;

/**
 * Main entry point class for the project
 */
@Log4j2
public class Main {

    private final FileHandler fileHandler;

    @Inject
    public Main(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    /**
     * Main entry point for the program
     * @param args command line args
     */
    public static void main(String[] args) {
        log.info("Entry Point");
        Main main = DaggerMain_MainComponent.create().getMainRunner();
        main.run();
    }

    /**
     * Main module entry point of the program
     */
    @Singleton
    @Component(modules = {InjectionConfig.class})
    public interface MainComponent {
        Main getMainRunner();
    }

    /**
     * Runs the main logic of the program to start the web server
     */
    public void run() {
        initExceptionHandler((e) -> log.error(e.toString()));
        staticFiles.location("/public");

        get("/", (request, response) -> {response.redirect("/index.html"); return null;});
        post("/file", fileHandler.handleFileUpload());
        get("/file/:identifier", fileHandler.handleFileDownload());
    }
}
