package ru.practicum.interaction.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedCommentsResponse {

    @Builder.Default
    private Map<Long, Long> approvedComments = new HashMap<>();
}