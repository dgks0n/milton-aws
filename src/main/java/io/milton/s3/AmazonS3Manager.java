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
import java.util.List;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public interface AmazonS3Manager {

    /**
     * Checks if the specified bucket exists. Amazon S3 buckets are named in a
     * global namespace; use this method to determine if a specified bucket name
     * already exists, and therefore can't be used to create a new bucket.
     * 
     * @param bucketName
     *            - The name of the bucket to check
     * @return The value true if the specified bucket exists in Amazon S3; the
     *         value false if there is no bucket in Amazon S3 with that name
     */
    boolean isRootBucket(String bucketName);
    
    /**
     * Creates a new Amazon S3 bucket with the specified name in the default
     * (US) region, Region.US_Standard
     * 
     * Every object stored in Amazon S3 is contained within a bucket. Buckets
     * partition the namespace of objects stored in Amazon S3 at the top level.
     * Within a bucket, any name can be used for objects. However, bucket names
     * must be unique across all of Amazon S3.
     * 
     * @param bucketName
     *            - The name of the bucket to create. All buckets in Amazon S3
     *            share a single namespace; ensure the bucket is given a unique
     *            name
     * @return The newly created bucket            
     */
    Bucket createBucket(String bucketName);
    
    /**
     * Deletes the specified bucket. All objects (and all object versions, if
     * versioning was ever enabled) in the bucket must be deleted before the
     * bucket itself can be deleted
     * 
     * @param bucketName
     *            - The name of the bucket to delete
     */
    boolean deleteBucket(String bucketName);
    
    /**
     * Returns a list of all Amazon S3 buckets that the authenticated sender of
     * the request owns. Users must authenticate with a valid AWS Access Key ID
     * that is registered with Amazon S3. Anonymous requests cannot list
     * buckets, and users cannot list buckets that they did not create.
     * 
     * @return A list of all of the Amazon S3 buckets owned by the authenticated
     *         sender of the request
     */
    List<Bucket> findBuckets();
    
    /**
     * Store an infinite amount of data in a bucket. Upload as many objects as
     * you like into an Amazon S3 bucket. Each object can contain up to 5 TB of
     * data. Each object is stored and retrieved using a unique
     * developer-assigned key.
     * 
     * @param bucketName
     *            - The name of an existing bucket, to which you have
     *            Permission.Write permission
     * @param keyName
     *            - The key under which to store the specified file
     * @param file
     *            - The file containing the data to be uploaded to Amazon S3
     */
    boolean uploadEntity(String bucketName, String keyName, File file);
    
    /**
	 * Uploads the specified input stream and object metadata to Amazon S3 under
	 * the specified bucket and key name
	 * 
	 * @param bucketName
	 *            - The name of an existing bucket, to which you have
	 *            Permission.Write permission
	 * @param keyName
	 *            - The key under which to store the specified file
	 * @param inputStream
	 *            - The input stream containing the data to be uploaded to
	 *            Amazon S3
	 * @param metadata
	 *            - Additional metadata instructing Amazon S3 how to handle the
	 *            uploaded data (e.g. custom user metadata, hooks for specifying
	 *            content type, etc.).
	 * @return TRUE if successful, otherwise FASLE
	 */
    boolean uploadEntity(String bucketName, String keyName, InputStream inputStream, ObjectMetadata metadata);

    /**
     * Deletes the specified object in the specified bucket. Once deleted, the
     * object can only be restored if versioning was enabled when the object was
     * deleted. If attempting to delete an object that does not exist, Amazon S3
     * returns a success message instead of an error message.
     * 
     * @param bucketName
     *            - The name of the Amazon S3 bucket containing the object to
     *            delete
     * @param keyName
     *            - The key of the object to delete
     * @return FALSE if it doesn't exist or cann't delete, otherwise TRUE
     */
    boolean deleteEntity(String bucketName, String keyName);
    
    /**
     * Deletes multiple objects in a single bucket from S3
     * 
     * @param bucketName
     *              - The name of an existing bucket
     */
    boolean deleteEntities(String bucketName);
    
    boolean publicEntity(String bucketName, String keyName);
    
    /**
     * Copies a source object to a new destination in Amazon S3. You need to
     * provide the request information, such as source bucket name, source key
     * name, destination bucket name, and destination key.
     * 
     * @param sourceBucketName
     *            - The name of the bucket containing the source object to copy
     * @param sourceKeyName
     *            - The key in the source bucket under which the source object
     *            is stored
     * @param destinationBucketName
     *            - The name of the bucket in which the new object will be
     *            created. This can be the same name as the source bucket's
     * @param destinationKeyName
     *            - The key in the destination bucket under which the new object
     *            will be created
     */
    boolean copyEntity(String sourceBucketName, String sourceKeyName, String destinationBucketName, 
            String destinationKeyName);

    boolean isPublicEntity(String bucketName, String keyName);

    boolean downloadEntity(String bucketName, String keyNotAvailable, File destinationFile);

    /**
     * Gets the object stored in Amazon S3 under the specified bucket and key.
     * 
     * Be extremely careful when using this method; the returned Amazon S3
     * object contains a direct stream of data from the HTTP connection. The
     * underlying HTTP connection cannot be closed until the user finishes
     * reading the data and closes the stream. Therefore:
     * 
     *          - Use the data from the input stream in Amazon S3 object as soon as possible
     *          - Close the input stream in Amazon S3 object as soon as possible
     * 
     * @param bucketName
     *              - The name of the bucket containing the desired object
     * @param keyName
     *              - The key under which the desired object is stored
     * @return The object stored in Amazon S3 in the specified bucket and key
     */
    InputStream downloadEntity(String bucketName, String keyName);
    
    S3Object findEntityByUniqueKey(String bucketName, String keyName);
    
    /**
	 * Returns a list of summary information about the objects in the specified
	 * buckets.
	 * 
	 * @param bucketName
     *              - The name of an existing bucket
     *              
	 * @return a list of S3 objects
	 */
    List<S3ObjectSummary> findEntityByBucket(String bucketName);
    
    /**
	 * This method to list object keys in a bucket for the given prefix
	 * 
	 * @param bucketName
     *              - The name of an existing bucket
	 * @param prefixKey
	 * 
	 * @return a list of S3 objects
	 */
    List<S3ObjectSummary> findEntityByPrefixKey(String bucketName, String prefixKey);
}
