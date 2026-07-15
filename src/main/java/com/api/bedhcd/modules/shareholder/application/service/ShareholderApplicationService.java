package com.api.bedhcd.modules.shareholder.application.service;

import com.api.bedhcd.modules.shareholder.api.v1.dto.request.CreateShareholderRequest;
import com.api.bedhcd.modules.shareholder.api.v1.dto.request.UpdateShareholderRequest;
import com.api.bedhcd.modules.shareholder.api.v1.dto.response.ShareholderResponse;
import com.api.bedhcd.modules.shareholder.application.mapper.ShareholderMapper;
import com.api.bedhcd.modules.shareholder.domain.exception.ShareholderException;
import com.api.bedhcd.modules.shareholder.domain.model.Shareholder;
import com.api.bedhcd.modules.shareholder.domain.repository.ShareholderRepository;
import com.api.bedhcd.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareholderApplicationService {

    private final ShareholderRepository shareholderRepository;
    private final ShareholderMapper shareholderMapper;
    private final PasswordEncoder passwordEncoder;

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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Shareholder saved = shareholderRepository.save(shareholder);
        return shareholderMapper.toResponse(saved);
    }

    /**
     * Xoá mềm cổ đông.
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
        
        shareholder.setEnabled(false); // soft delete: khoá tài khoản
        shareholder.setDeletedAt(LocalDateTime.now());
        shareholderRepository.save(shareholder);
    }
}
