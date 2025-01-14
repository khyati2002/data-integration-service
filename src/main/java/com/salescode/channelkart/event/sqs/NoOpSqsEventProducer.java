package com.salescode.channelkart.event.sqs;

import com.salescode.channelkart.event.Event;
import com.salescode.channelkart.event.EventProducer;
import com.salescode.channelkart.exceptions.EventException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * @author : Jinu
 * Date    : 8/2/2020
 **/
@ConditionalOnMissingBean(SqsEventProducer.class)
public class NoOpSqsEventProducer implements EventProducer {

   @Override
   public <T> String send(Event<T> event) {
      throw new EventException("could not send event through sql please configure sqs configuration for enabling this sqs producer");
   }
}
