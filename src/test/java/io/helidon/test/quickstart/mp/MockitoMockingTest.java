/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.test.quickstart.mp;

import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.model.SecretBundle;
import com.oracle.bmc.secrets.requests.GetSecretBundleByNameRequest;
import com.oracle.bmc.secrets.responses.GetSecretBundleByNameResponse;

import com.oracle.bmc.vault.VaultsClient;
import com.oracle.bmc.vault.model.Secret;
import com.oracle.bmc.vault.responses.CreateSecretResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MockitoMockingTest {
    private final static VaultsClient vaultsClient = mock(VaultsClient.class);
    private final static SecretsClient secretsClient = mock(SecretsClient.class);

    @BeforeAll
    static void beforeAll() {
        /*
        Stub secrets.getSecretBundleByName() and return mapped value of the provided secretKey.
        Throw an exception if secretKey is not found
         */
        doAnswer(invocationOnMock -> {
            GetSecretBundleByNameRequest getSecretBundleByNameRequest = invocationOnMock.getArgument(0);
            String secretKey = getSecretBundleByNameRequest.getSecretName();
            String base64Data =  FakeSecretsData.secretsData.get(secretKey);
            if (base64Data == null) {
                throw new RuntimeException("Unknown secret key");
            }
            return GetSecretBundleByNameResponse.builder()
                    .__httpStatusCode__(200)
                    .secretBundle(
                            SecretBundle.builder().secretBundleContent(
                                    Base64SecretBundleContentDetails.builder().content(base64Data).build()).build())
                    .build();
        }).when(secretsClient).getSecretBundleByName(any());

        // Stub vaults.createSecret() and return dummy secret OCID
        when(vaultsClient.createSecret(any())).thenReturn(
                CreateSecretResponse.builder()
                        .__httpStatusCode__(200)
                        .secret(Secret.builder().id(FakeSecretsData.createSecretId).build())
                        .build());
    }

    @Test
    void testGetUsernameAndPassword() {
        SecretsResource secretsResource = getSecretsResource();
        String secretKey = "username";
        Assertions.assertEquals(FakeSecretsData.getDecodedValue(secretKey), secretsResource.getSecret(secretKey));
        secretKey = "password";
        Assertions.assertEquals(FakeSecretsData.getDecodedValue(secretKey), secretsResource.getSecret(secretKey));
    }

    @Test
    void testGetUnknownSecret() {
        SecretsResource secretsResource = getSecretsResource();
        boolean callFailed = false;
        try {
            secretsResource.getSecret("Unknown");
        } catch(Throwable t) {
            callFailed = true;
        }
        Assertions.assertTrue(callFailed, "Expecting a failure on the getSecret() call");
    }

    @Test
    void testCreateSecret() {
        SecretsResource secretsResource = getSecretsResource();
        Assertions.assertEquals(FakeSecretsData.createSecretId, secretsResource.createSecret("NewSecret", "Value"));
    }

    private SecretsResource getSecretsResource() {
        SecretsProvider secretsProvider = new SecretsProvider(
                secretsClient, vaultsClient, "vaultId", "vaultCompartmentId", "vaultKeyId");
        return new SecretsResource(secretsProvider);
    }
}
