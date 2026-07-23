package com.codingshuttle.linkedInProject.ConnectionsService.repository;

import com.codingshuttle.linkedInProject.ConnectionsService.dto.ConnectionSuggestionDto;
import com.codingshuttle.linkedInProject.ConnectionsService.entity.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends Neo4jRepository<Person, Long> {

    Optional<Person> findByUserId(Long userId);

    @Query("match (personA:Person) -[:CONNECTED_TO]- (personB:Person) " +
            "where personA.userId = $userId " +
            "return personB")
    List<Person> getFirstDegreeConnections(Long userId);

    @Query("MATCH (p1:Person)-[r:REQUESTED_TO]->(p2:Person) " +
            "WHERE p1.userId = $senderId AND p2.userId = $receiverId " +
            "RETURN count(r) > 0")
    boolean connectionRequestExists(Long senderId, Long receiverId);

    @Query("MATCH (sender:Person)-[:REQUESTED_TO]->(receiver:Person) " +
            "WHERE receiver.userId = $userId " +
            "RETURN sender")
    List<Person> getPendingConnectionRequests(Long userId);

    @Query("MATCH (p1:Person)-[r:CONNECTED_TO]-(p2:Person) " +
            "WHERE p1.userId = $senderId AND p2.userId = $receiverId " +
            "RETURN count(r) > 0")
    boolean alreadyConnected(Long senderId, Long receiverId);

    @Query("MATCH (p1:Person), (p2:Person) " +
            "WHERE p1.userId = $senderId AND p2.userId = $receiverId " +
            "CREATE (p1)-[:REQUESTED_TO]->(p2)")
    void addConnectionRequest(Long senderId, Long receiverId);

    @Query("MATCH (p1:Person)-[r:REQUESTED_TO]->(p2:Person) " +
            "WHERE p1.userId = $senderId AND p2.userId = $receiverId " +
            "DELETE r " +
            "CREATE (p1)-[:CONNECTED_TO]->(p2)")
    void acceptConnectionRequest(Long senderId, Long receiverId);

    @Query("MATCH (p1:Person)-[r:REQUESTED_TO]->(p2:Person) " +
            "WHERE p1.userId = $senderId AND p2.userId = $receiverId " +
            "DELETE r")
    void rejectConnectionRequest(Long senderId, Long receiverId);

    // Undirected: CONNECTED_TO is stored one way but means a mutual link, so
    // either endpoint must be able to remove it.
    @Query("MATCH (p1:Person)-[r:CONNECTED_TO]-(p2:Person) " +
            "WHERE p1.userId = $userId AND p2.userId = $otherUserId " +
            "DELETE r")
    void removeConnection(Long userId, Long otherUserId);

    // FOLLOWS is directed, unlike CONNECTED_TO - following is one-way and needs
    // no acceptance. MERGE keeps it idempotent, so following twice is a no-op.
    @Query("MATCH (a:Person), (b:Person) " +
            "WHERE a.userId = $followerId AND b.userId = $followeeId " +
            "MERGE (a)-[:FOLLOWS]->(b)")
    void follow(Long followerId, Long followeeId);

    @Query("MATCH (a:Person)-[r:FOLLOWS]->(b:Person) " +
            "WHERE a.userId = $followerId AND b.userId = $followeeId " +
            "DELETE r")
    void unfollow(Long followerId, Long followeeId);

    @Query("MATCH (a:Person)-[:FOLLOWS]->(b:Person) " +
            "WHERE a.userId = $followerId AND b.userId = $followeeId " +
            "RETURN count(*) > 0")
    boolean isFollowing(Long followerId, Long followeeId);

    @Query("MATCH (me:Person)-[:FOLLOWS]->(followee:Person) " +
            "WHERE me.userId = $userId RETURN followee")
    List<Person> getFollowing(Long userId);

    @Query("MATCH (follower:Person)-[:FOLLOWS]->(me:Person) " +
            "WHERE me.userId = $userId RETURN follower")
    List<Person> getFollowers(Long userId);

    /**
     * People two hops away, ranked by how many connections you share. The
     * relationship patterns are undirected on purpose: CONNECTED_TO is written
     * one way but means a mutual connection, and a pending REQUESTED_TO in
     * either direction should keep someone out of the suggestions.
     */
    @Query("MATCH (me:Person)-[:CONNECTED_TO]-(mutual:Person)-[:CONNECTED_TO]-(candidate:Person) " +
            "WHERE me.userId = $userId " +
            "AND candidate.userId <> $userId " +
            "AND NOT (me)-[:CONNECTED_TO]-(candidate) " +
            "AND NOT (me)-[:REQUESTED_TO]-(candidate) " +
            "RETURN candidate.userId AS userId, candidate.name AS name, " +
            "count(DISTINCT mutual) AS mutualConnections " +
            "ORDER BY mutualConnections DESC, name ASC " +
            "LIMIT $limit")
    List<ConnectionSuggestionDto> getConnectionSuggestions(Long userId, Long limit);
}
