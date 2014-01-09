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

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public interface AmazonS3Manager {

    boolean isRootBucket();
    
    /**
     * Create and name a bucket that stores data. Buckets are the fundamental
     * container in Amazon S3 for data storage
     */
    void createBucket();
    
    /**
     * Delete a bucket that stored in Amazon S3 for the given bucket name
     * 
     */
    void deleteBucket();
    
    /**
     * Store an infinite amount of data in a bucket. Upload as many objects as
     * you like into an Amazon S3 bucket. Each object can contain up to 5 TB of
     * data. Each object is stored and retrieved using a unique
     * developer-assigned key.
     * 
     * @param keyName
     * @param file
     */
    void uploadEntity(String keyName, File file);
    
    void uploadEntity(String keyName, InputStream fileStream);

    void deleteEntity(String keyName);
    
    void publicEntity(String keyName);

    boolean isPublicEntity(String keyName);

    boolean downloadEntity(String keyNotAvailable, File destinationFile);

    InputStream downloadEntity(String keyName);

    String getResourceUrl(String keyName);
    
    S3Object findEntityByUniqueKey(String keyName);
    
    /**
	 * This method to list object keys in a bucket for the given prefix
	 * 
	 * @param prefixKey
	 * @return
	 */
    List<S3ObjectSummary> findEntityByPrefixKey(String prefixKey);
}
