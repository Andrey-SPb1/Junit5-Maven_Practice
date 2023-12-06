package com.junit.dao;

public class UserDaoMock extends UserDao {

    @Override
    public boolean delete(Integer userId) {
        return false;
    }
}
