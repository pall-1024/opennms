/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.apilayer.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opennms.topologies.service.api.EdgeMockUtil.PROTOCOL;
import static org.opennms.topologies.service.api.EdgeMockUtil.SOURCE_ID;
import static org.opennms.topologies.service.api.EdgeMockUtil.TARGET_NODE_ID;
import static org.opennms.topologies.service.api.EdgeMockUtil.addPort;
import static org.opennms.topologies.service.api.EdgeMockUtil.createEdge;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.opennms.features.situationfeedback.api.AlarmFeedback;
import org.opennms.integration.api.v1.model.Alarm;
import org.opennms.integration.api.v1.model.DatabaseEvent;
import org.opennms.integration.api.v1.model.InMemoryEvent;
import org.opennms.integration.api.v1.model.IpInterface;
import org.opennms.integration.api.v1.model.MetaData;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.integration.api.v1.model.Severity;
import org.opennms.integration.api.v1.model.SnmpInterface;
import org.opennms.integration.api.v1.model.TopologyEdge;
import org.opennms.integration.api.v1.model.TopologyPort;
import org.opennms.integration.api.v1.model.TopologySegment;
import org.opennms.integration.api.v1.model.immutables.ImmutableEventParameter;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsEventParameter;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsSnmpInterface;
import org.opennms.netmgt.model.monitoringLocations.OnmsMonitoringLocation;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyEdge;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyProtocol;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.topologies.service.api.EdgeMockUtil;

public class ModelMappersTest {
    private final NodeCriteriaCache nodeCriteriaCache = mock(NodeCriteriaCache.class);
    private final EdgeMapper edgeMapper = new EdgeMapper(nodeCriteriaCache);

    @Before
    public void setup() {
        when(nodeCriteriaCache.getNodeCriteria(Matchers.any(Long.class))).thenReturn(Optional.empty());
    }

    @Test
    public void canMapNodeToNodeEdge() {
        OnmsTopologyEdge mockEdge = createEdge();
        EdgeMockUtil.addNode(true, mockEdge);
        EdgeMockUtil.addNode(false, mockEdge);
        TopologyEdge mappedEdge = edgeMapper.toEdge(OnmsTopologyProtocol.create(PROTOCOL), mockEdge);

        assertThat(mappedEdge.getId(), equalTo(EdgeMockUtil.EDGE_ID));
        assertThat(mappedEdge.getProtocol().name(), equalTo(PROTOCOL));

        AtomicBoolean visitedSourceNode = new AtomicBoolean(false);
        AtomicBoolean visitedTargetNode = new AtomicBoolean(false);
        mappedEdge.visitEndpoints(new TopologyEdge.EndpointVisitor() {
            @Override
            public void visitSource(Node node) {
                assertThat(node.getId(), equalTo(EdgeMockUtil.SOURCE_NODE_ID));
                visitedSourceNode.set(true);
            }

            @Override
            public void visitTarget(Node node) {
                assertThat(node.getId(), equalTo(TARGET_NODE_ID));
                visitedTargetNode.set(true);
            }
        });
        assertThat(visitedSourceNode.get(), equalTo(true));
        assertThat(visitedTargetNode.get(), equalTo(true));
    }

    @Test
    public void canMapPortToPortEdge() {
        OnmsTopologyEdge mockEdge = createEdge();
        addPort(true, mockEdge);
        addPort(false, mockEdge);
        TopologyEdge mappedEdge = edgeMapper.toEdge(OnmsTopologyProtocol.create(PROTOCOL), mockEdge);

        assertThat(mappedEdge.getId(), equalTo(EdgeMockUtil.EDGE_ID));
        assertThat(mappedEdge.getProtocol().name(), equalTo(PROTOCOL));

        AtomicBoolean visitedSourcePort = new AtomicBoolean(false);
        AtomicBoolean visitedTargetPort = new AtomicBoolean(false);
        mappedEdge.visitEndpoints(new TopologyEdge.EndpointVisitor() {
            @Override
            public void visitSource(TopologyPort port) {
                assertThat(port.getId(), equalTo(SOURCE_ID));
                assertThat(port.getIfIndex(), equalTo(EdgeMockUtil.SOURCE_IFINDEX));
                assertThat(port.getTooltipText(), equalTo(EdgeMockUtil.SOURCE_TOOLTIP));
                assertThat(port.getNodeCriteria().getId(), equalTo(EdgeMockUtil.SOURCE_NODE_ID));
                visitedSourcePort.set(true);
            }

            @Override
            public void visitTarget(TopologyPort port) {
                assertThat(port.getId(), equalTo(EdgeMockUtil.TARGET_ID));
                assertThat(port.getIfIndex(), equalTo(EdgeMockUtil.TARGET_IFINDEX));
                assertThat(port.getTooltipText(), equalTo(EdgeMockUtil.TARGET_TOOLTIP));
                assertThat(port.getNodeCriteria().getId(), equalTo(TARGET_NODE_ID));
                visitedTargetPort.set(true);
            }
        });
        assertThat(visitedSourcePort.get(), equalTo(true));
        assertThat(visitedTargetPort.get(), equalTo(true));
    }

