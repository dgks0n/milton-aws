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

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3Object;

public class AmazonS3ManagerImpl implements AmazonS3Manager {
    
    private static final Logger LOG = LoggerFactory.getLogger(AmazonS3ManagerImpl.class);
    
    private static final String GROUPS_USERS = "http://acs.amazonaws.com/groups/global/AllUsers";
    
    private static final String AWS_END_POINT_START = "s3-";
    private static final String AWS_END_POINT_END = ".amazonaws.com";

    private static final String AWS_END_POINT_STANDARD = "s3.amazonaws.com";
    
    private AWSCredentialsProvider credentialsProvider;
    private AmazonS3 storageService;
    private String endpoint;
    
    public AmazonS3ManagerImpl(String region) {
        credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        storageService = new AmazonS3Client(credentialsProvider);
        if (!region.equals(AWS_END_POINT_STANDARD)) {
            endpoint = AWS_END_POINT_START + region + AWS_END_POINT_END;
        } else {
            endpoint = AWS_END_POINT_STANDARD;
        }

        storageService.setEndpoint(endpoint);
    }
    
    @Override
    public boolean isRootFolder(String folerName) {
        return storageService.doesBucketExist(folerName);
    }

    @Override
    public void createFolder(String folerName) {
        storageService.createBucket(folerName);
    }

    @Override
    public void deleteFolder(String folerName) {
        storageService.deleteBucket(folerName);
    }

    @Override
    public void uploadFile(String folderName, String fileName, File file) {
        storageService.putObject(folderName, fileName, file);
    }

    @Override
    public void deleteFile(String folderName, String fileName) {
        storageService.deleteObject(folderName, fileName);
    }

    @Override
    public void makePublic(String folderName, String fileName) {
        storageService.setObjectAcl(folderName, fileName, CannedAccessControlList.PublicRead);
    }

    @Override
    public boolean isFilePublic(String folderName, String fileName) {
        AccessControlList accessControlList = storageService.getObjectAcl(folderName, fileName);
        for (Iterator<Grant> iterator = accessControlList.getGrants().iterator(); iterator.hasNext();) {
                Grant grant = iterator.next();
                if(grant.getPermission().equals(Permission.Read) && grant.getGrantee().getIdentifier().equals(GROUPS_USERS))
                  return true;
        }
        return false;
    }

    @Override
    public boolean downloadFile(String folderName, String keyNotAvailable, File outputFile) {
        try {
            storageService.getObject(new GetObjectRequest(folderName, keyNotAvailable), outputFile);
            return true;
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
        return false;
    }

    @Override
    public void uploadFile(String folderName, String keyName, InputStream fileStream) {
        storageService.putObject(folderName, keyName, fileStream, null);
    }

    @Override
    public InputStream downloadFile(String folderName, String keyName) {
        S3Object s3Object = storageService.getObject(folderName, keyName);
        return s3Object.getObjectContent();
    }

    @Override
    public String getFileUrl(String folderName, String keyName) {
        if(isFilePublic(folderName, keyName))
            return "http://" + endpoint + File.separator + folderName + File.separator + keyName;
        
        return null;
    }

}
