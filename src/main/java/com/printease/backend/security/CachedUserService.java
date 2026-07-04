package com.printease.backend.security;

import com.printease.backend.entity.User;
import com.printease.backend.repository.ShopRepository;
import com.printease.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CachedUserService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    @Cacheable(value = "users", key = "#userId")
    @Transactional(readOnly = true)
    public Optional<User> getUserById(UUID userId) {
        return userRepository.findById(userId);
    }

    @Cacheable(value = "shopAdminIds", key = "#adminId")
    @Transactional(readOnly = true)
    public Optional<UUID> getShopIdByAdminId(UUID adminId) {
        return shopRepository.findByAdminId(adminId).map(shop -> shop.getId());
    }
}
