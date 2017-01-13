/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.html2Json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableResponseStreamBuilder;
import io.gravitee.gateway.api.http.stream.TransformableStreamBuilder;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.policy.api.annotations.OnResponseContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTMLToJSONTransformationPolicy {

    private final static String APPLICATION_JSON = MediaType.APPLICATION_JSON + "; charset=UTF-8";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HTMLToJSONTransformationPolicyConfiguration htmlToJSONTransformationPolicyConfiguration;

    public HTMLToJSONTransformationPolicy(final HTMLToJSONTransformationPolicyConfiguration htmlToJSONTransformationPolicyConfiguration) {
        this.htmlToJSONTransformationPolicyConfiguration = htmlToJSONTransformationPolicyConfiguration;
    }

    @OnResponseContent
    public ReadWriteStream onResponseContent(Response response) {
        return TransformableResponseStreamBuilder
                .on(response)
                .contentType(APPLICATION_JSON)
                .transform(input -> {
                    final Map<String, Object> jsonContent = new HashMap<>();

                    final Document document = Jsoup.parse(input.toString());

                    for (final HTMLToJSONTransformationPolicyConfiguration.Selector selector : htmlToJSONTransformationPolicyConfiguration.getSelectors()) {
                        if (selector.isArray()) {
                            final Elements selectedElement = document.select(selector.getSelector());
                            final List<String> elements = selectedElement.stream().map(Element::text).collect(Collectors.toList());
                            jsonContent.put(selector.getJsonName(), elements);
                        } else {
                            jsonContent.put(selector.getJsonName(), document.select(selector.getSelector()).text());
                        }
                    }

                    try {
                        return Buffer.buffer(objectMapper.writeValueAsString(jsonContent));
                    } catch (JsonProcessingException ex) {
                        throw new IllegalStateException("Unable to transform into JSON: " + ex.getMessage(), ex);
                    }
                })
                .build();
    }
}
