package com.oracle.coherence.cloud.amazon;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.tangosol.net.AddressProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The base version of the address provider doesn't filter the list of nodes by criteria such as instanceState, or
 * the tags applied to a node.  This version will apply filter rules to the node list such that the cluster is formed
 * using a list of nodes based on both tags and node state (won't add a terminated node for example).  Tags are additive
 * via an AND operator, that is if two name/value pairs are provided then all nodes must adhere to both name value pairs.
 * Matching is case-insensitive for both name and value.
 *
 * Created by mgreenwood on 1/13/15.
 */
public class EC2TaggedAddressProvider implements AddressProvider {

    private static final Logger logger = Logger.getLogger(EC2TaggedAddressProvider.class.getName());
    public static final String TANGOSOL_COHERENCE_EC2_USE_IAM = "tangosol.coherence.ec2.use.iam";
    public static final String TANGOSOL_COHERENCE_EC2_TAG_NAME = "tangosol.coherence.ec2.tag.name";
    public static final String TANGOSOL_COHERENCE_EC2_TAG_VALUE = "tangosol.coherence.ec2.tag.value";
    public static final String TANGOSOL_COHERENCE_EC2_REGION = "tangosol.coherence.ec2.region";
    public static final String TANGOSOL_COHERENCE_EC2ADDRESSPROVIDER_PORT =
            "tangosol.coherence.ec2addressprovider.port";
    public static final String DEFAULT_PORT = "6088";


    /**
     * The list of socket addresses to EC2 .
     */
    protected List<InetSocketAddress> wkaAddressList;

    /**
     * The current index in to the ArrayList.
     */
    protected Iterator<InetSocketAddress> wkaIterator;


    public EC2TaggedAddressProvider() throws IOException {
        if (logger.isLoggable(Level.CONFIG)) {
            //GLOBALYZER_IGNORE_NEXT_LINE
            logger.log(Level.CONFIG, "Initializing WKA list from EC2 Elastic IP to instance mapping.");
        }

        wkaAddressList = generateWKAList(getEC2Client());
        wkaIterator = wkaAddressList.iterator();
    }

    /**
     * Retrieve the EC2 client based on configuration options that allow for the usage of the EC2 assigned IAM role
     * or alternatively the credentials access_key/secret_key set as JVM arguments or property file elements
     *
     * @return
     * @throws IOException
     */
    protected AmazonEC2Client getEC2Client() throws IOException {
        ClientConfiguration clientConfig = new ClientConfiguration();
        //GLOBALYZER_IGNORE_NEXT_LINE
        String proxy = System.getenv("http_proxy");

        //GLOBALYZER_IGNORE_NEXT_LINE
        logger.log(Level.CONFIG, "Reading http_proxy parameter as: " + proxy);

        if (proxy != null && !proxy.isEmpty()) {
            try {
                URL url = new URL(proxy);
                clientConfig.setProxyHost(url.getHost());
                clientConfig.setProxyPort(url.getPort());
            }
            catch (IOException ioe) {
                // clientConfig reset to null
                clientConfig = null;
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.warning("Unable to parse http_proxy, ignoring current value: " + proxy);
                throw new RuntimeException("Unable to parse http_proxy setting:" + proxy, ioe);
            }
        } else {
            clientConfig = null;
        }

        AmazonEC2Client client = null;
        if (null != System.getProperty(TANGOSOL_COHERENCE_EC2_USE_IAM)
                && System.getProperty(TANGOSOL_COHERENCE_EC2_USE_IAM).equalsIgnoreCase("true")) {
            //GLOBALYZER_IGNORE_NEXT_LINE
            logger.log(Level.CONFIG, "Using EC2 IAM Role Credentials from metadata service");
            client = new AmazonEC2Client(new InstanceProfileCredentialsProvider());
        } else {
            //GLOBALYZER_IGNORE_NEXT_LINE
            logger.log(Level.CONFIG, "Using credentials with AWS secret / private key");
            client = new AmazonEC2Client(determineCredentials());
        }
        if (null != clientConfig) {
            //GLOBALYZER_IGNORE_NEXT_LINE
            logger.log(Level.CONFIG, "Setting proxy host = " + clientConfig.getProxyHost() + ", port = " + clientConfig.getProxyPort());
            client.setConfiguration(clientConfig);
        } else {
            //GLOBALYZER_IGNORE_NEXT_LINE
            logger.log(Level.CONFIG, "No Proxy Defined, set http_proxy in order to define an AWS proxy");
        }
        String region = System.getenv(TANGOSOL_COHERENCE_EC2_REGION);
        if (region == null) {
            region = "us-west-2";
        }
        //GLOBALYZER_IGNORE_NEXT_LINE
        logger.log(Level.CONFIG, "Using region: " + region);
        //GLOBALYZER_IGNORE_NEXT_LINE
        client.setEndpoint("https://ec2." + region + ".amazonaws.com/");
        return client;
    }

    public class NameValueFilter {
        public final String name;
        public final String value;

        public NameValueFilter(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    protected List<InetSocketAddress> generateWKAList(AmazonEC2 ec2)
    {
        List<NameValueFilter> filters = new ArrayList<>();
        Integer index = 1;
        // Build-up a list of tags with name / value pairs that we'll use to compare against the list of tags applied to the nodes
        // from describe nodes.  If the tags match for the instance, then we'll use that node in the cluster.  Also we'll check the
        // state of the node to assert that the node is alive (not terminated, not going down before adding it to the list of nodes
        // to emit
        while (System.getProperty(TANGOSOL_COHERENCE_EC2_TAG_NAME + index) != null &&
                System.getProperty(TANGOSOL_COHERENCE_EC2_TAG_VALUE + index) != null) {
            String name = System.getProperty(TANGOSOL_COHERENCE_EC2_TAG_NAME + index);
            String value = System.getProperty(TANGOSOL_COHERENCE_EC2_TAG_VALUE + index);
            NameValueFilter filter = new NameValueFilter(name.toUpperCase(), value.toUpperCase());
            //GLOBALYZER_IGNORE_NEXT_LINE
            logger.log(Level.CONFIG, "Read filter [ name = " + filter.name + ", values = " + filter.value + "]");
            filters.add(filter);
            index++;
        }

        String portString = System.getProperty(TANGOSOL_COHERENCE_EC2ADDRESSPROVIDER_PORT, DEFAULT_PORT);
        int wkaPort = Integer.parseInt(portString);

        // The list of private IP addresses that will form the cluster
        List<InetSocketAddress> resultList = new ArrayList<InetSocketAddress>();

        DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
        logger.info(describeInstancesResult.toString());

        List<Reservation> reservations = describeInstancesResult.getReservations();
        Set<Instance> instances = new HashSet<Instance>();

        for (Reservation reservation : reservations)
        {
            if (logger.isLoggable(Level.CONFIG)) {
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.log(Level.CONFIG, "Examining EC2 reservation:" + reservation);
            }
            for (Instance instance : reservation.getInstances()) {
                if (isInstanceUp(instance)) {
                    if (logger.isLoggable(Level.FINE)) {
                        //GLOBALYZER_IGNORE_NEXT_LINE
                        logger.fine("Instance: " + instance.getInstanceId() + " is up");
                    }
                    instances.add(instance);
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        //GLOBALYZER_IGNORE_NEXT_LINE
                        logger.fine("Instance: " + instance.getInstanceId() + " is down, ignoring");
                    }
                }
            }
        }

        Set<Instance> instancesToUse = null;
        if (!filters.isEmpty()) {
            if (logger.isLoggable(Level.FINE)) {
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.fine("Filtering instances");
            }
            instancesToUse = filterInstancesByTag(instances, filters, ec2);
        } else {
            instancesToUse = instances;
        }

        logAllInstances(instancesToUse);

        for (Instance instance : instancesToUse) {
            if (logger.isLoggable(Level.CONFIG)) {
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.log(Level.CONFIG, "EC2AddressProvider - adding {0} from instance {1} to WKA list",
                        new Object[]{instance.getPrivateIpAddress(), instance.getInstanceId()});
            }
            // add multiple ports in the event the service is available on another port
            resultList.add(new InetSocketAddress(instance.getPrivateIpAddress(), wkaPort));
            resultList.add(new InetSocketAddress(instance.getPrivateIpAddress(), wkaPort+1));
            resultList.add(new InetSocketAddress(instance.getPrivateIpAddress(), wkaPort+2));
            resultList.add(new InetSocketAddress(instance.getPrivateIpAddress(), wkaPort+3));
        }
        if (resultList.size() == 0) {
            //GLOBALYZER_IGNORE_NEXT_LINE
            throw new RuntimeException("The EC2AddressProvider could not find any instance mapped to an Elastic IP");
        }
        return resultList;
    }

    /**
     * Retrieve a list of instance-ids that match a set of predefined filter tags, since describe-tags iterates over
     * the collection of all resources we'll have in this list a list of identifiers for ec2 nodes only by filtering
     * that the resource-id starts with i- which indicates an EC2 instance
     *
     * @param filters
     *      the tag filters to match against
     * @param client
     *      the client to use in order to invoke the describe-tags api
     *
     * @return zero or more instance-id values that match the provided tags.  If there are no filters than all instance-ids
     * are returned, this method shouldn't be executed.
     */
    private Set<Instance> filterInstancesByTag(final Set<Instance> instances, final List<NameValueFilter> filters, final AmazonEC2 client) {
        DescribeTagsResult tagsResult = client.describeTags();

        Set<Instance> result = new HashSet<>();

        // map of identifiers to name/value pairs - the name and values are entered in the map as uppercase to ignore
        // case sensitivity
        HashMap<String, HashMap<String, String>> instanceNameValuePairs = new HashMap<>();

        // Walk the tags looking for instance tag name/value pairs to keep in a hashmap of a hashmap
        for (TagDescription tagDescription : tagsResult.getTags()) {
            String instanceId = tagDescription.getResourceId();
            if (instanceId.startsWith("i-")) {

                if (!instanceNameValuePairs.containsKey(instanceId)) {
                    instanceNameValuePairs.put(instanceId, new HashMap<String, String>());
                }
                if (tagDescription.getKey() != null && tagDescription.getValue() != null) {
                    instanceNameValuePairs.get(instanceId).put(tagDescription.getKey().toUpperCase(), tagDescription.getValue().toUpperCase());
                }
            }
        }

        // Walk the list of instances we've been provided to determine if the appropriate tags exist on that instance
        // in order to accept the instance as part of the cluster
        for (Instance instance : instances) {
            Boolean found = true;
            String instanceId = instance.getInstanceId();
            if (instanceNameValuePairs.containsKey(instanceId)) {
                for (NameValueFilter filter : filters) {
                    if (!(instanceNameValuePairs.get(instanceId).containsKey(filter.name) &&
                            instanceNameValuePairs.get(instanceId).get(filter.name).equals(filter.value))) {
                        found = false;
                        break;
                    }
                }
            }
            if (found) {
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.info("EC2 InstanceId: " + instanceId + " has the appropriate tags applied and is up or starting");
                result.add(instance);
            } else {
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.fine("EC2 InstanceId: " + instanceId + " does NOT have the appropriate tags applied");
            }
        }

        return result;
    }

    /**
     * Determine if the instance is coming up, up, coming down or down as two states
     *
     * @param instance
     *      the instance to check for up { pending, running }
     * @return true if the instance is pending or running
     */
    private static boolean isInstanceUp(final Instance instance) {

        if (null == instance || null == instance.getState()) {
            return false;
        }


        switch (instance.getState().getCode()) {
            case 0: // pending
            case 16: // running
                return true;
            case 32: // shutting down
            case 48: // terminated
            case 64: // stopping
            case 80: // stopped
            default:
                return false;
        }
    }



    /**
     * Logs the instances we found.
     *
     * @param instances the instances we are to log
     */
    protected void logAllInstances(Set<Instance> instances)
    {
        if (logger.isLoggable(Level.CONFIG))
        {
            //GLOBALYZER_IGNORE_NEXT_LINE
            logger.log(Level.CONFIG, "The following instances were found:");
            for (Iterator<Instance> instIter = instances.iterator(); instIter.hasNext();) {
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.log(Level.CONFIG, "EC2 instance:", instIter.next());
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void accept()
    {
        // Not called
    }


    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getNextAddress()
    {
        // Always increase index before use - initialized to -1
        if (wkaIterator.hasNext())
        {
            InetSocketAddress address = wkaIterator.next();

            if (logger.isLoggable(Level.FINEST)) {
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.log(Level.FINEST, "Returning WKA address {0}", address);
            }
            return address;
        }
        else
        {
            // We must now return null according to the AddressProvider contract to terminate iteration.
            // However, we must also reset the iterator so that the next call starts from the
            // beginning of the WKA list
            wkaIterator = wkaAddressList.iterator();
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void reject(Throwable exception)
    {
        // Not called
    }
    /**
     * This method determines what credentials to use for EC2 authentication.
     *
     * @return the {@link com.amazonaws.auth.AWSCredentials}
     *
     * @throws IOException if reading the property file fails
     */
    protected AWSCredentials determineCredentials() throws IOException
    {
        String accessKey = System.getProperty("tangosol.coherence.ec2addressprovider.accesskey");
        String secretKey = System.getProperty("tangosol.coherence.ec2addressprovider.secretkey");
        if ((accessKey == null) || (secretKey == null) || accessKey.equals("") || secretKey.equals(""))
        {
            if (logger.isLoggable(Level.CONFIG)) {
                //GLOBALYZER_IGNORE_NEXT_LINE
                logger.log(Level.CONFIG, "No EC2AddressProvider credential system properties provided.");
            }

            // Retrieve the credentials from a properties resource instead.

            String propertyResource = System.getProperty("tangosol.coherence.ec2addressprovider.propertyfile",
                    "AwsCredentials.properties");
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyResource);
            if (stream != null) {
                return new PropertiesCredentials(stream);
            }
            else {
                /*GLOBALYZER_START_IGNORE*/
                throw new RuntimeException(
                        "The EC2AddressProvider could not find any credentials, neither as system properties, nor as "
                                + propertyResource + " resource");
                /*GLOBALYZER_END_IGNORE*/
            }
        }
        else
        {
            return new BasicAWSCredentials(accessKey, secretKey);
        }
    }
}
