package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import java.util.Objects;

/** A separate repair candidate can reuse produced assets while a later screenplay is being drafted. */
final class ShortDramaMediaRepair {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ShortDramaMediaRepair() { }
    static Long sourceId(ShortDramaStoryboard candidate) {
        try {
            var value = JSON.readTree(Objects.toString(candidate.getContinuityJson(), "{}")).path("repair_source_board");
            long id = Long.parseLong(value.asText(""));
            return id > 0 && !Objects.equals(id, candidate.getId()) ? id : null;
        } catch (Exception ignored) { return null; }
    }
    static boolean usesProducedAssets(ShortDramaStoryboard candidate, ShortDramaStoryboard source) {
        if (source == null || !Objects.equals(sourceId(candidate), source.getId())) return false;
        if (!Objects.equals(candidate.getProjectId(), source.getProjectId())
            || !Objects.equals(candidate.getScriptId(), source.getScriptId())
            || !Objects.equals(candidate.getLocationName(), source.getLocationName())) return false;
        if (!"done".equals(source.getVideoStatus()) || source.getVideoId() == null || source.getVideoId().isBlank()
            || source.getVideoUrl() == null || source.getVideoUrl().isBlank()) return false;
        try {
            var cast = JSON.readTree(Objects.toString(candidate.getCharactersJson(), "[]"));
            return cast.isArray() && !cast.isEmpty() && cast.equals(JSON.readTree(source.getCharactersJson()));
        } catch (Exception ignored) { return false; }
    }
}
