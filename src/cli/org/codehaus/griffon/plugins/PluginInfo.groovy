/* Copyright 2004-2005 Graeme Rocher
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

package org.codehaus.griffon.plugins

import org.springframework.core.io.Resource
import groovy.util.slurpersupport.GPathResult

/**
 * A class used mainly by the build system that encapsulates access to information
 * about the underlying plugin by delegating to the methods in GriffonPluginUtils
 * 
 * @author Graeme Rocher
 * @since 1.1
 */

public class PluginInfo {

    Resource pluginDir
    def metadata

    public PluginInfo(Resource pluginDir) {
        super();
        if(pluginDir)
        this.pluginDir = pluginDir
        this.metadata = GriffonPluginUtils.getMetadataForPlugin(pluginDir)
    }


    /**
     * Returns the plugin's version
     */
    String getVersion() {
       return metadata.@version.text()
    }

    /**
     * Returns the plugin's name
     */
    String getName() {
        return metadata.@name.text()
    }

    /**
     * Obtains the plugins directory
     */
    Resource getPluginDirectory() {
        return pluginDir
    }

    /**
     * Returns the location of the descriptor
     */
    Resource getDescriptor() {
        GriffonPluginUtils.getDescriptorForPlugin(pluginDir)
    }

}