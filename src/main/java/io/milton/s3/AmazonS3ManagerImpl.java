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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

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
     * minimize costs, or address regulator requirements.
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
        
		// Checks if the specified bucket exists or not and
		// if doesn't exist then creates a new Amazon S3 bucket
		// with the specified name in the default (US) region
        if (!isRootBucket()) {
        	createBucket();
        }
    }

    @Override
    public boolean isRootBucket() {
        LOG.info("Checks if the specified bucket " + bucketName + " exists");
        try {
        	return amazonS3Client.doesBucketExist(bucketName);
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
        return false;
    }

    @Override
    public Bucket createBucket() {
        LOG.info("Creates a new Amazon S3 bucket " + bucketName + " with the specified name in the default (US) region");
        try {
        	return amazonS3Client.createBucket(bucketName);
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
        return null;
    }

    @Override
    public boolean deleteBucket() {
    	LOG.info("Deletes the specified bucket " + bucketName);
        try {
        	// Make sure delete all the entities in the bucket
        	deleteEntities();
        	// Delete the specified bucket for the given name
        	amazonS3Client.deleteBucket(bucketName);
        	return true;
        } catch (AmazonServiceException ase) {
        	LOG.error(ase.getMessage(), ase);
		} catch (AmazonClientException ace) {
			LOG.error(ace.getMessage(), ace);
		}
        return false;
    }
    
    @Override
    public List<Bucket> findBuckets() {
    	try {
    		return amazonS3Client.listBuckets();
        } catch (AmazonServiceException ase) {
        	LOG.error(ase.getMessage(), ase);
		} catch (AmazonClientException ace) {
			LOG.error(ace.getMessage(), ace);
		}
        return Collections.emptyList();
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

        try {
        	amazonS3Client.putObject(bucketName, keyName, fileStream, null);
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
    }

    @Override
    public void deleteEntity(String keyName) {
        LOG.info("Deletes the specified object " + keyName
                + " in the specified bucket " + bucketName);
        try {
        	amazonS3Client.deleteObject(bucketName, keyName);
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
    }
    
    @Override
	public void deleteEntities() {
		LOG.info("Deletes multiple objects in a bucket " + bucketName + " from Amazon S3");
		List<S3ObjectSummary> s3ObjectSummaries = findEntityByBucket();
		if (s3ObjectSummaries == null || s3ObjectSummaries.isEmpty())
			return;
		
		// Provide a list of object keys and versions.
		List<KeyVersion> keyVersions = new ArrayList<KeyVersion>();
		for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaries) {
			keyVersions.add( new KeyVersion(s3ObjectSummary.getKey()));
		}

		DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
			.withKeys(keyVersions);
		try {
		    DeleteObjectsResult deleteObjectsResult = amazonS3Client.deleteObjects(deleteObjectsRequest);
		    LOG.info("Successfully deleted all the " + deleteObjectsResult.getDeletedObjects().size() + " items.\n");
		} catch (MultiObjectDeleteException mode) {
		    LOG.error("Could not delete all the entities in a bucket " + bucketName, mode);
		}
	}

    @Override
    public void publicEntity(String keyName) {
        LOG.info("Sets the CannedAccessControlList for the specified object "
                + keyName
                + " in Amazon S3 using one of the pre-configured CannedAccessControlLists");
        
        try {
        	amazonS3Client.setObjectAcl(bucketName, keyName, CannedAccessControlList.PublicRead);
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
    }
    
    @Override
    public void copyEntity(String keyName, String targetBucketName,
            String targetKeyName) {
        
        // If target bucket name is null or empty, that mean copy inside current
        // bucket.
        if (StringUtils.isEmpty(targetBucketName)) {
            targetBucketName = bucketName;
        }
        
        LOG.info("Copies a source object " + keyName
                + " to a new destination bucket " + targetBucketName
                + " with specified key " + targetKeyName + " in Amazon S3");
        
        try {
        	amazonS3Client.copyObject(bucketName, keyName, targetBucketName, targetKeyName);
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
    }

    @Override
    public boolean isPublicEntity(String keyName) {
        LOG.info("Gets the AccessControlList (ACL) for the specified object "
                + keyName + " in the specified bucket " + bucketName);
        
        try {
        	AccessControlList accessControlList = amazonS3Client.getObjectAcl(bucketName, keyName);
            for (Iterator<Grant> iterator = accessControlList.getGrants().iterator(); iterator.hasNext();) {
                Grant grant = iterator.next();
                if (grant.getPermission().equals(Permission.Read)
                        && grant.getGrantee().getIdentifier().equals(GROUPS_USERS))
                    return true;
            }
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
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
        try {
        	S3Object s3Object = amazonS3Client.getObject(bucketName, keyName);
            return s3Object.getObjectContent();
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
        return null;
    }

    @Override
    public String getResourceUrl(String keyName) {
        LOG.info("Returns the URL to the key in the bucket given " + bucketName + ", using the client's scheme and endpoint");
        if (isPublicEntity(keyName))
            return "http://" + regionEndpoint + File.separatorChar + bucketName
                    + File.separatorChar + keyName;

        return null;
    }
    
    @Override
	public S3Object findEntityByUniqueKey(String keyName) {
    	if (StringUtils.isEmpty(keyName)) {
    		return null;
    	}
    	
    	LOG.info("Gets the object stored in Amazon S3 under the specified bucket "
                + bucketName + " and key " + keyName);
    	try {
    		return amazonS3Client.getObject(bucketName, keyName);
        } catch (AmazonServiceException ase) {
            LOG.warn(ase.getMessage(), ase);
        } catch (AmazonClientException ace) {
            LOG.warn(ace.getMessage(), ace);
        }
    	return null;
	}
    
    @Override
	public List<S3ObjectSummary> findEntityByBucket() {
		LOG.info("Returns a list of summary information about the objects in the specified buckets "
				+ bucketName);
		
		List<S3ObjectSummary> objectSummaries = new ArrayList<S3ObjectSummary>();
		ObjectListing objectListing;
		try {
			do {
				objectListing = amazonS3Client.listObjects(bucketName);
				for (final S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
					objectSummaries.add(objectSummary);
				}
				objectListing = amazonS3Client.listNextBatchOfObjects(objectListing);
			} while (objectListing.isTruncated());
			
		} catch (AmazonServiceException ase) {
			LOG.error("Caught an AmazonServiceException, "
					+ "which means your request made it "
					+ "to Amazon S3, but was rejected with an error response "
					+ "for some reason.", ase);
		} catch (AmazonClientException ace) {
			LOG.error("Caught an AmazonClientException, "
					+ "which means the client encountered "
					+ "an internal error while trying to communicate"
					+ " with S3, "
					+ "such as not being able to access the network.", ace);
		}
		
		return objectSummaries;
	}

	@Override
	public List<S3ObjectSummary> findEntityByPrefixKey(String prefixKey) {
		LOG.info("Returns a list of summary information about the objects in the specified bucket "
				+ bucketName + " for the specified " + prefixKey);
		
		List<S3ObjectSummary> objectSummaries = new ArrayList<S3ObjectSummary>();
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
			.withBucketName(bucketName)
			.withPrefix(prefixKey);
		ObjectListing objectListing;
		
		try {
			do {
				objectListing = amazonS3Client.listObjects(listObjectsRequest);
				for (final S3ObjectSummary objectSummary: objectListing.getObjectSummaries()) {
					objectSummaries.add(objectSummary);
				}
				listObjectsRequest.setMarker(objectListing.getNextMarker());
			} while (objectListing.isTruncated());
		} catch (AmazonServiceException ase) {
			LOG.error("Caught an AmazonServiceException, "
					+ "which means your request made it "
					+ "to Amazon S3, but was rejected with an error response "
					+ "for some reason.", ase);
		} catch (AmazonClientException ace) {
			LOG.error("Caught an AmazonClientException, "
					+ "which means the client encountered "
					+ "an internal error while trying to communicate"
					+ " with S3, "
					+ "such as not being able to access the network.", ace);
		}
		
		return objectSummaries;
	}
	
}
