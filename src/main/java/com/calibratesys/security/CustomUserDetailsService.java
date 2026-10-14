package com.calibratesys.security;

import com.calibratesys.entity.Staff;
import com.calibratesys.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to load Staff entity as Spring Security UserDetails by email or staffId.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    @Autowired
    public CustomUserDetailsService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Staff staff = staffRepository.findByEmailOrStaffId(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Staff user not found with email or staff ID: " + identifier));

        return new CustomUserDetails(staff);
    }
}
