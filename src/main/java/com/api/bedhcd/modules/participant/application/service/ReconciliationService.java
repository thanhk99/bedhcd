package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.participant.api.v1.dto.ReconciliationItemResponse;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import com.api.bedhcd.shared.dto.UserDTO;
import com.api.bedhcd.shared.dto.importing.ExpectedAttendanceImportRecord;
import com.api.bedhcd.shared.util.CccdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dùng chung logic đối chiếu giữa danh sách tham dự dự kiến (KSNB) và dữ liệu
 * trong meeting_participants. Dùng cho cả preview trước khi import và màn Đối soát.
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    public static final String STATUS_KHOP = "KHOP";
    public static final String STATUS_LECH = "LECH";
    public static final String REASON_NOT_FOUND_IN_SYSTEM = "NOT_FOUND_IN_SYSTEM";
    public static final String REASON_SHARE_MISMATCH = "SHARE_MISMATCH";

    private final ParticipantRepository participantRepository;
    private final IdentityPort identityPort;

    /**
     * Xây danh sách đối soát chi tiết.
     *
     * @param meetingId      cuộc họp
     * @param expectedByCccd map CCCD -> bản ghi dự kiến (file KSNB)
     */
    @Transactional(readOnly = true)
    public List<ReconciliationItemResponse> buildItems(String meetingId,
            Map<String, ExpectedAttendanceImportRecord> expectedByCccd) {
        Map<String, SystemShare> systemByCccd = loadSystemShares(meetingId);

        List<ReconciliationItemResponse> items = new ArrayList<>();

        // Phía file KSNB: chỉ xét những người có trong file
        expectedByCccd.forEach((cccd, exp) -> {
            String key = CccdUtil.normalizeCccd(cccd);
            SystemShare sys = systemByCccd.get(key);
            String fullName = sys != null ? sys.fullName : key;
            long expected = exp.getExpectedShares() != null ? exp.getExpectedShares() : 0L;

            if (sys == null) {
                items.add(build(key, fullName, expected, 0L, -expected, STATUS_LECH, REASON_NOT_FOUND_IN_SYSTEM));
            } else {
                long diff = sys.shares - expected;
                items.add(build(key, fullName, expected, sys.shares, diff,
                        diff == 0 ? STATUS_KHOP : STATUS_LECH,
                        diff == 0 ? null : REASON_SHARE_MISMATCH));
            }
        });

        items.sort(Comparator.comparing(ReconciliationItemResponse::getCccd,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return items;
    }

    private Map<String, SystemShare> loadSystemShares(String meetingId) {
        Map<String, SystemShare> map = new HashMap<>();
        for (Participant p : participantRepository.findByMeetingId(meetingId)) {
            // Bỏ phiếu con từ tách phiếu để không trùng người
            if (p.isSplitTicket()) {
                continue;
            }
            UserDTO user = identityPort.getUserInfo(p.getUserId());
            if (user == null || user.getCccd() == null || user.getCccd().isBlank()) {
                continue;
            }
            long shares = p.getSharesOwned() != null ? p.getSharesOwned() : 0L;
            map.putIfAbsent(CccdUtil.normalizeCccd(user.getCccd()), new SystemShare(user.getFullName(), shares));
        }
        return map;
    }

    private ReconciliationItemResponse build(String cccd, String fullName, long expected, long system,
            long difference, String status, String reason) {
        return ReconciliationItemResponse.builder()
                .cccd(cccd)
                .fullName(fullName)
                .expectedShares(expected)
                .systemShares(system)
                .difference(difference)
                .status(status)
                .reason(reason)
                .build();
    }

    private static class SystemShare {
        final String fullName;
        final long shares;

        SystemShare(String fullName, long shares) {
            this.fullName = fullName;
            this.shares = shares;
        }
    }
}
