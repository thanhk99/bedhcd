package com.api.bedhcd.modules.shareholder.infrastructure.persistence;

import com.api.bedhcd.modules.shareholder.domain.model.Shareholder;
import com.api.bedhcd.modules.shareholder.domain.repository.ShareholderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ShareholderRepositoryImpl implements ShareholderRepository {

    private final ShareholderJpaRepository jpaRepository;

    @Override
    public Optional<Shareholder> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Shareholder> findByCccd(String cccd) {
        return jpaRepository.findByCccd(cccd).map(this::toDomain);
    }

    @Override
    public Optional<Shareholder> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Shareholder save(Shareholder shareholder) {
        ShareholderEntity existingEntity = null;
        if (shareholder.getId() != null) {
            existingEntity = jpaRepository.findById(shareholder.getId()).orElse(null);
        }

        ShareholderEntity entity = toEntity(shareholder, existingEntity);
        ShareholderEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Shareholder> findAll(int page, int size) {
        return jpaRepository.findAllBySplitAccountFalse(PageRequest.of(page, size))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Shareholder> searchByKeyword(String keyword) {
        return jpaRepository.searchByKeyword(keyword)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Shareholder> searchByKeywordAndMeetingId(String keyword, String meetingId, int page, int size) {
        return jpaRepository.searchByKeywordAndMeetingId(keyword, meetingId, PageRequest.of(page, size))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Shareholder> searchTop10ByKeyword(String keyword) {
        return jpaRepository.searchTop10ByKeyword(keyword, PageRequest.of(0, 10))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.countBySplitAccountFalse();
    }

    @Override
    public long countByKeywordAndMeetingId(String keyword, String meetingId) {
        return jpaRepository.countByKeywordAndMeetingId(keyword, meetingId);
    }

    @Override
    public List<Shareholder> findAllActive() {
        return jpaRepository.findAllBySplitAccountFalse()
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    private Shareholder toDomain(ShareholderEntity entity) {
        return Shareholder.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .phoneNumber(entity.getPhoneNumber())
                .investorCode(entity.getInvestorCode())
                .cccd(entity.getCccd())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .password(entity.getPassword())
                .sharesOwned(entity.getSharesOwned())
                .enabled(entity.isEnabled())
                .splitAccount(entity.isSplitAccount())
                .status(entity.getShareholderStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private ShareholderEntity toEntity(Shareholder domain, ShareholderEntity existingEntity) {
        ShareholderEntity entity = existingEntity != null ? existingEntity : new ShareholderEntity();

        entity.setId(domain.getId());
        entity.setUsername(domain.getUsername());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setInvestorCode(domain.getInvestorCode());
        entity.setCccd(domain.getCccd());
        entity.setFullName(domain.getFullName());
        entity.setEmail(domain.getEmail());
        entity.setAddress(domain.getAddress());
        entity.setPassword(domain.getPassword());
        entity.setSharesOwned(domain.getSharesOwned());
        entity.setEnabled(domain.isEnabled());
        entity.setSplitAccount(domain.isSplitAccount());
        entity.setShareholderStatus(domain.getStatus());
        entity.setDeletedAt(domain.getDeletedAt());

        if (existingEntity == null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }

        return entity;
    }
}
