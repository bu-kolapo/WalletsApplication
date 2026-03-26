package com.wallets.exception;

import java.math.BigDecimal;

 public  class UsernameAlreadyExistsException extends RuntimeException {
     public UsernameAlreadyExistsException() {
         super("Username already exists ");
     }
 }
