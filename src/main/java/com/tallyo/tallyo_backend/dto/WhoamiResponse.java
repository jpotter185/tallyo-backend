package com.tallyo.tallyo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Who an OAuth token on the MCP routes belongs to, and what it grants. */
@Getter
@Builder
@AllArgsConstructor
public class WhoamiResponse {
    private String subject;
    private String username;
    private List<String> groups;
    private List<String> scopes;
}
