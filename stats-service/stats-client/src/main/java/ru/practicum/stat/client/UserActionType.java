package ru.practicum.stat.client;

public enum UserActionType {

    VIEW(0),
    REGISTER(1),
    LIKE(2);

    private final int protoNumber;

    UserActionType(int protoNumber) {
        this.protoNumber = protoNumber;
    }

    public int getProtoNumber() {
        return protoNumber;
    }
}