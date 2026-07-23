package com.codingshuttle.linkedInProject.ConnectionsService.consumer;

import com.codingshuttle.linkedInProject.ConnectionsService.service.PersonService;
import com.codingshuttle.linkedInProject.userService.event.UserCreatedEvent;
import com.codingshuttle.linkedInProject.userService.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceConsumer {

    private final PersonService personService;

    @KafkaListener(topics = "user_created_topic")
    public void handlePersonCreated(UserCreatedEvent userCreatedEvent) {
        log.info("handlePersonCreated: {}", userCreatedEvent);
        personService.createPerson(userCreatedEvent.getUserId(), userCreatedEvent.getName());
    }

    @KafkaListener(topics = "user_updated_topic")
    public void handlePersonUpdated(UserUpdatedEvent userUpdatedEvent) {
        log.info("handlePersonUpdated: {}", userUpdatedEvent);
        personService.updatePersonName(userUpdatedEvent.getUserId(), userUpdatedEvent.getName());
    }

}
