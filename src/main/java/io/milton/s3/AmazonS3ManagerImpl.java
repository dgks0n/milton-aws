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

    private static final String ENDPOINT_START = "s3-";
    private static final String ENDPOINT_END = ".amazonaws.com";
    private static final String ENDPOINT_STANDARD = "s3.amazonaws.com";

    private final AmazonS3 amazonS3Client;
    private final String regionEndpoint;
    
    private final String bucketName;

    /**
     * You can choose the geographical region where Amazon S3 will store the
     * buckets you create. You might choose a region to optimize latency,
     * minimize costs, or address regulatory requirements.
     * 
     * @param region
     */
    public AmazonS3ManagerImpl(String region, String bucketName) {
        // Set bucket name 
        this.bucketName = bucketName;
        
        LOG.info("Create an instance of the AmazonS3Client class by providing your "
                + "AWS Account or IAM user credentials (Access Key ID, Secret Access Key)");
        regionEndpoint = region.equals(ENDPOINT_STANDARD) ? ENDPOINT_STANDARD
                : ENDPOINT_START + region + ENDPOINT_END;

        // Create an instance of the AmazonS3Client class by providing your AWS
        // Account or IAM user credentials (Access Key ID, Secret Access Key)
        amazonS3Client = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider());
        amazonS3Client.setEndpoint(regionEndpoint);
    }

    @Override
    public boolean isRootBucket() {
        LOG.info("Checks if the specified bucket " + bucketName + " exists");
        return amazonS3Client.doesBucketExist(bucketName);
    }

    @Override
    public void createBucket() {
        LOG.info("Creates a new Amazon S3 bucket " + bucketName + " with the specified name in the default (US) region");
        amazonS3Client.createBucket(bucketName);
    }

    @Override
    public void deleteBucket() {
        LOG.info("Deletes the specified bucket " + bucketName);
        amazonS3Client.deleteBucket(bucketName);
    }

    @Override
    public void uploadEntity(String keyName, File file) {
        LOG.info("Uploads the specified file " + file.toString()
                + " to Amazon S3 under the specified bucket " + bucketName
                + " and key name " + keyName);
        amazonS3Client.putObject(bucketName, keyName, file);
    }

    @Override
    public void uploadEntity(String keyName, InputStream fileStream) {
        LOG.info("Uploads the specified input stream "
                + fileStream.toString()
                + " and object metadata to Amazon S3 under the specified bucket "
                + bucketName + " and key name " + keyName);
        amazonS3Client.putObject(bucketName, keyName, fileStream, null);
    }

    @Override
    public void deleteEntity(String keyName) {
        LOG.info("Deletes the specified object " + keyName
                + " in the specified bucket " + bucketName);
        amazonS3Client.deleteObject(bucketName, keyName);
    }

    @Override
    public void publicEntity(String keyName) {
        LOG.info("Sets the CannedAccessControlList for the specified object "
                + keyName
                + " in Amazon S3 using one of the pre-configured CannedAccessControlLists");
        amazonS3Client.setObjectAcl(bucketName, keyName, CannedAccessControlList.PublicRead);
    }

    @Override
    public boolean isPublicEntity(String keyName) {
        LOG.info("Gets the AccessControlList (ACL) for the specified object "
                + keyName + " in the specified bucket " + bucketName);
        AccessControlList accessControlList = amazonS3Client.getObjectAcl(bucketName, keyName);
        for (Iterator<Grant> iterator = accessControlList.getGrants().iterator(); iterator.hasNext();) {
            Grant grant = iterator.next();
            if (grant.getPermission().equals(Permission.Read)
                    && grant.getGrantee().getIdentifier().equals(GROUPS_USERS))
                return true;
        }
        return false;
    }

    @Override
    public boolean downloadEntity(String keyNotAvailable, File destinationFile) {
        LOG.info("Gets the object metadata for the object stored in Amazon S3 under the specified bucket "
                + bucketName
                + " and key "
                + keyNotAvailable
                + ", and saves the object contents to the specified file "
                + destinationFile.toString());
        try {
            amazonS3Client.getObject(new GetObjectRequest(bucketName, keyNotAvailable), destinationFile);
            return true;
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
        return false;
    }

    @Override
    public InputStream downloadEntity(String keyName) {
        LOG.info("Gets the object stored in Amazon S3 under the specified bucket "
                + bucketName + " and key " + keyName);
        S3Object s3Object = amazonS3Client.getObject(bucketName, keyName);
        return s3Object.getObjectContent();
    }

    @Override
    public String getResourceUrl(String keyName) {
        LOG.info("Returns the URL to the key in the bucket given " + bucketName + ", using the client's scheme and endpoint");
        if (isPublicEntity(keyName))
            return "http://" + regionEndpoint + File.separatorChar + bucketName
                    + File.separatorChar + keyName;

        return null;
    }

}
