package com.magicleap;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.google.common.io.Files;
import com.google.common.net.UrlEscapers;
import com.magicleap.storage.FileData;
import com.magicleap.storage.FileMappingDao;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import spark.HaltException;
import spark.Route;

import javax.inject.Inject;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;

import static spark.Spark.halt;

/**
 * handler for file upload/download related functions
 */
@Log4j2
public class FileHandler {

    private static final Duration DEFAULT_EXPIRE_DURATION = Duration.ofDays(1);
    private static final int MAX_FILE_SIZE = 10_000_000; //10 MB
    private static final int MAX_FILE_NAME_SIZE = 1024;
    private static final String BUCKET_NAME = "magicleap-bucket";

    private final AmazonS3 s3Client;
    private final FileMappingDao fileMappingDao;

    @Inject
    public FileHandler(AmazonS3 s3Client, FileMappingDao fileMappingDao) {
        this.s3Client = s3Client;
        this.fileMappingDao = fileMappingDao;
    }

    /**
     * Route for handling the file download
     *
     * @return the route
     */
    public Route handleFileDownload() {
        return (request, response) -> {
            try {
                String identifier = request.params("identifier");
                log.info("Identifier: " + identifier);

                FileData data = fileMappingDao.load(identifier);
                if (data == null) {
                    response.status(400);
                    return "Could not find a file with that identifier";
                }
                boolean fileExists = s3Client.doesObjectExist(BUCKET_NAME, identifier);

                if (!fileExists) {
                    response.status(400);
                    return "Could not find the file in the underlying storage with that identifier";
                }

                ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
                // Note: the filename content disposition field is non-standard:
                // https://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html#sec19.5.1
                // Should only need to replace quote characters since other characters are valid in filenames
                // Also note that on some platforms certain characters will automatically be replaced by browsers when
                // characters are invalid in the filename (i.e. the "/" character in Windows
                overrides.setContentDisposition("filename=\"" + data.getFilename().replace("\"", "\\\"") + "\"");
                GeneratePresignedUrlRequest generateRequest = new GeneratePresignedUrlRequest(BUCKET_NAME, identifier)
                        .withResponseHeaders(overrides);
                generateRequest.setExpiration(Date.from(Instant.now().plus(DEFAULT_EXPIRE_DURATION)));
                URL url = s3Client.generatePresignedUrl(generateRequest);

                response.redirect(url.toExternalForm());
                return null;
            }
            catch (HaltException e) {
                throw e;
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
                response.status(500);
                return "Failed to upload file";
            }
        };
    }

    /**
     * Route for handling the file upload
     *
     * @return the route
     */
    public Route handleFileUpload() {
        return (request, response) -> {
            try {
                log.debug(request.contentLength());
                if (request.contentLength() > MAX_FILE_SIZE) {
                    response.status(400);
                    return "File must be less than " + MAX_FILE_SIZE + " bytes";
                }

                request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/tmp"));

                Part part = request.raw().getPart("file");

                if (part.getSubmittedFileName().length() > MAX_FILE_NAME_SIZE) {
                    response.status(400);
                    return "File name must be less than " + MAX_FILE_NAME_SIZE + " characters";
                }
                FileData data = new FileData(part.getSubmittedFileName());
                fileMappingDao.save(data);

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(part.getContentType());
                try (InputStream input = part.getInputStream()) {
                    log.debug("identifier: " + data.getIdentifier());
                    s3Client.putObject(BUCKET_NAME, data.getIdentifier(), input, metadata);
                }
                return "Data has been uploaded with the following identifier: " + data.getIdentifier();
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
                response.status(500);
                return "Failed to upload file";
            }
        };
    }
}
