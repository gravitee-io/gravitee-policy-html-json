/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
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
 */
package io.gravitee.policy.html2Json;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@GatewayTest(v2ExecutionMode = ExecutionMode.V3)
public class HTMLToJSONTransformationPolicyV3IntegrationTest
    extends AbstractPolicyTest<HTMLToJSONTransformationPolicy, HTMLToJSONTransformationPolicyConfiguration> {

    @Test
    @DeployApi({ "/apis/v2/v2-proxy-response-transformation.json" })
    public void should_transform_html_to_json(HttpClient client) throws Exception {
        String backendResponse = loadResource("/html/lorem-ipsum.html");

        wiremock.stubFor(get("/team").willReturn(ok(backendResponse).withHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_HTML)));

        client
            .rxRequest(GET, "/post-valid-html-to-json")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue(result -> {
                assertThat(result.toString()).isEqualTo("{\"title\":\"Hello World\",\"content\":\"Lorem Ipsum\"}");
                return true;
            });

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DeployApi({ "/apis/v2/v2-proxy-missing-selector.json" })
    public void should_not_find_the_selector(HttpClient client) throws Exception {
        String backendResponse = loadResource("/html/lorem-ipsum.html");

        wiremock.stubFor(get("/team").willReturn(ok(backendResponse).withHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_HTML)));

        client
            .rxRequest(GET, "/post-valid-html-to-json")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue(result -> {
                assertThat(result.toString()).isEqualTo("{\"foo\":\"\"}");
                return true;
            });

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DeployApi({ "/apis/v2/v2-proxy-parent-selector.json" })
    public void should_combine_all_html_children_in_json_field(HttpClient client) throws Exception {
        String backendResponse = loadResource("/html/lorem-ipsum.html");

        wiremock.stubFor(get("/team").willReturn(ok(backendResponse).withHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_HTML)));

        client
            .rxRequest(GET, "/post-valid-html-to-json")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue(result -> {
                assertThat(result.toString()).isEqualTo("{\"content\":\"Hello World Lorem Ipsum\"}");
                return true;
            });

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DeployApi({ "/apis/v2/v2-proxy-array.json" })
    public void should_get_selector_as_array(HttpClient client) throws Exception {
        String backendResponse = loadResource("/html/array.html");

        wiremock.stubFor(get("/team").willReturn(ok(backendResponse).withHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_HTML)));

        client
            .rxRequest(GET, "/post-valid-html-to-json")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue(result -> {
                assertThat(result.toString())
                    .isEqualTo("{\"array\":[\"Neil Armstrong\",\"Alan Bean\",\"Peter Conrad\",\"Edgar Mitchell\",\"Alan Shepard\"]}");
                return true;
            });

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    private String loadResource(String resource) {
        try (InputStream is = this.getClass().getResourceAsStream(resource)) {
            return new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
