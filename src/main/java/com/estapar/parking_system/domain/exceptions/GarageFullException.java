package com.estapar.parking_system.domain.exceptions;

public class GarageFullException extends RuntimeException {
    public GarageFullException(String message) { super(message); }
}

