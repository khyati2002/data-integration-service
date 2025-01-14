package com.salescode.channelkart.event.embedded;

import com.salescode.channelkart.event.EventProducer;
import com.salescode.channelkart.event.EventProfile;
import com.salescode.channelkart.event.provider.EmbeddedEventProfile;
import com.salescode.channelkart.event.provider.EventProvider;
import com.salescode.channelkart.logging.EventLogger;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.utils.StringUtils;
import org.hibernate.event.spi.EventEngine;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : Jinu
 * Date    : 8/29/2020
 **/
public class EmbeddedEventProvider implements EventProvider {

   private final EmbeddedEventProfile profile;

   private final ExecutorService executorService;

   private final EmbeddedEventProducer eventProducer;

   public EmbeddedEventProvider(EmbeddedEventProfile profile) {
      this.profile = profile;
      this.executorService = embeddedEventExecutorService();
      this.eventProducer = createEventProducer();
   }

   private EmbeddedEventProducer createEventProducer() {
      var eventEngine = SpringContext.getBean(EventEngine.class);
      var eventLogger = SpringContext.getBean(EventLogger.class);
      return new EmbeddedEventProducer(this.profile, this.executorService, eventEngine, eventLogger);
   }

   public ExecutorService embeddedEventExecutorService() {
      int defaultQueueSize = 10;
      String queueSize = System.getProperty("EMBEDDED_EVENT_QUEUE_SIZE");
      if (StringUtils.isNotEmpty(queueSize)) {
         defaultQueueSize = Integer.parseInt(queueSize);
      }
      var threadPoolService = new ThreadPoolExecutor(2, profile.getMaximumThreads(), 5, TimeUnit.SECONDS,
              new LinkedBlockingQueue<>(defaultQueueSize), new CustomizableThreadFactory("embedded-event-"), new ThreadPoolExecutor.CallerRunsPolicy());
      return new DelegatingSecurityContextExecutorService(threadPoolService);
   }

   @Override
   public void shutDown() {
      if (this.executorService != null) {
         executorService.shutdown();
      }
   }

   @Override
   public EventProducer getProducer() {
      return this.eventProducer;
   }

   @Override
   public EventProfile getProfile() {
      return this.profile;
   }

   @Override
   public boolean isSameProfile(EventProfile profile) {
      return this.profile.equals(profile);
   }
}
