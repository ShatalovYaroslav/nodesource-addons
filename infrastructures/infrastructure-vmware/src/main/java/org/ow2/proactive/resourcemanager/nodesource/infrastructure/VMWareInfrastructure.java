/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2015 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.resourcemanager.nodesource.infrastructure;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class VMWareInfrastructure extends InfrastructureManager {

    public static final String INSTANCE_ID_NODE_PROPERTY = "instanceId";

    public static final String INFRASTRUCTURE_TYPE = "vmware";

    private static final Logger logger = Logger.getLogger(VMWareInfrastructure.class);

    @Configurable(description = "The VMWare_Username")
    protected String username = null;

    @Configurable(description = "The VMWare_Password")
    protected String password = null;

    @Configurable(description = "The VMWare_EndPoint")
    protected String endpoint = null;

    @Configurable(description = "Resource manager hostname or ip address")
    protected String rmHostname = generateDefaultRMHostname();

    @Configurable(description = "Connector-iaas URL")
    protected String connectorIaasURL = "http://" + generateDefaultRMHostname() + ":8080/connector-iaas";

    @Configurable(description = "Image")
    protected String image = null;

    @Configurable(description = "minumum RAM required (in Mega Bytes)")
    protected int ram = 512;

    @Configurable(description = "minimum number of CPU cores required")
    protected int cores = 1;

    @Configurable(description = "The virtual machine Username")
    protected String vmUsername = null;

    @Configurable(description = "TThe virtual machine Password")
    protected String vmPassword = null;

    @Configurable(description = "Total instance to create")
    protected int numberOfInstances = 1;

    @Configurable(description = "Total nodes to create per instance")
    protected int numberOfNodesPerInstance = 1;

    @Configurable(description = "Command used to download the worker jar")
    protected String downloadCommand = generateDefaultDownloadCommand();

    @Configurable(description = "Additional Java command properties (e.g. \"-Dpropertyname=propertyvalue\")")
    protected String additionalProperties = "-Dproactive.useIPaddress=true";

    protected ConnectorIaasController connectorIaasController = null;

    protected final Map<String, Set<String>> nodesPerInstances;

    /**
     * Default constructor
     */
    public VMWareInfrastructure() {
        nodesPerInstances = Maps.newConcurrentMap();
    }

    @Override
    public void configure(Object... parameters) {

        logger.info("Validating parameters : " + parameters);
        validate(parameters);

        this.username = parameters[0].toString().trim();
        this.password = parameters[1].toString().trim();
        this.endpoint = parameters[2].toString().trim();
        this.rmHostname = parameters[3].toString().trim();
        this.connectorIaasURL = parameters[4].toString().trim();
        this.image = parameters[5].toString().trim();
        this.ram = Integer.parseInt(parameters[6].toString().trim());
        this.cores = Integer.parseInt(parameters[7].toString().trim());
        this.vmUsername = parameters[8].toString().trim();
        this.vmPassword = parameters[9].toString().trim();
        this.numberOfInstances = Integer.parseInt(parameters[10].toString().trim());
        this.numberOfNodesPerInstance = Integer.parseInt(parameters[11].toString().trim());
        this.downloadCommand = parameters[12].toString().trim();
        this.additionalProperties = parameters[13].toString().trim();

        connectorIaasController = new ConnectorIaasController(connectorIaasURL, INFRASTRUCTURE_TYPE);

    }

    private void validate(Object[] parameters) {
        if (parameters == null || parameters.length < 14) {
            throw new IllegalArgumentException("Invalid parameters for VMWareInfrastructure creation");
        }

        if (parameters[0] == null) {
            throw new IllegalArgumentException("VMWare username must be specified");
        }

        if (parameters[1] == null) {
            throw new IllegalArgumentException("VMWare password must be specified");
        }

        if (parameters[2] == null) {
            throw new IllegalArgumentException("VMWare endpoint must be specified");
        }

        if (parameters[3] == null) {
            throw new IllegalArgumentException("The Resource manager hostname must be specified");
        }

        if (parameters[4] == null) {
            throw new IllegalArgumentException("The connector-iaas URL must be specified");
        }

        if (parameters[5] == null) {
            throw new IllegalArgumentException("The image id must be specified");
        }

        if (parameters[6] == null) {
            throw new IllegalArgumentException("The amount of minimum RAM required must be specified");
        }

        if (parameters[7] == null) {
            throw new IllegalArgumentException("The minimum number of cores required must be specified");
        }

        if (parameters[8] == null) {
            throw new IllegalArgumentException("The virtual machine username must be specified");
        }

        if (parameters[8] == null) {
            throw new IllegalArgumentException("The virtual machine password must be specified");
        }

        if (parameters[10] == null) {
            throw new IllegalArgumentException("The number of instances to create must be specified");
        }

        if (parameters[11] == null) {
            throw new IllegalArgumentException(
                "The number of nodes per instance to deploy must be specified");
        }

        if (parameters[12] == null) {
            throw new IllegalArgumentException("The download node.jar command must be specified");
        }

        if (parameters[13] == null) {
            parameters[13] = "";
        }
    }

    @Override
    public void acquireNode() {

        connectorIaasController.waitForConnectorIaasToBeUP();

        connectorIaasController.createInfrastructure(getInfrastructureId(), username, password, endpoint,
                false);

        String instanceTag = getInfrastructureId();

        Set<String> instancesIds = connectorIaasController.createInstances(getInfrastructureId(), instanceTag,
                image, numberOfInstances, cores, ram);

        logger.info("Instances ids created : " + instancesIds);

        for (String instanceId : instancesIds) {

            String fullScript = "-c '" + this.downloadCommand + ";nohup " +
                generateDefaultStartNodeCommand(instanceId) + "  &'";

            connectorIaasController.executeScriptWithCredentials(getInfrastructureId(), instanceId,
                    Lists.newArrayList(fullScript), vmUsername, vmPassword);
        }

    }

    @Override
    public void acquireAllNodes() {
        acquireNode();
    }

    @Override
    public void removeNode(Node node) throws RMException {

        String instanceId = getInstanceIdProperty(node);

        try {
            node.getProActiveRuntime().killNode(node.getNodeInformation().getName());

        } catch (Exception e) {
            logger.warn(e);
        }

        synchronized (this) {
            nodesPerInstances.get(instanceId).remove(node.getNodeInformation().getName());
            logger.info("Removed node : " + node.getNodeInformation().getName());

            if (nodesPerInstances.get(instanceId).isEmpty()) {
                connectorIaasController.terminateInstance(getInfrastructureId(), instanceId);
                nodesPerInstances.remove(instanceId);
                logger.info("Removed instance : " + instanceId);
            }
        }
    }

    @Override
    public void notifyAcquiredNode(Node node) throws RMException {

        String instanceId = getInstanceIdProperty(node);

        synchronized (this) {
            if (!nodesPerInstances.containsKey(instanceId)) {
                nodesPerInstances.put(instanceId, new HashSet<String>());
            }
            nodesPerInstances.get(instanceId).add(node.getNodeInformation().getName());
        }
    }

    @Override
    public String getDescription() {
        return "Handles nodes from the Amazon Elastic Compute Cloud Service.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getDescription();
    }

    private String generateDefaultRMHostname() {
        try {
            // best effort, may not work for all machines
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            logger.warn(e);
            return "localhost";
        }
    }

    private String generateDefaultDownloadCommand() {
        if (System.getProperty("os.name").contains("Windows")) {
            return "powershell -command \"& { (New-Object Net.WebClient).DownloadFile('http://" +
                this.rmHostname + ":8080/rest/node.jar" + "', 'node.jar') }\"";
        } else {
            return "wget -nv http://" + this.rmHostname + ":8080/rest/node.jar";
        }
    }

    private String generateDefaultStartNodeCommand(String instanceId) {
        try {
            String rmUrlToUse = rmUrl;

            String protocol = rmUrlToUse.substring(0, rmUrlToUse.indexOf(':')).trim();
            return "java -jar node.jar -Dproactive.communication.protocol=" + protocol +
                " -Dproactive.pamr.router.address=" + rmHostname + " -D" + INSTANCE_ID_NODE_PROPERTY + "=" +
                instanceId + " " + additionalProperties + " -r " + rmUrlToUse + " -s " +
                nodeSource.getName() + " -w " + numberOfNodesPerInstance;
        } catch (Exception e) {
            logger.error("Exception when generating the command, fallback on default value", e);
            return "java -jar node.jar -D" + INSTANCE_ID_NODE_PROPERTY + "=" + instanceId + " " +
                additionalProperties + " -r " + rmUrl + " -s " + nodeSource.getName() + " -w " +
                numberOfNodesPerInstance;
        }
    }

    private String getInstanceIdProperty(Node node) throws RMException {
        try {
            return node.getProperty(INSTANCE_ID_NODE_PROPERTY);
        } catch (ProActiveException e) {
            throw new RMException(e);
        }
    }

    private String getInfrastructureId() {
        return nodeSource.getName().trim().replace(" ", "_").toLowerCase();
    }

}