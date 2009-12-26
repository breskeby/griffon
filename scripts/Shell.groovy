/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Gant script that loads the Griffon interactive shell
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

//import org.codehaus.griffon.support.*
import org.codehaus.groovy.tools.shell.*

includeTargets << griffonScript("_GriffonBootstrap")

target (runShell: "Load the Griffon interactive shell") {
	depends( configureProxy, packageApp )
    jardir = ant.antProject.replaceProperties(config.griffon.jars.destDir)
    ant.copy(todir:jardir) { fileset(dir:"${griffonHome}/lib/", includes:"jline-*.jar") }
    classpath()
	shell()
}

target(default:"Runs an embedded application in a Groovy Shell") {

//    classLoader = new URLClassLoader([classesDir.toURI().toURL()] as URL[], rootLoader)
//    Thread.currentThread().setContextClassLoader(classLoader)
    loadApp()
    configureApp()
    def b = new Binding()
//    b.ctx = appCtx
//    b.griffonApplication = griffonApp

//    def original = Groovysh.metaClass.getMetaMethod("execute", [String] as Object[])
//    Groovysh.metaClass.execute = { String line ->
//        try {
//            def listeners = appCtx.getBeansOfType(PersistenceContextInterceptor)
//            listeners.each { k,v ->
//                v.init()
//            }
//
//            original.invoke(delegate, line)
//            listeners.each { k,v ->
//                v.flush()
//            }
//        }
//    finally {
//            listeners.each { k,v ->
//                v.destroy()
//            }
//        }
//    }

    def shell = new Groovysh(classLoader,b, new IO(System.in, System.out, System.err))
	shell.run([] as String[])
}
