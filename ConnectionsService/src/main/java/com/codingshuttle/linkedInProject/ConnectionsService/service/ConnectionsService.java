package com.codingshuttle.linkedInProject.ConnectionsService.service;

import com.codingshuttle.linkedInProject.ConnectionsService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.ConnectionsService.dto.ConnectionStatusDto;
import com.codingshuttle.linkedInProject.ConnectionsService.dto.ConnectionSuggestionDto;
import com.codingshuttle.linkedInProject.ConnectionsService.entity.Person;
import com.codingshuttle.linkedInProject.ConnectionsService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.ConnectionsService.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsService {

    private final PersonRepository personRepository;

    public List<Person> getFirstDegreeConnectionsOfUser(Long userId) {
        log.info("Getting first degree connections of user with ID: {}", userId);

        return personRepository.getFirstDegreeConnections(userId);
    }

    public void follow(Long followeeId) {
        Long followerId = AuthContextHolder.getCurrentUserId();

        if(followerId.equals(followeeId)) {
            throw new BadRequestException("You cannot follow yourself");
        }
        log.info("User with ID: {} following user with ID: {}", followerId, followeeId);
        personRepository.follow(followerId, followeeId);
    }

    public void unfollow(Long followeeId) {
        Long followerId = AuthContextHolder.getCurrentUserId();
        personRepository.unfollow(followerId, followeeId);
    }

    public List<Person> getFollowing(Long userId) {
        return personRepository.getFollowing(userId);
    }

    public List<Person> getFollowers(Long userId) {
        return personRepository.getFollowers(userId);
    }

    /** The user ids the current user follows - used by the feed fan-out. */
    public List<Long> getFollowingIds() {
        Long userId = AuthContextHolder.getCurrentUserId();
        return personRepository.getFollowing(userId).stream().map(Person::getUserId).toList();
    }

    public List<ConnectionSuggestionDto> getConnectionSuggestions(long limit) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Getting connection suggestions for user with ID: {}", userId);

        // Clamp here rather than trusting the query string - it lands in a
        // Cypher LIMIT, and an unbounded one would happily scan the graph.
        long safeLimit = Math.min(Math.max(limit, 1), 50);

        return personRepository.getConnectionSuggestions(userId, safeLimit);
    }

    public List<Person> getPendingRequests() {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Getting pending connection requests for user with ID: {}", userId);

        return personRepository.getPendingConnectionRequests(userId);
    }

    public void sendConnectionRequest(Long receiverId) {
        Long senderId = AuthContextHolder.getCurrentUserId();
        log.info("sending connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean alreadySentRequest = personRepository.connectionRequestExists(senderId, receiverId);
        if (alreadySentRequest) {
            throw new BadRequestException("Connection request already exists, cannot send again");
        }

        boolean alreadyConnected = personRepository.alreadyConnected(senderId, receiverId);
        if (alreadyConnected) {
            throw new BadRequestException("Already connected users, cannot add connection request");
        }

        personRepository.addConnectionRequest(senderId, receiverId);
        log.info("Successfully sent the connection request");
    }

    public void acceptConnectionRequest(Long senderId) {
        Long receiverId = AuthContextHolder.getCurrentUserId();
        log.info("Accepting a connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean alreadyConnected = personRepository.alreadyConnected(senderId, receiverId);
        if (alreadyConnected) {
            throw new BadRequestException("Already connected users, cannot accept connection request again");
        }

        boolean alreadySentRequest = personRepository.connectionRequestExists(senderId, receiverId);
        if (!alreadySentRequest) {
            throw new BadRequestException("No Connection request exists, cannot accept without request");
        }

        personRepository.acceptConnectionRequest(senderId, receiverId);

        log.info("Successfully accepted the connection request with senderId: {}, receiverId: {}", senderId,
                receiverId);

    }

    public ConnectionStatusDto getConnectionStatus(Long otherUserId) {
        Long userId = AuthContextHolder.getCurrentUserId();

        if (userId.equals(otherUserId)) {
            return new ConnectionStatusDto(ConnectionStatusDto.Status.SELF);
        }
        if (personRepository.alreadyConnected(userId, otherUserId)) {
            return new ConnectionStatusDto(ConnectionStatusDto.Status.CONNECTED);
        }
        if (personRepository.connectionRequestExists(userId, otherUserId)) {
            return new ConnectionStatusDto(ConnectionStatusDto.Status.PENDING_OUTGOING);
        }
        if (personRepository.connectionRequestExists(otherUserId, userId)) {
            return new ConnectionStatusDto(ConnectionStatusDto.Status.PENDING_INCOMING);
        }
        return new ConnectionStatusDto(ConnectionStatusDto.Status.NOT_CONNECTED);
    }

    public void removeConnection(Long otherUserId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Removing connection between userId: {} and otherUserId: {}", userId, otherUserId);

        if (userId.equals(otherUserId)) {
            throw new BadRequestException("Cannot remove a connection with yourself");
        }

        boolean alreadyConnected = personRepository.alreadyConnected(userId, otherUserId);
        if (!alreadyConnected) {
            throw new BadRequestException("You are not connected to this user");
        }

        personRepository.removeConnection(userId, otherUserId);
        log.info("Successfully removed the connection");
    }

    /**
     * Withdrawing your own outgoing request is the same edge deletion as
     * rejecting an incoming one - only the direction of who is acting differs.
     */
    public void withdrawConnectionRequest(Long receiverId) {
        Long senderId = AuthContextHolder.getCurrentUserId();
        log.info("Withdrawing connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean requestExists = personRepository.connectionRequestExists(senderId, receiverId);
        if (!requestExists) {
            throw new BadRequestException("No connection request exists, cannot withdraw it");
        }

        personRepository.rejectConnectionRequest(senderId, receiverId);
        log.info("Successfully withdrew the connection request");
    }

    public void rejectConnectionRequest(Long senderId) {
        Long receiverId = AuthContextHolder.getCurrentUserId();
        log.info("Rejecting a connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean alreadySentRequest = personRepository.connectionRequestExists(senderId, receiverId);
        if (!alreadySentRequest) {
            throw new BadRequestException("No Connection request exists, cannot reject it");
        }

        personRepository.rejectConnectionRequest(senderId, receiverId);

        log.info("Successfully rejected the connection request with senderId: {}, receiverId: {}", senderId,
                receiverId);
    }
}
