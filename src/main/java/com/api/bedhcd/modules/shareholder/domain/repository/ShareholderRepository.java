package com.api.bedhcd.modules.shareholder.domain.repository;

import com.api.bedhcd.modules.shareholder.domain.model.Shareholder;
import java.util.List;
import java.util.Optional;

public interface ShareholderRepository {
    Optional<Shareholder> findById(String id);
    Optional<Shareholder> findByCccd(String cccd);
    Optional<Shareholder> findByUsername(String username);

    Shareholder save(Shareholder shareholder);

    void deleteById(String id); // Có thể giữ lại hoặc không dùng, sẽ implement soft delete trong service

    List<Shareholder> findAll(int page, int size);

    List<Shareholder> searchByKeyword(String keyword);
    List<Shareholder> searchByKeywordAndMeetingId(String keyword, String meetingId, int page, int size);

    List<Shareholder> searchTop10ByKeyword(String keyword);

    long count();
    long countByKeywordAndMeetingId(String keyword, String meetingId);
}
