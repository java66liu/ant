/*
 * Copyright  2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.svn;

import java.io.File;

import org.apache.tools.ant.BuildFileTest;

/**
 * @version $Revision$
 */
public class AbstractSvnTaskTest extends BuildFileTest {

    public AbstractSvnTaskTest() {
        this( "AbstractCvsTaskTest" );
    }

    public AbstractSvnTaskTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/abstractsvntask.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testAbstractCvsTask() {
        executeTarget("all");
    }

    public void testRevisionAttribute() {
        File f = getProject().resolveFile("tmpdir/tpf/ebcdic.h");
        assertTrue("starting empty", !f.exists());
        expectLogContaining("revision-attribute", "A  tpf/ebcdic.h");
        assertTrue("now it is there", f.exists());
    }
}