    @Test
    public void canMapSegmentToSegmentEdge() {
        OnmsTopologyEdge mockEdge = createEdge();
        EdgeMockUtil.addSegment(true, mockEdge);
        EdgeMockUtil.addSegment(false, mockEdge);
        TopologyEdge mappedEdge = edgeMapper.toEdge(OnmsTopologyProtocol.create(PROTOCOL), mockEdge);

        assertThat(mappedEdge.getId(), equalTo(EdgeMockUtil.EDGE_ID));
        assertThat(mappedEdge.getProtocol().name(), equalTo(PROTOCOL));

        AtomicBoolean visitedSourceSegment = new AtomicBoolean(false);
        AtomicBoolean visitedTargetSegment = new AtomicBoolean(false);
        mappedEdge.visitEndpoints(new TopologyEdge.EndpointVisitor() {
            @Override
            public void visitSource(TopologySegment segment) {
                assertThat(segment.getId(), equalTo(SOURCE_ID));
                assertThat(segment.getTooltipText(), equalTo(EdgeMockUtil.SOURCE_TOOLTIP));
                assertThat(segment.getSegmentCriteria(), equalTo(String.format("%s:%s", PROTOCOL, SOURCE_ID)));
                visitedSourceSegment.set(true);
            }

            @Override
            public void visitTarget(TopologySegment segment) {
                assertThat(segment.getId(), equalTo(EdgeMockUtil.TARGET_ID));
                assertThat(segment.getTooltipText(), equalTo(EdgeMockUtil.TARGET_TOOLTIP));
                assertThat(segment.getSegmentCriteria(), equalTo(String.format("%s:%s", PROTOCOL,
                        EdgeMockUtil.TARGET_ID)));
                visitedTargetSegment.set(true);
            }
        });
        assertThat(visitedSourceSegment.get(), equalTo(true));
        assertThat(visitedTargetSegment.get(), equalTo(true));
    }

    @Test
    public void canMapPortToSegmentEdge() {
        OnmsTopologyEdge mockEdge = createEdge();
        addPort(true, mockEdge);
        EdgeMockUtil.addSegment(false, mockEdge);
        TopologyEdge mappedEdge = edgeMapper.toEdge(OnmsTopologyProtocol.create(PROTOCOL), mockEdge);

        assertThat(mappedEdge.getId(), equalTo(EdgeMockUtil.EDGE_ID));
        assertThat(mappedEdge.getProtocol().name(), equalTo(PROTOCOL));

        AtomicBoolean visitedSourcePort = new AtomicBoolean(false);
        AtomicBoolean visitedTargetSegment = new AtomicBoolean(false);
        mappedEdge.visitEndpoints(new TopologyEdge.EndpointVisitor() {
            @Override
            public void visitSource(TopologyPort port) {
                assertThat(port.getId(), equalTo(SOURCE_ID));
                assertThat(port.getIfIndex(), equalTo(EdgeMockUtil.SOURCE_IFINDEX));
                assertThat(port.getTooltipText(), equalTo(EdgeMockUtil.SOURCE_TOOLTIP));
                assertThat(port.getNodeCriteria().getId(), equalTo(EdgeMockUtil.SOURCE_NODE_ID));
                visitedSourcePort.set(true);
            }

            @Override
            public void visitTarget(TopologySegment segment) {
                assertThat(segment.getId(), equalTo(EdgeMockUtil.TARGET_ID));
                assertThat(segment.getTooltipText(), equalTo(EdgeMockUtil.TARGET_TOOLTIP));
                assertThat(segment.getSegmentCriteria(), equalTo(String.format("%s:%s", PROTOCOL,
                        EdgeMockUtil.TARGET_ID)));
                visitedTargetSegment.set(true);
            }
        });
        assertThat(visitedSourcePort.get(), equalTo(true));
        assertThat(visitedTargetSegment.get(), equalTo(true));
    }

