package com.codingshuttle.linkedInProject.ConnectionsService.service;

import com.codingshuttle.linkedInProject.ConnectionsService.entity.Person;
import com.codingshuttle.linkedInProject.ConnectionsService.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public void createPerson(Long userId, String name) {
        Person person = Person.builder().name(name).userId(userId).build();
        personRepository.save(person);
    }

    public void updatePersonName(Long userId, String name) {
        // A missing node is not an error worth failing the listener over - the
        // user_created event may simply not have landed yet.
        personRepository.findByUserId(userId).ifPresentOrElse(person -> {
            person.setName(name);
            personRepository.save(person);
        }, () -> log.warn("No Person node for userId {}, skipping name update", userId));
    }

}
