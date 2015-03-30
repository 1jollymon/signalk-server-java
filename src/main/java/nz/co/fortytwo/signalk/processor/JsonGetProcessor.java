/*
 *
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package nz.co.fortytwo.signalk.processor;

import static nz.co.fortytwo.signalk.util.JsonConstants.CONTEXT;
import static nz.co.fortytwo.signalk.util.JsonConstants.GET;
import static nz.co.fortytwo.signalk.util.JsonConstants.SIGNALK_FORMAT;
import static nz.co.fortytwo.signalk.util.JsonConstants.VESSELS;

import java.util.Map;

import mjson.Json;
import nz.co.fortytwo.signalk.handler.JsonGetHandler;
import nz.co.fortytwo.signalk.model.SignalKModel;
import nz.co.fortytwo.signalk.util.Constants;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.websocket.WebsocketConstants;
import org.apache.log4j.Logger;

/**
 * Handles  json messages with 'get' requests
 * 
 * @author robert
 * 
 */
public class JsonGetProcessor extends SignalkProcessor implements Processor{

	private static Logger logger = Logger.getLogger(JsonGetProcessor.class);
	//private static DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
	private JsonGetHandler handler = new JsonGetHandler();
	
	public void process(Exchange exchange) throws Exception {
		
		try {
			if(exchange.getIn().getBody()==null ||!(exchange.getIn().getBody() instanceof Json)) return;
			String wsSession = exchange.getIn().getHeader(WebsocketConstants.CONNECTION_KEY, String.class);
			if(wsSession==null){
				return;
			}
			Json json = exchange.getIn().getBody(Json.class);
			//avoid full signalk syntax
			if(json.has(VESSELS))return;
			if(json.has(CONTEXT) && (json.has(GET))){
				SignalKModel temp = handler.handle(signalkModel, json);
				//also STOMP headers etc, swap replyTo
				Map<String, Object> headers = exchange.getIn().getHeaders();
				headers.put(SIGNALK_FORMAT, handler.getFormat(json));
				
				//also STOMP headers etc, swap replyTo
				//STOMP
				if(headers!=null && headers.containsKey(Constants.REPLY_TO)){
					headers.put(Constants.DESTINATION, headers.get(Constants.REPLY_TO));
					headers.remove(Constants.REPLY_TO);
				}
				//for MQTT
				if(json.has(Constants.REPLY_TO)){
					headers.put(Constants.DESTINATION, json.at(Constants.REPLY_TO).asString());
				}
				json.delAt(GET);
				logger.debug("headers:"+headers);
				outProducer.sendBodyAndHeaders(temp, headers);
				exchange.getIn().setBody(json);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}


	

	
}
