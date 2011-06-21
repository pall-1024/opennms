/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2011 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *     along with OpenNMS(R).  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information contact: 
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.features.poller.remote.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.Window;

import de.novanic.eventservice.client.event.RemoteEventService;
import de.novanic.eventservice.client.event.RemoteEventServiceFactory;

public class Main implements EntryPoint {
    
    private class DeferredCommandExecutor implements CommandExecutor{

        public void schedule(IncrementalCommand command) {
            DeferredCommand.addCommand(command);
        }

        public void schedule(Command command) {
            DeferredCommand.addCommand(command);
        }
        
    }
    
    private HandlerManager m_eventBus;

    public void onModuleLoad() {
        m_eventBus = new HandlerManager(null);
        Application application = new Application(getEventBus());
        MapPanel mapPanel = createMap(application);
        
        LocationStatusServiceAsync remoteService = GWT.create(LocationStatusService.class);
        RemoteEventService remoteEventService = RemoteEventServiceFactory.getInstance().getRemoteEventService();
        application.initialize(new DefaultApplicationView(application, getEventBus(), mapPanel), remoteService, remoteEventService, new DeferredCommandExecutor());

    }

    private MapPanel createMap(Application application) {
        MapPanel mapPanel;
        if (getMapType().equals("Mapquest")) {
            mapPanel = new MapQuestMapPanel(getEventBus());
        } else if (getMapType().equals("GoogleMaps")) {
            mapPanel = new GoogleMapsPanel(getEventBus());
        } else if (getMapType().equals("OpenLayers")) {
            mapPanel = new OpenLayersMapPanel(getEventBus());
        } else {
            Window.alert("unknown map implementation: " + getMapType());
            throw new RuntimeException("unknown map implementation: " + getMapType());
        }
        return mapPanel;
    }

    /**
     * <p>getMapImplementationType</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public native String getMapType() /*-{
        return $wnd.mapImplementation;
    }-*/;

    public HandlerManager getEventBus() {
        return m_eventBus;
    }

}
