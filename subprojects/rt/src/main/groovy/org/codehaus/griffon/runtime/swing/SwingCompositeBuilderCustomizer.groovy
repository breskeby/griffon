/*
 * Copyright 2008-2011 the original author or authors.
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
package org.codehaus.griffon.runtime.swing

import groovy.swing.factory.ComponentFactory
import groovy.swing.factory.LayoutFactory
import groovy.swing.factory.ScrollPaneFactory
import groovy.swing.factory.TableFactory
import java.awt.LayoutManager
import org.codehaus.griffon.runtime.builder.UberBuilder
import org.codehaus.griffon.runtime.util.DefaultCompositeBuilderCustomizer
import javax.swing.*

/**
 * Swing based implementation of the <code>CompositeBuilderCustomizer</code> interface.
 *
 * @author Andres Almiray
 * @since 0.9.3
 */
class SwingCompositeBuilderCustomizer extends DefaultCompositeBuilderCustomizer {
    @Override
    void registerBeanFactory(UberBuilder uberBuilder, String name, String groupName, Class<?> beanClass) {
        if (LayoutManager.isAssignableFrom(beanClass)) {
            uberBuilder.registerFactory(name, groupName, new LayoutFactory(beanClass))
        } else if (JScrollPane.isAssignableFrom(beanClass)) {
            uberBuilder.registerFactory(name, groupName, new ScrollPaneFactory(beanClass))
        } else if (JTable.isAssignableFrom(beanClass)) {
            uberBuilder.registerFactory(name, groupName, new TableFactory(beanClass))
        } else if (JComponent.isAssignableFrom(beanClass)
                || JApplet.isAssignableFrom(beanClass)
                || JDialog.isAssignableFrom(beanClass)
                || JFrame.isAssignableFrom(beanClass)
                || JWindow.isAssignableFrom(beanClass)
        ) {
            uberBuilder.registerFactory(name, groupName, new ComponentFactory(beanClass))
        } else {
            uberBuilder.registerBeanFactory(name, groupName, beanClass)
        }
    }
}
