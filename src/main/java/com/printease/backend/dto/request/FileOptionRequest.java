package com.printease.backend.dto.request;

import lombok.Data;

/**
 * Per-file print options sent as a JSON string array element
 * within the multipart job submission.
 */
@Data
public class FileOptionRequest {

    private boolean colorPrint;
    private int copies = 1;
    private boolean doubleSided;
}
