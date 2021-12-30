package com.dailu.nioclient.exception;

public class CustomRuntimeException extends RuntimeException{

    public CustomRuntimeException(String message){
        super(message);
    }

    public CustomRuntimeException(String message,Exception e){
        super(message,e);
    }
}
