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

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;

/**
 * This sample demonstrates how to make basic requests to Amazon SimpleDB using
 * the AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
 * account, and be signed up to use Amazon SimpleDB. For more information on
 * Amazon SimpleDB, see http://aws.amazon.com/simpledb.
 * <p>
 * <b>Important:</b> Be sure to fill in your AWS access credentials in the
 *                   AwsCredentials.properties file before you try to run this
 *                   sample.
 * http://aws.amazon.com/security-credentials
 */
public class AmazonSimpleDBStore {

    public static void main(String[] args) throws Exception {
        /*
         * Important: Be sure to fill in your AWS access credentials in the
         *            AwsCredentials.properties file before you try to run this
         *            sample.
         * http://aws.amazon.com/security-credentials
         */
    	AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        AmazonSimpleDB sdb = new AmazonSimpleDBClient(credentialsProvider);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        sdb.setRegion(usWest2);

        System.out.println("===========================================");
        System.out.println("Getting Started with Amazon SimpleDB");
        System.out.println("===========================================\n");

        try {
            // Create a domain
            String myDomain = "MyStore";
            System.out.println("Creating domain called " + myDomain + ".\n");
            sdb.createDomain(new CreateDomainRequest(myDomain));

            // List domains
            System.out.println("Listing all domains in your account:\n");
            for (String domainName : sdb.listDomains().getDomainNames()) {
                System.out.println("  " + domainName);
            }
            System.out.println();

            // Put data into a domain
            System.out.println("Putting data into " + myDomain + " domain.\n");
            sdb.batchPutAttributes(new BatchPutAttributesRequest(myDomain, createSampleData()));

            // Select data from a domain
            // Notice the use of backticks around the domain name in our select expression.
            String selectExpression = "select * from `" + myDomain + "` where Category = 'Clothes'";
            System.out.println("Selecting: " + selectExpression + "\n");
            SelectRequest selectRequest = new SelectRequest(selectExpression);
            for (Item item : sdb.select(selectRequest).getItems()) {
                System.out.println("  Item");
                System.out.println("    Name: " + item.getName());
                for (Attribute attribute : item.getAttributes()) {
                    System.out.println("      Attribute");
                    System.out.println("        Name:  " + attribute.getName());
                    System.out.println("        Value: " + attribute.getValue());
                }
            }
            System.out.println();

            // Delete values from an attribute
            System.out.println("Deleting Blue attributes in Item_O3.\n");
            Attribute deleteValueAttribute = new Attribute().withName("Color").withValue("Blue");
            sdb.deleteAttributes(new DeleteAttributesRequest(myDomain, "Item_03")
                    .withAttributes(deleteValueAttribute));

            // Delete an attribute and all of its values
            System.out.println("Deleting attribute Year in Item_O3.\n");
            sdb.deleteAttributes(new DeleteAttributesRequest(myDomain, "Item_03")
                    .withAttributes(new Attribute().withName("Year")));

            // Replace an attribute
            System.out.println("Replacing Size of Item_03 with Medium.\n");
            ReplaceableAttribute replaceableAttribute = new ReplaceableAttribute()
                    .withName("Size")
                    .withValue("Medium")
                    .withReplace(true);
            sdb.putAttributes(new PutAttributesRequest()
                    .withDomainName(myDomain)
                    .withItemName("Item_03")
                    .withAttributes(replaceableAttribute));

            // Delete an item and all of its attributes
            System.out.println("Deleting Item_03.\n");
            sdb.deleteAttributes(new DeleteAttributesRequest(myDomain, "Item_03"));

            // Delete a domain
            System.out.println("Deleting " + myDomain + " domain.\n");
            sdb.deleteDomain(new DeleteDomainRequest(myDomain));
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon SimpleDB, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with SimpleDB, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    /**
     * Creates an array of SimpleDB ReplaceableItems populated with sample data.
     *
     * @return An array of sample item data.
     */
    private static List<ReplaceableItem> createSampleData() {
        List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();

        sampleData.add(new ReplaceableItem().withName("Item_01").withAttributes(
                new ReplaceableAttribute().withName("Category").withValue("Clothes"),
                new ReplaceableAttribute().withName("Subcategory").withValue("Sweater"),
                new ReplaceableAttribute().withName("Name").withValue("Cathair Sweater"),
                new ReplaceableAttribute().withName("Color").withValue("Siamese"),
                new ReplaceableAttribute().withName("Size").withValue("Small"),
                new ReplaceableAttribute().withName("Size").withValue("Medium"),
                new ReplaceableAttribute().withName("Size").withValue("Large")));

        sampleData.add(new ReplaceableItem().withName("Item_02").withAttributes(
                new ReplaceableAttribute().withName("Category").withValue("Clothes"),
                new ReplaceableAttribute().withName("Subcategory").withValue("Pants"),
                new ReplaceableAttribute().withName("Name").withValue("Designer Jeans"),
                new ReplaceableAttribute().withName("Color").withValue("Paisley Acid Wash"),
                new ReplaceableAttribute().withName("Size").withValue("30x32"),
                new ReplaceableAttribute().withName("Size").withValue("32x32"),
                new ReplaceableAttribute().withName("Size").withValue("32x34")));

        sampleData.add(new ReplaceableItem().withName("Item_03").withAttributes(
                new ReplaceableAttribute().withName("Category").withValue("Clothes"),
                new ReplaceableAttribute().withName("Subcategory").withValue("Pants"),
                new ReplaceableAttribute().withName("Name").withValue("Sweatpants"),
                new ReplaceableAttribute().withName("Color").withValue("Blue"),
                new ReplaceableAttribute().withName("Color").withValue("Yellow"),
                new ReplaceableAttribute().withName("Color").withValue("Pink"),
                new ReplaceableAttribute().withName("Size").withValue("Large"),
                new ReplaceableAttribute().withName("Year").withValue("2006"),
                new ReplaceableAttribute().withName("Year").withValue("2007")));

        sampleData.add(new ReplaceableItem().withName("Item_04").withAttributes(
                new ReplaceableAttribute().withName("Category").withValue("Car Parts"),
                new ReplaceableAttribute().withName("Subcategory").withValue("Engine"),
                new ReplaceableAttribute().withName("Name").withValue("Turbos"),
                new ReplaceableAttribute().withName("Make").withValue("Audi"),
                new ReplaceableAttribute().withName("Model").withValue("S4"),
                new ReplaceableAttribute().withName("Year").withValue("2000"),
                new ReplaceableAttribute().withName("Year").withValue("2001"),
                new ReplaceableAttribute().withName("Year").withValue("2002")));

        sampleData.add(new ReplaceableItem().withName("Item_05").withAttributes(
                new ReplaceableAttribute().withName("Category").withValue("Car Parts"),
                new ReplaceableAttribute().withName("Subcategory").withValue("Emissions"),
                new ReplaceableAttribute().withName("Name").withValue("O2 Sensor"),
                new ReplaceableAttribute().withName("Make").withValue("Audi"),
                new ReplaceableAttribute().withName("Model").withValue("S4"),
                new ReplaceableAttribute().withName("Year").withValue("2000"),
                new ReplaceableAttribute().withName("Year").withValue("2001"),
                new ReplaceableAttribute().withName("Year").withValue("2002")));

        return sampleData;
    }
}
