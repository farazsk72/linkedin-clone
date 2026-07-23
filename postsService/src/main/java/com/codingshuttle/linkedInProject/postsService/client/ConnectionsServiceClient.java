package com.codingshuttle.linkedInProject.postsService.client;

import com.codingshuttle.linkedInProject.postsService.dto.PersonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "connections-service", path = "/connections", url = "${CONNECTIONS_SERVICE_URI:}")
public interface ConnectionsServiceClient {

    @GetMapping("/core/{userId}/first-degree")
    List<PersonDto> getFirstDegreeConnections(@PathVariable Long userId);

    /**
     * The user ids the caller follows. The X-User-Id header is forwarded by the
     * Feign interceptor, so this resolves to whoever is making the feed request.
     */
    @GetMapping("/core/following-ids")
    List<Long> getFollowingIds();

    /**
     * The caller's relationship to {@code userId}. Used for the CONNECTIONS-only
     * visibility check, which asks "is the viewer connected to the owner" rather
     * than pulling the owner's whole connection list - so the list endpoint can
     * stay locked to self.
     */
    @GetMapping("/core/status/{userId}")
    com.codingshuttle.linkedInProject.postsService.dto.ConnectionStatusDto getConnectionStatus(
            @PathVariable Long userId);
}
