package com.junit.service;

import com.junit.entity.User;
import com.junit.paramresolver.UserServiceParamResolver;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.RepeatedTest.LONG_DISPLAY_NAME;

@Tag("user")
@Tag("fast")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@ExtendWith(
        UserServiceParamResolver.class
)
public class UserServiceTest {

    private static final User IVAN = User.of(1, "Ivan", "213");
    private static final User PETR = User.of(2, "Petr", "421");

    private UserService userService;

    UserServiceTest(TestInfo testInfo) {
        System.out.println();
    }

    @BeforeAll
    void init() {
        System.out.println("Before all: " + this);
    }

    @BeforeEach
    void prepare(UserService userService) {
        System.out.println("Before each: " + this);
        this.userService = userService;
    }

    @Test
    @Order(1)
    @DisplayName("Users will be empty if no user added")
    void usersEmptyIfNoUsersAdded(UserService userService) {
        System.out.println("Test 1: " + this);
        var users = userService.getAll();

//        MatcherAssert.assertThat(users, empty());
        assertTrue(users.isEmpty(), "List of users is empty");
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(IVAN, PETR);

        Map<Integer, User> users = userService.getAllConvertedById();

//        MatcherAssert.assertThat(users, IsMapContaining.hasKey(IVAN.getId()));
        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
                () -> assertThat(users).containsValues(IVAN, PETR)
        );

    }

    @Test
    @Order(2)
    void usersSizeIfUsersAdded() {
        System.out.println("Test 2: " + this);
        userService.add(IVAN);
        userService.add(PETR);

        var users = userService.getAll();

        assertThat(users).hasSize(2);
//        assertEquals(2, users.size());
    }

    @AfterEach
    void deleteDataFromDatabase() {
        System.out.println("After each: " + this);
    }

    @AfterAll
    void closeConnectionPool() {
        System.out.println("After all: " + this);
    }

    @Nested
    @DisplayName("Test user login functionality")
    @Tag("login")
//    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    class LoginTest {
        @Test
        @Disabled("flaky, need to see")
        void loginFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUsername(), "test");
            assertTrue(maybeUser.isEmpty());
        }

//        @Test
        @RepeatedTest(value = 5, name = LONG_DISPLAY_NAME)
        void loginFailIfUserDoesNotExist(RepetitionInfo repetitionInfo) {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login("test", IVAN.getPassword());
            assertTrue(maybeUser.isEmpty());
        }

        @Test
        void CheckLoginFunctionalityPerformance() {
            System.out.println(Thread.currentThread().getName());
            var optionalUser = assertTimeoutPreemptively(Duration.ofMillis(200L), () -> {
                System.out.println(Thread.currentThread().getName());
                Thread.sleep(300L);
                return userService.login("test", IVAN.getPassword());
            });
        }

        @Test
        void loginSuccessIfUserExists() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUsername(), IVAN.getPassword());

            assertThat(maybeUser).isPresent();
            maybeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));

//        assertTrue(maybeUser.isPresent());
//        maybeUser.ifPresent(user -> assertEquals(IVAN, user));
        }

        @Test
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> {
                        var exception = assertThrows(IllegalArgumentException.class, () -> userService.login(null, "test"),
                                "Login should throw exception on null username");
                        assertThat(exception.getMessage()).isEqualTo("Username or password is null");
                    },
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("test", null),
                            "Login should throw exception on null password")
            );
        }

        @ParameterizedTest(name = "{arguments} test")
//        @NullAndEmptySource
//        @ValueSource(
//                strings = {"Ivan", "Petr"}
//        )
//        @CsvSource({
//                "Ivan, 213",
//                "Petr, 421"
//        })
//        @CsvFileSource(resources = "/login-test-data.csv", delimiter = ',', numLinesToSkip = 1)
        @MethodSource("com.junit.service.UserServiceTest#getArgumentsForLoginTest")
        void loginParameterizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, PETR);

            var optionalUser = userService.login(username, password);
            assertThat(optionalUser).isEqualTo(user);
        }

    }

    @DisplayName("login param test")
    static Stream<Arguments> getArgumentsForLoginTest() {
        return Stream.of(
                Arguments.of("Ivan", "213", Optional.of(IVAN)), // Check users
                Arguments.of("Petr", "421", Optional.of(PETR)),
                Arguments.of("Petr", "test", Optional.empty()), // Check username
                Arguments.of("test", "213", Optional.empty()) // Check password
        );
    }
}
