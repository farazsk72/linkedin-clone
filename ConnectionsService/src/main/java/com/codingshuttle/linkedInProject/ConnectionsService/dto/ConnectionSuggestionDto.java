package com.codingshuttle.linkedInProject.ConnectionsService.dto;

import lombok.Data;

/**
 * Not a Person node - the suggestion query returns projected columns, so the
 * property names here have to match the aliases in the Cypher RETURN clause.
 */
@Data
public class ConnectionSuggestionDto {
    private Long userId;
    private String name;
    private Long mutualConnections;
}
