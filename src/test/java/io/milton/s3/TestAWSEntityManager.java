package io.milton.s3;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestAWSEntityManager {

    AmazonS3Manager entityManager = null;
    
    String bucketName = "milton-s3-demo-" + UUID.randomUUID();
    
    String region = "us-west-2";
    
    @Before
    public void setUp() {
        entityManager = new AmazonS3ManagerImpl(region);
        assertFalse(entityManager.isRootFolder(bucketName));
        
        if(!entityManager.isRootFolder(bucketName))
            entityManager.createFolder(bucketName);
        
        assertTrue("Bucket not available: " + bucketName, entityManager.isRootFolder(bucketName));              
    }
    
    @After
    public void tearDown() {
        entityManager.deleteFolder(bucketName);
        assertFalse("Failed to remove Bucket " + bucketName, entityManager.isRootFolder(bucketName));
    }
    
    @Test
    public void testDownloadFileDoesNotExist() throws IOException {
        String folderName = "TEST";
        String keyNameNotAvailable = folderName + File.separator + UUID.randomUUID() + ".txt";
        File outputFile = null;
        
        outputFile = File.createTempFile("not-exist-in-aws", ".txt");
        boolean actually = entityManager.downloadFile(folderName, keyNameNotAvailable, outputFile);
        assertFalse(actually);
    }
    
    @Test
    public void testUploadFileToAWSAndDownloadFile() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("/test.txt");
        
        String folderName = "TEST";
        String keyName = folderName + File.separator + UUID.randomUUID() + ".txt";
        File outputFile = null;

        entityManager.uploadFile(bucketName, keyName, new File(resource.toURI()));
        
        outputFile = File.createTempFile("downloaded-file-aws", ".txt");
        assertTrue(entityManager.downloadFile(bucketName, keyName, outputFile));
        assertTrue("Error downloading file", outputFile.length() > 0);
        
        entityManager.deleteFile(bucketName, keyName);
        entityManager.deleteFile(bucketName, folderName);
        
        // Remove temp file
        if (outputFile.exists())
            outputFile.delete();
    }

}
