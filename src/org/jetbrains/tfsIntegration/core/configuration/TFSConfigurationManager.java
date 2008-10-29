/*
 * Copyright 2000-2008 JetBrains s.r.o.
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

package org.jetbrains.tfsIntegration.core.configuration;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@State(
  name = "org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager",
  storages = {@Storage(
    id = "other",
    file = "$APP_CONFIG$/tfs.xml")})
public class TFSConfigurationManager implements PersistentStateComponent<TFSConfigurationManager.State> {

  public static class State {
    @MapAnnotation(entryTagName = "server", keyAttributeName = "uri", surroundValueWithTag = false)
    public Map<String, ServerConfiguration> config = new HashMap<String, ServerConfiguration>();

    public State() {
      this(new HashMap<String, ServerConfiguration>());
    }

    public State(final Map<String, ServerConfiguration> config) {
      this.config = config;
    }
  }

  private Map<String, ServerConfiguration> myServersConfig = new HashMap<String, ServerConfiguration>();

  @NotNull
  public static synchronized TFSConfigurationManager getInstance() {
    return ServiceManager.getService(TFSConfigurationManager.class);
  }

  /**
   * @return null if not found
   */
  @Nullable
  public synchronized Credentials getCredentials(@NotNull URI serverUri) {
    final ServerConfiguration serverConfiguration = myServersConfig.get(toString(serverUri));
    return serverConfiguration != null ? serverConfiguration.getCredentials() : null;
  }

  @Nullable
  public URI getProxyUri(@NotNull URI serverUri) {
    final ServerConfiguration serverConfiguration = myServersConfig.get(toString(serverUri));
    try {
      return serverConfiguration != null ? new URI(serverConfiguration.getProxyUri()) : null;
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public void setProxyUri(@NotNull URI serverUri, @NotNull URI proxyUri) {
    getOrCreateServerConfiguration(serverUri).setProxyUri(toString(proxyUri));
  }

  public synchronized void storeCredentials(@NotNull URI serverUri, final @NotNull Credentials credentials) {
    getOrCreateServerConfiguration(serverUri).setCredentials(credentials);
  }

  public synchronized void resetStoredPasswords() {
    for (ServerConfiguration serverConfiguration : myServersConfig.values()) {
      final Credentials credentials = serverConfiguration.getCredentials();
      if (credentials != null) {
        credentials.resetPassword();
      }
    }
  }

  public void loadState(final State state) {
    myServersConfig = state.config;
  }

  public State getState() {
    return new State(myServersConfig);
  }

  @NotNull
  private ServerConfiguration getOrCreateServerConfiguration(URI serverUri) {
    final String uri = toString(serverUri);
    ServerConfiguration config = myServersConfig.get(uri);
    if (config == null) {
      config = new ServerConfiguration();
      myServersConfig.put(uri, config);
    }
    return config;
  }

  private static String toString(URI uri) {
    try {
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null).toString();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
