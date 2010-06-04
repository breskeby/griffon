/*
 * Copyright 2008-2010 the original author or authors.
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
package griffon.util.internal

import griffon.builder.UberBuilder
import griffon.core.GriffonApplication
import griffon.core.ArtifactManager
import griffon.core.ModelArtifactHandler
import griffon.core.ViewArtifactHandler
import griffon.core.ControllerArtifactHandler
import griffon.core.ServiceArtifactHandler
import griffon.util.Metadata
import griffon.util.Environment
import griffon.util.UIThreadHelper
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * Utility class for boostrapping an application and handling of MVC groups.</p>
 * @author Danno Ferrin
 * @author Andres Almiray
 */
class GriffonApplicationHelper {
    /**
     * Setups an application.<p>
     * This method performs the following tasks<ul>
     * <li>Sets "griffon.start.dir" as system property.</li>
     * <li>Calls the Initialize life cycle script.</li>
     * <li>Reads runtime and builder configuration.</li>
     * <li>Setups basic artifact handlers.</li>
     * <li>Initializes available addons.</li>
     * </ul>
     *
     * @param app the current Griffon application
     */
    static void prepare(GriffonApplication app) {
        app.bindings.app = app

        Metadata.current.getGriffonStartDir()
        Metadata.current.getGriffonWorkingDir()

        MetaClass appMetaClass = app.metaClass
        appMetaClass.newInstance = GriffonApplicationHelper.&newInstance.curry(app)
        appMetaClass.createMVCGroup = GriffonApplicationHelper.&newInstance.curry(app)
        appMetaClass.createMVCGroup = {Object... args ->
            GriffonApplicationHelper.createMVCGroup(app, *args)
        }
        appMetaClass.buildMVCGroup = {Object... args ->
            GriffonApplicationHelper.buildMVCGroup(app, *args)
        }
        appMetaClass.destroyMVCGroup = GriffonApplicationHelper.&destroyMVCGroup.curry(app)
        UIThreadHelper.enhance(appMetaClass)

        ConfigSlurper configSlurper = new ConfigSlurper(Environment.current.name)
        app.config = configSlurper.parse(app.configClass)
        app.builderConfig = configSlurper.parse(app.builderClass)

        def eventsClass = app.eventsClass
        if (eventsClass) {
            app.eventsConfig = eventsClass.newInstance()
            app.addApplicationEventListener(app.eventsConfig)
        }
        
        app.initialize()

        app.metaClass.artifactManager = ArtifactManager.instance
        ArtifactManager.instance.app = app
        ArtifactManager.instance.with {
            registerArtifactHandler(new ModelArtifactHandler())
            registerArtifactHandler(new ViewArtifactHandler())
            registerArtifactHandler(new ControllerArtifactHandler())
            registerArtifactHandler(new ServiceArtifactHandler())
            loadArtifactMetadata()
        }

        AddonHelper.handleAddonsAtStartup(app)

        // copy mvc groups in config to app, casting to strings in a new map
        app.config.mvcGroups.each {k, v->
            app.addMvcGroup(k, v.inject([:]) {m, e->m[e.key as String] = e.value as String; m})
        }
    }

    /**
     * Performs the startup sequence, mainly instantiates all starting MVC groups.<p>
     *
     * @param app the current Griffon application
     */
    static void startup(GriffonApplication app) {
        // init the builders
        // this is where a composite gets made and composites are added
        // for now we punt and make a SwingBuilder

        app.config.application.startupGroups.each {group ->
            createMVCGroup(app, group) 
        }

        app.startup();
    }

    /**
     * Sets a property value ignoring any MissingPropertyExceptions.<p>
     *
     * @param receiver the objet where the property will be set
     * @param property the name of the property to set
     * @param value the value to set on the property
     */
    static void safeSet(reciever, property, value) {
        try {
            reciever."$property" = value
        } catch (MissingPropertyException mpe) {
            if (mpe.property != property) {
                throw mpe
            }
            /* else ignore*/
        }
    }

