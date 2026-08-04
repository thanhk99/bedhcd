package com.api.bedhcd.modules.shareholder.application.service;

import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.resolution.domain.model.Resolution;
import com.api.bedhcd.modules.resolution.domain.model.VotingOption;
import com.api.bedhcd.modules.resolution.domain.repository.ResolutionRepository;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import com.api.bedhcd.modules.voting.domain.model.Vote;
import com.api.bedhcd.modules.shareholder.api.v1.dto.request.CreateShareholderRequest;
import com.api.bedhcd.modules.shareholder.api.v1.dto.request.UpdateShareholderRequest;
import com.api.bedhcd.modules.shareholder.api.v1.dto.response.ShareholderResponse;
import com.api.bedhcd.modules.shareholder.api.v1.dto.response.VoteHistoryResponse;
import com.api.bedhcd.modules.shareholder.application.mapper.ShareholderMapper;
import com.api.bedhcd.modules.shareholder.domain.exception.ShareholderException;
import com.api.bedhcd.modules.shareholder.domain.model.Shareholder;
import com.api.bedhcd.modules.shareholder.domain.repository.ShareholderRepository;
import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.shared.dto.PageResponse;
import com.api.bedhcd.shared.domain.enums.ShareholderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareholderApplicationService {

    private final ShareholderRepository shareholderRepository;
    private final ShareholderMapper shareholderMapper;
    private final PasswordEncoder passwordEncoder;
    private final ParticipantPort participantPort;
    private final VotingPort votingPort;
    private final ResolutionRepository resolutionRepository;
    private final MeetingPort meetingPort;
    private final IdentityPort identityPort;
    private final CacheManager cacheManager;

    @Cacheable(value = "shareholders:page", key = "#page + '-' + #size + '-' + (#keyword != null ? #keyword : '') + '-' + (#meetingId != null ? #meetingId : '')")
    @Transactional(readOnly = true)
    public PageResponse<ShareholderResponse> getShareholders(int page, int size, String keyword, String meetingId) {
        List<ShareholderResponse> pageItems;
        long total;

        if (meetingId != null && !meetingId.isBlank()) {
            pageItems = shareholderRepository.searchByKeywordAndMeetingId(keyword, meetingId, page, size).stream()
                    .map(shareholderMapper::toResponse)
                    .collect(Collectors.toList());
            total = shareholderRepository.countByKeywordAndMeetingId(keyword, meetingId);
        } else if (keyword != null && !keyword.isBlank()) {
            // Re-using searchByKeywordAndMeetingId but passing null for meetingId could be cleaner,
            // but we can stick to using the existing searchByKeyword or searchByKeywordAndMeetingId
            pageItems = shareholderRepository.searchByKeywordAndMeetingId(keyword, null, page, size).stream()
                    .map(shareholderMapper::toResponse)
                    .collect(Collectors.toList());
            total = shareholderRepository.countByKeywordAndMeetingId(keyword, null);
        } else {
            pageItems = shareholderRepository.findAll(page, size).stream()
                    .map(shareholderMapper::toResponse)
                    .collect(Collectors.toList());
            total = shareholderRepository.count();
        }

        return PageResponse.of(pageItems, total, page, size);
    }

    /**
     * Tìm kiếm top 10 cổ đông theo từ khóa (autocomplete).
     * Cache vì dữ liệu cổ đông ít thay đổi.
     */
    @Cacheable(value = "shareholders:search", key = "#keyword")
    @Transactional(readOnly = true)
    public List<ShareholderResponse> searchShareholders(String keyword) {
        return shareholderRepository.searchTop10ByKeyword(keyword).stream()
                .map(shareholderMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin chi tiết cổ đông theo ID.
     * Cache vì dữ liệu cổ đông ít thay đổi.
     */
    @Cacheable(value = "shareholders", key = "#id")
    @Transactional(readOnly = true)
    public ShareholderResponse getShareholder(String id) {
        Shareholder shareholder = shareholderRepository.findById(id)
                .orElseThrow(() -> ShareholderException.notFound(id));
        return shareholderMapper.toResponse(shareholder);
    }

    /**
     * Cập nhật thông tin cổ đông. Evict cache của cổ đông đó và toàn bộ search
     * cache.
     */
    @Caching(evict = {
            @CacheEvict(value = "shareholders", key = "#id"),
            @CacheEvict(value = "shareholders:search", allEntries = true),
            @CacheEvict(value = "shareholders:page", allEntries = true)
    })
    @Transactional
    public ShareholderResponse updateShareholder(String id, UpdateShareholderRequest request) {
        Shareholder shareholder = shareholderRepository.findById(id)
                .orElseThrow(() -> ShareholderException.notFound(id));

        shareholder.validateEditable();

        if (request.getFullName() != null) {
            shareholder.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            shareholder.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            shareholder.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            shareholder.setAddress(request.getAddress());
        }
        if (request.getEnabled() != null) {
            shareholder.setEnabled(request.getEnabled());
        }
        if (request.getSharesOwned() != null) {
            shareholder.setSharesOwned(request.getSharesOwned());
        }

        Shareholder updated = shareholderRepository.save(shareholder);
        return shareholderMapper.toResponse(updated);
    }

    /**
     * Tạo mới cổ đông.
     */
    @Caching(evict = {
            @CacheEvict(value = "shareholders:search", allEntries = true),
            @CacheEvict(value = "shareholders:page", allEntries = true)
    })
    @Transactional
    public ShareholderResponse createShareholder(CreateShareholderRequest request) {
        if (shareholderRepository.findByCccd(request.getCccd()).isPresent()) {
            throw ShareholderException.badRequest("Cổ đông với CCCD " + request.getCccd() + " đã tồn tại");
        }

        Shareholder shareholder = Shareholder.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getCccd()) // mặc định username = cccd
                .password(passwordEncoder.encode(request.getCccd())) // mật khẩu mặc định = cccd
                .cccd(request.getCccd())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .sharesOwned(request.getSharesOwned() != null ? request.getSharesOwned() : 0L)
                .enabled(true)
                .status(ShareholderStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Shareholder saved = shareholderRepository.save(shareholder);
        participantPort.createParticipant(request.getMeetingId(), saved.getId(), saved.getSharesOwned());
        return shareholderMapper.toResponse(saved);
    }

    /**
     * Xoá cổ đông.
     * Không cho phép xóa khi trạng thái là LOCKED (đã gửi email).
     */
    @Caching(evict = {
            @CacheEvict(value = "shareholders", key = "#id"),
            @CacheEvict(value = "shareholders:search", allEntries = true),
            @CacheEvict(value = "shareholders:page", allEntries = true)
    })
    @Transactional
    public void deleteShareholder(String id) {
        Shareholder shareholder = shareholderRepository.findById(id)
                .orElseThrow(() -> ShareholderException.notFound(id));

        // Nghiệp vụ domain: chặn xóa khi đang LOCKED
        shareholder.validateDeletable();

        shareholderRepository.deleteById(id);
    }

    /**
     * Lấy lịch sử biểu quyết của cổ đông đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public List<VoteHistoryResponse> getVotingHistory() {
        String userId = identityPort.getCurrentUserId();
        if (userId == null) {
            throw ShareholderException.notFound("current user");
        }

        List<Vote> votes = votingPort.getVotesByUser(userId);

        return votes.stream().map(vote -> {
            // Lấy thông tin nghị quyết
            String resolutionTitle = null;
            String meetingId = null;
            String meetingTitle = null;
            String votingOptionName = null;

            if (vote.getResolutionId() != null) {
                Resolution resolution = resolutionRepository.findById(vote.getResolutionId()).orElse(null);
                if (resolution != null) {
                    resolutionTitle = resolution.getTitle();
                    meetingId = resolution.getMeetingId();
                    meetingTitle = meetingPort.getMeetingName(meetingId);

                    if (vote.getVotingOptionId() != null && resolution.getOptions() != null) {
                        votingOptionName = resolution.getOptions().stream()
                                .filter(o -> o.getId().equals(vote.getVotingOptionId()))
                                .map(VotingOption::getName)
                                .findFirst().orElse(vote.getVotingOptionId());
                    }
                }
            }

            return VoteHistoryResponse.builder()
                    .voteId(vote.getId() != null ? vote.getId().toString() : null)
                    .resolutionId(vote.getResolutionId())
                    .resolutionTitle(resolutionTitle)
                    .meetingId(meetingId)
                    .meetingTitle(meetingTitle)
                    .votingOptionId(vote.getVotingOptionId())
                    .votingOptionName(votingOptionName)
                    .voteWeight(vote.getVoteWeight())
                    .ipAddress(vote.getIpAddress())
                    .userAgent(vote.getUserAgent())
                    .votedAt(vote.getVotedAt())
                    .action("VOTE_CAST")
                    .build();
        }).collect(Collectors.toList());
    }

    @Caching(evict = {
            @CacheEvict(value = "shareholders", key = "#shareholderId"),
            @CacheEvict(value = "shareholders:search", allEntries = true),
            @CacheEvict(value = "shareholders:page", allEntries = true)
    })
    @Transactional
    public ShareholderResponse sendSimulatedEmailAndLock(String shareholderId) {
        Shareholder shareholder = shareholderRepository.findById(shareholderId)
                .orElseThrow(() -> ShareholderException.notFound(shareholderId));

        // In log giả lập email
        log.info("=== GIẢ LẬP GỬI EMAIL THÀNH CÔNG ===");
        log.info("Gửi tới: {}", shareholder.getEmail());
        log.info("Tiêu đề: Thông tin tài khoản tham dự Đại Hội Cổ Đông");
        log.info("Nội dung: Tài khoản: {} | Mật khẩu: (Đã mã hóa)", shareholder.getUsername());
        log.info("======================================");

        // Chốt thông tin
        shareholder.lockInfo();
        Shareholder saved = shareholderRepository.save(shareholder);

        return shareholderMapper.toResponse(saved);
    }

    @Caching(evict = {
            @CacheEvict(value = "shareholders", allEntries = true),
            @CacheEvict(value = "shareholders:search", allEntries = true),
            @CacheEvict(value = "shareholders:page", allEntries = true)
    })
    @Transactional
    public void sendSimulatedEmailAndLockBatch(String meetingId) {
        // Lấy danh sách cổ đông trong cuộc họp
        java.util.List<String> userIds = participantPort.getParticipantUserIds(meetingId);
        if (userIds == null || userIds.isEmpty()) return;

        for (String userId : userIds) {
            shareholderRepository.findById(userId).ifPresent(sh -> {
                if (sh.getStatus() == ShareholderStatus.ACTIVE) {
                    log.info("=== GIẢ LẬP GỬI EMAIL BATCH THÀNH CÔNG ===");
                    log.info("Gửi tới: {} (Cổ đông: {})", sh.getEmail(), sh.getFullName());
                    log.info("============================================");

                    sh.lockInfo();
                    shareholderRepository.save(sh);
                }
            });
        }
    }

    @Async("taskExecutor")
    public void sendSimulatedEmailAndLockAllAsync() {
        log.info("Bắt đầu gửi email giả lập cho tất cả cổ đông ACTIVE...");

        // Lấy danh sách tất cả cổ đông active (không split account)
        List<Shareholder> shareholders = shareholderRepository.findAllActive();

        for (Shareholder shareholder : shareholders) {
            if (shareholder.getStatus() == ShareholderStatus.ACTIVE) {
                log.info("=== GIẢ LẬP GỬI EMAIL THÀNH CÔNG ===");
                log.info("Gửi tới: {} (Cổ đông: {})", shareholder.getEmail(), shareholder.getFullName());
                log.info("Tiêu đề: Thông tin tài khoản tham dự Đại Hội Cổ Đông");
                log.info("Nội dung: Tài khoản: {} | Mật khẩu: (Đã mã hóa)", shareholder.getUsername());
                log.info("======================================");

                // Chốt thông tin cổ đông
                shareholder.lockInfo();
                shareholderRepository.save(shareholder);
            }
        }

        // Dọn dẹp cache Redis sau khi hoàn tất - flush toàn bộ cache liên quan đến shareholders
        log.info("Dọn dẹp cache Redis sau khi gửi email cho tất cả cổ đông...");

        try {
            if (cacheManager != null) {
                // Flush các cache cần thiết để cập nhật dữ liệu mới
                Cache pageCache = cacheManager.getCache("shareholders:page");
                Cache searchCache = cacheManager.getCache("shareholders:search");

                if (pageCache != null) {
                    pageCache.clear();
                    log.info("Đã flush cache shareholders:page");
                }
                if (searchCache != null) {
                    searchCache.clear();
                    log.info("Đã flush cache shareholders:search");
                }
            } else {
                log.warn("CacheManager không khả dụng để flush cache");
            }
        } catch (Exception e) {
            log.error("Lỗi khi flush cache", e);
        }
    }

}