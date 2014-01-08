/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.s3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestAmazonS3Manager {

    AmazonS3Manager entityManager = null;
    
    String bucketName = "milton-s3-demo-" + UUID.randomUUID();
    
    String region = "us-west-2";
    
    @Before
    public void setUp() {
        entityManager = new AmazonS3ManagerImpl(region, bucketName);
        assertFalse(entityManager.isRootBucket());
        
        if(!entityManager.isRootBucket())
            entityManager.createBucket();
        
        assertTrue("Bucket not available: " + bucketName, entityManager.isRootBucket());              
    }
    
    @After
    public void tearDown() {
        entityManager.deleteBucket();
        assertFalse("Failed to remove Bucket " + bucketName, entityManager.isRootBucket());
    }
    
    @Test
    public void testDownloadFileDoesNotExist() throws IOException {
        String keyNameNotAvailable = "TEST" + File.separator + UUID.randomUUID() + ".txt";
        
        File outputFile = File.createTempFile("not-exist-in-aws", ".txt");
        boolean actually = entityManager.downloadEntity(keyNameNotAvailable, outputFile);
        assertFalse(actually);
    }
    
    @Test
    public void testUploadFileToAWSAndDownloadFile() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("/test.txt");
        
        String folderName = "TEST";
        String keyName = folderName + File.separator + UUID.randomUUID() + ".txt";
        File outputFile = null;

        entityManager.uploadEntity(keyName, new File(resource.toURI()));
        
        outputFile = File.createTempFile("downloaded-file-aws", ".txt");
        assertTrue(entityManager.downloadEntity(keyName, outputFile));
        assertTrue("Error downloading file", outputFile.length() > 0);
        
        entityManager.deleteEntity(keyName);
        entityManager.deleteEntity(folderName);
        
        // Remove temp file
        if (outputFile.exists())
            outputFile.delete();
    }

}
