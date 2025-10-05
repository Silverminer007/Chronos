package de.chronoslive.dtos;

public record DateDto(Long id, String name, String description, String start, String end, String venue, Long group_id,
                      Long linked_to, String status) {
}
