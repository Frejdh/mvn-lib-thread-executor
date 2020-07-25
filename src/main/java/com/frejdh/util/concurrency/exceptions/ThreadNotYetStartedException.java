package com.frejdh.util.concurrency.exceptions;

@SuppressWarnings("InnerClassMayBeStatic")
public class ThreadNotYetStartedException extends RuntimeException {
	public ThreadNotYetStartedException() {
		super();
	}

	public ThreadNotYetStartedException(String msg) {
		super(msg);
	}

	public ThreadNotYetStartedException(String msg, Throwable exception) {
		super(msg, exception);
	}

}
