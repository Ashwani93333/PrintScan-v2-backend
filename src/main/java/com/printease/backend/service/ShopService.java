package com.printease.backend.service;

import com.printease.backend.dto.request.RegisterShopRequest;
import com.printease.backend.dto.request.ShopCreateRequest;
import com.printease.backend.dto.request.ShopProfileUpdateRequest;
import com.printease.backend.dto.response.ShopRequirementsResponse;
import com.printease.backend.dto.response.ShopResponse;
import com.printease.backend.entity.Shop;
import com.printease.backend.entity.ShopRequirements;
import com.printease.backend.entity.User;
import com.printease.backend.entity.enums.Role;
import com.printease.backend.exception.BadRequestException;
import com.printease.backend.exception.ResourceNotFoundException;
import com.printease.backend.repository.ShopRepository;
import com.printease.backend.repository.ShopRequirementsRepository;
import com.printease.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopRequirementsRepository shopRequirementsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QrCodeGenerator qrCodeGenerator;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ===== Public endpoints =====

    @Transactional(readOnly = true)
    public ShopResponse getPublicShopBySlug(String slug) {
        log.info("getPublicShopBySlug | slug={}", slug);
        Shop shop = shopRepository.findBySlugAndIsApprovedTrue(slug)
                .orElseThrow(() -> {
                    log.warn("getPublicShopBySlug | NOT FOUND (or not approved) | slug={}", slug);
                    return new ResourceNotFoundException("Shop", "slug", slug);
                });
        log.info("getPublicShopBySlug | FOUND | shopId={} | name={}", shop.getId(), shop.getName());
        return toShopResponse(shop);
    }

    @Transactional
    public void incrementQrVisits(String slug) {
        log.info("incrementQrVisits | slug={}", slug);
        Shop shop = shopRepository.findBySlugAndIsApprovedTrue(slug)
                .orElseThrow(() -> {
                    log.warn("incrementQrVisits | Shop NOT FOUND | slug={}", slug);
                    return new ResourceNotFoundException("Shop", "slug", slug);
                });
        shop.setQrVisits(shop.getQrVisits() + 1);
        shopRepository.save(shop);
        log.info("incrementQrVisits | slug={} | newCount={}", slug, shop.getQrVisits());
    }

    //public shop register
    @Transactional
    public ShopResponse registerShop(RegisterShopRequest request) {
        validateUniqueShopFields(request.getSlug(), request.getAdminEmail());

        // Create the admin user — inactive by default, SuperAdmin must approve
        User admin = User.builder()
                .name(request.getAdminName())
                .email(request.getAdminEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .phone(request.getPhone())
                .isActive(false)
                .build();
        admin = userRepository.save(admin);
        log.info("registerShop | Admin user created (INACTIVE) | email={} | userId={}", admin.getEmail(), admin.getId());

        // Create the shop (unapproved)
        Shop shop = Shop.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .description(request.getDescription())
                .isApproved(false)
                .admin(admin)
                .qrVisits(0)
                .build();
        
        String qrUrl = frontendUrl + "/shops/" + request.getSlug();
        shop.setQrCode(qrCodeGenerator.generateQrCodeBase64(qrUrl, 300, 300));
        
        shop = shopRepository.save(shop);

        // Create default requirements
        ShopRequirements requirements = ShopRequirements.builder()
                .shop(shop)
                .build();
        shopRequirementsRepository.save(requirements);
        shop.setRequirements(requirements);

        log.info("registerShop | New shop registration: {} (slug: {}, unapproved, admin inactive)", shop.getName(), shop.getSlug());
        return toShopResponse(shop);
    }

    // ===== Admin endpoints =====

    @Transactional(readOnly = true)
    public ShopResponse getShopById(UUID shopId) {
        log.info("getShopById | shopId={}", shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> {
                    log.warn("getShopById | NOT FOUND | shopId={}", shopId);
                    return new ResourceNotFoundException("Shop", "id", shopId);
                });
        log.info("getShopById | FOUND | name={}", shop.getName());
        return toShopResponse(shop);
    }

    @Transactional
    public ShopResponse updateShopProfile(UUID shopId, ShopProfileUpdateRequest request) {
        log.info("updateShopProfile | shopId={}", shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> {
                    log.warn("updateShopProfile | NOT FOUND | shopId={}", shopId);
                    return new ResourceNotFoundException("Shop", "id", shopId);
                });

        if (request.getName() != null) shop.setName(request.getName());
        if (request.getAddress() != null) shop.setAddress(request.getAddress());
        if (request.getPhone() != null) shop.setPhone(request.getPhone());
        if (request.getEmail() != null) shop.setEmail(request.getEmail());
        if (request.getDescription() != null) shop.setDescription(request.getDescription());

        if (request.getRequirements() != null) {
            ShopRequirements req = shop.getRequirements();
            if (req == null) {
                req = ShopRequirements.builder().shop(shop).build();
            }
            ShopProfileUpdateRequest.RequirementsUpdate reqUpdate = request.getRequirements();
            if (reqUpdate.getAcceptedFormats() != null) {
                req.setAcceptedFormats(String.join(",", reqUpdate.getAcceptedFormats()));
            }
            if (reqUpdate.getMaxFileSizeMb() != null) req.setMaxFileSizeMb(reqUpdate.getMaxFileSizeMb());
            if (reqUpdate.getMaxFilesPerJob() != null) req.setMaxFilesPerJob(reqUpdate.getMaxFilesPerJob());
            if (reqUpdate.getPricePerPageBW() != null) req.setPricePerPageBw(reqUpdate.getPricePerPageBW());
            if (reqUpdate.getPricePerPageColor() != null) req.setPricePerPageColor(reqUpdate.getPricePerPageColor());
            shopRequirementsRepository.save(req);
            shop.setRequirements(req);
        }

        shop = shopRepository.save(shop);
        log.info("Shop profile updated: {}", shop.getId());
        return toShopResponse(shop);
    }

    // ===== Super Admin endpoints =====

    @Transactional(readOnly = true)
    public Page<ShopResponse> getAllShops(Pageable pageable) {
        log.info("getAllShops | page={} | size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<ShopResponse> result = shopRepository.findAll(pageable).map(this::toShopResponse);
        log.info("getAllShops | returned {} of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Transactional
    public ShopResponse createShopByAdmin(ShopCreateRequest request) {
        validateUniqueShopFields(request.getSlug(), request.getAdminEmail());

        // SuperAdmin-created shops are pre-approved, so admin user is active immediately
        User admin = User.builder()
                .name(request.getAdminName())
                .email(request.getAdminEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .phone(request.getPhone())
                .isActive(true)
                .build();
        admin = userRepository.save(admin);
        log.info("createShopByAdmin | Admin user created (ACTIVE) | email={}", admin.getEmail());

        Shop shop = Shop.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .description(request.getDescription())
                .isApproved(true) // Pre-approved when created by Super Admin
                .admin(admin)
                .qrVisits(0)
                .build();
                
        String qrUrl = frontendUrl + "/shops/" + request.getSlug();
        shop.setQrCode(qrCodeGenerator.generateQrCodeBase64(qrUrl, 300, 300));
        
        shop = shopRepository.save(shop);

        ShopRequirements requirements = ShopRequirements.builder()
                .shop(shop)
                .build();
        shopRequirementsRepository.save(requirements);
        shop.setRequirements(requirements);

        log.info("Shop created by Super Admin: {} (slug: {}, pre-approved, admin active)", shop.getName(), shop.getSlug());
        return toShopResponse(shop);
    }

    @Transactional
    public ShopResponse approveShop(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
        shop.setIsApproved(true);
        shopRepository.save(shop);

        // Activate the admin user so they can log in
        User admin = shop.getAdmin();
        admin.setIsActive(true);
        userRepository.save(admin);

        log.info("Shop approved: {} | Admin user activated: {}", shopId, admin.getEmail());
        return toShopResponse(shop);
    }

    @Transactional
    public ShopResponse disapproveShop(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
        shop.setIsApproved(false);
        shopRepository.save(shop);

        // Deactivate the admin user so they cannot log in
        User admin = shop.getAdmin();
        admin.setIsActive(false);
        userRepository.save(admin);

        log.info("Shop disapproved: {} | Admin user deactivated: {}", shopId, admin.getEmail());
        return toShopResponse(shop);
    }

    @Transactional
    public void rejectShop(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
        User admin = shop.getAdmin();

        // Delete shop first (cascades to requirements), then delete the admin user
        shopRepository.delete(shop);
        userRepository.delete(admin);
        log.info("Shop rejected and deleted: {} | Admin user deleted: {}", shopId, admin.getEmail());
    }

    @Transactional
    public void deleteShop(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
        User admin = shop.getAdmin();

        // Delete shop first (cascades to requirements), then delete the admin user
        shopRepository.delete(shop);
        userRepository.delete(admin);
        log.info("Shop hard deleted: {} | Admin user deleted: {}", shopId, admin.getEmail());
    }

    // ===== Helpers =====

    private void validateUniqueShopFields(String slug, String adminEmail) {
        if (shopRepository.existsBySlug(slug)) {
            log.warn("Validation FAILED | Duplicate slug={}", slug);
            throw new BadRequestException("A shop with slug '" + slug + "' already exists");
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.warn("Validation FAILED | Duplicate admin email={}", adminEmail);
            throw new BadRequestException("A user with email '" + adminEmail + "' already exists");
        }
    }

    public ShopResponse toShopResponse(Shop shop) {
        User admin = shop.getAdmin();
        ShopRequirements req = shop.getRequirements();

        ShopRequirementsResponse reqResponse = null;
        if (req != null) {
            List<String> formats = Arrays.asList(req.getAcceptedFormats().split(","));
            reqResponse = ShopRequirementsResponse.builder()
                    .acceptedFormats(formats)
                    .maxFileSizeMb(req.getMaxFileSizeMb())
                    .maxFilesPerJob(req.getMaxFilesPerJob())
                    .pricePerPageBW(req.getPricePerPageBw())
                    .pricePerPageColor(req.getPricePerPageColor())
                    .build();
        }

        return ShopResponse.builder()
                .id(shop.getId().toString())
                .name(shop.getName())
                .slug(shop.getSlug())
                .address(shop.getAddress())
                .phone(shop.getPhone())
                .email(shop.getEmail())
                .description(shop.getDescription())
                .adminIsActive(admin != null ? admin.getIsActive() : null)
                .isApproved(shop.getIsApproved())
                .adminId(admin != null ? admin.getId().toString() : null)
                .adminEmail(admin != null ? admin.getEmail() : null)
                .adminName(admin != null ? admin.getName() : null)
                .createdAt(shop.getCreatedAt())
                .qrVisits(shop.getQrVisits())
                .qrCode(shop.getQrCode())
                .requirements(reqResponse)
                .build();
    }
}
