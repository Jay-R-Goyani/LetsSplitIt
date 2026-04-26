package com.jay.LetsSplitIt;

import com.jay.LetsSplitIt.Entities.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTests {

    private User newUser() {
        User user = new User();
        user.setName("Jay");
        user.setEmail("jay@example.com");
        user.setPassword("secret");
        return user;
    }

    private void invokePrePersist(User user) throws Exception {
        Method m = User.class.getDeclaredMethod("onCreate");
        m.setAccessible(true);
        m.invoke(user);
    }

    private void invokePreUpdate(User user) throws Exception {
        Method m = User.class.getDeclaredMethod("onUpdate");
        m.setAccessible(true);
        m.invoke(user);
    }

    @Test
    void noArgsConstructorCreatesEmptyUserWithDefaultRole() {
        User user = new User();

        assertNull(user.getId());
        assertEquals("USER", user.getRole());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
    }

    @Test
    void settersPopulateRequiredFields() {
        User user = newUser();

        assertEquals("Jay", user.getName());
        assertEquals("jay@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals("USER", user.getRole());
    }

    @Test
    void allArgsConstructorSetsEveryField() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User user = new User(id, "Jay", "jay@example.com", "secret", "ADMIN", null, now, now);

        assertEquals(id, user.getId());
        assertEquals("Jay", user.getName());
        assertEquals("jay@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals("ADMIN", user.getRole());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    void allArgsConstructorRejectsNullName() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class,
                () -> new User(UUID.randomUUID(), null, "jay@example.com", "secret", "USER", null, now, now));
    }

    @Test
    void allArgsConstructorRejectsNullEmail() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class,
                () -> new User(UUID.randomUUID(), "Jay", null, "secret", "USER", null, now, now));
    }

    @Test
    void allArgsConstructorRejectsNullPassword() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class,
                () -> new User(UUID.randomUUID(), "Jay", "jay@example.com", null, "USER", null, now, now));
    }

    @Test
    void setNameRejectsNull() {
        User user = newUser();
        assertThrows(NullPointerException.class, () -> user.setName(null));
    }

    @Test
    void setEmailRejectsNull() {
        User user = newUser();
        assertThrows(NullPointerException.class, () -> user.setEmail(null));
    }

    @Test
    void setPasswordRejectsNull() {
        User user = newUser();
        assertThrows(NullPointerException.class, () -> user.setPassword(null));
    }

    @Test
    void prePersistStampsCreatedAndUpdatedTimestamps() throws Exception {
        User user = newUser();
        Instant before = Instant.now();

        invokePrePersist(user);

        Instant after = Instant.now();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
        assertTrue(!user.getCreatedAt().isBefore(before) && !user.getCreatedAt().isAfter(after));
    }

    @Test
    void prePersistPreservesExistingRole() throws Exception {
        User user = newUser();
        user.setRole("ADMIN");

        invokePrePersist(user);

        assertEquals("ADMIN", user.getRole());
    }

    @Test
    void prePersistAssignsDefaultRoleWhenNull() throws Exception {
        User user = newUser();
        user.setRole(null);

        invokePrePersist(user);

        assertEquals("USER", user.getRole());
    }

    @Test
    void preUpdateRefreshesUpdatedAtOnly() throws Exception {
        User user = newUser();
        invokePrePersist(user);
        Instant originalCreated = user.getCreatedAt();
        Instant originalUpdated = user.getUpdatedAt();

        Thread.sleep(5);
        invokePreUpdate(user);

        assertEquals(originalCreated, user.getCreatedAt());
        assertNotEquals(originalUpdated, user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(originalUpdated));
    }

    @Test
    void equalsAndHashCodeUseAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User a = new User(id, "Jay", "jay@example.com", "secret", "USER", null, now, now);
        User b = new User(id, "Jay", "jay@example.com", "secret", "USER", null, now, now);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.setName("Other");
        assertNotEquals(a, b);
    }
}