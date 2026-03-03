package com.farmer.scheme.service;

import com.farmer.scheme.dto.SchemeDTO;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeService {
    private final SchemeRepository schemeRepository;

    @Transactional
    public void loadSchemesFromCsv() {
        try {
            if (schemeRepository.count() > 0) {
                log.info("Schemes already loaded in database");
                return;
            }

            ClassPathResource resource = new ClassPathResource("data/Expanded_Crop_Schemes_Individual_Rows_ENHANCED_with_Ministry_Address.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));

            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] fields = parseCsvLine(line);
                if (fields.length >= 11) {
                    Scheme scheme = new Scheme();
                    scheme.setSchemeName(fields[0].trim());
                    scheme.setCenterStateName(fields[1].trim());
                    scheme.setSchemeDetails(fields[2].trim());
                    scheme.setWebsiteLink(fields[3].trim());
                    scheme.setPhone(fields[4].trim());
                    scheme.setEmail(fields[5].trim());
                    scheme.setCommodityName(fields[6].trim());
                    scheme.setResponsibleMinistry(fields[7].trim());
                    scheme.setImplementingOffice(fields[8].trim());
                    scheme.setOfficeAddress(fields[9].trim());
                    scheme.setAddressSource(fields[10].trim());

                    schemeRepository.save(scheme);
                }
            }
            reader.close();
            log.info("Successfully loaded schemes from CSV");
        } catch (Exception e) {
            log.error("Error loading schemes from CSV", e);
        }
    }

    private String[] parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    public Page<SchemeDTO> searchSchemes(String commodity, String state, String center, Pageable pageable) {
        Page<Scheme> schemes;
        
        if (commodity != null && state != null) {
            schemes = schemeRepository.findByCommodityNameContainingIgnoreCaseAndCenterStateNameContainingIgnoreCase(
                    commodity, state, pageable);
        } else if (commodity != null) {
            schemes = schemeRepository.findByCommodityNameContainingIgnoreCase(commodity, pageable);
        } else if (state != null) {
            schemes = schemeRepository.findByCenterStateNameContainingIgnoreCase(state, pageable);
        } else if (center != null) {
            schemes = schemeRepository.findByOfficeAddressContainingIgnoreCase(center, pageable);
        } else {
            schemes = schemeRepository.findAll(pageable);
        }
        
        return schemes.map(this::convertToDTO);
    }

    private SchemeDTO convertToDTO(Scheme scheme) {
        return new SchemeDTO(
                scheme.getId(),
                scheme.getSchemeName(),
                scheme.getCenterStateName(),
                scheme.getSchemeDetails(),
                scheme.getWebsiteLink(),
                scheme.getPhone(),
                scheme.getEmail(),
                scheme.getCommodityName(),
                scheme.getResponsibleMinistry(),
                scheme.getImplementingOffice(),
                scheme.getOfficeAddress(),
                scheme.getAddressSource()
        );
    }
}
