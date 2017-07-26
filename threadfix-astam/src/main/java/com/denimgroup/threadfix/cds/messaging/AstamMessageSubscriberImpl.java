// Copyright 2017 Secure Decisions, a division of Applied Visions, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// This material is based on research sponsored by the Department of Homeland
// Security (DHS) Science and Technology Directorate, Cyber Security Division
// (DHS S&T/CSD) via contract number HHSP233201600058C.

package com.denimgroup.threadfix.cds.messaging;

import com.denimgroup.threadfix.logging.SanitizedLogger;
import com.google.protobuf.InvalidProtocolBufferException;
import com.secdec.astam.common.messaging.Messaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.jms.*;


/**
 * Created by amohammed on 7/17/2017.
 */

@Component
public class AstamMessageSubscriberImpl implements AstamMessageSubscriber, Runnable{

    private static final SanitizedLogger LOGGER = new SanitizedLogger(AstamMessageSubscriberImpl.class);

    private Connection connection;
    private Session session;
    private String topicString;

    @Autowired private AstamMessageTrigger messageTrigger;


    public AstamMessageSubscriberImpl(){
    }

    @Override
    public void setup(@Nonnull Connection connection, @Nonnull String topicString){
        this.connection = connection;
        this.topicString = topicString;
    }

    public void receiveMessage() throws JMSException, com.google.protobuf.InvalidProtocolBufferException {
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicString);
        String subName = Thread.currentThread().getName();
        TopicSubscriber topicSubscriber = session.createDurableSubscriber(topic, subName);
        Message message = topicSubscriber.receive();
        if(message instanceof BytesMessage){
            BytesMessage bytesMessage = session.createBytesMessage();
            bytesMessage = (BytesMessage) message;
            byte[] bytes = new byte[(int) ((BytesMessage) message).getBodyLength()];
            bytesMessage.readBytes(bytes);
            Messaging.AstamMessage astamMessage = Messaging.AstamMessage.parseFrom(bytes);
            LOGGER.info("ASTAM message received: " + astamMessage.toString());
            messageTrigger.parse(astamMessage);
        }
    }

    @Override
    public void run() {
        try {
            receiveMessage();
        } catch (InvalidClientIDException ice){
            LOGGER.error("Error caused by invalid Client ID: ", ice);
        }catch (JMSException jmse) {
            LOGGER.error("JMS Exception error: ", jmse);
        } catch (InvalidProtocolBufferException ipbe){
            LOGGER.error("Error parsing astam protobuf message", ipbe);
        }
    }
}
