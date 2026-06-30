package com.example.financetracker.finance;

import com.example.financetracker.finance.category.Category;
import com.example.financetracker.finance.category.CategoryRepository;
import com.example.financetracker.finance.category.OperationType;
import com.example.financetracker.finance.client.AuthUserResponse;
import com.example.financetracker.finance.client.AuthUsersClient;
import com.example.financetracker.finance.group.FamilyGroup;
import com.example.financetracker.finance.group.FamilyGroupRepository;
import com.example.financetracker.finance.group.FamilyMember;
import com.example.financetracker.finance.group.FamilyMemberRepository;
import com.example.financetracker.finance.group.FamilyRole;
import com.example.financetracker.finance.operation.Operation;
import com.example.financetracker.finance.operation.OperationRepository;
import com.example.financetracker.finance.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceControllerIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OUTSIDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private FamilyGroupRepository groupRepository;

    @Autowired
    private FamilyMemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OperationRepository operationRepository;

    @BeforeEach
    void setUp() {
        operationRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
        groupRepository.deleteAll();
    }

    @Test
    void createGroupAddsCurrentUserAsOwner() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER_ID, "owner@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Demo family ",
                                  "description": " test group "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Demo family"))
                .andExpect(jsonPath("$.ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.members[0].userId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"));

        assertThat(memberRepository.findAllByUserId(OWNER_ID))
                .singleElement()
                .extracting(FamilyMember::getRole)
                .isEqualTo(FamilyRole.OWNER);
    }

    @Test
    void ownerCanAddMemberButRegularMemberCannot() throws Exception {
        FamilyGroup group = saveGroupWithMember(OWNER_ID, FamilyRole.OWNER);

        mockMvc.perform(post("/api/v1/groups/{groupId}/members", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER_ID, "owner@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "member@example.com",
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(MEMBER_ID.toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(post("/api/v1/groups/{groupId}/members", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(MEMBER_ID, "member@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "outsider@example.com",
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only owner or admin can manage group members"));
    }

    @Test
    void createPersonalCategoryRejectsDuplicateNameForSameUserAndType() throws Exception {
        String token = bearerToken(OWNER_ID, "owner@example.com");

        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Groceries ",
                                  "type": "EXPENSE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.ownerUserId").value(OWNER_ID.toString()));

        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "groceries",
                                  "type": "EXPENSE"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createOperationNormalizesCurrencyAndFiltersVisibleOperations() throws Exception {
        Category ownCategory = saveCategory("Groceries", OperationType.EXPENSE, OWNER_ID, null);
        Category outsiderCategory = saveCategory("Books", OperationType.EXPENSE, OUTSIDER_ID, null);

        mockMvc.perform(post("/api/v1/operations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER_ID, "owner@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "type": "EXPENSE",
                                  "amount": 42.50,
                                  "currency": "usd",
                                  "operationDate": "2026-06-28",
                                  "description": " lunch "
                                }
                                """.formatted(ownCategory.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.amount").value(42.50));

        mockMvc.perform(post("/api/v1/operations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OUTSIDER_ID, "outsider@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "type": "EXPENSE",
                                  "amount": 15.00,
                                  "currency": "USD",
                                  "operationDate": "2026-06-28"
                                }
                                """.formatted(outsiderCategory.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/operations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER_ID, "owner@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("lunch"));
    }

    @Test
    void personalOperationCannotUseAnotherUsersCategory() throws Exception {
        Category outsiderCategory = saveCategory("Private", OperationType.EXPENSE, OUTSIDER_ID, null);

        mockMvc.perform(post("/api/v1/operations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER_ID, "owner@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "type": "EXPENSE",
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "operationDate": "2026-06-28"
                                }
                                """.formatted(outsiderCategory.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Personal operation can use only personal or default categories"));
    }

    @Test
    void cannotViewOrUpdateAnotherUsersPersonalOperation() throws Exception {
        Category ownerCategory = saveCategory("Private", OperationType.EXPENSE, OWNER_ID, null);
        Operation operation = saveOperation(OWNER_ID, null, ownerCategory.getId(), OperationType.EXPENSE);

        mockMvc.perform(get("/api/v1/operations/{operationId}", operation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OUTSIDER_ID, "outsider@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this operation"));

        mockMvc.perform(patch("/api/v1/operations/{operationId}", operation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OUTSIDER_ID, "outsider@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 99.00
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot manage another user's personal operation"));
    }

    @Test
    void outsiderCannotAccessGroup() throws Exception {
        FamilyGroup group = saveGroupWithMember(OWNER_ID, FamilyRole.OWNER);

        mockMvc.perform(get("/api/v1/groups/{groupId}", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OUTSIDER_ID, "outsider@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this group"));

        mockMvc.perform(get("/api/v1/operations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OUTSIDER_ID, "outsider@example.com"))
                        .param("groupId", group.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this group"));
    }

    @Test
    void memberCannotManageMembersOrGroupCategories() throws Exception {
        FamilyGroup group = saveGroupWithMember(OWNER_ID, FamilyRole.OWNER);
        addMember(group.getId(), MEMBER_ID, FamilyRole.MEMBER);
        Category groupCategory = saveCategory("Shared food", OperationType.EXPENSE, null, group.getId());

        mockMvc.perform(post("/api/v1/groups/{groupId}/members", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(MEMBER_ID, "member@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "outsider@example.com",
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only owner or admin can manage group members"));

        mockMvc.perform(put("/api/v1/categories/{categoryId}", groupCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(MEMBER_ID, "member@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated shared food",
                                  "type": "EXPENSE"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only group owner or admin can manage group categories"));
    }

    @Test
    void createOperationValidatesAmountCurrencyAndDate() throws Exception {
        Category category = saveCategory("Groceries", OperationType.EXPENSE, OWNER_ID, null);
        String token = bearerToken(OWNER_ID, "owner@example.com");

        mockMvc.perform(post("/api/v1/operations")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "type": "EXPENSE",
                                  "amount": -1.00,
                                  "currency": "USD",
                                  "operationDate": "2026-06-28"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").value("must be greater than 0.0"));

        mockMvc.perform(post("/api/v1/operations")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "type": "EXPENSE",
                                  "amount": 10.00,
                                  "currency": "USDD",
                                  "operationDate": "2026-06-28"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.currency").value("size must be between 3 and 3"));

        mockMvc.perform(post("/api/v1/operations")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "type": "EXPENSE",
                                  "amount": 10.00,
                                  "currency": "USD"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.operationDate").value("must not be null"));
    }

    @Test
    void defaultCategoriesCannotBeUpdatedOrDeleted() throws Exception {
        Category defaultCategory = saveDefaultCategory("Food", OperationType.EXPENSE);
        String token = bearerToken(OWNER_ID, "owner@example.com");

        mockMvc.perform(put("/api/v1/categories/{categoryId}", defaultCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated food",
                                  "type": "EXPENSE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Default categories cannot be updated"));

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", defaultCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Default categories cannot be deleted"));
    }

    @Test
    void internalReportEndpointRequiresInternalToken() throws Exception {
        mockMvc.perform(get("/internal/operations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER_ID, "owner@example.com"))
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/internal/operations")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER_ID, "owner@example.com"))
                        .header("X-Internal-Token", "test-internal-token")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk());
    }

    private FamilyGroup saveGroupWithMember(UUID userId, FamilyRole role) {
        FamilyGroup group = new FamilyGroup();
        group.setName("Group");
        group.setOwnerUserId(userId);
        FamilyGroup savedGroup = groupRepository.save(group);

        FamilyMember member = new FamilyMember();
        member.setGroupId(savedGroup.getId());
        member.setUserId(userId);
        member.setRole(role);
        memberRepository.save(member);
        return savedGroup;
    }

    private FamilyMember addMember(UUID groupId, UUID userId, FamilyRole role) {
        FamilyMember member = new FamilyMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        return memberRepository.save(member);
    }

    private Category saveCategory(String name, OperationType type, UUID ownerUserId, UUID groupId) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setOwnerUserId(ownerUserId);
        category.setGroupId(groupId);
        category.setDefault(false);
        return categoryRepository.save(category);
    }

    private Category saveDefaultCategory(String name, OperationType type) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setDefault(true);
        return categoryRepository.save(category);
    }

    private Operation saveOperation(UUID userId, UUID groupId, UUID categoryId, OperationType type) {
        Operation operation = new Operation();
        operation.setUserId(userId);
        operation.setGroupId(groupId);
        operation.setCategoryId(categoryId);
        operation.setType(type);
        operation.setAmount(new BigDecimal("10.00"));
        operation.setCurrency("USD");
        operation.setOperationDate(LocalDate.parse("2026-06-28"));
        operation.setDescription("Private operation");
        return operationRepository.save(operation);
    }

    private String bearerToken(UUID userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claim("userId", userId.toString())
                .claim("email", email)
                .claim("role", "ROLE_USER")
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }

    @TestConfiguration
    static class TestAuthUsersClientConfig {

        @Bean
        @Primary
        AuthUsersClient authUsersClient() {
            return new AuthUsersClient(null, null) {
                @Override
                public void requireUserExists(UUID userId) {
                    // Tests exercise finance-service behavior without calling auth-service.
                }

                @Override
                public AuthUserResponse getUserByEmail(String email) {
                    return switch (email) {
                        case "owner@example.com" -> new AuthUserResponse(OWNER_ID, email);
                        case "member@example.com" -> new AuthUserResponse(MEMBER_ID, email);
                        case "outsider@example.com" -> new AuthUserResponse(OUTSIDER_ID, email);
                        default -> new AuthUserResponse(UUID.fromString("00000000-0000-0000-0000-000000000099"), email);
                    };
                }
            };
        }
    }
}
