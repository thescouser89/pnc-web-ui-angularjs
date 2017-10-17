/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.messaging;

import org.jboss.pnc.messaging.spi.Message;
import org.jboss.pnc.messaging.spi.MessagingRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Stateless
public class MessageSender {

    private Logger logger = LoggerFactory.getLogger(MessageSender.class);

//    @Inject
//    private JMSContext context;

//    @Resource
//    private SessionContext context;

//    @Resource(mappedName = "/jms/ConnectionFactory")
//    private ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "java:/jms/queue/pncTopic")
    private Destination destination;

    private Connection connection;
    private Session session;
    private MessageProducer messageProducer;

    @PostConstruct
    public void init() {
        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            messageProducer = session.createProducer(destination);
            logger.info("JMS client ID {}.", connection.getClientID());
            logger.info("JMSXPropertyNames {}.", connection.getMetaData().getJMSXPropertyNames());
        } catch (JMSException e) {
            logger.error("Failed to initialize JMS.");
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                logger.error("Failed to close JMS connection.");
            }
        }
    }

    /**
     * @throws MessagingRuntimeException
     */
    public void sendToTopic(Message message) {
        sendToTopic(message.toJson());
    }

    /**
     * @throws MessagingRuntimeException
     */
    public void sendToTopic(String message) {
        sendToTopic(message, Collections.EMPTY_MAP);
    }

    /**
     * @throws MessagingRuntimeException
     */
    public void sendToTopic(String message, Map<String, String> headers) {
        TextMessage textMessage;
        try {
            textMessage = session.createTextMessage(message);
            textMessage.setStringProperty("producer", "PNC");
        } catch (JMSException e) {
           throw new MessagingRuntimeException(e);
        }
        if (textMessage == null) {
            logger.error("Unable to create textMessage.");
            throw new MessagingRuntimeException("Unable to create textMessage.");
        }

        headers.forEach((k, v) -> {
            try {
                textMessage.setStringProperty(k, v);
            } catch (JMSException e) {
                throw new MessagingRuntimeException(e);
            }
        });
        try {
            messageProducer.send(textMessage);
        } catch (JMSException e) {
            throw new MessagingRuntimeException(e);
        }
    }

}
