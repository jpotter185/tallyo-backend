package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.dto.InjuryReportResponse;
import com.tallyo.tallyo_backend.enums.League;

public interface InjuryService {
    InjuryReportResponse getInjuries(League league);

    // Returns the number of teams successfully refreshed.
    int refreshInjuries(League league);
}
