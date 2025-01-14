package com.salescode.channelkart.event.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.event.EventProfile;
import com.salescode.channelkart.models.Profile;
import com.salescode.channelkart.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SQS {

  private static final Logger log = LoggerFactory.getLogger(SQS.class);

  private final SqsEventProfile sqsProfile;
  Profile profile;
  SqsEventProvider eventProvider;
  private static Map<String,AmazonSQS> sqsMap = new HashMap<>();
  public SQS(Profile profile){
    this.profile=profile;
    this.sqsProfile = (SqsEventProfile) EventProfile.createFrom(this.profile);
  }


  private AmazonSQS getSQS(Profile profile){
    if(sqsMap.containsKey(profile.getName())){
      return sqsMap.get(profile.getName());
    }
    SqsEventProfile sqsEventProfile = (SqsEventProfile) EventProfile.createFrom(profile);
    AmazonSQS sqs= AmazonSQSClientBuilder.standard()
        .withRegion(sqsEventProfile.getRegion())
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
            sqsEventProfile.getAwsAccessKey(), sqsEventProfile.getAwsSecretKey())))
        .build();

    sqsMap.put(profile.getName(),sqs);
    return sqs;
  }

  public void send(Object message){
    send(JSONUtils.toJsonNode(message));
  }
  public void send(JsonNode message){
    CompletableFuture.runAsync(() -> {
      try {
        AmazonSQS sqs = getSQS(profile);
        String queueUrl = sqs.getQueueUrl(sqsProfile.getQueueName()).getQueueUrl();
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody(message.toString());
        SendMessageResult sendMessageResult = sqs.sendMessage(sendMessageRequest);
        log.debug("successfully send the message to sqs:{}", sendMessageResult);
      } catch (Exception e) {
        log.error("Could not send the message to sqs, message:{}", message, e);
      }
    });

  }

  public void sendWithPromise(Object message){
    CompletableFuture.runAsync(() -> {
      try {
        AmazonSQS sqs = getSQS(profile);
        String queueUrl = sqs.getQueueUrl(sqsProfile.getQueueName()).getQueueUrl();
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(message.toString());
        SendMessageResult sendMessageResult = sqs.sendMessage(sendMessageRequest);
        log.debug("successfully send the message to sqs:{}", sendMessageResult);
      } catch (Exception e) {
        log.error("Could not send the message to sqs, message:{}", message, e);
      }
    }).join();
  }

  public static void main(String[] args) {
    Profile p = new Profile();
    p.setType("sqs");
    ObjectNode attributes = JSONUtils.getObjectMapper().createObjectNode();
    attributes.put("queueName","channelkart-notifications");
    attributes.put("region","ap-south-1");
    attributes.put("awsAccessKey","AKIAQDZHVLDKGH44KL5H");
    attributes.put("awsSecretKey","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");
    attributes.put("providerType","SQS");
    p.setAttributes(attributes);
    SQS s = new SQS(p);
    ObjectNode test = JSONUtils.getObjectMapper().createObjectNode();
    test.put("subject","Order Failure");
    test.put("shortDescription","Failed to place Order Test from SQS");
    test.put("type","Order");
    test.put("lob","Test");
    ObjectNode user = JSONUtils.getObjectMapper().createObjectNode();
    user.put("mobileNumber","123456789");
    user.put("loginId","ABCD");
    user.put("outletCode","OU123");
    test.put("user",user);
    s.send(test);
    try {
      Thread.sleep(100000);
    } catch (InterruptedException e) {
      log.error("stacktrace", e);
    }

  }

}
