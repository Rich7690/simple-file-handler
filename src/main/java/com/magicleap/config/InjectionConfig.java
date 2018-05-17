package com.magicleap.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import dagger.Module;
import dagger.Provides;

/**
 * Configuration definition for dependency injection
 */
@Module
public class InjectionConfig {

    @Provides
    public AmazonS3 getS3Client() {
        return AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
    }

    @Provides
    public AmazonDynamoDB getDynamoDBMapper() {
        return AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
    }
}
