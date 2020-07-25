package com.frejdh.util.concurrency;

import com.frejdh.util.common.AnsiColor;
import com.frejdh.util.common.AnsiLogger;
import com.frejdh.util.common.annotations.Warning;
import com.frejdh.util.concurrency.exceptions.ThreadNotYetStartedException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Executor of threads with an optional callback method running on the main thread.
 */
@SuppressWarnings({"WeakerAccess", "unused", "BooleanMethodIsAlwaysInverted"})
public class ThreadExecutor extends Thread {

	/**
	 * Override the callback() to set an optional callback method (for the main thread).
	 */
	public interface Callback {
		void callback();
	}

	public enum ThreadState {
		INITIALIZED,
		RUNNING_TASK,
		RUNNING_CALLBACK,
		FINISHED
	}

	private Runnable runnable; // this::placeholderMethod;
	private Callback callback = this::placeholderMethod;
	private ThreadState stateOfThread;

	// Default values. Also referenced in javadoc
	public static final String DEFAULT_THREAD_NAME = "undefined-thread";
	private static final boolean DEFAULT_GLOBAL_PRINT_STACKTRACE = true;
	private static final boolean DEFAULT_GLOBAL_PRINT_STATUS = false;

	// Debugging options
	private boolean isPrintingStacktrace;
	private boolean isPrintingStatus;
	private volatile static boolean isPrintingStacktraceDefault = DEFAULT_GLOBAL_PRINT_STACKTRACE;
	private volatile static boolean isPrintingStatusDefault = DEFAULT_GLOBAL_PRINT_STATUS;

	@SuppressWarnings("FieldCanBeLocal")
	private volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler = (thread, exception) -> {
		AnsiLogger.error("Thread ", AnsiColor.BRIGHT_BLUE, this.getName(), AnsiColor.DEFAULT, " caught an unhandled exception: " + exception.getClass().getCanonicalName());
		System.out.flush();
		if (isPrintingStacktrace)
			exception.printStackTrace();
	};

	/**
	 * Execute in a new thread without a callback method. Recommended to override parameters methods with lambdas.
	 * <br><br>
	 * Method override example:<br>
	 * ThreadExecutor executor = new ThreadExecutor(<br>
	 * &emsp;() -> System.out.println("Test");<br>
	 * );
	 *
	 * @param threadName Name of the thread (optional)
	 * @param runnable What to execute
	 */
	public ThreadExecutor(@Nullable String threadName, @NonNull Runnable runnable) {
		this.runnable = runnable;
		setName(threadName != null ? threadName : DEFAULT_THREAD_NAME);
		this.isPrintingStatus = isPrintingStatusDefault();
		this.isPrintingStacktrace = isPrintingStacktraceDefault();
		super.setUncaughtExceptionHandler(defaultUncaughtExceptionHandler);
		this.stateOfThread = ThreadState.INITIALIZED;
	}

	/**
	 * Execute in a new thread without a callback method. Recommended to override parameters methods with lambdas.
	 * <br><br>
	 * Method override example:<br>
	 * ThreadExecutor executor = new ThreadExecutor(<br>
	 * &emsp;() -> System.out.println("Test");<br>
	 * );
	 *
	 * @param runnable What to execute
	 */
	public ThreadExecutor(@NonNull Runnable runnable) {
		this(null, runnable);
	}

	/**
	 * Execute in a new thread with a given callback method. Recommended to override parameters methods with lambdas.
	 * <br><br>
	 * Method override example:<br>
	 * ThreadExecutor executor = new ThreadExecutor(<br>
	 * &emsp;() -> { System.out.println("Test"); }<br>
	 * );
	 *
	 * @param threadName Name of the thread (optional)
	 * @param runnable What to execute
	 * @param callback Callback method once finished
	 */
	public ThreadExecutor(@Nullable String threadName, @NonNull Runnable runnable, @NonNull Callback callback) {
		this(null, runnable);
		this.callback = callback;
	}

	/**
	 * Execute in a new thread with a given callback method. Recommended to override parameters methods with lambdas.
	 * <br><br>
	 * Method override example:<br>
	 * ThreadExecutor executor = new ThreadExecutor(<br>
	 * &emsp;() -> { System.out.println("Test"); }<br>
	 * );
	 *
	 * @param runnable What to execute
	 * @param callback Callback method once finished
	 */
	public ThreadExecutor(@NonNull Runnable runnable, @NonNull Callback callback) {
		this(null, runnable, callback);
	}

	/**
	 * This methods runs the runnable and executes the callback method.
	 * Use {@link #start()} instead to execute the runnable since this method alone won't execute on a new thread.
	 */
	@Override
	@Warning("Don't use this method directly. Use start() or execute() instead")
	public final void run() {

		if (isPrintingStatus)
			AnsiLogger.information(AnsiColor.GREEN, "INFORMATION ", AnsiColor.DEFAULT, "The thread ", AnsiColor.BRIGHT_BLUE, getName(), AnsiColor.DEFAULT, " has started");

		try {
			setStateOfThread(ThreadState.RUNNING_TASK);
			runnable.run();

			if (isPrintingStatus)
				AnsiLogger.information("The thread ", AnsiColor.BRIGHT_BLUE, getName(), AnsiColor.DEFAULT, " has finished its primary task. Executing callback...");

			setStateOfThread(ThreadState.RUNNING_CALLBACK);
			callback.callback();

			setStateOfThread(ThreadState.FINISHED);
			if (isPrintingStatus)
				AnsiLogger.information("The thread ", AnsiColor.BRIGHT_BLUE, getName(), AnsiColor.DEFAULT, " has finished");

		} catch (Exception e) {
			getUncaughtExceptionHandler().uncaughtException(this, e);
		}
	}

