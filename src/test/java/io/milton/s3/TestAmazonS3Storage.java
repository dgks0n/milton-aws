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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class TestAmazonS3Storage {

    @Test
    public void testAmazonS3Storage() throws IOException {
        /*
         * This credentials provider implementation loads your AWS credentials
         * from a properties file at the root of your classpath.
         * 
         * Important: Be sure to fill in your AWS access credentials in the
         * AwsCredentials.properties file before you try to run this sample.
         * http://aws.amazon.com/security-credentials
         */
        AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        AmazonS3 amazonS3 = new AmazonS3Client(credentialsProvider);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        amazonS3.setRegion(usWest2);
    
        String bucketName = "my-first-s3-bucket-" + UUID.randomUUID();
        String key = "MyObjectKey";
    
        try {
            /*
             * Create a new S3 bucket - Amazon S3 bucket names are globally
             * unique, so once a bucket name has been taken by any user, you
             * can't create another bucket with that same name.
             * 
             * You can optionally specify a location for your bucket if you want
             * to keep your data closer to your applications or users.
             */
            assertNotNull(amazonS3.createBucket(bucketName));
    
            /*
             * List the buckets in your account
             */
            for (Bucket bucket : amazonS3.listBuckets()) {
                assertNotNull(bucket.getName());
            }
    
            /*
             * Upload an object to your bucket - You can easily upload a file to
             * S3, or upload directly an InputStream if you know the length of
             * the data in the stream. You can also specify your own metadata
             * when uploading to S3, which allows you set a variety of options
             * like content-type and content-encoding, plus additional metadata
             * specific to your applications.
             */
            assertNotNull(amazonS3.putObject(new PutObjectRequest(bucketName, key, createSampleFile())));
    
            /*
             * Download an object - When you download an object, you get all of
             * the object's metadata and a stream from which to read the
             * contents. It's important to read the contents of the stream as
             * quickly as possibly since the data is streamed directly from
             * Amazon S3 and your network connection will remain open until you
             * read all the data or close the input stream.
             * 
             * GetObjectRequest also supports several other options, including
             * conditional downloading of objects based on modification times,
             * ETags, and selectively downloading a range of an object.
             */
            S3Object object = amazonS3.getObject(new GetObjectRequest(bucketName, key));
            
            assertNotNull(object);
            assertNotNull(object.getObjectMetadata().getContentType());
            displayTextInputStream(object.getObjectContent());
    
            /*
             * List objects in your bucket by prefix - There are many options
             * for listing the objects in your bucket. Keep in mind that buckets
             * with many objects might truncate their results when listing their
             * objects, so be sure to check if the returned object listing is
             * truncated, and use the AmazonS3.listNextBatchOfObjects(...)
             * operation to retrieve additional results.
             */
            ObjectListing objectListing = amazonS3.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix("My"));
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                assertNotNull(objectSummary.getKey());
                assertNotNull(objectSummary.getSize());
            }
    
            /*
             * Delete an object - Unless versioning has been turned on for your
             * bucket, there is no way to undelete an object, so use caution
             * when deleting objects.
             */
            amazonS3.deleteObject(bucketName, key);
    
            /*
             * Delete a bucket - A bucket must be completely empty before it can
             * be deleted, so remember to delete any objects from your buckets
             * before you try to delete them.
             */
            amazonS3.deleteBucket(bucketName);
        } catch (AmazonServiceException ase) {
            fail("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
        } catch (AmazonClientException ace) {
            fail("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
        }
    }

    /**
     * Creates a temporary file with text data to demonstrate uploading a file
     * to Amazon S3
     * 
     * @return A newly created temporary file with text data.
     * 
     * @throws IOException
     */
    private File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("01234567890112345678901234\n");
        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
        writer.write("01234567890112345678901234\n");
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.close();

        return file;
    }

    /**
     * Displays the contents of the specified input stream as text.
     * 
     * @param input
     *            The input stream to display as text.
     * 
     * @throws IOException
     */
    private void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;

            System.out.println("    " + line);
        }
        System.out.println();
    }
}
