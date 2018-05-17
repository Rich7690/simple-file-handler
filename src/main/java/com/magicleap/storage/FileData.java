package com.magicleap.storage;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * File data object to handle mapping identifiers to their file name
 */
@DynamoDBTable(tableName = "FileMapping")
@RequiredArgsConstructor
@Data
public class FileData {

    @DynamoDBHashKey
    private String identifier;
    private String filename;
    @DynamoDBVersionAttribute
    private Long version;

    public FileData(String filename) {
        this.filename = filename;
    }
}