	/**
	 * The same as {@link #start()}.
	 */
	public void execute() {
		start();
	}

	/**
	 * Check if the thread has finished or not
	 *
	 * @return True if the thread is finished, otherwise false
	 * @throws ThreadNotYetStartedException If the thread never was started before calling this method
	 */
	public boolean isThreadFinished() throws ThreadNotYetStartedException {
		if (ThreadState.INITIALIZED.equals(stateOfThread))
			throw new ThreadNotYetStartedException("Thread not yet started");
		return ThreadState.FINISHED.equals(stateOfThread);
	}

	/**
	 * Join() with ThreadNotYetStartedException.
	 */
	public void joinWithException() throws ThreadNotYetStartedException {
		try {
			if (!isThreadStarted())
				throw new ThreadNotYetStartedException("Thread not yet started");
			this.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Empty method used as a placeholder.
	 */
	private void placeholderMethod() {
	}

	/**
	 * Alternative method to {@link #setName} that is nullable.
	 *
	 * @param threadName Name of the thread, null resets the name to the default value {@value #DEFAULT_THREAD_NAME}.
	 */
	public synchronized void setThreadName(@Nullable String threadName) {
		if (threadName == null)
			setName(DEFAULT_THREAD_NAME);
		else
			setName(threadName);
	}

	/**
	 * Sets an exception handler for uncaught exceptions.
	 * <br><br>
	 * Example of the variable being set with lambda:<br>
	 * ThreadExecutor executor = new ThreadExecutor(<br>
	 * &emsp;() -> { System.out.println("Init"); }<br>
	 * );<br>
	 * setUncaughtExceptionHandler(<br>
	 * &emsp;(thread, exception) -> { exception.printStackTrace(); }<br>
	 * );
	 *
	 * @param uncaughtExceptionHandler Exception handler to be used. Or 'null' if none
	 */
	@Override
	public synchronized void setUncaughtExceptionHandler(@Nullable UncaughtExceptionHandler uncaughtExceptionHandler) {
		super.setUncaughtExceptionHandler(uncaughtExceptionHandler);
	}

	@Override
	public synchronized UncaughtExceptionHandler getUncaughtExceptionHandler() {
		return super.getUncaughtExceptionHandler();
	}

	/**
	 * Check whether the thread will print a stacktrace in the default exception handler or not. Default value is {@value DEFAULT_GLOBAL_PRINT_STACKTRACE}.
	 *
	 * @return True if it will print the stacktrace, otherwise false
	 */
	public synchronized boolean isPrintingStacktrace() {
		return isPrintingStacktrace;
	}

	public synchronized boolean isThreadStarted() {
		return !stateOfThread.equals(ThreadState.INITIALIZED);
	}

	private synchronized void setStateOfThread(@NotNull ThreadState state) {
		this.stateOfThread = state;
	}

	public synchronized ThreadState getStateOfThread() {
		return stateOfThread;
	}

	/**
	 * Set whether the thread should print a stacktrace in the default exception handler or not for THIS instance.
	 * Default value is {@value DEFAULT_GLOBAL_PRINT_STACKTRACE}.
	 *
	 * @param printingStacktrace The new value
	 */
	public synchronized void setPrintingStacktrace(boolean printingStacktrace) {
		this.isPrintingStacktrace = printingStacktrace;
	}

	/**
	 * Check if the thread statuses will be printed or not during execution for THIS instance.
	 * Default is the value of the {@link #isPrintingStatusDefault()} method during the constructor call.
	 *
	 * @return A boolean on whether the thread status will be printed or not
	 */
	public synchronized boolean isPrintingStatus() {
		return isPrintingStatus;
	}

	public synchronized void setPrintingStatus(boolean printingStatus) {
		isPrintingStatus = printingStatus;
	}

	/**
	 * Check if the thread statuses will be printed by default or not for ALL instances.
	 * Default value is {@value DEFAULT_GLOBAL_PRINT_STATUS}.
	 *
	 * @return A boolean on whether the thread status will be printed or not by default
	 */
	public synchronized static boolean isPrintingStatusDefault() {
		return isPrintingStatusDefault;
	}

	/**
	 * Set whether the thread should print its statuses or not by default for ALL instances.
	 * The initialising value is {@value DEFAULT_GLOBAL_PRINT_STATUS}.
	 * This is a volatile variable and will affect all new instances of this class.
	 * This will only affect the default values of future instances and not any pre-existing ones.
	 *
	 * @param isPrintingStatusDefault True if all new instances should print the statuses by default, else false
	 */
	public synchronized static void setPrintingStatusDefault(boolean isPrintingStatusDefault) {
		ThreadExecutor.isPrintingStatusDefault = isPrintingStatusDefault;
	}

	/**
	 * Check if the thread stacktraces will be printed by default or not for ALL instances.
	 * Default value is {@value DEFAULT_GLOBAL_PRINT_STACKTRACE}.
	 *
	 * @return A boolean on whether the thread stacktraces will be printed or not by default
	 */
	public synchronized static boolean isPrintingStacktraceDefault() {
		return isPrintingStatusDefault;
	}

	/**
	 * Set whether the thread should print its stacktraces or not by default for ALL instances.
	 * The initialising value is {@value DEFAULT_GLOBAL_PRINT_STACKTRACE}.
	 * This is a volatile variable and will affect all new instances of this class.
	 * This will only affect the default values of future instances and not any pre-existing ones.
	 *
	 * @param isPrintingStatusDefault True if all new instances should print the stacktraces by default, else false
	 */
	public synchronized static void setPrintingStacktraceDefault(boolean isPrintingStatusDefault) {
		ThreadExecutor.isPrintingStatusDefault = isPrintingStatusDefault;
	}


}
