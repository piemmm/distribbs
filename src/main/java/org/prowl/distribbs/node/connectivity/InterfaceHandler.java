package org.prowl.distribbs.node.connectivity;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.services.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InterfaceHandler {

    private static final Log LOG = LogFactory.getLog("InterfaceHandler");

    private SubnodeConfiguration configuration;

    private List<Interface> interfaces = new ArrayList<>();


    public InterfaceHandler(SubnodeConfiguration configuration) throws IOException {
        this.configuration = configuration;
        parseConfiguration();
    }

    /**
     * Parse the configuration and setup the interface
     */
    public void parseConfiguration() {
        // Get a list of connectors from the config file
        List<HierarchicalConfiguration> interfaceConfigurations = configuration.configurationsAt("interface");

        // Go create and configure each one.
        for (HierarchicalConfiguration interfaceConfiguration : interfaceConfigurations) {
            String className = interfaceConfiguration.getString("type");
            Interface anInterface = null;
            try {
                anInterface = (Interface) Class.forName(className).getConstructor(HierarchicalConfiguration.class).newInstance(interfaceConfiguration);
                interfaces.add(anInterface);
                LOG.info("Added interface: " + className);
            } catch (Throwable e) {
                // Something blew up. Log it and carry on.
                LOG.error("Unable to add interface: " + className, e);
            }


            List<HierarchicalConfiguration> slistConfigurations = interfaceConfiguration.configurationsAt("services");
            if (slistConfigurations.size() > 0) {
                HierarchicalConfiguration slistConfiguration = slistConfigurations.get(0);
                List<HierarchicalConfiguration> serviceConfigurations = slistConfiguration.configurationsAt("service");
                for (HierarchicalConfiguration serviceConfiguration : serviceConfigurations) {
                    String serviceName = serviceConfiguration.getRoot().getValue().toString();
                    Service service = DistriBBS.INSTANCE.getServiceHandler().getServiceForName(serviceName);
                    if (service == null) {
                        LOG.fatal("Unable to find service: " + serviceName + " for interface type: " + className);
                        System.exit(1);
                    }
                    LOG.info("Registering service: " + serviceName + " to interface: " + className);
                    anInterface.registerService(service);
                }
            }
        }


        // If there are no connectors configured then exit as there's little point in
        // continuing.
        if (interfaces.size() == 0) {
            LOG.error("Not starting as no interfaces have been configured");
            System.exit(1);
        }

    }

    public void start() {
        LOG.info("Starting interface...");
        for (Interface iface : interfaces) {
            try {
                LOG.info("Starting: " + iface.getName());
                iface.start();

            } catch (Throwable e) {
                LOG.error("Unable to start interface: " + iface.getName(), e);
            }
        }
    }

    /**
     * Get the interface that services the requested radio port (eg: 0=144MHz,
     * 1=433MHZ, etc)
     *
     * @param port
     * @return the port, or null if the port does not exist
     */
    public Interface getPort(int port) {
        if (port < interfaces.size()) {
            return interfaces.get(port);
        }
        return null;
    }

    /**
     * Returns a list of interfaces
     *
     * @return
     */
    public List<Interface> getPorts() {
        return new ArrayList(interfaces);
    }


}
