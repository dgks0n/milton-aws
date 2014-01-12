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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.service.AmazonStorageService;
import io.milton.s3.service.AmazonStorageServiceImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class TestAmazonStorage {

    AmazonStorageService amazonStorageService;
    
    String bucketName = "milton-s3-demo-" + UUID.randomUUID();
    
    @Before
    public void setUp() throws Exception {
        amazonStorageService = new AmazonStorageServiceImpl(Region.getRegion(Regions.US_WEST_2));
        amazonStorageService.createBucket(bucketName);
    }

    @After
    public void tearDown() throws Exception {
        amazonStorageService.deleteBucket(bucketName);
    }
    
    @Test
    public void testCreatedRootFolder() {
        Folder rootFolder = amazonStorageService.findRootFolder(bucketName);
        
        assertNotNull(rootFolder);
    }
    
    @Test
    public void testCreateFolder() {
        Folder rootFolder = amazonStorageService.findRootFolder(bucketName);
        assertNotNull(rootFolder);
        
        Folder folder = new Folder("Test 1", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, folder, null));
        
        folder = new Folder("Test 2", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, folder, null));
        
        folder = new Folder("Test 3", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, folder, null));
        
        assertEquals(3, amazonStorageService.findEntityByParent(bucketName, rootFolder).size());
    }
    
    @Test
    public void testRenameFolder() {
        Folder rootFolder = amazonStorageService.findRootFolder(bucketName);
        assertNotNull(rootFolder);
        
        Folder folder = new Folder("Test 1", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, folder, null));
        assertEquals(1, amazonStorageService.findEntityByParent(bucketName, rootFolder).size());
        
        boolean isRenamed = amazonStorageService.updateEntityByUniqueId(bucketName, folder, 
                rootFolder, "Test Renamed", true);
        
        assertTrue(isRenamed);
        assertEquals(1, amazonStorageService.findEntityByParent(bucketName, rootFolder).size());
    }

    @Test
    public void testUploadEntityToBucket() throws IOException {
        Folder rootFolder = amazonStorageService.findRootFolder(bucketName);
        assertNotNull(rootFolder);
        
        java.io.File file = new java.io.File("src/test/resources/test/1c8e930f68f4c260760e0d2e238e905a978e4259");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        
        InputStream inputStream = new FileInputStream(file);
        
        File entity = new File("1c8e930f68f4c260760e0d2e238e905a978e4259", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, entity, inputStream));
        
        inputStream.close();
    }
    
    @Test
    public void testRenameFile() throws IOException {
        Folder rootFolder = amazonStorageService.findRootFolder(bucketName);
        assertNotNull(rootFolder);
        
        java.io.File file = new java.io.File("src/test/resources/test/1c8e930f68f4c260760e0d2e238e905a978e4259");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        
        InputStream inputStream = new FileInputStream(file);
        
        File entity = new File("1c8e930f68f4c260760e0d2e238e905a978e4259", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, entity, inputStream));
        
        boolean isSuccess = amazonStorageService.updateEntityByUniqueId(bucketName, entity, rootFolder, 
                "File Renamed", true);
        assertTrue(isSuccess);
        assertEquals(1, amazonStorageService.findEntityByParent(bucketName, rootFolder).size());
        
        inputStream.close();
    }
    
    @Test
    public void testMovingFile() throws IOException {
        Folder rootFolder = amazonStorageService.findRootFolder(bucketName);
        assertNotNull(rootFolder);
        
        Folder folder1 = new Folder("Test 1", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, folder1, null));
        assertEquals(1, amazonStorageService.findEntityByParent(bucketName, rootFolder).size());
        
        Folder folder2 = new Folder("Test 2", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, folder2, null));
        assertEquals(2, amazonStorageService.findEntityByParent(bucketName, rootFolder).size());
        
        java.io.File file = new java.io.File("src/test/resources/test/1c8e930f68f4c260760e0d2e238e905a978e4259");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        
        InputStream inputStream = new FileInputStream(file);
        
        File entity = new File("1c8e930f68f4c260760e0d2e238e905a978e4259", folder1);
        assertTrue(amazonStorageService.putEntity(bucketName, entity, inputStream));
        
        boolean isSuccess = amazonStorageService.updateEntityByUniqueId(bucketName, entity, folder2, 
                "1c8e930f68f4c260760e0d2e238e905a978e4259", false);
        assertTrue(isSuccess);
        assertEquals(1, amazonStorageService.findEntityByParent(bucketName, folder2).size());
        
        inputStream.close();
    }
    
    @Test
    public void testGetChildrenOfRoot() throws IOException {
        Folder rootFolder = amazonStorageService.findRootFolder(bucketName);
        assertNotNull(rootFolder);
        
        java.io.File file = new java.io.File("src/test/resources/test/1c8e930f68f4c260760e0d2e238e905a978e4259");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        
        InputStream inputStream = new FileInputStream(file);
        
        File entity = new File("1c8e930f68f4c260760e0d2e238e905a978e4259", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, entity, inputStream));
        inputStream.close();
        
        file = new java.io.File("src/test/resources/test/1cf8d9a9824c83b082565eb8d2d79e9dd264d7b9");
        assertTrue(file.exists());
        assertTrue(file.isFile());

        inputStream = new FileInputStream(file);
        
        entity = new File("1c8e930f68f4c260760e0d2e238e905a978e4259", rootFolder);
        assertTrue(amazonStorageService.putEntity(bucketName, entity, inputStream));
        
        List<Entity> children = amazonStorageService.findEntityByParent(bucketName, rootFolder);
        assertNotNull(children);
        assertEquals(2, children.size());
        
        inputStream.close();
    }

}
