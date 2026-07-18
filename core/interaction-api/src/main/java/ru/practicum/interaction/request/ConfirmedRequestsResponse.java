package ru.practicum.interaction.request;

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
public class ConfirmedRequestsResponse {

    @Builder.Default
    private Map<Long, Long> confirmedRequests = new HashMap<>();
}