    /**
     * Executes a script inside the UI Thread.<p>
     * On Swing this would be the Event Dispatch Thread.
     *
     */
    public static void runScriptInsideUIThread(String scriptName, GriffonApplication app) {
        def script
        try {
            script = GriffonApplicationHelper.classLoader.loadClass(scriptName).newInstance(app.bindings)
        } catch (ClassNotFoundException cnfe) {
            if (cnfe.message == scriptName) {
                // the script must not exist, do nothing
                //LOGME - may be because of chained failures
                return
            } else {
                throw cnfe;
            }
        }
        
        script.isUIThread = UIThreadHelper.instance.&isUIThread
        script.execAsync = UIThreadHelper.instance.&executeAsync
        script.execSync = UIThreadHelper.instance.&executeSync
        script.execOutside = UIThreadHelper.instance.&executeOutside
        script.execFuture = {Object... args -> UIThreadHelper.instance.executeFuture(*args) }

        UIThreadHelper.instance.executeSync(script)
    }

    /**
     * Creates a new instance of the specified class.<p>
     * Publishes a <strong>NewInstance</strong> event with the following arguments<ul>
     * <li>klass - the target Class</li>
     * <li>type - the type of the instance (i.e, 'controller','service')</li>
     * <li>instance - the newly created instance</li>
     * </ul>
     *
     * @param app the current GriffonApplication
     * @param klass the target Class from which the instance will be created
     * @param type optional type parameter, used when publishing a 'NewInstance' event
     *
     * @return a newly created instance of type klass
     */
    public static Object newInstance(GriffonApplication app, Class klass, String type = "") {
        def instance = klass.newInstance()
        app.event("NewInstance",[klass,type,instance])
        return instance
    }

    public static createMVCGroup(GriffonApplication app, String mvcType) {
        createMVCGroup(app, mvcType, mvcType, [:])
    }

    public static createMVCGroup(GriffonApplication app, String mvcType, String mvcName) {
        createMVCGroup(app, mvcType, mvcName, [:])
    }

    public static createMVCGroup(GriffonApplication app, String mvcType, Map bindArgs) {
        createMVCGroup(app, mvcType, mvcType, bindArgs)
    }

    public static createMVCGroup(GriffonApplication app, Map bindArgs, String mvcType, String mvcName) {
        createMVCGroup(app, mvcType, mvcName, bindArgs)
    }

    public static createMVCGroup(GriffonApplication app, Map bindArgs, String mvcType) {
        createMVCGroup(app, mvcType, mvcType, bindArgs)
    }

    public static createMVCGroup(GriffonApplication app, String mvcType, String mvcName, Map bindArgs) {
        Map results = buildMVCGroup(app, bindArgs, mvcType, mvcName)
        return [results.model, results.view, results.controller]
    }

    public static Map buildMVCGroup(GriffonApplication app, String mvcType, String mvcName = mvcType) {
        buildMVCGroup(app, [:], mvcType, mvcName)
    }

