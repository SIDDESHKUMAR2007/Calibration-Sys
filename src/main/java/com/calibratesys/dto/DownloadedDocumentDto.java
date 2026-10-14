package com.calibratesys.dto;

import org.springframework.core.io.Resource;

public class DownloadedDocumentDto {

    private final Resource resource;
    private final String filename;
    private final String contentType;
    private final long contentLength;

    public DownloadedDocumentDto(Resource resource, String filename, String contentType, long contentLength) {
        this.resource = resource;
        this.filename = filename;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public Resource getResource() {
        return resource;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }
}
