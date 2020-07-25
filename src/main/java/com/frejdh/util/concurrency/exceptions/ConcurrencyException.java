package com.frejdh.util.concurrency.exceptions;

/**
 * To be used for concurrency related exceptions
 */
public class ConcurrencyException extends RuntimeException {

	public ConcurrencyException() {
		super();
	}

	public ConcurrencyException(String msg) {
		super(msg);
	}

	public ConcurrencyException(String msg, Throwable e) {
		super(msg, e);
	}
}
