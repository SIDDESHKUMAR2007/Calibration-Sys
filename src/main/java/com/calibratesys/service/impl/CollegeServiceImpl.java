package com.calibratesys.service.impl;

import com.calibratesys.dto.CollegeRequestDto;
import com.calibratesys.dto.CollegeResponseDto;
import com.calibratesys.entity.College;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.CollegeRepository;
import com.calibratesys.repository.LaboratoryRepository;
import com.calibratesys.service.CollegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CollegeServiceImpl implements CollegeService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9\\s-]{7,25}$");

    private final CollegeRepository collegeRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final com.calibratesys.service.AuditService auditService;

    @Autowired
    public CollegeServiceImpl(CollegeRepository collegeRepository,
                              LaboratoryRepository laboratoryRepository,
                              com.calibratesys.service.AuditService auditService) {
        this.collegeRepository = collegeRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public CollegeResponseDto createCollege(CollegeRequestDto request) {
        validateRequest(request);

        String code = request.getCode().trim();
        if (collegeRepository.existsByCode(code)) {
            throw new ApiException("College with code '" + code + "' already exists", HttpStatus.CONFLICT);
        }

        College college = new College(
                code,
                request.getName().trim(),
                StringUtils.hasText(request.getAddress()) ? request.getAddress().trim() : null,
                StringUtils.hasText(request.getContactEmail()) ? request.getContactEmail().trim() : null,
                StringUtils.hasText(request.getContactPhone()) ? request.getContactPhone().trim() : null
        );

        College saved = collegeRepository.save(college);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.COLLEGE_CREATED,
                com.calibratesys.entity.enums.AuditEntityType.COLLEGE,
                saved.getId(),
                "College " + saved.getCode() + " was created.",
                null,
                java.util.Map.of("code", saved.getCode(), "name", saved.getName())
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CollegeResponseDto getCollegeById(Long id) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new ApiException("College not found with id: " + id, HttpStatus.NOT_FOUND));
        return mapToDto(college);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollegeResponseDto> getAllColleges() {
        return collegeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CollegeResponseDto updateCollege(Long id, CollegeRequestDto request) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new ApiException("College not found with id: " + id, HttpStatus.NOT_FOUND));

        validateRequest(request);

        String newCode = request.getCode().trim();
        if (collegeRepository.existsByCodeAndIdNot(newCode, id)) {
            throw new ApiException("College with code '" + newCode + "' already exists", HttpStatus.CONFLICT);
        }

        String oldCode = college.getCode();
        String oldName = college.getName();

        college.setCode(newCode);
        college.setName(request.getName().trim());
        college.setAddress(StringUtils.hasText(request.getAddress()) ? request.getAddress().trim() : null);
        college.setContactEmail(StringUtils.hasText(request.getContactEmail()) ? request.getContactEmail().trim() : null);
        college.setContactPhone(StringUtils.hasText(request.getContactPhone()) ? request.getContactPhone().trim() : null);

        College updated = collegeRepository.save(college);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.COLLEGE_UPDATED,
                com.calibratesys.entity.enums.AuditEntityType.COLLEGE,
                updated.getId(),
                "College " + updated.getCode() + " was updated.",
                java.util.Map.of("code", oldCode, "name", oldName),
                java.util.Map.of("code", updated.getCode(), "name", updated.getName())
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteCollege(Long id) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new ApiException("College not found with id: " + id, HttpStatus.NOT_FOUND));

        // Enforce delete safety: check if laboratories are associated
        if (laboratoryRepository.existsByCollegeId(id)) {
            throw new ApiException(
                    "Cannot delete college because laboratories are associated with it. Delete or reassign laboratories first.",
                    HttpStatus.CONFLICT
            );
        }

        collegeRepository.delete(college);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.COLLEGE_DELETED,
                com.calibratesys.entity.enums.AuditEntityType.COLLEGE,
                id,
                "College " + college.getCode() + " was deleted.",
                java.util.Map.of("code", college.getCode(), "name", college.getName()),
                null
        );
    }

    private void validateRequest(CollegeRequestDto request) {
        if (request == null) {
            throw new ApiException("College request body is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new ApiException("College code is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new ApiException("College name is required", HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.hasText(request.getContactEmail())) {
            String email = request.getContactEmail().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new ApiException("Contact email format is invalid: " + email, HttpStatus.BAD_REQUEST);
            }
        }
        if (StringUtils.hasText(request.getContactPhone())) {
            String phone = request.getContactPhone().trim();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new ApiException("Contact phone format is invalid: " + phone, HttpStatus.BAD_REQUEST);
            }
        }
    }

    private CollegeResponseDto mapToDto(College college) {
        int labCount = college.getLaboratories() != null ? college.getLaboratories().size() : 0;
        return new CollegeResponseDto(
                college.getId(),
                college.getCode(),
                college.getName(),
                college.getAddress(),
                college.getContactEmail(),
                college.getContactPhone(),
                labCount,
                college.getCreatedAt(),
                college.getUpdatedAt()
        );
    }
}
