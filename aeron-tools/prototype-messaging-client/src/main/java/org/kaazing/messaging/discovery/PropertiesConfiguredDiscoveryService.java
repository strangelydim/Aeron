/*
    Copyright 2015 Kaazing Corporation

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package org.kaazing.messaging.discovery;

import org.kaazing.messaging.destination.MessageFlow;
import org.kaazing.messaging.destination.Topic;
import org.kaazing.messaging.transport.TransportHandle;
import uk.co.real_logic.aeron.common.uri.AeronUri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.function.Consumer;

//TODO(JAF): Support other types besides topics
public class PropertiesConfiguredDiscoveryService extends BasicDiscoveryService
{
    private final Properties properties;
    private final String classpathFileName;
    public PropertiesConfiguredDiscoveryService(String classpathFileName) throws IOException
    {
        this.classpathFileName = classpathFileName;
        this.properties = loadPropertiesFromFile(classpathFileName);
    }

    public PropertiesConfiguredDiscoveryService(Properties properties)
    {
        this.classpathFileName = null;
        this.properties = properties;
    }
    protected Properties loadPropertiesFromFile(String fileName) throws IOException
    {
        Properties props = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
        if(input == null)
        {
            throw new FileNotFoundException("Properties file not found: " + fileName);
        }
        else
        {
            props.load(input);
        }
        if(input != null)
        {
            input.close();
        }
        return props;
    }

    protected void loadTransportHandlesFromProperties()
    {
        Enumeration e = properties.propertyNames();

        while (e.hasMoreElements())
        {
            String logicalName = (String) e.nextElement();
            String transportHandlesValue = properties.getProperty(logicalName);
            String[] transportHandles = transportHandlesValue.split(";");
            for(int i = 0; i < transportHandles.length; i++)
            {
                TransportHandle transportHandle = null;
                String transportHandleStr = transportHandles[i];
                if(transportHandleStr.startsWith("aeron"))
                {
                    AeronUri aeronUri = AeronUri.parse(transportHandleStr);

                     transportHandle = new TransportHandle(transportHandleStr, TransportHandle.Type.Aeron);
                    if(aeronUri.get("streamId") != null)
                    {
                        transportHandle.getExtras().put("streamId", aeronUri.get("streamId"));
                    }
                    register(new Topic(logicalName), transportHandle);
                }
                else if(transportHandleStr.startsWith("amqp"))
                {
                    transportHandle = new TransportHandle(transportHandleStr, TransportHandle.Type.AMQP);
                    register(new Topic(logicalName), transportHandle);
                }
                else
                {
                    //TODO(JAF): Add logging and ability to try other formats
                    System.out.println("Unknown transport type" + transportHandle);
                }
            }
        }
    }

    @Override
    public void addInterestListChangedListener(MessageFlow messageFlow, Consumer<InterestListChangedEvent> listener)
    {

    }

    @Override
    public PropertiesConfiguredDiscoveryService start()
    {
        loadTransportHandlesFromProperties();
        return this;
    }

    @Override
    public PropertiesConfiguredDiscoveryService stop()
    {
        return this;
    }

}
