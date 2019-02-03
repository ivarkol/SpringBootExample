package ru.airiva.springbootexample.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RestController
@RequestMapping("/test")
public class SpringBootController {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootController.class);

    @GetMapping
    @ApiOperation(value = "For controller testing")
    public String test() {
        return "test";
    }

    @PostMapping("/zip")
    @ApiOperation(
            value = "Upload zip file",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ApiResponses(
            @ApiResponse(code = 200, message = "File uploaded successful")
    )
    public ResponseEntity<String> unzip(
            @ApiParam(name = "zip", value = "Select zip file", required = true)
            @RequestPart("zip") MultipartFile zip) throws IOException {

        long start = System.currentTimeMillis();

        Path unzipPath = Paths.get("/home/ivan/temp/");
        Path zipFilePath = unzipPath.resolve(zip.getOriginalFilename());
        File zipFile = Files.createFile(zipFilePath).toFile();

        //copy zip file
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(zipFile))) {
            IOUtils.copy(zip.getInputStream(), outputStream);
        }


        //uncompress file
        logger.info("Uncompressing file {} started", zip.getOriginalFilename());
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path uncompressedFilePath = unzipPath.resolve(entry.getName());
                logger.debug("Uncompressing file: {}", uncompressedFilePath.toString());
                if (entry.isDirectory()) {
                    Files.createDirectories(uncompressedFilePath);
                } else {
                    Files.createFile(uncompressedFilePath);
                    try (BufferedInputStream bis = new BufferedInputStream(zf.getInputStream(entry));
                         BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(uncompressedFilePath.toFile()))) {
                        while (bis.available() > 0) {
                            fos.write(bis.read());
                        }
                    }
                }
            }
            Files.delete(zipFilePath);
        }

        long end = System.currentTimeMillis();
        long duration = (end - start)/1000;
        logger.info("Uncompressing file {} finished in {}", zip.getOriginalFilename(), duration);

        return ResponseEntity.ok("All good!");
    }

}
