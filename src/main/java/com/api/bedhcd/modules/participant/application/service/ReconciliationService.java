package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.participant.api.v1.dto.ReconciliationItemResponse;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.model.ProxyDelegation;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import com.api.bedhcd.modules.participant.domain.repository.ProxyDelegationRepository;
import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.dto.UserDTO;
import com.api.bedhcd.shared.dto.importing.ExpectedAttendanceImportRecord;
import com.api.bedhcd.shared.util.CccdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Logic đối chiếu giữa danh sách tham dự dự kiến (KSNB import 4 cột)
 * và dữ liệu thực tế điểm danh / in phiếu trong meeting_participants.
 * Hiển thị song song cặp dữ liệu Import vs Thực tế.
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    public static final String STATUS_KHOP = "KHOP";
    public static final String STATUS_LECH = "LECH";

    // Scenario Constants
    public static final String SCENARIO_DIRECT = "DIRECT";
    public static final String SCENARIO_PROXY_MATCHED = "PROXY_MATCHED";
    public static final String SCENARIO_DELEGATED_TO_OTHER = "DELEGATED_TO_OTHER";
    public static final String SCENARIO_REVOKED_THEN_DIRECT = "REVOKED_THEN_DIRECT";
    public static final String SCENARIO_EXPECTED_DIRECT_BUT_DELEGATED = "EXPECTED_DIRECT_BUT_DELEGATED";
    public static final String SCENARIO_PROXY_NOT_PRINTED = "PROXY_NOT_PRINTED";
    public static final String SCENARIO_NOT_PRINTED = "NOT_PRINTED";
    public static final String SCENARIO_NO_SHOW = "NO_SHOW";

    // Reason Constants mới theo yêu cầu:
    // 1. Chưa tham dự
    public static final String REASON_NOT_SHOW = "NOT_SHOW";
    // 2. Đã huỷ tham dự
    public static final String REASON_CANCELLED_PARTICIPATION = "CANCELLED_PARTICIPATION";
    // 3. Đã huỷ uỷ quyền
    public static final String REASON_CANCELLED_DELEGATION = "CANCELLED_DELEGATION";
    // 4. CĐ/UQ không tồn tại
    public static final String REASON_NOT_FOUND_IN_SYSTEM = "NOT_FOUND_IN_SYSTEM";
    // 5. Lệch số cổ phần
    public static final String REASON_SHARE_MISMATCH = "SHARE_MISMATCH";

    private final ParticipantRepository participantRepository;
    private final ProxyDelegationRepository proxyDelegationRepository;
    private final IdentityPort identityPort;

    @Transactional(readOnly = true)
    public List<ReconciliationItemResponse> buildItems(String meetingId,
            Map<String, ExpectedAttendanceImportRecord> expectedByCccd) {

        // 1. Load tất cả participants của cuộc họp (chỉ lấy bản ghi chính, bỏ split ticket)
        List<Participant> allParticipants = participantRepository.findByMeetingId(meetingId);
        Map<String, Participant> participantByUserId = new HashMap<>();
        for (Participant p : allParticipants) {
            if (!p.isSplitTicket()) {
                participantByUserId.put(p.getUserId(), p);
            }
        }

        // 2. Load tất cả uỷ quyền ACTIVE của cuộc họp
        List<ProxyDelegation> activeDelegations = proxyDelegationRepository.findActiveByMeetingId(meetingId);
        Map<String, List<ProxyDelegation>> activeDelegationsByDelegator = activeDelegations.stream()
                .collect(Collectors.groupingBy(ProxyDelegation::getDelegatorId));

        // 3. Batch resolve tất cả CCCD xuất hiện trong file KSNB
        Set<String> allCccdsToLookup = new HashSet<>();
        expectedByCccd.forEach((cccdKey, exp) -> {
            if (cccdKey != null && !cccdKey.isBlank()) {
                allCccdsToLookup.add(CccdUtil.normalizeCccd(cccdKey));
            }
            if (exp.getProxyCccd() != null && !exp.getProxyCccd().isBlank()) {
                allCccdsToLookup.add(CccdUtil.normalizeCccd(exp.getProxyCccd()));
            }
        });

        // Batch 1 query duy nhất cho tất cả CCCDs
        Map<String, UserDTO> userMapByCccd = identityPort.getUsersByCccds(allCccdsToLookup);

        // Batch 1 query cho thông tin các proxyUserId chưa có
        Set<String> extraUserIds = new HashSet<>();
        activeDelegations.forEach(d -> {
            if (d.getProxyId() != null) extraUserIds.add(d.getProxyId());
            if (d.getDelegatorId() != null) extraUserIds.add(d.getDelegatorId());
        });
        Map<String, UserDTO> userMapById = identityPort.getUsersMapByIds(extraUserIds);

        List<ReconciliationItemResponse> items = new ArrayList<>();

        // 4. Duyệt từng bản ghi trong file KSNB (Zero DB calls trong vòng lặp!)
        expectedByCccd.forEach((cccdKey, exp) -> {
            String normCccd = CccdUtil.normalizeCccd(cccdKey);
            String normImportProxyCccd = (exp.getProxyCccd() != null && !exp.getProxyCccd().isBlank())
                    ? CccdUtil.normalizeCccd(exp.getProxyCccd())
                    : null;

            long importDirectShares = exp.getExpectedShares() != null ? exp.getExpectedShares() : 0L;
            long importProxyShares = exp.getProxyShares() != null ? exp.getProxyShares() : 0L;
            long totalRequiredShares = importDirectShares + importProxyShares;

            // Resolve cổ đông chính từ Memory Map
            UserDTO shareholderUser = userMapByCccd.get(normCccd);
            String shareholderUserId = shareholderUser != null ? shareholderUser.getId() : null;

            String fullName = shareholderUser != null ? shareholderUser.getFullName() : normCccd;
            Participant shareholderPart = shareholderUserId != null
                    ? participantByUserId.get(shareholderUserId)
                    : null;

            String shareholderStatus = shareholderPart != null && shareholderPart.getStatus() != null
                    ? shareholderPart.getStatus().name()
                    : (shareholderUser != null ? "PENDING" : "NOT_FOUND");

            // Tính actualDirectShares: nếu attendingShares = 0 nhưng đã PRINT,
            // fallback về sharesOwned - delegatedShares (trường hợp in thẻ nhưng chưa update shares)
            long actualDirectShares = (shareholderPart != null && shareholderPart.getAttendingShares() != null
                    && shareholderPart.getAttendingShares() > 0)
                    ? shareholderPart.getAttendingShares()
                    : (shareholderPart != null && shareholderPart.getStatus() == ParticipantStatus.PRINT
                            ? Math.max(0L, (shareholderPart.getSharesOwned() != null ? shareholderPart.getSharesOwned() : 0L)
                                    - (shareholderPart.getDelegatedShares() != null ? shareholderPart.getDelegatedShares() : 0L))
                            : 0L);

            // Resolve người nhận uỷ quyền theo File (từ Memory Map)
            UserDTO importProxyUser = normImportProxyCccd != null ? userMapByCccd.get(normImportProxyCccd) : null;
            String importProxyUserId = importProxyUser != null ? importProxyUser.getId() : null;
            String importProxyName = importProxyUser != null ? importProxyUser.getFullName() : normImportProxyCccd;

            // Thông tin thực tế uỷ quyền
            String actualProxyCccd = null;
            String actualProxyName = null;
            String actualProxyStatus = null;
            long actualProxyShares = 0L;

            String actualScenario = null;
            boolean isShareholderPrinted = shareholderPart != null && shareholderPart.getStatus() == ParticipantStatus.PRINT;

            if (normImportProxyCccd == null) {
                // --- CASE FILE NÓI TỰ THAM DỰ ---
                if (isShareholderPrinted) {
                    actualScenario = SCENARIO_DIRECT;
                } else if (shareholderUserId != null) {
                    String sId = shareholderUserId;
                    List<ProxyDelegation> delList = activeDelegationsByDelegator.getOrDefault(sId, Collections.emptyList());
                    
                    // Tìm proxy thực tế (ưu tiên người đã PRINT)
                    ProxyDelegation activeDel = delList.stream()
                            .filter(d -> {
                                Participant p = participantByUserId.get(d.getProxyId());
                                return p != null && p.getStatus() == ParticipantStatus.PRINT;
                            })
                            .findFirst()
                            .orElse(delList.isEmpty() ? null : delList.get(0));

                    if (activeDel != null) {
                        actualScenario = SCENARIO_EXPECTED_DIRECT_BUT_DELEGATED;
                        UserDTO actualProxyUser = userMapById.get(activeDel.getProxyId());
                        if (actualProxyUser != null) {
                            actualProxyCccd = actualProxyUser.getCccd();
                            actualProxyName = actualProxyUser.getFullName();
                        }
                        Participant actualProxyPart = participantByUserId.get(activeDel.getProxyId());
                        if (actualProxyPart != null) {
                            actualProxyStatus = actualProxyPart.getStatus() != null ? actualProxyPart.getStatus().name() : "PENDING";
                            actualProxyShares = actualProxyPart.getAttendingShares() != null ? actualProxyPart.getAttendingShares() : 0L;
                            if (actualProxyShares == 0L && actualProxyPart.getReceivedProxyShares() != null) {
                                actualProxyShares = actualProxyPart.getReceivedProxyShares();
                            }
                            // Fallback: đọc sharesDelegated từ delegation (trường hợp split ticket đã reset về 0)
                            if (actualProxyShares == 0L && activeDel.getSharesDelegated() != null) {
                                actualProxyShares = activeDel.getSharesDelegated();
                            }
                        }
                    } else if (shareholderPart != null && shareholderPart.getStatus() == ParticipantStatus.CHECKED_IN) {
                        actualScenario = SCENARIO_NOT_PRINTED;
                    } else {
                        actualScenario = SCENARIO_NO_SHOW;
                    }
                } else {
                    actualScenario = SCENARIO_NO_SHOW;
                }
            } else {
                // --- CASE FILE NÓI UỶ QUYỀN CHO B ---
                if (isShareholderPrinted) {
                    // Thu hồi uỷ quyền, cổ đông tự đến PRINT
                    actualScenario = SCENARIO_REVOKED_THEN_DIRECT;
                } else if (shareholderUserId != null) {
                    String sId = shareholderUserId;
                    String bId = importProxyUserId;
                    List<ProxyDelegation> delList = activeDelegationsByDelegator.getOrDefault(sId, Collections.emptyList());

                    // Check B trước
                    boolean bHasActiveDel = bId != null && delList.stream().anyMatch(d -> bId.equals(d.getProxyId()));
                    Participant bPart = bId != null ? participantByUserId.get(bId) : null;

                    if (bHasActiveDel && bPart != null && bPart.getStatus() == ParticipantStatus.PRINT) {
                        actualScenario = SCENARIO_PROXY_MATCHED;
                        actualProxyCccd = normImportProxyCccd;
                        actualProxyName = importProxyName;
                        actualProxyStatus = bPart.getStatus().name();
                        actualProxyShares = bPart.getAttendingShares() != null ? bPart.getAttendingShares() : 0L;
                        if (actualProxyShares == 0L && bPart.getReceivedProxyShares() != null) {
                            actualProxyShares = bPart.getReceivedProxyShares();
                        }
                        // Fallback: đọc sharesDelegated từ delegation (trường hợp split ticket đã reset về 0)
                        if (actualProxyShares == 0L) {
                            ProxyDelegation bDel = delList.stream()
                                    .filter(d -> bId.equals(d.getProxyId())).findFirst().orElse(null);
                            if (bDel != null && bDel.getSharesDelegated() != null) {
                                actualProxyShares = bDel.getSharesDelegated();
                            }
                        }
                    } else {
                        // Check xem có UQ ACTIVE sang C ≠ B không
                        ProxyDelegation otherDel = delList.stream()
                                .filter(d -> bId == null || !bId.equals(d.getProxyId()))
                                .filter(d -> {
                                    Participant p = participantByUserId.get(d.getProxyId());
                                    return p != null && p.getStatus() == ParticipantStatus.PRINT;
                                })
                                .findFirst()
                                .orElse(delList.stream().filter(d -> bId == null || !bId.equals(d.getProxyId())).findFirst().orElse(null));

                        if (otherDel != null) {
                            actualScenario = SCENARIO_DELEGATED_TO_OTHER;
                            UserDTO actualProxyUser = userMapById.get(otherDel.getProxyId());
                            if (actualProxyUser != null) {
                                actualProxyCccd = actualProxyUser.getCccd();
                                actualProxyName = actualProxyUser.getFullName();
                            }
                            Participant targetProxyPart = participantByUserId.get(otherDel.getProxyId());
                            if (targetProxyPart != null) {
                                actualProxyStatus = targetProxyPart.getStatus() != null ? targetProxyPart.getStatus().name() : "PENDING";
                                actualProxyShares = targetProxyPart.getAttendingShares() != null ? targetProxyPart.getAttendingShares() : 0L;
                                if (actualProxyShares == 0L && targetProxyPart.getReceivedProxyShares() != null) {
                                    actualProxyShares = targetProxyPart.getReceivedProxyShares();
                                }
                                // Fallback: đọc sharesDelegated từ delegation (trường hợp split ticket đã reset về 0)
                                if (actualProxyShares == 0L && otherDel.getSharesDelegated() != null) {
                                    actualProxyShares = otherDel.getSharesDelegated();
                                }
                            }
                        } else if (bPart != null && bPart.getStatus() == ParticipantStatus.CHECKED_IN) {
                            actualScenario = SCENARIO_PROXY_NOT_PRINTED;
                            actualProxyCccd = normImportProxyCccd;
                            actualProxyName = importProxyName;
                            actualProxyStatus = bPart.getStatus().name();
                        } else {
                            actualScenario = SCENARIO_NO_SHOW;
                        }
                    }
                } else {
                    actualScenario = SCENARIO_NO_SHOW;
                }
            }

            // Tính tổng CP thực tế mang đến (direct + proxy thực tế)
            long totalActualShares = actualDirectShares + actualProxyShares;

            // Đánh giá KHOP vs LECH:
            // KHOP khi thuộc 1 trong 3 scenario: DIRECT, PROXY_MATCHED, DELEGATED_TO_OTHER 
            // VÀ tổng CP thực tế >= tổng CP trong file
            boolean isScenarioMatched = SCENARIO_DIRECT.equals(actualScenario)
                    || SCENARIO_PROXY_MATCHED.equals(actualScenario)
                    || SCENARIO_DELEGATED_TO_OTHER.equals(actualScenario);

            boolean isMatched = isScenarioMatched && totalActualShares >= totalRequiredShares;

            String status = isMatched ? STATUS_KHOP : STATUS_LECH;
            String reason = null;

            if (!isMatched) {
                if (shareholderUser == null || (normImportProxyCccd != null && importProxyUser == null)) {
                    reason = REASON_NOT_FOUND_IN_SYSTEM;
                } else if (SCENARIO_REVOKED_THEN_DIRECT.equals(actualScenario)) {
                    reason = REASON_CANCELLED_DELEGATION;
                } else if (SCENARIO_EXPECTED_DIRECT_BUT_DELEGATED.equals(actualScenario)) {
                    reason = REASON_CANCELLED_PARTICIPATION;
                } else if (totalActualShares < totalRequiredShares && totalActualShares > 0) {
                    reason = REASON_SHARE_MISMATCH;
                } else {
                    reason = REASON_NOT_SHOW;
                }
            }

            long difference = totalActualShares - totalRequiredShares;

            items.add(ReconciliationItemResponse.builder()
                    .cccd(normCccd)
                    .fullName(fullName)
                    .shareholderStatus(shareholderStatus)
                    .importDirectShares(importDirectShares)
                    .actualDirectShares(actualDirectShares)
                    .importProxyCccd(normImportProxyCccd)
                    .importProxyName(importProxyName)
                    .importProxyShares(importProxyShares)
                    .actualProxyCccd(actualProxyCccd)
                    .actualProxyName(actualProxyName)
                    .actualProxyStatus(actualProxyStatus)
                    .actualProxyShares(actualProxyShares)
                    .actualScenario(actualScenario)
                    .status(status)
                    .reason(reason)
                    .difference(difference)
                    .build());
        });

        items.sort(Comparator.comparing(ReconciliationItemResponse::getCccd,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return items;
    }
}
