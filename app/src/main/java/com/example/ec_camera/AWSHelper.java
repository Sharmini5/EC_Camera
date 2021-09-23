package com.example.ec_camera;


import android.content.Context;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionResult;
import com.amazonaws.services.rekognition.model.DeleteCollectionRequest;
import com.amazonaws.services.rekognition.model.DeleteCollectionResult;
import com.amazonaws.services.rekognition.model.Face;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.ListCollectionsRequest;
import com.amazonaws.services.rekognition.model.ListCollectionsResult;
import com.amazonaws.services.rekognition.model.ListFacesRequest;
import com.amazonaws.services.rekognition.model.ListFacesResult;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;

public class AWSHelper {
    /*************************** S3 methods *************************************/
    public static CognitoCachingCredentialsProvider cogniocredentialsProvider(CognitoCachingCredentialsProvider credentialsProvider, Context context){
        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                Constants.POOLID, // Identity Pool ID
                Regions.valueOf(Constants.REGION) // Region
        );
        return credentialsProvider;
    }

    public static void setAmazonS3Client(CognitoCachingCredentialsProvider credentialsProvider, AmazonS3 s3Client){
        // Create an S3 client
        s3Client = new AmazonS3Client(credentialsProvider);
        // Set the region of your S3 bucket
        s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));
    }

    public static void setTransferUtility(TransferUtility transferUtility, Context context, AmazonS3 s3Client){
        transferUtility = new TransferUtility(s3Client, context);
    }

    public static TransferObserver setFileToUpload(TransferObserver transferObserver, TransferUtility transferUtility, String filename, File fileToUpload){
        transferObserver = transferUtility.upload(
                Constants.S3_BUCKET,     /* The bucket to upload to */
                filename,    /* The key for the uploaded object */
                fileToUpload       /* The file where the data to upload exists */
        );
        return transferObserver;
    }

    public static TransferObserver setFileToDownload(TransferObserver transferObserver, TransferUtility transferUtility, String filename, File fileToDownload){
        transferObserver = transferUtility.download(
                Constants.S3_BUCKET,     /* The bucket to download from */
                filename,    /* The key for the object to download */
                fileToDownload        /* The file to download the object to */
        );
        return transferObserver;
    }

    /***************** Start collection methods ********************/
    // Create Collection
    public static void createCollection(String COLLECTION_ID, AmazonRekognition amazonRekognitionClient){
        System.out.println("Creating collections: " + COLLECTION_ID);
        CreateCollectionResult createCollectionResult = callCreateCollection(COLLECTION_ID, amazonRekognitionClient);
        System.out.println("CollectionArn : " +
                createCollectionResult.getCollectionArn());
        System.out.println("Status code : " +
                createCollectionResult.getStatusCode().toString());
    }

    //List Collections
    public static void listCollections(AmazonRekognition amazonRekognitionClient){
        System.out.println("Listing collections");
        int limit = 1;
        ListCollectionsResult listCollectionsResult = null;
        String paginationToken = null;
        do {
            if (listCollectionsResult != null) {
                paginationToken = listCollectionsResult.getNextToken();
            }
            listCollectionsResult = callListCollections(paginationToken, limit,
                    amazonRekognitionClient);

            List< String > collectionIds = listCollectionsResult.getCollectionIds();
            for (String resultId: collectionIds) {
                System.out.println("listCollections :" + resultId);
            }
        } while (listCollectionsResult != null && listCollectionsResult.getNextToken() !=
                null);
    }

    //Delete Collection
    public static void deleteCollection(String COLLECTION_ID, AmazonRekognition amazonRekognitionClient){
        System.out.println("Deleting collections");
        DeleteCollectionResult deleteCollectionResult = callDeleteCollection(COLLECTION_ID, amazonRekognitionClient);
        System.out.println(COLLECTION_ID + ": " + deleteCollectionResult.getStatusCode().toString());
    }

    public static CreateCollectionResult callCreateCollection(String collectionId, AmazonRekognition amazonRekognition) {
        CreateCollectionRequest request = new CreateCollectionRequest()
                .withCollectionId(collectionId);
        return amazonRekognition.createCollection(request);
    }

    public static DeleteCollectionResult callDeleteCollection(String collectionId, AmazonRekognition amazonRekognition) {
        DeleteCollectionRequest request = new DeleteCollectionRequest()
                .withCollectionId(collectionId);
        return amazonRekognition.deleteCollection(request);
    }

    public static ListCollectionsResult callListCollections(String paginationToken, int limit, AmazonRekognition amazonRekognition) {
        ListCollectionsRequest listCollectionsRequest = new ListCollectionsRequest()
                .withMaxResults(limit)
                .withNextToken(paginationToken);
        return amazonRekognition.listCollections(listCollectionsRequest);
    }
    /***************** End collection methods ********************/


    /***************** Start face images methods ********************/
    public static void storeImages(String COLLECTION_ID, String S3_BUCKET, String fileName, String externalImageId, AmazonRekognition amazonRekognitionClient){
        // 1. Index face 1
        Image image = AWSHelper.getImageUtil(S3_BUCKET, fileName);
        IndexFacesResult indexFacesResult = AWSHelper.callIndexFaces(COLLECTION_ID, externalImageId, "ALL", image, amazonRekognitionClient);
        System.out.println(externalImageId + " added");
        List<FaceRecord> faceRecords = indexFacesResult.getFaceRecords();
        for (FaceRecord faceRecord: faceRecords) {
            System.out.println("Face detected: Faceid is " +
                    faceRecord.getFace().getFaceId());
        }
    }

    public static void listImages(String COLLECTION_ID, AmazonRekognition amazonRekognitionClient){
        Gson gson = new Gson();
        ListFacesResult listFacesResult = null;
        System.out.println("Faces in collection " + COLLECTION_ID);

        String paginationToken = null;
        do {
            if (listFacesResult != null) {
                paginationToken = listFacesResult.getNextToken();
            }
            listFacesResult = AWSHelper.callListFaces(COLLECTION_ID, 1, paginationToken, amazonRekognitionClient);
            List <Face> faces = listFacesResult.getFaces();
            for (Face face: faces) {
                System.out.println(gson.toJson(face));
            }
        } while (listFacesResult != null && listFacesResult.getNextToken() != null);
    }

    private static IndexFacesResult callIndexFaces(String collectionId, String externalImageId, String attributes, Image image, AmazonRekognition amazonRekognition) {
        IndexFacesRequest indexFacesRequest = new IndexFacesRequest()
                .withImage(image)
                .withCollectionId(collectionId)
                .withExternalImageId(externalImageId)
                .withDetectionAttributes(attributes);
        return amazonRekognition.indexFaces(indexFacesRequest);
    }

    private static ListFacesResult callListFaces(String collectionId, int limit, String paginationToken, AmazonRekognition amazonRekognition) {
        ListFacesRequest listFacesRequest = new ListFacesRequest()
                .withCollectionId(collectionId)
                .withMaxResults(limit)
                .withNextToken(paginationToken);
        return amazonRekognition.listFaces(listFacesRequest);
    }

    private static Image getImageUtil(String bucket, String key) {
        return new Image()
                .withS3Object(new S3Object()
                        .withBucket(bucket)
                        .withName(key));
    }
    /***************** End face images methods ********************/

    // image identification
    public static SearchFacesByImageResult callSearchFacesByImage(String collectionId, Image image, Float threshold, int maxFaces, AmazonRekognition amazonRekognition) {
        SearchFacesByImageRequest searchFacesByImageRequest = new SearchFacesByImageRequest()
                .withCollectionId(collectionId)
                .withImage(image)
                .withFaceMatchThreshold(threshold)
                .withMaxFaces(maxFaces);
        return amazonRekognition.searchFacesByImage(searchFacesByImageRequest);
    }

    public static void downloadHeadCountPdf(Context context) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(Constants.POOLID, Regions.valueOf(Constants.REGION), clientConfiguration);

        File file = new File(context.getFilesDir(), "MyDir");
        if (!file.exists()) {
            file.mkdir();
        }

        TransferUtility transferUtility = TransferUtility.builder().context(OpenCameraActivity.getMainActivity().getApplicationContext())
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .s3Client(new AmazonS3Client(credentialsProvider))
                .build();
        TransferObserver downloadObserver = transferUtility.download(Constants.BUCKET_NAME,"img/postuploads/preproduction_1553070264-abb290003809720a8bb5210d9310d1aa.png",
                file);

        downloadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    System.out.println("Download Comleted");
                } else {
                    System.out.println("Function Running");
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
            }
        });
    }

}