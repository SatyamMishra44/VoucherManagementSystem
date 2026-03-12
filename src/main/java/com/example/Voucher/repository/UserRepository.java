package com.example.Voucher.repository;


import com.example.Voucher.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmailAndTenantId(String email, Long tenantId);
    boolean existsByPhoneNumberAndTenantId(String phoneNumber, Long tenantId);
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    Optional<User> findByIdAndTenantId(Long userId, Long tenantId);

    List<User> findAllByTenantId(Long tenantId);
    List<User> findAllByTenantIdAndRoles_Name(Long tenantId, String roleName);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.tenantId = :tenantId
              AND (:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))
              AND (:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
              AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:phoneNumber IS NULL OR u.phoneNumber LIKE CONCAT('%', :phoneNumber, '%'))
              AND (:enabled IS NULL OR u.enabled = :enabled)
            ORDER BY u.createdAt DESC
            """)
    List<User> findAllByTenantIdWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("email") String email,
            @Param("phoneNumber") String phoneNumber,
            @Param("enabled") Boolean enabled
    );
}