    public static Map buildMVCGroup(GriffonApplication app, Map bindArgs, String mvcType, String mvcName = mvcType) {
        if (!app.mvcGroups.containsKey(mvcType)) {
            throw new RuntimeException("Unknown MVC type \"$mvcType\".  Known types are ${app.mvcGroups.keySet()}")
        }

        def argsCopy = [app:app, mvcType:mvcType, mvcName:mvcName]
        argsCopy.putAll(app.bindings.variables)
        argsCopy.putAll(bindArgs)

        // figure out what the classes are and prep the metaclass
        def klassMap = [:]
        ClassLoader classLoader = app.getClass().classLoader
        app.mvcGroups[mvcType].each {k, v ->
            Class klass = ArtifactManager.instance.getArtifactInfo(v)?.klass
            if(!klass) klass = classLoader.loadClass(v);

            // inject defaults into emc
            // this also insures EMC metaclasses later
            klass.metaClass.app = app
            klass.metaClass.createMVCGroup = {Object... args ->
                GriffonApplicationHelper.createMVCGroup(app, *args)
            }
            klass.metaClass.buildMVCGroup = {Object... args ->
                GriffonApplicationHelper.buildMVCGroup(app, *args)
            }
            klass.metaClass.destroyMVCGroup = GriffonApplicationHelper.&destroyMVCGroup.curry(app)
            klass.metaClass.newInstance = {Object... args ->
                GriffonApplicationHelper.newInstance(app, *args)
            }
            UIThreadHelper.enhance(klass.metaClass)
            klassMap[k] = klass
        }

        // create the builder
        UberBuilder builder = CompositeBuilderHelper.createBuilder(app, klassMap)
        argsCopy.each {k, v -> builder.setVariable k, v }

        // instantiate the parts
        def instanceMap = [:]
        klassMap.each {k, v ->
            if (argsCopy.containsKey(k)) {
                // use provided value, even if null
                instanceMap[k] = argsCopy[k]
            } else {
                // otherwise create a new value
                def instance = newInstance(app, v, k)
                instanceMap[k] = instance
                argsCopy[k] = instance

                // all scripts get the builder as their binding
                if (instance instanceof Script) {
                    instance.binding = builder;
                }
            }
        }
        instanceMap.builder = builder
        argsCopy.builder = builder
        
        // special case --
        // controller gets application listeners
        // addApplicationListener method is null safe
        app.addApplicationEventListener(instanceMap.controller)

        // mutually set each other to the available fields and inject args
        instanceMap.each {k, v ->
            // loop on the instance map to get just the instances
            if (v instanceof Script)  {
                v.binding.variables.putAll(argsCopy)
            } else {
                // set the args and instances
                InvokerHelper.setProperties(v, argsCopy)
            }
        }

        // store the refs in the app caches
        app.models[mvcName] = instanceMap.model
        app.views[mvcName] = instanceMap.view
        app.controllers[mvcName] = instanceMap.controller
        app.builders[mvcName] = instanceMap.builder
        app.groups[mvcName] = instanceMap

        // initialize the classes and call scripts
        instanceMap.each {k, v ->
            if (v instanceof Script) {
                // special case: view gets executed in the UI thread always
                if (k == 'view') {
//                    builder.edt({builder.build(v)})
                    UIThreadHelper.instance.executeSync { builder.build(v) }
                } else {
                    // non-view gets built in the builder
                    // they can switch into the UI thread as desired
                    builder.build(v)
                }
            } else if (k != 'builder') {
                try {
                    v.mvcGroupInit(argsCopy)
                } catch (MissingMethodException mme) {
                    if (mme.method != 'mvcGroupInit') {
                        throw mme
                    }
                    // MME on mvcGroupInit means they didn't define
                    // an init method.  This is not an error.
                }
            }
        }

        app.event("CreateMVCGroup",[mvcName, instanceMap.model, instanceMap.view, instanceMap.controller, mvcType, instanceMap])
        return instanceMap
    }

    /**
     * Cleanups and removes an MVC group from the application.<p>
     * Calls <strong>mvcGroupDestroy()</strong> on model, view and controller
     * if the method is defined.<p>
     * Publishes a <strong>DestroyMVCGroup</strong> event with the following arguments<ul>
     * <li>mvcName - the name of the group</li>
     * </ul>
     *
     * @param app the current Griffon application
     * @param mvcName name of the group to destroy
     */
    public static destroyMVCGroup(GriffonApplication app, String mvcName) {
        app.removeApplicationEventListener(app.controllers[mvcName])
        [app.models, app.views, app.controllers].each {
            def part = it.remove(mvcName)
            if ((part != null)  & !(part instanceof Script)) {
                try {
                    part.mvcGroupDestroy()
                } catch (MissingMethodException mme) {
                    if (mme.method != 'mvcGroupDestroy') {
                        throw mme
                    }
                    // MME on mvcGroupDestroy means they didn't define
                    // a destroy method.  This is not an error.
                }
            }
        }

        try {
            app.builders[mvcName]?.dispose()
        } catch(MissingMethodException mme) {
            // TODO find out why this call breaks applet mode on shutdown
        }

        // remove the refs from the app caches
        app.models.remove(mvcName)
        app.views.remove(mvcName)
        app.controllers.remove(mvcName)
        app.builders.remove(mvcName)
        app.groups.remove(mvcName)

        app.event("DestroyMVCGroup",[mvcName])
    }
}
