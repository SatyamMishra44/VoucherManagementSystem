package com.example.Voucher.serviceImpl;

import com.example.Voucher.dto.onboarding.TenantOnboardingRequestResponseDto;
import com.example.Voucher.dto.onboarding.TenantSignupRequestDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.TenantOnboardingRequest;
import com.example.Voucher.entity.TenantOnboardingStatus;
import com.example.Voucher.entity.User;
import com.example.Voucher.platform.TenantAuditLog;
import com.example.Voucher.repository.RoleRepository;
import com.example.Voucher.repository.TenantAuditLogRepository;
import com.example.Voucher.repository.TenantOnboardingRequestRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.security.RoleProperties;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class TenantOnboardingServiceImplTest {

    @Mock
    private TenantOnboardingRequestRepository tenantOnboardingRequestRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleProperties roleProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantAuditLogRepository tenantAuditLogRepository;

    private TenantOnboardingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantOnboardingServiceImpl(
                tenantOnboardingRequestRepository,
                tenantRepository,
                userRepository,
                roleRepository,
                roleProperties,
                passwordEncoder,
                tenantAuditLogRepository
        );
    }

    @Test
    void submitOnboardingRequest_duplicateTenantCode_throws() {
        TenantSignupRequestDto request = buildSignupRequest();
        when(tenantRepository.findByTenantCode("TENANT"))
                .thenReturn(Optional.of(new Tenant("TENANT", "Acme", TenantType.ORGANIZATION)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitOnboardingRequest(request)
        );
        assertTrue(ex.getMessage().contains("Tenant code already exists"));
    }

    @Test
    void submitOnboardingRequest_adminEmailInUse_throws() {
        TenantSignupRequestDto request = buildSignupRequest();
        when(tenantRepository.findByTenantCode("TENANT")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitOnboardingRequest(request)
        );
        assertTrue(ex.getMessage().contains("Admin email already in use"));
    }

    @Test
    void submitOnboardingRequest_adminPhoneInUse_throws() {
        TenantSignupRequestDto request = buildSignupRequest();
        when(tenantRepository.findByTenantCode("TENANT")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitOnboardingRequest(request)
        );
        assertTrue(ex.getMessage().contains("Admin phone number already in use"));
    }

    @Test
    void submitOnboardingRequest_duplicateTenantCodeRequest_throws() {
        TenantSignupRequestDto request = buildSignupRequest();
        when(tenantRepository.findByTenantCode("TENANT")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);
        when(tenantOnboardingRequestRepository.existsByTenantCodeAndStatusIn(eq("TENANT"), anyCollection()))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitOnboardingRequest(request)
        );
        assertTrue(ex.getMessage().contains("Tenant code already has an active onboarding request"));
    }

    @Test
    void submitOnboardingRequest_success_encodesAndTrims() {
        TenantSignupRequestDto request = buildSignupRequest();
        request.setTenantName("  Acme  ");
        request.setAdminFirstName("  Alice  ");
        request.setAdminLastName("  Admin  ");
        request.setAdminEmail("  ADMIN@EXAMPLE.COM  ");
        request.setAdminPhoneNumber(" 1234567890 ");
        request.setAdminPassword("  secret123  ");
        request.setNotes("  note  ");

        when(tenantRepository.findByTenantCode("TENANT")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);
        when(tenantOnboardingRequestRepository.existsByTenantCodeAndStatusIn(eq("TENANT"), anyCollection()))
                .thenReturn(false);
        when(tenantOnboardingRequestRepository.existsByAdminEmailAndStatusIn(eq("admin@example.com"), anyCollection()))
                .thenReturn(false);
        when(tenantOnboardingRequestRepository.existsByAdminPhoneNumberAndStatusIn(eq("1234567890"), anyCollection()))
                .thenReturn(false);

        when(passwordEncoder.encode("  secret123  ")).thenReturn("encoded");
        when(tenantOnboardingRequestRepository.save(any(TenantOnboardingRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TenantOnboardingRequestResponseDto dto = service.submitOnboardingRequest(request);

        assertEquals("TENANT", dto.getTenantCode());
        assertEquals("Acme", dto.getTenantName());
        assertEquals("Alice", dto.getAdminFirstName());
        assertEquals("Admin", dto.getAdminLastName());
        assertEquals("admin@example.com", dto.getAdminEmail());
        assertEquals("1234567890", dto.getAdminPhoneNumber());
        assertEquals("note", dto.getNotes());
        assertEquals(TenantOnboardingStatus.PENDING.name(), dto.getStatus());

        ArgumentCaptor<TenantOnboardingRequest> captor = ArgumentCaptor.forClass(TenantOnboardingRequest.class);
        verify(tenantOnboardingRequestRepository).save(captor.capture());
        TenantOnboardingRequest saved = captor.getValue();
        assertEquals("encoded", saved.getAdminPasswordHash());
    }

    @Test
    void reviewRequest_requestNotFound_throws() {
        when(tenantOnboardingRequestRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewRequest(1L, true, "ok", 10L)
        );
        assertTrue(ex.getMessage().contains("Onboarding request not found"));
    }

    @Test
    void reviewRequest_notPending_throws() {
        TenantOnboardingRequest request = new TenantOnboardingRequest(
                "Acme",
                "TENANT",
                "Alice",
                "Admin",
                "admin@example.com",
                "1234567890",
                "hash",
                null
        );
        request.markRejected(99L, "no");
        when(tenantOnboardingRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewRequest(1L, true, "ok", 10L)
        );
        assertTrue(ex.getMessage().contains("Only pending requests can be reviewed"));
    }

    @Test
    void reviewRequest_rejectWithoutComment_throws() {
        TenantOnboardingRequest request = buildPendingRequest();
        when(tenantOnboardingRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewRequest(1L, false, " ", 10L)
        );
        assertTrue(ex.getMessage().contains("Rejection comment is required"));
    }

    @Test
    void reviewRequest_rejectsAndSaves() {
        TenantOnboardingRequest request = buildPendingRequest();
        when(tenantOnboardingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(tenantOnboardingRequestRepository.save(request)).thenReturn(request);

        TenantOnboardingRequestResponseDto dto = service.reviewRequest(1L, false, "No", 10L);

        assertEquals(TenantOnboardingStatus.REJECTED.name(), dto.getStatus());
        verify(tenantOnboardingRequestRepository).save(request);
        verify(tenantAuditLogRepository, never()).save(any(TenantAuditLog.class));
    }

    @Test
    void reviewRequest_approvesAndSaves() {
        TenantOnboardingRequest request = buildPendingRequest();
        setRequestId(request, 1L);
        when(tenantOnboardingRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        when(tenantRepository.findByTenantCode("TENANT")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);
        when(tenantOnboardingRequestRepository.existsByTenantCodeAndStatusInAndIdNot(
                eq("TENANT"), anyCollection(), eq(1L)
        )).thenReturn(false);
        when(tenantOnboardingRequestRepository.existsByAdminEmailAndStatusInAndIdNot(
                eq("admin@example.com"), anyCollection(), eq(1L)
        )).thenReturn(false);
        when(tenantOnboardingRequestRepository.existsByAdminPhoneNumberAndStatusInAndIdNot(
                eq("1234567890"), anyCollection(), eq(1L)
        )).thenReturn(false);

        when(roleProperties.getTenantAdmin()).thenReturn("TENANT_ADMIN");
        when(roleRepository.findByName("TENANT_ADMIN")).thenReturn(Optional.of(new Role("TENANT_ADMIN", "")));

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tenantOnboardingRequestRepository.save(request)).thenReturn(request);

        TenantOnboardingRequestResponseDto dto = service.reviewRequest(1L, true, "ok", 10L);

        assertEquals(TenantOnboardingStatus.APPROVED.name(), dto.getStatus());
        verify(tenantRepository).save(any(Tenant.class));
        verify(userRepository).save(any(User.class));
        verify(tenantOnboardingRequestRepository).save(request);
        verify(tenantAuditLogRepository).save(any(TenantAuditLog.class));
    }

    private TenantSignupRequestDto buildSignupRequest() {
        TenantSignupRequestDto request = new TenantSignupRequestDto();
        request.setTenantName("Acme");
        request.setTenantCode("tenant");
        request.setAdminFirstName("Alice");
        request.setAdminLastName("Admin");
        request.setAdminEmail("admin@example.com");
        request.setAdminPhoneNumber("1234567890");
        request.setAdminPassword("secret123");
        request.setNotes("note");
        return request;
    }

    private TenantOnboardingRequest buildPendingRequest() {
        return new TenantOnboardingRequest(
                "Acme",
                "TENANT",
                "Alice",
                "Admin",
                "admin@example.com",
                "1234567890",
                "hash",
                null
        );
    }

    private void setRequestId(TenantOnboardingRequest request, Long id) {
        try {
            java.lang.reflect.Field field = TenantOnboardingRequest.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(request, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
