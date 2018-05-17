package com.magicleap.storage;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Data access layer for file mappings
 */
public class FileMappingDao {

    private final DynamoDBMapper mapper;

    @Inject
    public FileMappingDao(AmazonDynamoDB client) {
        this.mapper = new DynamoDBMapper(client);
    }

    /**
     * Saves file data
     * @param data the data
     */
    public void save(FileData data) {
        if(data.getIdentifier() == null) {
            // replacing the - makes for easier copy and paste in a browser
            data.setIdentifier(UUID.randomUUID().toString().replace("-",""));
        }
        mapper.save(data);
    }

    /**
     * Loads the file data
     * @param identifier the identifier
     * @return the file data object if it exists; otherwise, null
     */
    public FileData load(String identifier) {
        return mapper.load(FileData.class, identifier);
    }
}
