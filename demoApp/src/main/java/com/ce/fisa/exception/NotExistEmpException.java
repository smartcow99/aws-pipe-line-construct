package com.ce.fisa.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NotExistEmpException extends Exception{
	
	public NotExistEmpException(String msg) {
		super(msg); //getMessage() 데이터 활용
	}
	
}
