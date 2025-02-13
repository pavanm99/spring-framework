/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Pointcut and method matcher for use as a simple <b>cflow</b>-style pointcut.
 *
 * <p>Note that evaluating such pointcuts is 10-15 times slower than evaluating
 * normal pointcuts, but they are useful in some cases.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("serial")
public class ControlFlowPointcut implements Pointcut, ClassFilter, MethodMatcher, Serializable {

	/**
	 * The class against which to match.
	 * <p>Available for use in subclasses since 6.1.
	 */
	protected final Class<?> clazz;

	/**
	 * The method against which to match, potentially {@code null}.
	 * <p>Available for use in subclasses since 6.1.
	 */
	@Nullable
	protected final String methodName;

	private final AtomicInteger evaluationCount = new AtomicInteger();


	/**
	 * Construct a new pointcut that matches all control flows below the given class.
	 * @param clazz the class
	 */
	public ControlFlowPointcut(Class<?> clazz) {
		this(clazz, null);
	}

	/**
	 * Construct a new pointcut that matches all calls below the given method
	 * in the given class.
	 * <p>If no method name is given, the pointcut matches all control flows
	 * below the given class.
	 * @param clazz the class
	 * @param methodName the name of the method (may be {@code null})
	 */
	public ControlFlowPointcut(Class<?> clazz, @Nullable String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		this.clazz = clazz;
		this.methodName = methodName;
	}


	/**
	 * Subclasses can override this for greater filtering (and performance).
	 * <p>The default implementation always returns {@code true}.
	 */
	@Override
	public boolean matches(Class<?> clazz) {
		return true;
	}

	/**
	 * Subclasses can override this if it's possible to filter out some candidate classes.
	 * <p>The default implementation always returns {@code true}.
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

	@Override
	public boolean isRuntime() {
		return true;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		incrementEvaluationCount();

		for (StackTraceElement element : new Throwable().getStackTrace()) {
			if (element.getClassName().equals(this.clazz.getName()) &&
					(this.methodName == null || element.getMethodName().equals(this.methodName))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the number of times {@link #matches(Method, Class, Object...)} has been
	 * evaluated.
	 * <p>Useful for optimization and testing purposes.
	 */
	public int getEvaluations() {
		return this.evaluationCount.get();
	}

	/**
	 * Increment the {@link #getEvaluations() evaluation count}.
	 * @since 6.1
	 * @see #matches(Method, Class, Object...)
	 */
	protected final void incrementEvaluationCount() {
		this.evaluationCount.incrementAndGet();
	}


	@Override
	public ClassFilter getClassFilter() {
		return this;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ControlFlowPointcut that &&
				this.clazz.equals(that.clazz)) &&
				ObjectUtils.nullSafeEquals(this.methodName, that.methodName));
	}

	@Override
	public int hashCode() {
		int code = this.clazz.hashCode();
		if (this.methodName != null) {
			code = 37 * code + this.methodName.hashCode();
		}
		return code;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": class = " + this.clazz.getName() + "; methodName = " + this.methodName;
	}

}
