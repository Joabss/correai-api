package com.correai.api.adapter.in.web.stats;

import com.correai.api.domain.port.in.stats.PersonalRecordsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class PersonalRecordController {
    private final PersonalRecordsUseCase records;

    public PersonalRecordController(PersonalRecordsUseCase records) {
        this.records = records;
    }

    @GetMapping("/stats/records")
    public List<PersonalRecordsUseCase.PersonalRecord> records(@RequestAttribute("userId") UUID userId) {
        return records.records(userId);
    }
}