    @Test
    public void canMapInMemoryEvent() {
        Event event = new Event();
        event.setUei("test.uei");
        event.setSource("test.source");
        event.setSeverity("MINOR");
        event.setNodeid(12345L);
        String parmName = "test.parm.name";
        String parmValue = "test.parm.value";
        event.setParmCollection(Collections.singletonList(new Parm(parmName, parmValue)));

        InMemoryEvent inMemoryEvent = ModelMappers.toEvent(event);
        assertThat(inMemoryEvent.getUei(), equalTo(event.getUei()));
        assertThat(inMemoryEvent.getSource(), equalTo(event.getSource()));
        assertThat(inMemoryEvent.getSeverity(), equalTo(Severity.MINOR));
        assertThat(inMemoryEvent.getNodeId(), equalTo(event.getNodeid().intValue()));
        assertThat(inMemoryEvent.getParameters(), hasItems(ImmutableEventParameter.newInstance(parmName, parmValue)));
        assertThat(inMemoryEvent.getParameterValue(parmName).get(), equalTo(parmValue));
        assertThat(inMemoryEvent.getParametersByName(parmName), hasItems(ImmutableEventParameter.newInstance(parmName,
                parmValue)));
    }

    @Test
    public void canMapDatabaseEvent() {
        OnmsEvent event = new OnmsEvent();
        event.setEventUei("test.uei");
        event.setId(1);
        String eventName = "test.name";
        String eventValue = "test.value";
        event.setEventParameters(Collections.singletonList(new OnmsEventParameter(null, eventName, eventValue, null)));

        DatabaseEvent databaseEvent = ModelMappers.toEvent(event);
        assertThat(databaseEvent.getUei(), equalTo(event.getEventUei()));
        assertThat(databaseEvent.getId(), equalTo(event.getId()));
        assertThat(databaseEvent.getParameters(), hasItems(ImmutableEventParameter.newInstance(eventName, eventValue)));
        assertThat(databaseEvent.getParametersByName(eventName),
                hasItems(ImmutableEventParameter.newInstance(eventName, eventValue)));
    }

    @Test
    public void canMapNode() throws UnknownHostException {
        OnmsNode onmsNode = new OnmsNode();
        onmsNode.setId(1);
        onmsNode.setForeignSource("test.fs");
        onmsNode.setForeignId("test.fid");
        onmsNode.setLabel("test.label");
        String locationName = "test.location";
        onmsNode.setLocation(new OnmsMonitoringLocation(locationName, null));

        onmsNode.addMetaData("test.context", "test.key", "test.value");

        onmsNode.setAsset("vendor","test.vendor");
        onmsNode.setAsset("modelNumber","test.model");
        onmsNode.setAsset("description","test.description");
        onmsNode.setAsset("assetNumber","test.assetnum");
        onmsNode.setAsset("operatingSystem","test.os");
        onmsNode.setAsset("region","test.region");
        onmsNode.setAsset("division","test.division");
        onmsNode.setAsset("department","test.department");
        onmsNode.setAsset("building","test.building");
        onmsNode.setAsset("floor","test.floor");

        onmsNode.setAsset("address1","test.addr1");
        onmsNode.setAsset("address2","test.addr2");
        onmsNode.setAsset("city","test.city");
        onmsNode.setAsset("state","test.state");
        onmsNode.setAsset("zip","test.zip");
        onmsNode.setAsset("country","country");
        onmsNode.setAsset("longitude","100.0");
        onmsNode.setAsset("latitude","200.0");

        OnmsSnmpInterface onmsSnmpInterface = new OnmsSnmpInterface();
        onmsSnmpInterface.setIfDescr("test.ifdescr");
        onmsSnmpInterface.setIfName("test.ifname");
        onmsSnmpInterface.setIfIndex(1);
        onmsNode.setSnmpInterfaces(Collections.singleton(onmsSnmpInterface));

        OnmsIpInterface onmsIpInterface = new OnmsIpInterface();
        onmsIpInterface.setIpAddress(InetAddress.getLocalHost());
        onmsIpInterface.setSnmpInterface(onmsSnmpInterface);
        onmsIpInterface.addMetaData("test.context", "test.key", "test.value");
        onmsNode.setIpInterfaces(Collections.singleton(onmsIpInterface));

        Node node = ModelMappers.toNode(onmsNode);
        assertThat(node.getId(), equalTo(onmsNode.getId()));
        assertThat(node.getForeignSource(), equalTo(onmsNode.getForeignSource()));
        assertThat(node.getForeignId(), equalTo(onmsNode.getForeignId()));
        assertThat(node.getLabel(), equalTo(onmsNode.getLabel()));
        assertThat(node.getLocation(), equalTo(onmsNode.getLocation().getLocationName()));

        assertThat(onmsNode.getAsset("vendor"), equalTo("test.vendor"));
        assertThat(onmsNode.getAsset("modelNumber"), equalTo("test.model"));
        assertThat(onmsNode.getAsset("description"), equalTo("test.description"));
        assertThat(onmsNode.getAsset("assetNumber"), equalTo("test.assetnum"));
        assertThat(onmsNode.getAsset("operatingSystem"), equalTo("test.os"));
        assertThat(onmsNode.getAsset("region"), equalTo("test.region"));
        assertThat(onmsNode.getAsset("division"), equalTo("test.division"));
        assertThat(onmsNode.getAsset("department"), equalTo("test.department"));
        assertThat(onmsNode.getAsset("building"), equalTo("test.building"));
        assertThat(onmsNode.getAsset("floor"), equalTo("test.floor"));

        assertThat(onmsNode.getAsset("address1"), equalTo("test.addr1"));
        assertThat(onmsNode.getAsset("address2"), equalTo("test.addr2"));
        assertThat(onmsNode.getAsset("city"), equalTo("test.city"));
        assertThat(onmsNode.getAsset("state"), equalTo("test.state"));
        assertThat(onmsNode.getAsset("zip"), equalTo("test.zip"));
        assertThat(onmsNode.getAsset("country"), equalTo("country"));
        assertThat(onmsNode.getAsset("longitude"), equalTo("100.0"));
        assertThat(onmsNode.getAsset("latitude"), equalTo("200.0"));

        MetaData metaData = node.getMetaData().get(0);
        assertThat(node.getMetaData().size(), equalTo(onmsNode.getMetaData().size()));
        assertThat(metaData.getContext(), equalTo(onmsNode.getMetaData().get(0).getContext()));
        assertThat(metaData.getKey(), equalTo(onmsNode.getMetaData().get(0).getKey()));
        assertThat(metaData.getValue(), equalTo(onmsNode.getMetaData().get(0).getValue()));

        IpInterface ipInterface = node.getIpInterfaces().get(0);
        assertThat(ipInterface.getIpAddress(), equalTo(onmsNode.getIpInterfaces().iterator().next().getIpAddress()));
        assertThat(ipInterface.getSnmpInterface().get().getIfIndex(), equalTo(onmsNode.getSnmpInterfaces().iterator().next().getIfIndex()));

        assertThat(ipInterface.getMetaData().get(0).getKey(),
                equalTo(onmsNode.getIpInterfaces().iterator().next().getMetaData().get(0).getKey()));

        SnmpInterface snmpInterface = node.getSnmpInterfaces().get(0);
        assertThat(snmpInterface.getIfDescr(), equalTo(onmsNode.getSnmpInterfaces().iterator().next().getIfDescr()));
        assertThat(snmpInterface.getIfName(), equalTo(onmsNode.getSnmpInterfaces().iterator().next().getIfName()));
        assertThat(snmpInterface.getIfIndex(), equalTo(onmsNode.getSnmpInterfaces().iterator().next().getIfIndex()));

        assertThat(node.getMetaData().size(), equalTo(onmsNode.getMetaData().size()));
    }

