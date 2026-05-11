package org.rap.algotutorbe.iam.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.iam.domain.model.Role;
import org.rap.algotutorbe.iam.domain.model.RoleCode;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.RoleRepository;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Bắt đầu tạo seed data...");

        // 1. TẠO ROLES TRƯỚC
        seedRole(RoleCode.USER, "Người dùng", "Quyền người dùng cơ bản");
        seedRole(RoleCode.ADMIN, "Quản trị viên", "Quyền quản trị viên");
        seedRole(RoleCode.EDITOR, "Biên tập viên", "Quyền quản lý nội dung");

        // 2. TẠO USERS VÀ GÁN ROLE
        // Trong thực tế, chuỗi "123456" nên được mã hóa (VD: passwordEncoder.encode("123456"))
        String rawPassword = "P@ssword123@";
        String hashedPassword = passwordEncoder.encode(rawPassword);
        seedUser("admin_user", "admin@example.com", hashedPassword, RoleCode.ADMIN);
        seedUser("editor_user", "editor@example.com", hashedPassword, RoleCode.EDITOR);
        seedUser("normal_user", "user@example.com", hashedPassword, RoleCode.USER);

        log.info("Khởi tạo seed data hoàn tất!");
    }

    private void seedRole(RoleCode code, String name, String description) {
        if (!roleRepository.existsByCode(code)) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            roleRepository.save(role);
            log.info("Đã tạo mới role: {}", code);
        }
    }

    private void seedUser(String userName, String email, String password, RoleCode roleCode) {
        // Kiểm tra xem user đã tồn tại chưa để tránh lỗi Unique Constraint
        if (!userRepository.existsByUsername(userName)) {

            // Tìm Role tương ứng từ Database
            Role userRole = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Role: " + roleCode));

            User user = new User();
            user.setUsername(userName);
            user.setEmail(email);
            user.setPasswordHashed(password);
            user.setEnabled(true);
            user.setRole(userRole);

            userRepository.save(user);
            log.info("Đã tạo mới user: {} với quyền: {}", userName, roleCode);
        }
    }
}