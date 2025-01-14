package com.salescode.channelkart.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EventLogger {

    private static final Logger log = LoggerFactory.getLogger(EventLogger.class);

    private final TaskService taskService;

    private final PropertyRegistry propertyRegistry;

    public EventLogger(TaskService taskService, PropertyRegistry propertyRegistry) {
        this.taskService = taskService;
        this.propertyRegistry = propertyRegistry;
    }


    public <T> Task logEventTask(Event<T> event, EventProviderType eventProviderType) {
        Task task = createNewTask(event);
        task.setStatus(Task.TaskStatus.INPROGRESS);
        ObjectNode attributes = getAttributesAsObjectNode(task);
        attributes.put("eventPublishedAt", Instant.now().toString());
        attributes.put("provider", eventProviderType.name());
        if (event instanceof LoggableEvent) {
            setAdditionalLoggingAttributes((LoggableEvent) event, attributes);
        }
        task.setAttributes(attributes);
        Task savedEvent = SecurityContextUtils.switchWithLOB(event.getLob(), () -> saveEventTask(task), true);
        if (event instanceof MutableEvent) {
            // Need to improvise this code... System should allow any events
            // if it's not MutableEvent... wrap it with MutableEvent
            ((MutableEvent) event).setId(savedEvent.getId());
        } else {
            log.error("The event class:{} is not MutableEvent type", event.getClass());
            throw new IllegalStateException("Given event is not MutableEvent");
        }
        return savedEvent;
    }

    public void updateEventTask(Task task) {
        taskService.save(task);
    }

    private void setAdditionalLoggingAttributes(LoggableEvent event, ObjectNode attributes) {
        event.getLoggingAttributes().forEach(attributes::put);
    }

    private ObjectNode getAttributesAsObjectNode(Task task) {
        JsonNode attributes = task.getAttributes();
        return attributes == null ? JSONUtils.getObjectMapper().createObjectNode() : attributes.deepCopy();
    }

    public <T> void logEventFailed(Event<T> event, String reason) {
        Task task = findOrCreateTask(event);
        task.setStatus(Task.TaskStatus.FAILURE);
        ObjectNode response = JSONUtils.getObjectMapper().createObjectNode();
        response.put("eventFailedTime", Instant.now().toString());
        response.put("eventFailureReason", reason);
        task.setTaskResponse(response);
        saveEventTask(task);
    }

    public void logEventSuccess(String eventId){
        taskService.deleteById(eventId);
    }

    private Task saveEventTask(Task task) {
        if (propertyRegistry.getAsBoolean(PropertyDefinition.DISABLE_EVENT_LOGS)) {
            return task;
        }
        return taskService.save(task);
    }


    private <T> Task findOrCreateTask(Event<T> event) {
        Task task = taskService.findById(event.getId());
        if (task == null) {
            task = createNewTask(event);
        }
        return task;
    }

    private <T> Task createNewTask(Event<T> event) {
        Task task = new Task();
        task.setType("Event");
        task.setAttributes(getDefaultAttributes(event));
        task.setResponse(new Task.TaskResponse());
        task.setTaskResponse(JSONUtils.getObjectMapper().createObjectNode());
        task.setId(event.getId());
        task.setCreatedBy(event.getUserName());
        task.setModifiedBy(event.getUserName());
        task.setLob(event.getLob());
        return task;
    }

    private <T> JsonNode getDefaultAttributes(Event<T> event) {
        return new JsonNodeBuilder()
                .with("lob", event.getLob())
                .with("topic", event.getTopic().value())
                .with("userName", event.getUserName())
                .build();
    }

    public EventResponse getEventResponse(Event<?> event) {
        Task task = taskService.findById(event.getId());
        JsonNode response = task.getTaskResponse();
        return JSONUtils.getObjectMapper().convertValue(response, EventResponse.class);
    }
}
