package com.tallyo.tallyo_backend.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    void testPageResponse_WithData() {
        // Arrange
        List<String> content = List.of("item1", "item2", "item3");
        Pageable pageable = PageRequest.of(0, 10);
        Page<String> page = new PageImpl<>(content, pageable, 3);

        // Act
        PageResponse<String> response = new PageResponse<>(page);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(3, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void testPageResponse_Empty() {
        // Arrange
        List<String> emptyContent = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 10);
        Page<String> page = new PageImpl<>(emptyContent, pageable, 0);

        // Act
        PageResponse<String> response = new PageResponse<>(page);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void testPageResponse_Pagination() {
        // Arrange
        List<String> content = List.of("item1", "item2");
        Pageable pageable = PageRequest.of(1, 2);
        Page<String> page = new PageImpl<>(content, pageable, 5);

        // Act
        PageResponse<String> response = new PageResponse<>(page);

        // Assert
        assertEquals(1, response.getPage());
        assertEquals(2, response.getSize());
        assertEquals(5, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertFalse(response.isFirst());
        assertFalse(response.isLast());
    }

    @Test
    void testPageResponse_LastPage() {
        // Arrange
        List<String> content = List.of("item4", "item5");
        Pageable pageable = PageRequest.of(2, 2);
        Page<String> page = new PageImpl<>(content, pageable, 5);

        // Act
        PageResponse<String> response = new PageResponse<>(page);

        // Assert
        assertEquals(2, response.getPage());
        assertTrue(response.isLast());
        assertFalse(response.isFirst());
    }
}
