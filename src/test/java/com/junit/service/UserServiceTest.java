package com.junit.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {
    public static void main(String[] args) {
        System.out.println();
    }
    @Test
    void usersEmptyIfNoUsersAdded() {
        var userService = new UserService();
        var users = userService.getAll();
        assertFalse(users.isEmpty(), () -> "List of users is empty");
    }
}
