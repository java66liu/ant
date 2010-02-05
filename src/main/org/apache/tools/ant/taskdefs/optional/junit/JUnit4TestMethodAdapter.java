/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 2008 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Iterator;
import java.util.List;
import junit.framework.JUnit4TestAdapterCache;
import junit.framework.Test;
import junit.framework.TestResult;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

/**
 * Adapter between JUnit 3.8.x API and JUnit 4.x API for execution of tests
 * and listening of events (test start, test finish, test failure).
 * The constructor is passed a JUnit 4 test class and a list of name of methods
 * in it that should be executed. Method {@link #run run(TestResult)} executes
 * the given JUnit-4-style test methods and notifies the given {@code TestResult}
 * object using its old (JUnit 3.8.x style) API.
 *
 * @author  Marian Petras
 */
public class JUnit4TestMethodAdapter implements Test {

    private final Class testClass;
    private final String[] methodNames;
    private final Runner runner;
    private final Cache cache;

    /**
     * Creates a new adapter for the given class and a method within the class.
     * 
     * @param testClass test class containing the method to be executed
     * @param methodNames names of the test methods that are to be executed
     * @exception  java.lang.IllegalArgumentException
     *             if any of the arguments is {@code null}
     *             or if any of the given method names is {@code null} or empty
     */
    public JUnit4TestMethodAdapter(final Class testClass,
                                   final String[] methodNames) {
        if (testClass == null) {
            throw new IllegalArgumentException("testClass is <null>");
        }
        if (methodNames == null) {
            throw new IllegalArgumentException("methodNames is <null>");
        }
        for (int i = 0; i < methodNames.length; i++) {
            if (methodNames[i] == null) {
                throw new IllegalArgumentException("method name #" + i + " is <null>");
            }
            if (methodNames[i].length() == 0) {
                throw new IllegalArgumentException("method name #" + i + " is empty");
            }
        }
        this.testClass = testClass;
        this.methodNames = methodNames;
        this.cache = Cache.instance;

        // Warning: If 'testClass' is an old-style (pre-JUnit-4) class,
        // then all its test methods will be executed by the returned runner!
        Request request;
        if (methodNames.length == 1) {
            request = Request.method(testClass, methodNames[0]);
        } else {
            request = Request.aClass(testClass).filterWith(
                            new MultipleMethodsFilter(testClass, methodNames));
        }
        runner = request.getRunner();
    }

    public int countTestCases() {
        return runner.testCount();
    }

    public Description getDescription() {
        return runner.getDescription();
    }

    public List/*<Test>*/ getTests() {
        return cache.asTestList(getDescription());
    }

    public Class getTestClass() {
        return testClass;
    }
    
    public void run(final TestResult result) {
        runner.run(cache.getNotifier(result));
    }

    public String toString() {
        String testClassName = testClass.getName();
        StringBuilder buf = new StringBuilder(testClassName.length()
                                              + 12 * methodNames.length)
                            .append(':');
        if (methodNames.length != 0) {
            buf.append(methodNames[0]);
            for (int i = 1; i < methodNames.length; i++) {
                buf.append(',')
                   .append(methodNames[i]);
            }
        }
        return buf.toString();
    }

    private static final class MultipleMethodsFilter extends Filter {

        private final Description methodsListDescription;
        private final Class testClass;
        private final String[] methodNames;

        private MultipleMethodsFilter(Class testClass, String[] methodNames) {
            if (testClass == null) {
                throw new IllegalArgumentException("testClass is <null>");
            }
            if (methodNames == null) {
                throw new IllegalArgumentException("methodNames is <null>");
            }
            methodsListDescription = Description.createSuiteDescription(testClass);
            for (int i = 0; i < methodNames.length; i++) {
                methodsListDescription.addChild(
                        Description.createTestDescription(testClass, methodNames[i]));
            }
            this.testClass = testClass;
            this.methodNames = methodNames;
        }

        public boolean shouldRun(Description description) {
            if (methodNames.length == 0) {
                return false;
            }
            if (description.isTest()) {
                Iterator/*<Description>*/ it = methodsListDescription.getChildren().iterator();
                while (it.hasNext()) {
                    Description methodDescription = (Description) it.next();
                    if (methodDescription.equals(description)) {
                        return true;
                    }
                }
            } else {
                Iterator/*<Description>*/ it = description.getChildren().iterator();
                while (it.hasNext()) {
                    Description each = (Description) it.next();
                    if (shouldRun(each)) {
                        return true;
                    }
                }
            }
            return false;					
        }

        public String describe() {
            StringBuilder buf = new StringBuilder(40);
            if (methodNames.length == 0) {
                buf.append("No methods");
            } else {
                buf.append(methodNames.length == 1 ? "Method" : "Methods");
                buf.append(' ');
                buf.append(methodNames[0]);
                for (int i = 1; i < methodNames.length; i++) {
                    buf.append(',').append(methodNames[i]);
                }
            }
            buf.append('(').append(testClass.getName()).append(')');
            return buf.toString();
        }

    }

    /**
     * Effectively a copy of {@code JUnit4TestAdapterCache}, except that its
     * method {@code getNotifier()} does not require an argument
     * of type {@code JUnit4TestAdapter}.
     */
    private static final class Cache extends JUnit4TestAdapterCache {

	private static final Cache instance = new Cache();

	public static JUnit4TestAdapterCache getDefault() {
            return instance;
	}
	
	public RunNotifier getNotifier(final TestResult result) {
            RunNotifier notifier = new RunNotifier();
            notifier.addListener(new RunListener() {
                    public void testFailure(Failure failure) throws Exception {
                        result.addError(asTest(failure.getDescription()),
                                        failure.getException());
                    }

                    public void testFinished(Description description)
                                    throws Exception {
                        result.endTest(asTest(description));
                    }

                    public void testStarted(Description description)
                                    throws Exception {
                        result.startTest(asTest(description));
                    }
            });
            return notifier;
	}

    }

}