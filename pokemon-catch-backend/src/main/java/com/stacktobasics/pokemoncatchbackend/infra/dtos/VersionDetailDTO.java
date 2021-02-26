package com.stacktobasics.pokemoncatchbackend.infra.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VersionDetailDTO {
    public Integer maxChance;
    @JsonProperty("encounter_details")
    public List<EncounterDetailsDTO> encounterDetails;
    public VersionDTO version;
}
