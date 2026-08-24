package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.participant.api.v1.dto.ReconciliationItemResponse;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.model.ProxyDelegation;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import com.api.bedhcd.modules.participant.domain.repository.ProxyDelegationRepository;
import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.dto.UserDTO;
import com.api.bedhcd.shared.dto.importing.ExpectedAttendanceImportRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ProxyDelegationRepository proxyDelegationRepository;

    @Mock
    private IdentityPort identityPort;

    @InjectMocks
    private ReconciliationService reconciliationService;

    private String meetingId = "m1";

    @Test
    @DisplayName("Cổ đông tự tham dự và đã in thẻ (status == PRINT) -> KHOP (DIRECT)")
    void testDirectMatch() {
        String cccd = "001099000001";
        String userId = "user1";

        ExpectedAttendanceImportRecord expRecord = ExpectedAttendanceImportRecord.builder()
                .cccd(cccd)
                .expectedShares(100L)
                .build();
        Map<String, ExpectedAttendanceImportRecord> expectedMap = Collections.singletonMap(cccd, expRecord);

        Participant participant = Participant.builder()
                .meetingId(meetingId)
                .userId(userId)
                .status(ParticipantStatus.PRINT)
                .attendingShares(100L)
                .build();

        UserDTO user = UserDTO.builder().id(userId).fullName("Nguyen Van A").cccd(cccd).build();

        when(participantRepository.findByMeetingId(meetingId)).thenReturn(Collections.singletonList(participant));
        when(proxyDelegationRepository.findActiveByMeetingId(meetingId)).thenReturn(Collections.emptyList());
        when(identityPort.getUsersByCccds(any())).thenReturn(Collections.singletonMap(cccd, user));
        when(identityPort.getUsersMapByIds(any())).thenReturn(Collections.singletonMap(userId, user));

        List<ReconciliationItemResponse> result = reconciliationService.buildItems(meetingId, expectedMap);

        assertEquals(1, result.size());
        ReconciliationItemResponse item = result.get(0);
        assertEquals("KHOP", item.getStatus());
        assertEquals("DIRECT", item.getActualScenario());
        assertEquals(100L, item.getActualDirectShares());
        assertNull(item.getReason());
    }

    @Test
    @DisplayName("Cổ đông uỷ quyền cho B, B đã in thẻ (PRINT) và có UQ ACTIVE -> KHOP (PROXY_MATCHED)")
    void testProxyMatch() {
        String delegatorCccd = "001099000001";
        String proxyCccd = "001099000002";
        String delegatorId = "user1";
        String proxyId = "user2";

        ExpectedAttendanceImportRecord expRecord = ExpectedAttendanceImportRecord.builder()
                .cccd(delegatorCccd)
                .proxyCccd(proxyCccd)
                .expectedShares(0L)
                .proxyShares(200L)
                .build();
        Map<String, ExpectedAttendanceImportRecord> expectedMap = Collections.singletonMap(delegatorCccd, expRecord);

        Participant proxyPart = Participant.builder()
                .meetingId(meetingId)
                .userId(proxyId)
                .status(ParticipantStatus.PRINT)
                .attendingShares(200L)
                .build();

        ProxyDelegation activeDelegation = ProxyDelegation.builder()
                .meetingId(meetingId)
                .delegatorId(delegatorId)
                .proxyId(proxyId)
                .sharesDelegated(200L)
                .status(DelegationStatus.ACTIVE)
                .build();

        UserDTO delegatorUser = UserDTO.builder().id(delegatorId).fullName("Nguyen Van A").cccd(delegatorCccd).build();
        UserDTO proxyUser = UserDTO.builder().id(proxyId).fullName("Tran Van B").cccd(proxyCccd).build();
        Map<String, UserDTO> cccdMap = new HashMap<>();
        cccdMap.put(delegatorCccd, delegatorUser);
        cccdMap.put(proxyCccd, proxyUser);

        Map<String, UserDTO> idMap = new HashMap<>();
        idMap.put(delegatorId, delegatorUser);
        idMap.put(proxyId, proxyUser);

        when(participantRepository.findByMeetingId(meetingId)).thenReturn(Collections.singletonList(proxyPart));
        when(proxyDelegationRepository.findActiveByMeetingId(meetingId)).thenReturn(Collections.singletonList(activeDelegation));
        when(identityPort.getUsersByCccds(any())).thenReturn(cccdMap);
        when(identityPort.getUsersMapByIds(any())).thenReturn(idMap);

        List<ReconciliationItemResponse> result = reconciliationService.buildItems(meetingId, expectedMap);

        assertEquals(1, result.size());
        ReconciliationItemResponse item = result.get(0);
        assertEquals("KHOP", item.getStatus());
        assertEquals("PROXY_MATCHED", item.getActualScenario());
        assertEquals(200L, item.getActualProxyShares());
    }

    @Test
    @DisplayName("Cổ đông uỷ quyền lại cho C (khác B trong file), C đã in thẻ -> KHOP (DELEGATED_TO_OTHER)")
    void testDelegatedToOtherMatch() {
        String delegatorCccd = "001099000001";
        String proxyCccdInFile = "001099000002"; // B trong file
        String actualProxyCccd = "001099000003"; // C thực tế trong hệ thống

        String delegatorId = "user1";
        String proxyInFileId = "user2";
        String actualProxyId = "user3";

        ExpectedAttendanceImportRecord expRecord = ExpectedAttendanceImportRecord.builder()
                .cccd(delegatorCccd)
                .proxyCccd(proxyCccdInFile)
                .expectedShares(0L)
                .proxyShares(150L)
                .build();
        Map<String, ExpectedAttendanceImportRecord> expectedMap = Collections.singletonMap(delegatorCccd, expRecord);

        Participant actualProxyPart = Participant.builder()
                .meetingId(meetingId)
                .userId(actualProxyId)
                .status(ParticipantStatus.PRINT)
                .attendingShares(150L)
                .build();

        ProxyDelegation delegationToC = ProxyDelegation.builder()
                .meetingId(meetingId)
                .delegatorId(delegatorId)
                .proxyId(actualProxyId)
                .sharesDelegated(150L)
                .status(DelegationStatus.ACTIVE)
                .build();

        UserDTO delegatorUser = UserDTO.builder().id(delegatorId).fullName("Nguyen Van A").cccd(delegatorCccd).build();
        UserDTO proxyInFileUser = UserDTO.builder().id(proxyInFileId).fullName("Tran Van B").cccd(proxyCccdInFile).build();
        UserDTO actualProxyUser = UserDTO.builder().id(actualProxyId).fullName("Hoang Van C").cccd(actualProxyCccd).build();

        Map<String, UserDTO> cccdMap = new HashMap<>();
        cccdMap.put(delegatorCccd, delegatorUser);
        cccdMap.put(proxyCccdInFile, proxyInFileUser);

        Map<String, UserDTO> idMap = new HashMap<>();
        idMap.put(delegatorId, delegatorUser);
        idMap.put(proxyInFileId, proxyInFileUser);
        idMap.put(actualProxyId, actualProxyUser);

        when(participantRepository.findByMeetingId(meetingId)).thenReturn(Collections.singletonList(actualProxyPart));
        when(proxyDelegationRepository.findActiveByMeetingId(meetingId)).thenReturn(Collections.singletonList(delegationToC));
        when(identityPort.getUsersByCccds(any())).thenReturn(cccdMap);
        when(identityPort.getUsersMapByIds(any())).thenReturn(idMap);

        List<ReconciliationItemResponse> result = reconciliationService.buildItems(meetingId, expectedMap);

        assertEquals(1, result.size());
        ReconciliationItemResponse item = result.get(0);
        assertEquals("KHOP", item.getStatus());
        assertEquals("DELEGATED_TO_OTHER", item.getActualScenario());
        assertEquals(actualProxyCccd, item.getActualProxyCccd());
        assertEquals(150L, item.getActualProxyShares());
    }

    @Test
    @DisplayName("Cổ đông thu hồi uỷ quyền, tự tham dự PRINT -> LECH (REVOKED_THEN_DIRECT)")
    void testRevokedThenDirectMismatch() {
        String delegatorCccd = "001099000001";
        String proxyCccdInFile = "001099000002";
        String delegatorId = "user1";
        String proxyInFileId = "user2";

        ExpectedAttendanceImportRecord expRecord = ExpectedAttendanceImportRecord.builder()
                .cccd(delegatorCccd)
                .proxyCccd(proxyCccdInFile)
                .expectedShares(0L)
                .proxyShares(150L)
                .build();
        Map<String, ExpectedAttendanceImportRecord> expectedMap = Collections.singletonMap(delegatorCccd, expRecord);

        // Cổ đông tự đến và PRINT
        Participant shareholderPart = Participant.builder()
                .meetingId(meetingId)
                .userId(delegatorId)
                .status(ParticipantStatus.PRINT)
                .attendingShares(150L)
                .build();

        UserDTO delegatorUser = UserDTO.builder().id(delegatorId).fullName("Nguyen Van A").cccd(delegatorCccd).build();
        UserDTO proxyInFileUser = UserDTO.builder().id(proxyInFileId).fullName("Tran Van B").cccd(proxyCccdInFile).build();

        Map<String, UserDTO> cccdMap = new HashMap<>();
        cccdMap.put(delegatorCccd, delegatorUser);
        cccdMap.put(proxyCccdInFile, proxyInFileUser);

        Map<String, UserDTO> idMap = new HashMap<>();
        idMap.put(delegatorId, delegatorUser);
        idMap.put(proxyInFileId, proxyInFileUser);

        when(participantRepository.findByMeetingId(meetingId)).thenReturn(Collections.singletonList(shareholderPart));
        when(proxyDelegationRepository.findActiveByMeetingId(meetingId)).thenReturn(Collections.emptyList());
        when(identityPort.getUsersByCccds(any())).thenReturn(cccdMap);
        when(identityPort.getUsersMapByIds(any())).thenReturn(idMap);

        List<ReconciliationItemResponse> result = reconciliationService.buildItems(meetingId, expectedMap);

        assertEquals(1, result.size());
        ReconciliationItemResponse item = result.get(0);
        assertEquals("LECH", item.getStatus());
        assertEquals("REVOKED_THEN_DIRECT", item.getActualScenario());
        assertEquals(150L, item.getActualDirectShares());
    }
}
