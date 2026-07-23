package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrendingHashtagDto {
    private String tag;
    private long postCount;
}
