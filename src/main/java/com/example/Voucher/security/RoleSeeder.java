package com.example.Voucher.security;

import com.example.Voucher.entity.Role;
import com.example.Voucher.repository.RoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final RoleProperties roleProperties;

    public RoleSeeder(RoleRepository roleRepository, RoleProperties roleProperties) {
        this.roleRepository = roleRepository;
        this.roleProperties = roleProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRoleIfMissing(roleProperties.getAdmin(), "System administrator");
        seedRoleIfMissing(roleProperties.getUser(), "Standard user");
    }

    private void seedRoleIfMissing(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(new Role(name, description));
        }
    }
}