    @Test
    public void canMapAlarmFeedback() {
        AlarmFeedback alarmFeedback = AlarmFeedback.newBuilder()
                .withSituationKey("test.key")
                .withSituationFingerprint("test.finger")
                .withAlarmKey("test.alarmkey")
                .withFeedbackType(AlarmFeedback.FeedbackType.FALSE_POSITIVE)
                .withReason("test.reason")
                .withUser("test.user")
                .withTimestamp(100L)
                .build();

        org.opennms.integration.api.v1.model.AlarmFeedback apiAlarmFeedback = ModelMappers.toFeedback(alarmFeedback);
        assertThat(apiAlarmFeedback.getSituationKey(), equalTo(alarmFeedback.getSituationKey()));
        assertThat(apiAlarmFeedback.getSituationFingerprint(), equalTo(alarmFeedback.getSituationFingerprint()));
        assertThat(apiAlarmFeedback.getAlarmKey(), equalTo(alarmFeedback.getAlarmKey()));
        assertThat(apiAlarmFeedback.getFeedbackType().name(), equalTo(alarmFeedback.getFeedbackType().name()));
        assertThat(apiAlarmFeedback.getReason(), equalTo(alarmFeedback.getReason()));
        assertThat(apiAlarmFeedback.getUser(), equalTo(alarmFeedback.getUser()));
        assertThat(apiAlarmFeedback.getTimestamp(), equalTo(alarmFeedback.getTimestamp()));
    }

    @Test
    public void canHandleObjectNotFoundExceptionsWhenMappingAlarms() {
        // Map a trivial alarm first
        OnmsAlarm alarm = new OnmsAlarm();
        Alarm apiAlarm = ModelMappers.toAlarm(alarm);
        assertThat(apiAlarm.getReductionKey(), equalTo(alarm.getReductionKey()));

        // Now let's throw an exception when calling getLastEvent()
        alarm = mock(OnmsAlarm.class);
        when(alarm.getLastEvent()).thenThrow(new ObjectNotFoundException(1, OnmsEvent.class.getCanonicalName()));
        apiAlarm = ModelMappers.toAlarm(alarm);
        // No last event should be set
        assertThat(apiAlarm.getLastEvent(), nullValue());
    }
}
